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
#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/split.hpp>
#include "stdlib.h"
#include "VMServices.hpp"


/* C macrs to create strings from tokens */
#define _STRING(s) #s
#define STRING(s) _STRING(s)


int port = oasis::DEFAULT_PORT;
//flag used to indicate that the service should pause on startup (wait for signal to trace)
int pause_on_start = 0;
//flag used to indicate that we are running junit tests. Only trace test code.
int junit_mode = 0;


/* ------------------------------------------------------------------------- */
/* Some constants maximum sizes */

const int MAX_TOKEN_LENGTH = 16;
const int MAX_THREAD_NAME_LENGTH = 512;
const int MAX_METHOD_NAME_LENGTH = 1024;



static JavaVM			*jvm = NULL;

static bool finished = false;

static oasis::ServerAgent* _jvmti_agent = NULL;
static oasis::Server* _jni_server = NULL;

/* Callback for JVMTI_EVENT_VM_START */
/* Callback for JVMTI_EVENT_VM_INIT */

extern "C" {

static void JNICALL cbVMStart(jvmtiEnv *jvmti, JNIEnv *env) {
	if (finished) return;
	DEBUG_PRINT("Starting VM");
	_jvmti_agent->VMStart(jvmti, env);
	if (_jvmti_agent->IsVMStarted()) {
		DEBUG_PRINT("VM Started");
	}
}

static void JNICALL cbVMInit(jvmtiEnv *jvmti, JNIEnv *env, jthread thread) {
	if (finished) return;
	DEBUG_PRINT("Initing VM");
	try {
		std::cout << "Connecting Oasis Server on port " << port << std::endl;
		_jni_server->Connect();
		_jni_server->Start();
		std::cout << "Server Connected" << std::endl;
	} catch (oasis::ServerException &ex) {
		std::cerr << ex.what() << std::endl;
		exit(ex.GetError());
	}
	//initialise the agent.
	_jvmti_agent->Init(jvmti, env);
	_jvmti_agent->ThreadStarted(thread, jvmti, env);

}

/* Callback for JVMTI_EVENT_VM_DEATH */
static void JNICALL cbVMDeath(jvmtiEnv *jvmti, JNIEnv *env) {
	if (finished) return;
	DEBUG_PRINT("VM Death");
	_jni_server->Quit();
	_jvmti_agent->VMDeath(jvmti, env);
}

/* Callback for JVMTI_EVENT_THREAD_START */
static void JNICALL cbThreadStart(jvmtiEnv *jvmti, JNIEnv *env, jthread thread) {

	if (finished) return;
	DEBUG_PRINT("THREAD START");
	_jvmti_agent->ThreadStarted(thread, jvmti, env);
}

/* Callback for JVMTI_EVENT_THREAD_END */
static void JNICALL cbThreadEnd(jvmtiEnv *jvmti, JNIEnv *env, jthread thread) {
	if (finished) return;
	DEBUG_PRINT("THREAD END");
#ifdef DEBUG
	jint thread_count;
	jthread *threads;
	jvmti->GetAllThreads(&thread_count, &threads);
	for (int i = 0; i < thread_count; i++) {
		jvmtiThreadInfo info;
		jvmti->GetThreadInfo(threads[i], &info);
		DEBUG_PRINT(info.name << " " << ((info.is_daemon) ? "true" : "false"));
		jvmti->Deallocate((unsigned char*)info.name);
	}
	jvmti->Deallocate((unsigned char*)threads);
	DEBUG_PRINT(thread_count << " THREADS");
#endif
//	try {
//		jvmtiThreadInfo thrInfo;
//		CheckJVMTIError(jvmti->GetThreadInfo(thread, &thrInfo));
//		char* mainThreadName = "main";
//		if (strcmp(thrInfo.name, mainThreadName) == 0) {
//			::server->Quit();
//		}
//
//		CheckJVMTIError(jvmti->Deallocate((unsigned char*)thrInfo.name));
//	} catch (JVMTIException &e) {
//		std::cerr << "JVMTI Error: ending thread" << std::endl;
//		exit(e.GetError());
//	}
	_jvmti_agent->ThreadExited(jvmti, env, thread);
}


static void JNICALL cbMethodEntry(jvmtiEnv *jvmti,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method) {
	if (finished) return;
	_jvmti_agent->MethodEntered(jvmti, jni_env, thread, method);

}

static void JNICALL cbMethodExit(jvmtiEnv *jvmti,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method,
            jboolean was_popped_by_exception,
            jvalue return_value) {
	if (finished) return;
	_jvmti_agent->MethodExited(jvmti, jni_env, thread, method, was_popped_by_exception, return_value);
}

static void JNICALL cbException(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID method,
								jlocation location, jobject exception, jmethodID catch_method, jlocation catch_location) {
	if (finished) return;
	DEBUG_PRINT("calling exception thrown");
	//::agent->ExceptionThrown(jvmti, env, thread, method, exception, location, catch_method, catch_location);
}

static void JNICALL cbExceptionCatch(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID method, jlocation location, jobject exception) {
	if (finished) return;
	//::agent->ExceptionCaught(jvmti, env, thread, method, exception, location);
}

static void JNICALL cbClassPrepare(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jclass clazz) {
	if (finished) return;
	_jvmti_agent->ClassPrepared(jvmti, env, thread, clazz);
}



static void parse_agent_options(char *options) {

	/* Parse options and set flags in gdata */
	if(options == NULL) {
		return;
	}

	/* Get the first token from the options string. */
	std::string opts_string(options);
	//tokenize
	std::vector<std::string> tokens;
	boost::algorithm::split(tokens, opts_string, boost::algorithm::is_any_of(",="));
	std::vector<std::string>::iterator tokenIterator;
	/* While not at the end of the options string, process this option. */
	for(tokenIterator = tokens.begin(); tokenIterator != tokens.end(); tokenIterator++) {
		std::string token = *(tokenIterator);
		DEBUG_PRINT("Reading option " << token);
		if(token == "help") {
			std::cout << "OASIS Tracing Agent" << std::endl <<
				std::endl <<
				" java -agentlib:CJVMTracer[=options] ..." << std::endl <<
				std::endl <<
				"The options are comma separated:" << std::endl <<
				"\t help\t\t\t Print help information" << std::endl <<
				"\t port=n\t\t\t Listen on port for remote commands" << std::endl;
			exit(0);
		} else if (token == "port") {
            tokenIterator++;
            std::string port_string = *(tokenIterator);
            std::istringstream input(port_string);
            if (!(input >> port)) {
            	std::cerr << "Unable to read port " << port_string << std::endl;
            	exit(1);
            }
            std::cout << "Opening on port " << port << std::endl;

		}  else if (token == "pause") {
			tokenIterator++;
			std::string is_paused = *(tokenIterator);
			pause_on_start = (is_paused == "on");
			std::cout<< "Setting pause state to " << is_paused << std::endl;
		} else if (token == "junit") {
			tokenIterator++;
			std::string is_junit = *(tokenIterator);
			junit_mode = (is_junit == "true");
			std::cout << "Tracing JUnit Tests";
		} else if (!token.empty()) {
			/* Unknown token */
			std::cerr << "ERROR: Unknown option: " << token;
		}
	}
}

#ifdef WIN32
//mingw and boost conflict with a link error on _tls_used. Defining this call
//back will get rid of the error. I'm not sure of the side-effects, though.
extern "C" void tss_cleanup_implemented() {}
#endif

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
	try {
		std::cout << "Using Boost 1.51.0" << std::endl;
		std::cout << "JVMTI 32bit Build" << std::endl;
		std::cout << "Loading Agent" << std::endl;
		DEBUG_PRINT("Loading Agent");
		/* Set default options */
		port = oasis::DEFAULT_PORT;
		DEBUG_PRINT("Parsing Options");
		/* Parse any options supplied on java command line */
		parse_agent_options(options);
		_jvmti_agent = new oasis::ServerAgent(vm);
		DEBUG_PRINT("Agent allocated at " << std::hex << (intptr_t)(_jvmti_agent));
		_jni_server = new oasis::Server(port, _jvmti_agent);
		jvmtiCapabilities		capabilities;
		jvmtiEventCallbacks		callbacks;

		/* Setup initial global agent data area
		 * Use of static/extern data should be handled carefully here.
		 * We need to make sure that we are able to cleanup after ourselves
		 * so anything allocated in this library needs to be freed in
		 * the Agent_OnUnload() function.
		 */
		jvm = vm;
		std::cout <<"Starting Call Agent..." << std::endl;

		DEBUG_PRINT("Getting JVMTI");
		//get the JNI environment
		jvmtiEnv *jvmti = _jvmti_agent->GetJVMTI();
		//JNIEnv *jni = ::agent->GetJNI();



		/* Immediately after getting the jvmtiEnv* we need to ask for the
		 * capabilities this agent will need.  In this case, we need to make
		 * sure that we can get all class load hooks.
		 */
		DEBUG_PRINT("Setting Capabilities");
		(void)memset(&capabilities,0, sizeof(capabilities));
		capabilities.can_generate_all_class_hook_events = 0;
		capabilities.can_generate_exception_events = 1;
		capabilities.can_generate_method_entry_events = 1;
		capabilities.can_generate_method_exit_events = 1;
		capabilities.can_access_local_variables = 1;
		capabilities.can_get_line_numbers = 1;
		capabilities.can_get_bytecodes = 1;
		capabilities.can_get_source_debug_extension = 1;


		oasis::CheckJVMTIError(jvmti->AddCapabilities(&capabilities));
		oasis::CheckJVMTIError(jvmti->GetCapabilities(&capabilities));
		if (capabilities.can_access_local_variables != 1) {
			std::cerr << "Missing capability: access local variables";
			throw JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
		} if (capabilities.can_generate_method_entry_events != 1) {
			std::cerr << "Missing capability: generate method entry";
			throw JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
		} if (capabilities.can_generate_method_exit_events != 1) {
			std::cerr << "Missing capability: generate method exit";
			throw JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
		}
		/* Next we need to provide the pointers to the callback functions
		 * to this jvmtiEnv*
		 */

		DEBUG_PRINT("Setting Callbacks");
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
		/* JVMTI_EVENT_CLASS_PREPARE */
		callbacks.ClassPrepare			= &cbClassPrepare;

		oasis::CheckJVMTIError(jvmti->SetEventCallbacks(&callbacks, (jint)sizeof(callbacks)));

		/* At first the only initial events we are interested in are VM
		 * initialization, VM death, and Class File Loads.
		 * Once the VM is initialized we will request more events.
		 */
		oasis::CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_START, (jthread) NULL));
		oasis::CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, (jthread) NULL));
		oasis::CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, (jthread) NULL));
		oasis::CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, (jthread) NULL));
		oasis::CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, (jthread) NULL));
		oasis::CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, (jthread) NULL));
		oasis::CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_METHOD_ENTRY, (jthread) NULL));
		oasis::CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_METHOD_EXIT, (jthread) NULL));
		//
		if (_jvmti_agent->IsStoringBundles()) {
		//	CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, (jthread) NULL));
		}
