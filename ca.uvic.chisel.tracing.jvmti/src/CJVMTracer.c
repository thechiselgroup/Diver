/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	David Oulette - initial C implementation
 *     Del Myers - C++ implementation
 *******************************************************************************/
// CJVMTracer.cpp : Defines the exported functions for the DLL application.
//
#include "stdafx.h"
#include "stdlib.h"
#include "CJVMTracer.h"
#include "java_crw_demo.h"
#include "communication/oasis_server.h"
#include "utils/jvmti_utils.h"

/* ------------------------------------------------------------------------- */
/* Some constants maximum sizes */

#define MAX_TOKEN_LENGTH 16
#define MAX_THREAD_NAME_LENGTH 512
#define MAX_METHOD_NAME_LENGTH 1024

/* Some constant names that tie to Java class/method names.
 *    We assume the Java class whose static methods we will be calling
 *    looks like:
 *
 * public class JVMTrace {
 *    private static int engaged;
 *    private static native void _method_entry(Object thr, int cnum, int mnum);
 *    public static void method_entry(int cnum, int mnum)
 *    {
 *       if(engaged != 0) {
 *          _method_entry(Thread.currentThread(), cnum, mnum);
 *       }
 *    }
 *    private static native void _method_exit(Object thr, int cnum, int mnum);
 *    public static void method_exit(int cnum, int mnum)
 *    {
 *       if(engaged != 0) {
 *          _method_exit(Thread.currentThread(), cnum, mnum);
 *       }
 *    }
 * }
 *
 * The engaged field allows us to inject all classes (even system classes)
 * and delay the actual calls to the native code until the VM has reached
 * a safe time to call native methods (Past the JVMTI VM_START event).
 *
 */

#define JVMTRACE_class			ca/gc/drdc/oasis/tracing/cjvmtracer/internal/JVMTrace		/* Name of class we are using */
#define JVMTRACE_entry			method_entry	/* Name of java entry method */
#define JVMTRACE_exit			method_exit		/* Name of java exit method */
#define JVMTRACE_native_entry	_method_entry	/* Name of java entry native */
#define JVMTRACE_native_exit	_method_exit	/* Name of java exit native */
#define JVMTRACE_engaged		engaged			/* Name of java static field */

/* C macrs to create strings from tokens */
#define _STRING(s) #s
#define STRING(s) _STRING(s)

#define BUF_SIZE 255
#define DEFAULT_PORT "27015"

FILE * volatile file = NULL;
/* thread control */
static int tracing = 0;

void file_message(const char * format, ...)
{
	if(tracing != 0) {
	    va_list ap;

		va_start(ap, format);
		(void)vfprintf(file, format, ap);
		va_end(ap);
	}

}
/* ------------------------------------------------------------------------- */

/* Data structure to hold method and class information in agent */

typedef struct MethodInfo {
	const char *name;		/* Method name */
	const char *signature;	/* Method signature */
} MethodInfo;

typedef struct ClassInfo {
	const char *name;		/* Class name */
	int			mcount;		/* Method count */
	MethodInfo *methods;	/* Method information */
} ClassInfo;

/* Global agent data strucutres */
typedef struct {
	/* JVMTI Environment */
	jvmtiEnv		*jvmti;
	jboolean		vm_is_dead;
	jboolean		vm_is_started;
	jboolean		vm_is_inited;
	/* Data access lock */
	jrawMonitorID	lock;
	/* Options */
	char			*include;
	char			*exclude;
	int				max_count;
	char 			*port;
	int				start;
	/* ClassInfo Table */
	ClassInfo		*classes;
	jint			ccount;
} GlobalAgentData;
static GlobalAgentData	*gdata;
static JavaVM			*jvm;


/* Enter a critical section by doing a JVMTI Raw Monitor Enter */
static void enter_critical_section(jvmtiEnv *jvmti) {
	jvmtiError error;
	error = (*jvmti)->RawMonitorEnter(jvmti, gdata->lock);
	check_jvmti_error(jvmti, error, "Cannot enter with raw monitor");
}

/* Exit a critical section by doing a JVMTI Raw Monitor Exit */
static void exit_critical_section(jvmtiEnv *jvmti) {
	jvmtiError error;
	error = (*jvmti)->RawMonitorExit(jvmti, gdata->lock);
	check_jvmti_error(jvmti, error, "Cannot exit with raw monitor");
}


/* Get a name for jthread */
static void get_thread_name(jvmtiEnv *jvmti, jthread thread, char *tname, int maxlen) {
	jvmtiThreadInfo info;
	jvmtiError      error;

	/* Make sure the stack variables are garbage free */
	(void)memset(&info, 0, sizeof(info));

	/* Assume the name is unknown for now */
	(void)strcpy(tname, "Unknown");

	/* Get the thread information, which includes the name. */
	error = (*jvmti)->GetThreadInfo(jvmti, thread, &info);
	check_jvmti_error(jvmti, error, "Cannot get thread info");

	/* The thread might not have a name, be careful here. */
	if(info.name != NULL) {
		int len;

		/* Copy the thread name into tname if it will fit */
		len = (int)strlen(info.name);
		if (len < maxlen) {
			(void)strcpy(tname, info.name);
		}

		/* Every string allocated by JVMTI needs to be freed */
		deallocate(jvmti, (void*)info.name);
	}
}