//		if (!pause_on_start) {
//			oasis::CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, (jthread) NULL));
//			oasis::CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, (jthread) NULL));
//		}

		if (pause_on_start && !junit_mode) {
			_jvmti_agent->PauseAgent();
		} if (junit_mode) {
			_jvmti_agent->SetJUnit(true);
		}
		/*error = jvmti->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, (jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");*/

		/* Here we create a raw monitor for our use in this agent to
		 * protect critical sections of code.
		 *
	error = jvmti->CreateRawMonitor("agent data", &(gdata->lock));
	check_jvmti_error(jvmti, error, "Cannot create a raw monitor");
		 */
		/* Add tracer jar file to boot classpath */
		/* TODO: add code to programatically find the location of the dll
	error = jvmti->AddToBootstrapClassLoaderSearch(jvmti, "C:\\JVMTrace.jar");
    check_jvmti_error(jvmti, error, "Cannot add to boot classpath");*/

	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error " << ex << " loading JVMTI agent" << std::endl;
		return ex;
	}

	DEBUG_PRINT("Agent Loaded")
	/* We return JNI_OK to signify success */
	return JNI_OK;
}

/* Agent_OnUnload: This is called immediately before the shared library is
 * unloaded.  This is the last code executed.
 */
JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
	DEBUG_PRINT("Agent Unload");
	finished = true;
	_jvmti_agent->Unload();
	delete _jvmti_agent;
	_jvmti_agent = NULL;
	delete _jni_server;
	_jni_server = NULL;
}

} //end extern "C"