/* Callback from java_crw_demo() that gives us mnum mappings */
static void mnum_callbacks(unsigned cnum, const char **names, const char **sigs, int mcount) {
	ClassInfo *cp;
	int		  mnum;

	if(cnum >= (unsigned)gdata->ccount) {
		fatal_error("ERROR: Class number out of range\n");
	}
	if(mcount == 0) {
		return;
	}

	cp		   = gdata->classes + (int)cnum;
	cp->mcount = mcount;
	cp->methods = (MethodInfo*)calloc(mcount, sizeof(MethodInfo));
	if(cp->methods == NULL) {
		fatal_error("ERROR: Out of malloc memory\n");
	}

	for(mnum = 0; mnum < mcount; mnum++) {
		MethodInfo *mp;
		mp = cp->methods + mnum;
		mp->name = (const char *)strdup(names[mnum]);
		if(mp->name == NULL) {
			fatal_error("ERROR: Out of malloc memory\n");
		}
		mp->signature = (const char*)strdup(sigs[mnum]);
		if(mp->signature == NULL) {
			fatal_error("ERROR: Out of malloc memory\n");
		}
	}
}

/**handles the initial protocol setup */
int handshake_handler() {
	/* JNI Stuff */
//	jint		res;
//	JNIEnv		*env;
//	jclass		klass;
//	jfieldID	field;
//	HINSTANCE	hLibJVM;


//	enter_critical_section(gdata->jvmti); {
//		res = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
//		if(res != JNI_OK) {
//			stdout_message("******* GetEnv failed *******\n");
//			return 1;
//		}
//		klass = (*env)->FindClass(env, STRING(JVMTRACE_class));
//		if(klass == NULL) {
//			stdout_message("******* FindClass failed *******\n");
//			return 1;
//		}
//		field = (*env)->GetStaticFieldID(env, klass, STRING(JVMTRACE_engaged), "I");
//		if(field == NULL) {
//			stdout_message("******* GetStaticFieldID failed *******\n");
//			return 1;
//		}
//	} exit_critical_section(gdata->jvmti);
	/* Wait for a connect command */
	OasisCommand* cmd;
	stdout_message("Waiting for connect command\n");
	cmd = oasis_receive_command();
	if (cmd == NULL) {
		return -1;
	}
	if (cmd->command != CONNECT_COMMAND) {
		oasis_close_command(cmd);
		return 1; /* expected a 'c' */
	}
	stdout_message("Got connect command\n");
	oasis_close_command(cmd);
	cmd = oasis_create_command(ACK_COMMAND, (unsigned short)1);
	if (cmd == NULL) {
		return -1;
	}
	*(cmd->data) = CONNECT_COMMAND;
	oasis_send_command(cmd);
	oasis_close_command(cmd);

	cmd = oasis_receive_command();
	if (cmd == NULL) {
		return -1;
	} else if (cmd->command == START_COMMAND || cmd->command == FILE_COMMAND) {
		if (cmd->data_len <= 0) {
			return 1;
		}
		enter_critical_section(gdata->jvmti); {
			char filename[cmd->data_len+1];
			filename[cmd->data_len] = '\0'; //null-terminate
			memcpy(filename, cmd->data, cmd->data_len);
			file = fopen(filename, "w+");
			if(file == NULL) {
				fatal_error("ERROR: Cannot open trace file");
			}
//			(*env)->SetStaticIntField(env, klass, field, 1);
			if (cmd->command == START_COMMAND) {
				tracing = 1;
			}
		} exit_critical_section(gdata->jvmti);
	} else {
		/** expected start command */
		return 1;
	}
	char acknowledgement = cmd->command;
	oasis_close_command(cmd);
	cmd = oasis_create_command(ACK_COMMAND, 1);
	if (cmd == NULL) {
		return -1;
	}
	*(cmd->data) = acknowledgement;
	oasis_send_command(cmd);
	oasis_close_command(cmd);
	return 0;
}

int cleanup_client() {
	return 0;
}

int initialize_client() {
	return 0;
}

/**
 * Handles the commands that come from the oasis server.
 */
int command_handler(OasisCommand *cmd) {
	/* JNI Stuff */
	jint		res;
//	JNIEnv		*env;
//	jclass		klass;
//	jfieldID	field;
//	HINSTANCE	hLibJVM;
//	res = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
//	if(res != JNI_OK) {
//		stdout_message("******* GetEnv failed *******\n");
//		return 1;
//	}
//	OasisCommand localCommand;
	enter_critical_section(gdata->jvmti); {
//		stdout_message("Message Recieved\n");
//
//		klass = (*env)->FindClass(env, STRING(JVMTRACE_class));
//		if(klass == NULL) {
//			stdout_message("******* FindClass failed *******\n");
//		}
//		field = (*env)->GetStaticFieldID(env, klass, STRING(JVMTRACE_engaged), "I");
//		if(field == NULL) {
//			stdout_message("******* GetStaticFieldID failed *******\n");
//			return 1;
//		}
//		char *sendbuf;
//		int iResult;
		switch (cmd->command) {
		case 'b':
			//start tracing
			tracing = 1;
//			(*env)->SetStaticIntField(env, klass, field, 1);
		case 'f':
			if (cmd->data_len > 0) {

				if (file != NULL) {
					//close the old file
					fclose(file);
				}
				char filename[cmd->data_len];
				strcpy(filename, cmd->data);
				file = fopen(filename, "w+");
				if(file == NULL) {
					fatal_error("ERROR: Cannot open trace file");
				}
			}
		case 'p':
			/* pause tracing command */
//			(*env)->SetStaticIntField(env, klass, field, 0);
			/* Change the tracing flag */
			tracing = 0;
			break;
		case 'r':
			/* resume tracing command */
//			(*env)->SetStaticIntField(env, klass, field, 1);
			/* Change the tracing flag */
			tracing = 1;
			break;

		}
	} exit_critical_section(gdata->jvmti);
	return 0;
}

/* Java Native Method for entry */
static void JVMTRACE_native_entry(JNIEnv *env, jclass klass, jobject thread, jint cnum, jint mnum) {
	enter_critical_section(gdata->jvmti); {
		/* It's possible we get here right after VmDeath event, be careful */
		if(!gdata->vm_is_dead) {
			ClassInfo	*cp;
			MethodInfo	*mp;
			jint		frameCount;
			jvmtiError	error;

			if(cnum >= gdata->ccount) {
				fatal_error("ERROR: Class number out of range\n");
			}
			cp = gdata->classes + cnum;
			if(mnum >= cp->mcount) {
				fatal_error("ERROR: Method number out of range\n");
			}
			mp = cp->methods + mnum;


			if(interested((char*)cp->name, (char*)mp->name, gdata->include, gdata->exclude)) {

				if(gdata->vm_is_inited) {
					char tname[MAX_THREAD_NAME_LENGTH];
					error = (*(gdata->jvmti))->GetFrameCount(gdata->jvmti, (jthread)thread, &frameCount);
					check_jvmti_error(gdata->jvmti, error, "Can not get the frame count.");
					get_thread_name(gdata->jvmti, thread, tname, sizeof(tname));
					file_message("ENTERRR!!!! %d %s>%s.%s %s\n", frameCount, tname, cp->name, mp->name, mp->signature);
				} else {
					file_message(">%s.%s %s\n", cp->name, mp->name, mp->signature);
				}
			}
		}
	} exit_critical_section(gdata->jvmti);
}

/* Java Native Method for exit */
static void JVMTRACE_native_exit(JNIEnv *env, jclass kalss, jobject thread, jint cnum, jint mnum) {
	enter_critical_section(gdata->jvmti); {
		/* It's possible we get here right after VmDeath event, be careful */
		if(!gdata->vm_is_dead) {
			ClassInfo  *cp;
			MethodInfo *mp;
			jint		frameCount;
			jvmtiError	error;

			if(cnum >= gdata->ccount) {
				fatal_error("ERROR: Class number out of range\n");
			}
			cp = gdata->classes + cnum;
			if(mnum >= cp->mcount) {
				fatal_error("ERROR: Method number out of range\n");
			}
			mp = cp->methods + mnum;


			if(interested((char*)cp->name, (char*)mp->name, gdata->include, gdata->exclude)) {

				if(gdata->vm_is_inited) {
					char tname[MAX_THREAD_NAME_LENGTH];
					error = (*(gdata->jvmti))->GetFrameCount(gdata->jvmti, (jthread)thread, &frameCount);
					check_jvmti_error(gdata->jvmti, error, "Can not get the frame count.");
					get_thread_name(gdata->jvmti, thread, tname, sizeof(tname));
					file_message("%d %s<%s.%s %s\n", frameCount, tname, cp->name, mp->name, mp->signature);
				} else {
					file_message("<%s.%s %s\n", cp->name, mp->name, mp->signature);
				}
			}
		}
	} exit_critical_section(gdata->jvmti);
}

/* Callback for JVMTI_EVENT_VM_START */
static void JNICALL cbVMStart(jvmtiEnv *jvmti, JNIEnv *env) {
	enter_critical_section(jvmti); {
		jclass		klass;
		jfieldID	field;
		int			rc;

		/* Java Native Methods for class
		static JNINativeMethod registry[2] = {
			{STRING(JVMTRACE_native_entry), "(Ljava/lang/Object;II)V", (void*)&JVMTRACE_native_entry},
			{STRING(JVMTRACE_native_exit), "(Ljava/lang/Object;II)V", (void*)&JVMTRACE_native_exit}
		};
		/
		/* The VM has started */
		file_message("VMStart\n");

		/* Register Natives for class whose methods we use
		klass = (*env)->FindClass(env, STRING(JVMTRACE_class));
		if(klass == NULL) {
			fatal_error("ERROR: JNI: Cannot find %s with FindClass\n", STRING(JVMTRACE_class));
		}
		rc = (*env)->RegisterNatives(env, klass, registry, 2);
		if(rc != 0) {
			fatal_error("ERROR: JNI: Cannor register native methods for %s\n", STRING(JVMTRACE_class));
		}
		if(gdata->start) {
			field = (*env)->GetStaticFieldID(env, klass, STRING(JVMTRACE_engaged), "I");
			if(field == NULL) {
				fatal_error("ERROR: JNI: Cannot get field from %s\n", STRING(JVMTRACE_class));
			}
			(*env)->SetStaticIntField(env, klass, field, 1);
			tracing = 1;
		}
		*/
		tracing = 1;
		/* Indicate VM has started */
		gdata->vm_is_started = JNI_TRUE;
	} exit_critical_section(jvmti);
}

/* Callback for JVMTI_EVENT_VM_INIT */
static void JNICALL cbVMInit(jvmtiEnv *jvmti, JNIEnv *env, jthread thread) {
	enter_critical_section(jvmti); {
		char	tname[MAX_THREAD_NAME_LENGTH];
		static	jvmtiEvent events[] = { JVMTI_EVENT_THREAD_START, JVMTI_EVENT_THREAD_END, JVMTI_EVENT_EXCEPTION, JVMTI_EVENT_EXCEPTION_CATCH };
		int		i;



		/* The VM has started. */
		get_thread_name(jvmti, thread, tname, sizeof(tname));
		/* file_message("VMInit %s\n", tname); */

		/* The VM is now initialized, at this time we make our requests for additional events. */
		for(i = 0; i < (int)sizeof(events)/sizeof(jvmtiEvent); i++) {
			jvmtiError error;

			/* Setup event notification modes */
			error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, events[i], (jthread)NULL);
			check_jvmti_error(jvmti, error, "Cannot set event notification");
		}
		gdata->vm_is_inited = JNI_TRUE;
	} exit_critical_section(jvmti);

	OasisCallbacks callbacks;
	callbacks.cleanup=&cleanup_client;
	callbacks.command_recieved=&command_handler;
	callbacks.handshake=&handshake_handler;
	callbacks.initialize=&initialize_client;

	if (!gdata->start) {
		stdout_message("Connecting Oasis Server on port %s\n", gdata->port);
		int error = oasis_start_server(gdata->port, 1, &callbacks);
		if (error) {
			fatal_error("ERROR: Could not open tracing server (%d)!", error);
		}
		stdout_message("Server Connected\n");
	}


}

/* Callback for JVMTI_EVENT_VM_DEATH */
static void JNICALL cbVMDeath(jvmtiEnv *jvmti, JNIEnv *env) {
	enter_critical_section(jvmti); {
		jclass		klass;
		jfieldID	field;

		/* The VM has died */
		file_message("VMDeath\n");

		/* Disengage calls in MTRACE_class
		klass = (*env)->FindClass(env, STRING(JVMTRACE_class));
		if(klass == NULL) {
			fatal_error("ERROR: JNI: Cannot find %s with FindClass\n", STRING(JVMTRACE_class));
		}
		field = (*env)->GetStaticFieldID(env, klass, STRING(JVMTRACE_engaged), "I");
		if(field == NULL) {
			fatal_error("ERROR: JNI: Cannot get field from %s\n", STRING(JVMTRACE_class));
		}
		(*env)->SetStaticIntField(env, klass, field, 0);

		/* The critical secton here is important to hold back the VM death until
		 * all other callbacks have completed.
		 */

		/* Since this critical section could be holding up other threads
		 * in other event callbacks, we need to indicate that the VM is
		 * dead so that the other callbacks can short circuit their work.
		 * We don't expect any further events after VMDeath but we do need
		 * to be careful that existing threads might be in out own agent
		 * callback code.
		 */
		gdata->vm_is_dead = JNI_TRUE;
	} exit_critical_section(jvmti);
}

/* Callback for JVMTI_EVENT_THREAD_START */
static void JNICALL cbThreadStart(jvmtiEnv *jvmti, JNIEnv *env, jthread thread) {
	enter_critical_section(jvmti); {
		/* It's possible we get here right after VMDeath event, be careful */
		if(!gdata->vm_is_dead) {
			char tname[MAX_THREAD_NAME_LENGTH];
			get_thread_name(jvmti, thread, tname, sizeof(tname));
			file_message("ThreadStart %s\n", tname);
		}
	} exit_critical_section(jvmti);
}

/* Callback for JVMTI_EVENT_THREAD_END */
static void JNICALL cbThreadEnd(jvmtiEnv *jvmti, JNIEnv *env, jthread thread) {
	enter_critical_section(jvmti); {
		/* It's possible we get here right after VMDeat event, be careful */
		if(!gdata->vm_is_dead) {
			char tname[MAX_THREAD_NAME_LENGTH];
			get_thread_name(jvmti, thread, tname, sizeof(tname));
			file_message("ThreadEnd %s\n", tname);
		}
	} exit_critical_section(jvmti);
}

/* Callback for JVMTI_EVENT_CLASS_FILE_LOAD_HOOK */
static void JNICALL cbClassFileLoadHookOLD(jvmtiEnv *jvmti, JNIEnv *env,
										jclass class_being_redefined, jobject loader,
										const char* name, jobject protection_domain,
										jint class_data_len, const unsigned char* class_data,
										jint* new_class_data_len, unsigned char** new_class_data) {
	enter_critical_section(jvmti); {
		/* It's possible we get here right after VMDeath event, be careful */
		if(!gdata->vm_is_dead) {
			const char* classname;

			/* Name could be NULL */
			if(name == NULL) {
				classname = java_crw_demo_classname(class_data, class_data_len, NULL);
				if(classname == NULL) {
					fatal_error("ERROR: No classname inside classfile\n");
				}
			} else {
				classname = strdup(name);
				if(classname == NULL) {
					fatal_error("ERROR: Out of malloc memory\n");
				}
			}

			*new_class_data_len = 0;
			*new_class_data     = NULL;

			/* The tracker class itself? */
			if(interested((char*)classname, "", gdata->include, gdata->exclude) &&
					strcmp(classname, STRING(JVMTRACE_class)) != 0) {
				jint			cnum;
				int				system_class;
				unsigned char	*new_image;
				long			new_length;
				ClassInfo		*cp;

				cnum = gdata->ccount++;

				/* Save away class information */
				if(gdata->classes == NULL) {
					gdata->classes = (ClassInfo*)malloc(gdata->ccount*sizeof(ClassInfo));
				} else {
					gdata->classes = (ClassInfo*)realloc((void*)gdata->classes, gdata->ccount*sizeof(ClassInfo));
				}
				if(gdata->classes == NULL) {
					fatal_error("ERROR: Out of malloc memory\n");
				}
				cp		 = gdata->classes + cnum;
				cp->name = (const char*) strdup(classname);
				if(cp->name == NULL) {
					fatal_error("ERROR: Out of malloc memory\n");
				}
				cp->mcount  = 0;
				cp->methods = NULL;

				/* Is it a system class? If the clas load is before VmStart
				 * then we will consider it a system class that should
				 * be treated carefully.  (See java_crw_demo)
				 */
				system_class = 0;
				if(!gdata->vm_is_started) {
					system_class = 1;
				}

				new_image = NULL;
				new_length = 0;

				/* Call the class file reader/writer demo code */
				java_crw_demo(cnum, classname, class_data, class_data_len, system_class,
					STRING(JVMTRACE_class), "L" STRING(JVMTRACE_class) ";",
					STRING(JVMTRACE_entry), "(II)V",
					STRING(JVMTRACE_exit), "(II)V",
					NULL, NULL, NULL, NULL,
					&new_image, &new_length, NULL, &mnum_callbacks);

				/* If we got back a new class image, return it back as "the"
				 * new class image.  This must be JVMTI Allocate space. */
				if(new_length > 0) {
					unsigned char *jvmti_space;
					jvmti_space = (unsigned char *)allocate(jvmti, (jint)new_length);
					(void)memcpy((void*)jvmti_space, (void*)new_image, (int)new_length);
					*new_class_data_len = (jint) new_length;
					*new_class_data = jvmti_space; /* VM will deallocate */
				}

				/* Always free up the space we get from java_crw_demo() */
				if(new_image != NULL) {
					(void)free((void*)new_image); /* Free malloc() space with free() */
				}
			}
			(void)free((void*)classname);
		}
	} exit_critical_section(jvmti);
}


static void JNICALL cbMethodEntry(jvmtiEnv *jvmti,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method) {

	enter_critical_section(jvmti); {
			/* It's possible we get here right after VmDeath event, be careful */
			jvmtiError error;
			if(!gdata->vm_is_dead) {
				char* class_signature;
				methodSignature *signature;
				jint frameCount;
				jint localVariableCount = 0;
				jvmtiLocalVariableEntry *variable_table;
				variable_table = NULL;
				jclass cp;
				error = (*jvmti)->GetMethodDeclaringClass(jvmti, method, &cp);
				check_jvmti_error(jvmti, error, "Can not get declaring class.");

				signature = new_method_signature(jvmti, method);

				error = (*jvmti)->GetClassSignature(jvmti, cp, &class_signature, NULL);
				check_jvmti_error(jvmti, error, "Can not get class signature");

				(*jni_env)->DeleteLocalRef(jni_env, cp);

				if (!is_native(signature)) {
					error = (*jvmti)->GetLocalVariableTable(jvmti, method, &localVariableCount, &variable_table);
//					if (1) {
//						localVariableCount = 0;
//						variable_table = NULL;
//					}
					if (error != JVMTI_ERROR_ABSENT_INFORMATION) {
						check_jvmti_error(jvmti, error, "Can not get local variable table");
					}
				}
				int line_number = get_current_line_of_execution(jvmti, thread);
				if(interested(class_signature, signature->method_name, gdata->include, gdata->exclude)) {
					if(gdata->vm_is_inited) {
						char tname[MAX_THREAD_NAME_LENGTH];
						get_thread_name(gdata->jvmti, thread, tname, sizeof(tname));
						error = (*jvmti)->GetFrameCount(jvmti, thread, &frameCount);
						check_jvmti_error(gdata->jvmti, error, "Can not get the frame count.");
						int method_static = is_static(signature);
						file_message("<ENTRY>\n");
						file_message("\tline=%d\n", get_current_line_of_execution(jvmti, thread));
						file_message("\tthread=%s\n", tname);
						file_message("\tdepth=%d\n", frameCount);
						file_message("\tclass=%s\n", class_signature);
						file_message("\tmethod=%s\n", signature->method_name);
						file_message("\tsignature=%s\n", signature->raw_signature);
						file_message("\tstatic=%d\n", method_static);

						if (variable_table != NULL) {
							/*log the value of each variable */
							jint ivalue = 0;
							jfloat fvalue = 0;
							jlong lvalue = 0;
							jdouble dvalue = 0;
							jint hash = 0;
							jobject ovalue = 0;
							int var = 0;
							int variable_count = signature->num_parameters;
							if (!method_static) {
								variable_count += 1;
							}
							if (variable_count > localVariableCount) {
								variable_count = localVariableCount;
							}
							/* get the this pointer */
							for (; var < variable_count; var++) {
								char type = (variable_table[var]).signature[0];
								switch(type) {
								case TYPE_CHAR:
									(*jvmti)->GetLocalInt(jvmti, thread, 0, variable_table[var].slot, &ivalue);
									file_message("\tvar%d=%c\n", var, ivalue);
									break;
								case TYPE_BOOLEAN:
								case TYPE_BYTE:
								case TYPE_SHORT:
								case TYPE_INT:
									(*jvmti)->GetLocalInt(jvmti, thread, 0, variable_table[var].slot, &ivalue);
									file_message("\tvar%d=%d\n", var, ivalue);
									break;
								case TYPE_FLOAT:
									(*jvmti)->GetLocalFloat(jvmti, thread, 0, variable_table[var].slot, &fvalue);
									file_message("\tvar%d=%d\n", var, fvalue);
									break;
								case TYPE_LONG:
									(*jvmti)->GetLocalLong(jvmti, thread, 0, variable_table[var].slot, &lvalue);
									file_message("\tvar%d=%d\n", var, lvalue);
									break;
								case TYPE_DOUBLE:
									(*jvmti)->GetLocalDouble(jvmti, thread, 0, variable_table[var].slot, &dvalue);
									file_message("\tvar%d=%f\n", var, dvalue);
									break;
								default:
									error = (*jvmti)->GetLocalObject(jvmti, thread, 0, variable_table[var].slot, &ovalue);
									if (error) {
										hash = 0;
									} else {
										error = (*jvmti)->GetObjectHashCode(jvmti, ovalue, &hash);
										(*jni_env)->DeleteLocalRef(jni_env, ovalue);
										if (error) {
											hash = 0;
										}
									}
									file_message("\tvar%d=%x\n", var, hash);
									break;
								}
							}
						}
						file_message("</ENTRY>\n");

						file_message("%d %s>%s.%s %s [%d]\n", frameCount, tname, class_signature, signature->method_name, signature->raw_signature, line_number);
					} else {
						file_message(">%s.%s %s [%d]\n", class_signature, signature->method_name, signature->raw_signature, line_number);
					}
				}
				close_method_signature(signature);
				int i = 0;
				for (i = 0; i < localVariableCount; i++) {
					deallocate(jvmti, variable_table[i].generic_signature);
					deallocate(jvmti, variable_table[i].name);
					deallocate(jvmti, variable_table[i].signature);
				}
				if (variable_table != NULL) {
					deallocate(jvmti, variable_table);
				}
				deallocate(jvmti, class_signature);
			}
		} exit_critical_section(gdata->jvmti);

}

static void JNICALL cbMethodExit(jvmtiEnv *jvmti,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method,
            jboolean was_popped_by_exception,
            jvalue return_value) {
	enter_critical_section(jvmti); {
		/* It's possible we get here right after VmDeath event, be careful */
		jvmtiError error;
		if(!gdata->vm_is_dead) {
			char* method_name;
			char* method_signature;
			char* class_signature;
			jint frameCount;
			jclass cp;
			error = (*jvmti)->GetMethodDeclaringClass(jvmti, method, &cp);
			check_jvmti_error(jvmti, error, "Can not get declaring class.");

			error = (*jvmti)->GetMethodName(jvmti, method, &method_name, &method_signature, NULL);
			check_jvmti_error(jvmti, error, "Can not get method name.");

			error = (*jvmti)->GetClassSignature(jvmti, cp, &class_signature, NULL);
			check_jvmti_error(jvmti, error, "Can not get class signature");

			if(interested(class_signature, method_name, gdata->include, gdata->exclude)) {
				if(gdata->vm_is_inited) {
					char tname[MAX_THREAD_NAME_LENGTH];
					error = (*jvmti)->GetFrameCount(jvmti, thread, &frameCount);
					check_jvmti_error(gdata->jvmti, error, "Can not get the frame count.");
					get_thread_name(gdata->jvmti, thread, tname, sizeof(tname));
					file_message("%d %s<%s.%s %s\n", frameCount, tname, class_signature, method_name, method_signature);
				} else {
					file_message("<%s.%s %s\n", class_signature, method_name, method_signature);
				}
			}
			deallocate(jvmti, method_name);
			deallocate(jvmti, method_signature);
			deallocate(jvmti, class_signature);
		}
	} exit_critical_section(gdata->jvmti);
}

static void JNICALL cbException(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID method,
								jlocation location, jobject exception, jmethodID catch_method, jlocation catch_location) {
	enter_critical_section(jvmti); {
		/* It's possible we get here right after VMDeath event */
		if(!gdata->vm_is_dead) {
			jclass thrown_from_class;
//			jclass caught_by_class;
			jvmtiError error;
			char* cname;
			char* mname;
			char* sig;
			jint		frameCount;

			error = (*jvmti)->GetMethodDeclaringClass(jvmti, method, &thrown_from_class);
			check_jvmti_error(jvmti, error, "Cannot get the class the exception is thrown from");
/*
			error = (*jvmti)->GetMethodDeclaringClass(jvmti, catch_method, &caught_by_class);
			check_jvmti_error(jvmti, error, "Cannot get the class the exception is caught by");
*/

			/* Print info on the class/method this exception is thrown from */
			error = (*jvmti)->GetClassSignature(jvmti, thrown_from_class, &cname, NULL);
			check_jvmti_error(jvmti, error, "Cannot get the signature of the class this exception is thrown from");
			error = (*jvmti)->GetMethodName(jvmti, method, &mname, &sig, NULL);
			check_jvmti_error(jvmti, error, "Cannot get the name and signature of the method this exception is thrown from");

			if(gdata->vm_is_inited) {
				char tname[MAX_THREAD_NAME_LENGTH];
				error = (*(gdata->jvmti))->GetFrameCount(gdata->jvmti, thread, &frameCount);
				check_jvmti_error(gdata->jvmti, error, "Can not get the frame count.");
				get_thread_name(gdata->jvmti, thread, tname, sizeof(tname));
				file_message("%d %s!>%s.%s %s\n", frameCount, tname, cname, mname, sig);
			} else {
				file_message("!>%s.%s %s\n", cname, mname, sig);
			}
			deallocate(jvmti, (void*)cname);
			deallocate(jvmti, (void*)mname);
			deallocate(jvmti, (void*)sig);

			/* Print info on the class/method this exception is caught by */
/*			error = (*jvmti)->GetClassSignature(jvmti, caught_by_class, &cname, NULL);
			check_jvmti_error(jvmti, error, "Cannot get the signature of the class this exception is thrown from");
			error = (*jvmti)->GetMethodName(jvmti, catch_method, &mname, &sig, NULL);
			check_jvmti_error(jvmti, error, "Cannot get the name and signature of the method this exception is thrown from");
			file_message("Exception caught by %s.%s(%s)\n", cname, mname, sig);
			deallocate(jvmti, (void*)*cname);
			deallocate(jvmti, (void*)*mname);
			deallocate(jvmti, (void*)*sig);
*/
		}
	} exit_critical_section(jvmti);
}

static void JNICALL cbExceptionCatch(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID method, jlocation location, jobject exception) {
	enter_critical_section(jvmti); {
		/* It's possible we get here right after VMDeath event */
		if(!gdata->vm_is_dead) {
			jclass caught_by_class;
			jvmtiError error;
			char* cname;
			char* mname;
			char* sig;
			jint frameCount;
			jthread currentThread;

			error = (*jvmti)->GetMethodDeclaringClass(jvmti, method, &caught_by_class);
			check_jvmti_error(jvmti, error, "Cannot get the class the exception is caught by");

			/* Print info on the class/method this exception is caught by */
			error = (*jvmti)->GetClassSignature(jvmti, caught_by_class, &cname, NULL);
			check_jvmti_error(jvmti, error, "Cannot get the signature of the class this exception is thrown from");
			error = (*jvmti)->GetMethodName(jvmti, method, &mname, &sig, NULL);
			check_jvmti_error(jvmti, error, "Cannot get the name and signature of the method this exception is thrown from");


			if(gdata->vm_is_inited) {
				char tname[MAX_THREAD_NAME_LENGTH];
				error = (*(gdata->jvmti))->GetFrameCount(gdata->jvmti, thread, &frameCount);
				check_jvmti_error(gdata->jvmti, error, "Can not get the frame count.");
				get_thread_name(gdata->jvmti, thread, tname, sizeof(tname));
				file_message("%d %s!<%s.%s %s\n", frameCount, tname, cname, mname, sig);
			} else {
				file_message("!<%s.%s %s\n", cname, mname, sig);
			}
			deallocate(jvmti, (void*)*cname);
			deallocate(jvmti, (void*)*mname);
			deallocate(jvmti, (void*)*sig);
		}
	} exit_critical_section(jvmti);
}



static void parse_agent_options(char *options) {
	char token[MAX_TOKEN_LENGTH];
	char *next;

	/* Parse options and set flags in gdata */
	if(options == NULL) {
		return;
	}

	/* Get the first token from the options string. */
	next = get_token(options, ",=", token, sizeof(token));
	/* While not at the end of the options string, process this option. */
	while(next != NULL) {
		if(strcmp(token,"help")==0) {
			stdout_message("OASIS Tracing Agent\n");
			stdout_message("\n");
			stdout_message(" java -agentlib:CJVMTracer[=options] ...\n");
			stdout_message("\n");
			stdout_message("The options are comma separated:\n");
			stdout_message("\t help\t\t\t Print help information\n");
			stdout_message("\t port=n\t\t\t Listen on port n for remote commands\n");
			stdout_message("\t start=filename\t\t Start tracing immediately and write trace to filename\n");
			stdout_message("");
			stdout_message("");
			exit(0);
		} else if (strcmp(token,"port")==0) {
            char number[MAX_TOKEN_LENGTH];

		    /* Get the numeric option */
		    next = get_token(next, ",=", number, (int)sizeof(number));
			/* Check for token scan error */
		    if ( next==NULL ) {
				fatal_error("ERROR: port=n option error\n");
			}
			/* Save port value */
			gdata->port = (char*) calloc(strlen(number)+1, 1);
			strcpy(gdata->port, number);
		} else if(strcmp(token,"start")==0) {
			char filename[MAX_TOKEN_LENGTH];

			next = get_token(next, ",=", filename, (int)sizeof(filename));
			if(next == NULL) {
				fatal_error("ERROR: start=filename option error\n");
			}
			gdata->start = 1;
			file = fopen(filename, "w+");
			if(file == NULL) {
				fatal_error("ERROR: Cannot open trace file");
			}
		} else if(token[0] != 0) {
			/* Unknown token */
			fatal_error("ERROR: Unknown option: %s\n", token);
		}
		next = get_token(next, ",=", token, sizeof(token));
	}
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
	static GlobalAgentData  data;
	jvmtiEnv			   *jvmti;
	jvmtiError			    error;
	jint					res;
	jvmtiCapabilities		capabilities;
	jvmtiEventCallbacks		callbacks;

	/* Setup initial global agent data area
	 * Use of static/extern data should be handled carefully here.
	 * We need to make sure that we are able to cleanup after ourselves
	 * so anything allocated in this library needs to be freed in
	 * the Agent_OnUnload() function.
	 */
	(void)memset((void*)&data, 0, sizeof(data));
	gdata = &data;
	jvm = vm;
	printf("Starting Call Agent...");
	/* First thing we need to do is get the jvmtiEnv* or JVMTI Environment */
	res = (*vm)->GetEnv(vm, (void **)&jvmti, JVMTI_VERSION_1);
	if(res != JNI_OK) {
		/* This means that the VM was unable to obtain this version of the
		 * JVMTI interface, this  is a fatal error.
		 */
		fatal_error("ERROR: Unable to access JVMTI Version 1 (0x%x),"
			" is your JDK a 5.0 or newer version?"
			" JNIEnv's GetEnv() returned %d\n",
			JVMTI_VERSION_1, res);
	}

	/* Here we save the jvmtiEnv* for Agent_OnUnload() */
	gdata->jvmti = jvmti;

	/* Set default options */
	gdata->port = (char*) calloc(strlen(DEFAULT_PORT)+1, 1);
	strcpy(gdata->port, DEFAULT_PORT);
	/* Parse any options supplied on java command line */
	parse_agent_options(options);

	/* Immediately after getting the jvmtiEnv* we need to ask for the
	 * capabilities this agent will need.  In this case, we need to make
	 * sure that we can get all class load hooks.
	 */
	(void)memset(&capabilities,0, sizeof(capabilities));
	capabilities.can_generate_all_class_hook_events = 0;
	capabilities.can_generate_exception_events = 1;
	capabilities.can_generate_method_entry_events = 1;
	capabilities.can_generate_method_exit_events = 1;
	capabilities.can_access_local_variables = 1;
	capabilities.can_get_line_numbers = 1;
	capabilities.can_get_bytecodes = 1;
	capabilities.can_get_source_debug_extension = 1;
	error = (*jvmti)->AddCapabilities(jvmti, &capabilities);
	check_jvmti_error(jvmti, error, "Unable to get necessary JVMTI capabilities.");

	/* Next we need to provide the pointers to the callback functions
	 * to this jvmtiEnv*
	 */
	(void)memset(&callbacks, 0, sizeof(callbacks));
	/* JVMTI_EVENT_VM_START */
	callbacks.VMStart				= &cbVMStart;
	/* JVMTI_EVENT_VM_INIT */
	callbacks.VMInit				= &cbVMInit;
	/* JVMTI_EVENT_VM_DEATH */
	callbacks.VMDeath				= &cbVMDeath;
	/* JVMTI_EVENT_CLASS_FILE_LOAD_HOOK */
	/*callbacks.ClassFileLoadHook		= &cbClassFileLoadHook;*/
	/* JVMTI_EVENT_THREAD_START */
	callbacks.ThreadStart			= &cbThreadStart;
	/* JVMTI_EVENT_THREAD_END */
	callbacks.ThreadEnd				= &cbThreadEnd;
	/* JVMTI_EXCEPTION */
	callbacks.Exception				= &cbException;
	/* JVMTI_EXCEPTION_CATCH */
	callbacks.ExceptionCatch		= &cbExceptionCatch;
	/* JVMTI_EVENT_METHOD_ENTRY */
	callbacks.MethodEntry			= &cbMethodEntry;
	/* JVMTI_EVENT_METHOD_EXIT */
	callbacks.MethodExit			= &cbMethodExit;

	error = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, (jint)sizeof(callbacks));
	check_jvmti_error(jvmti, error, "Cannot set jvmti callbacks");

	/* At first the only initial events we are interested in are VM
	 * initialization, VM death, and Class File Loads.
	 * Once the VM is initialized we will request more events.
	 */
	error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_START, (jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, (jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, (jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, (jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, (jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	/*error = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, (jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");*/

	/* Here we create a raw monitor for our use in this agent to
	 * protect critical sections of code.
	 */
	error = (*jvmti)->CreateRawMonitor(jvmti, "agent data", &(gdata->lock));
	check_jvmti_error(jvmti, error, "Cannot create a raw monitor");

	/* Add tracer jar file to boot classpath */
	/* TODO: add code to programatically find the location of the dll
	error = (*jvmti)->AddToBootstrapClassLoaderSearch(jvmti, "C:\\JVMTrace.jar");
    check_jvmti_error(jvmti, error, "Cannot add to boot classpath");*/



	/* We return JNI_OK to signify success */
	return JNI_OK;
}

/* Agent_OnUnload: This is called immediately before the shared library is
 * unloaded.  This is the last code executed.
 */
JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
	/* Make sure all malloc/calloc/strdup space is freed */
	if(gdata->include != NULL) {
		(void)free((void*)gdata->include);
		gdata->include = NULL;
	}
	if(gdata->exclude != NULL) {
		(void)free((void*)gdata->exclude);
		gdata->exclude = NULL;
	}
	if(gdata->classes != NULL) {
		int cnum;
		for(cnum = 0; cnum < gdata->ccount; cnum++) {
			ClassInfo *cp;
			cp = gdata->classes + cnum;
			(void)free((void*)cp->name);
			if(cp->mcount > 0) {
				int mnum;
				for(mnum = 0; mnum < cp->mcount; mnum++) {
					MethodInfo *mp;
					mp = cp->methods + mnum;
					(void)free((void*)mp->name);
					(void)free((void*)mp->signature);
				}
				(void)free((void*)cp->methods);
			}
		}
		(void)free((void*)gdata->classes);
		gdata->classes = NULL;
	}
	/* make sure to close the server */
	if (oasis_is_open()) {
		oasis_close_server();
	}
	if(file != NULL) {
		fclose(file);
	}
}


