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
/*
 * AgentData.cpp
 *
 *  Created on: 4-Mar-2009
 *      Author: Del Myers
 */

#define BOOST_THREAD_USE_LIB

#include "utils/JVMTIUtilities.hpp"
#include "stdafx.h"
#include <iosfwd>
#include <iomanip>
#include <boost/format.hpp>
#include <boost/filesystem.hpp>
#include <boost/date_time/posix_time/ptime.hpp>


using namespace boost;

namespace oasis {
//////////////// Java Threads



CachedMethodEvent::CachedMethodEvent(boost::shared_ptr<MethodEnter> event) :
		event(event), triggered(false), marked(false) {}

void CachedMethodEvent::Trigger() {
	triggered = true;
}
bool CachedMethodEvent::IsTriggered() {
	return triggered;
}

bool CachedMethodEvent::IsMarked() {
	return marked;
}

void CachedMethodEvent::Mark() {
	marked = true;
}

void CachedMethodEvent::Flush(ofstream &stream) {
	event->write(stream);
}


JavaThreadData::JavaThreadData(unsigned int local_id, JVMTIAgent* agent, jthread localThreadReference, jvmtiEnv* jvmti, JNIEnv* jni) :
	local_id(local_id),
	agent(agent),
	logfile(),
	mutex(),
	paused(true),
	pending_resume(false),
	pending_pause(false),
	is_inited(false),
	thread(NULL),
	methodSignatures(),
	cachedStack(),
	thread_recursing(false),
	thread_recursion_count(0),
	junitMethod(0),
	runningTest(false) {
	this->native_thread = this_thread::get_id();
	try {
		DEBUG_PRINT("Entered new JavaThreadData");
		this->thread = jni->NewGlobalRef(localThreadReference);
		if (this->thread == NULL) {
			std::cerr << "Could not create global reference for thread";
			exit(1);
		}
		DEBUG_PRINT("Attempting to reset JavaThreadData");
		Reset(jvmti, jni);
	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error: " << ex << " Creating thread";
		exit(ex);
	}

}

JavaThreadData::~JavaThreadData() {
	unique_lock<recursive_mutex> lock(mutex);
	if (logfile.is_open()) {
		logfile.flush();
		logfile.close();
	}
	try {
		JNIEnv* jni = agent->GetJNI();
		if (jni != NULL) {
			jni->DeleteGlobalRef(thread);
		}
		thread = NULL;
	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error: " << ex << " Disposing thread data";
		exit (ex);
	}
}

void JavaThreadData::CloseLog() {
	unique_lock<recursive_mutex> lock(mutex);
	if (logfile.is_open()) {
		logfile.flush();
		logfile.close();
	}
}

ostream *JavaThreadData::GetStream() {
	return &logfile;
}

void JavaThreadData::SetLog(const char *filename) {
	CloseLog();
	unique_lock<recursive_mutex> lock(mutex);
	logfile.open(filename, ios::out | ios::binary);


	if (logfile.is_open()) {
		HeaderEvent headerEvent;
		headerEvent.write(logfile);
		logfile.flush();
		if (!is_inited) {
			Reset(agent->GetJVMTI(), agent->GetJNI());
		}
		ThreadInit event;
		event.thread_id = local_id;
		event.thread_name_len = name.length();
		event.thread_name = (char*)(name.c_str());
            event.time = agent->TimeFromStart();
            event.write(logfile);
            logfile.flush();
            //make sure to get the stack trace when it starts
            paused = true;
            pending_resume = true;
        }
        else{
            std::cerr << "Error opening " << filename << " for writing." << endl;
        }
    }
/*
void JavaThreadData::PushJUnitStack(shared_ptr<MethodEnter> enter) {
	//first, check to see if the stack is empty. If yes, then we check to
	//make sure this is a JUnit reflective call.
	bool push = false;
	if (cachedStack.size() == 0) {
		//is it a JUNit reflective call?
		string methodName = enter->method_name;
		string className = enter->class_name;
		//in JUnit3, the root of a run is in a method called "runTest"
		if (methodName == "runTest") {

			if (className == "Ljunit/framework/TestCase;") {
				//store it
				push = true;
			}
		}
		//in JUnit 4, the root of a run is in a method called "invokeExplosively" on a
		//FrameWorkMethod
		if (methodName == "invokeExplosively") {
			//check to make sure that the root is in the junit package
			//check to make sure that the root is in a TestCase class
						std::cerr << className << std::endl;
			if (className == "Lorg/junit/runners/model/FrameworkMethod;") {
				push = true;
			}
		}
	} else {
		//just push it... we are already in the JUnit root.
		push = true;
	}
	if (push) {
		std::cerr << "pushing " << enter->method_name << std::endl;
		shared_ptr<CachedMethodEvent> cached(new CachedMethodEvent(enter));
		cachedStack.push_back(cached);
	}
}

void JavaThreadData::PopJUnitStack() {
	//just pop it if it is not empty
	if (cachedStack.size() > 0) {
		shared_ptr<CachedMethodEvent> cached = cachedStack.back();
		std::cerr << "popping " << cached->event->method_name << std::endl;
		cachedStack.pop_back();
	}
}
*/


    void JavaThreadData::WriteMethodEvent(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID, unsigned long  time)
    {
    	MethodSignature *sig = GetMethodSignature(jvmti, env, methodID);
    	shared_ptr<MethodEnter> enter_event(new MethodEnter());
        /* It's possible we get here right after VmDeath event, be careful */
        //		jclass cp;
        //
        //		char* class_name;
        //		error = jvmti->GetMethodDeclaringClass(methodID, &cp);
        //		CheckJVMTIError(error, "Getting declaring class");
        //		error = jvmti->GetClassSignature(cp, &class_name, NULL);
        //		CheckJVMTIError(error, "Getting class signature");

        enter_event->class_name = (__int8*)sig->ReferenceClassName().c_str();
        enter_event->class_name_len = sig->ReferenceClassName().length();
        enter_event->time = time;
        jlocation location;
        jmethodID method_ptr;
        jvmtiError error = jvmti->GetFrameLocation(thread, 1, &method_ptr, &location);
        enter_event->method_name = (__int8*)sig->ReferenceMethodName().c_str();
        enter_event->method_name_len = sig->ReferenceMethodName().length();
        enter_event->method_sig_len = sig->ReferenceRawSignature().length();
        enter_event->method_sig = (__int8*)sig->ReferenceRawSignature().c_str();
        enter_event->modifiers = sig->GetModifiers();
        enter_event->line_number = -1;
        if (error == JVMTI_ERROR_ABSENT_INFORMATION || error == JVMTI_ERROR_NO_MORE_FRAMES) {
			} else {
				CheckJVMTIError(error);
				MethodSignature* callerSig = GetMethodSignature(jvmti, env, method_ptr);
				if (callerSig != NULL) {
					enter_event->line_number = callerSig->LocateLine(jvmti, location);
				}
			}
        if(agent->IsStoringVariables()){
            //TODO: put variables here
        }
        //std::cout << enter_event.class_name << std::endl;
        //if we are in JUnit mode, store the stack, so that we can check to see
        //if what is being called is a JUnit method.
        if (agent->IsJUnit()) {
        	//don't recurse
        	thread_recursing = true;
        	if (!runningTest && agent->IsJUnitMethod(jvmti, env, methodID)) {
        		junitMethod = methodID;
        		runningTest = true;
        	}
        	thread_recursing = false;
        	if (runningTest) {
        		enter_event->write(logfile);
        	}
        } else {
        	enter_event->write(logfile);
        }

    }

    void JavaThreadData::MethodEntered(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID)
    {
        //can only recurse within the same thread, don't bother locking.
        if(thread_recursing)
            return;

        unique_lock<recursive_mutex> lock(mutex);
        if(!logfile.is_open()){
            return;
        }
        unsigned long time = agent->TimeFromStart();
        //stringstream output;
        jvmtiPhase phase;
        CheckJVMTIError(jvmti->GetPhase(&phase));
        if(phase != JVMTI_PHASE_LIVE)
            return;

        try {
		if (pending_pause) {
			try {
				DEBUG_PRINT("Pausing Thread");
				cachedStack.clear();
				paused = true;
				pending_pause = false;

				logfile.flush();
				PauseEvent pe;
				pe.time = time;
				stringstream stack(stringstream::out);
				PrintStackTrace(jvmti, env, stack);
				pe.stack_trace_len = stack.str().size();
				char* stack_string = new char[pe.stack_trace_len+1];
				strcpy(stack_string, stack.str().c_str());
				pe.stack_trace = stack_string;
				pe.write(logfile);
				paused = true;
				pending_resume = false;
				cachedStack.clear();
				delete[] stack_string;

				CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_METHOD_ENTRY, thread));
				CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_METHOD_EXIT, thread));
				return; // don't process any more.
			} catch (jvmtiError& ex) {
				std::cerr << "JVMTI Error: " << ex << " Paused method";
				exit(ex);
			}

		} else if (pending_resume) {
			paused = false;
			pending_resume = false;
			std::cerr << "Pending resume" << std::endl;
			if (agent->IsFiltering()) {
				cachedStack.clear();
			}
			try {
				logfile.flush();
				ResumeEvent pe;
				pe.time = time;
				stringstream stack;
				PrintStackTrace(jvmti, env, stack);
				pe.stack_trace_len = stack.str().length();
				char* stack_string = new char[pe.stack_trace_len+1];
				strcpy(stack_string, stack.str().c_str());
				pe.stack_trace = stack_string;
#ifdef DEBUG
				//there sems to be a problem with the stack getting garbled some times
				//check it here.
				try {
					regex rexp("([/\\.a-zA-Z0-9\\$\\;\\[\\]:\\(\\)\\-\\<\\>]|\\s)+");
					if (!regex_match(pe.stack_trace, rexp)) {
						std::cerr << "Bad Stack Trace: " << std::endl << pe.stack_trace;
					}
				} catch (std::exception &e) {
					std::cerr << "Bad Regular Expression";
				}
#endif
				pe.write(logfile);
				delete[] stack_string;
			} catch (jvmtiError& ex) {
				std::cerr << "JVMTI Error: " << ex << " Resuming method";
				exit(ex);
			}
			if (agent->IsFiltering()) {
				RebuildStack(jvmti, env);
			}
		}
		if (paused) return;


		if (agent->IsFiltering()) {
			DoFilteredMethodEnter(jvmti, env, methodID, 1);
		} else {
			WriteMethodEvent(jvmti, env, methodID, time);
		}
	} catch (jvmtiError& ex) {
		std::cerr << "JVMTI Error: " << ex << " Entering Method";
		exit(ex);
	}
    }
    void JavaThreadData::RebuildStack(jvmtiEnv *jvmti, JNIEnv *env) throw (jvmtiError)
    {
        //print out the frames, from last to first
        cachedStack.clear();
        jint frameCount;
        jint numFrames;
        CheckJVMTIError(jvmti->GetFrameCount(thread, &numFrames));
        jvmtiFrameInfo *frameBuffer = new jvmtiFrameInfo[numFrames];
        CheckJVMTIError(jvmti->GetStackTrace(thread, 0, numFrames, frameBuffer, &frameCount));
        int frame = 0;
        for(frame = frameCount - 1;frame >= 1;frame--){
            DoFilteredMethodEnter(jvmti, env, frameBuffer[frame].method, frame + 1);
        }
        delete [] frameBuffer;
    }

    void JavaThreadData::DoFilteredMethodEnter(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID, int stackDepth) throw (jvmtiError)
    {
        shared_ptr<MethodEnter> enter_event(new MethodEnter());
        /* It's possible we get here right after VmDeath event, be careful */
        MethodSignature *sig = GetMethodSignature(jvmti, env, methodID);
        enter_event->class_name = (__int8*)sig->ReferenceClassName().c_str();
        enter_event->class_name_len = sig->ReferenceClassName().length();
        enter_event->time = agent->TimeFromStart();
        jlocation location;
        jmethodID method_ptr;
        jvmtiError error = jvmti->GetFrameLocation(thread, stackDepth, &method_ptr, &location);
        enter_event->method_name = (__int8*)sig->ReferenceMethodName().c_str();
        enter_event->method_name_len = sig->ReferenceMethodName().length();
        enter_event->method_sig_len = sig->ReferenceRawSignature().length();
        enter_event->method_sig = (__int8*)sig->ReferenceRawSignature().c_str();
        enter_event->modifiers = sig->GetModifiers();
        enter_event->line_number = -1;
        if (error == JVMTI_ERROR_ABSENT_INFORMATION || error == JVMTI_ERROR_NO_MORE_FRAMES) {
	} else {
		CheckJVMTIError(error);
		MethodSignature* callerSig = GetMethodSignature(jvmti, env, method_ptr);
		if (callerSig != NULL) {
			enter_event->line_number = callerSig->LocateLine(jvmti, location);
		}
	}
        if(agent->IsStoringVariables()){
            //TODO: put variables here
        }
        //std::cout << enter_event.class_name << std::endl;
        shared_ptr<CachedMethodEvent> cached(new CachedMethodEvent(enter_event));
        //if the last call was a trigger, than this call should be recorded
        //as well
        if(cachedStack.size() > 0){
            shared_ptr<CachedMethodEvent> last = cachedStack.back();
            if(last->IsTriggered()){
                cached->Mark();
                cached->Flush(logfile);
            }
        }

        cachedStack.push_back(cached);
        //check to see if the event is interesting
        if(agent->PassesFilter(jvmti, methodID)){
            cached->Trigger();
            if(!cached->IsMarked()){
                //output the stack up to this point.
                //iterate to the last mark
                list<shared_ptr<CachedMethodEvent> >::iterator it = cachedStack.end();
                --it;
                while(it != cachedStack.begin()){
                    if((*it)->IsMarked()){
                        break;
                    }
                    --it;
                }

                while(it != cachedStack.end()){
                    if(!(*it)->IsMarked()){
                        (*it)->Flush(logfile);
                    }
                    (*it)->Mark();
                    ++it;
                }

            }

        }

    }

    MethodSignature *JavaThreadData::GetMethodSignature(jvmtiEnv *jvmti, JNIEnv *jni, jmethodID methodID)
    {
        unordered_map<jmethodID,MethodSignature*>::iterator it = methodSignatures.find(methodID);
        if(it == methodSignatures.end()){
            MethodSignature *sig = new MethodSignature(jvmti, methodID);
            methodSignatures.insert(pair<jmethodID,MethodSignature*>(methodID, sig));
        }
        return methodSignatures[methodID];
    }

    void JavaThreadData::ClassPrepared(jvmtiEnv *jvmti, JNIEnv *env, jclass clazz)
    {
        unique_lock<recursive_mutex> lock(mutex);
        char *className = NULL;
        jmethodID *methods;
        jint methodCount = 0;
        string bundle = "";
        try {
		CheckJVMTIError(jvmti->GetClassSignature(clazz, &className, NULL));
		if (className != NULL) {
			CheckJVMTIError(jvmti->GetClassMethods(clazz, &methodCount, &methods));
			bundle = PrependBundleName(jvmti, env, clazz);
			std::cout << "Class " << className << " bundle " << bundle << std::endl;
			for (int i = 0; i < methodCount; i++) {
				char *methodName = NULL;
				char *methodSignature = NULL;
				CheckJVMTIError(jvmti->GetMethodName(methods[i], &methodName, &methodSignature, NULL));
				if (methodName != NULL) {
					if (methodSignature != NULL) {
						std::cout << "\t" << methodName << methodSignature << std::endl;
						agent->Deallocate(jvmti, (void*)methodSignature);
					}
					agent->Deallocate(jvmti, (void*)methodName);
				}
			}
			agent->Deallocate(jvmti, (void*)className);
		}
		if (methods != NULL) {
			agent->Deallocate(jvmti, (void*)methods);
		}

	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error: " << ex << " Preparing class";
		exit(ex);
	}
    }

    /**
 * target must be an array of size 1024.
 * @param target
 * @param class_name
 * @param cp
 */
    string JavaThreadData::PrependBundleName(jvmtiEnv *jvmti, JNIEnv *env, jclass cp)
    {
        string bundle_string = "";
        if(env->PushLocalFrame(10) < 0){
            return bundle_string;
        }
        thread_recursing = true;
        thread_recursion_count++;
        jobject classLoader;
        try {
		CheckJVMTIError(jvmti->GetClassLoader(cp, &classLoader));
		if (classLoader != NULL) {
			//get the bundle
			jclass classLoaderClass = env->GetObjectClass(classLoader);
			if (env->ExceptionCheck()) {
				env->ExceptionClear();
			} else if (classLoaderClass != NULL) {
				jmethodID bundleMethod = env->GetMethodID(classLoaderClass, "getBundle", "()Lorg/osgi/framework/Bundle;");
				if (env->ExceptionCheck()) {
					env->ExceptionClear();
				} else if (bundleMethod != NULL) {
					jobject bundle = env->CallObjectMethod(classLoader, bundleMethod);
					if (env->ExceptionCheck()) {
						env->ExceptionClear();
					} else if (bundle != NULL) {
						//get the bundle name
						jclass bundleClass = env->GetObjectClass(bundle);
						jmethodID nameID = env->GetMethodID(bundleClass, "getSymbolicName", "()Ljava/lang/String;");
						if (env->ExceptionCheck()) {
							env->ExceptionClear();
						} else if (nameID != NULL) {
							//					get the name
							jstring bundleName = (jstring)env->CallObjectMethod(bundle, nameID);
							if (env->ExceptionCheck()) {
								env->ExceptionClear();
							} else if (bundleName != NULL) {
								//finally, store the name.
								const char* nativeName = env->GetStringUTFChars(bundleName, NULL);
								if (env->ExceptionCheck()) {
									env->ExceptionClear();
								} else if (nativeName != NULL) {
									int classLoaderSize = strlen(nativeName);
									if (classLoaderSize > 0) {
										bundle_string = nativeName;
									}
									env->ReleaseStringUTFChars(bundleName, nativeName);
								}
							}
						}
					}
				}
			}
		}
	} catch (jvmtiError) {
		//ignore it}
	}
        env->PopLocalFrame(NULL);
        thread_recursion_count--;
        if(thread_recursion_count == 0){
            thread_recursing = false;
        }
        return bundle_string;
    }

    Variable **JavaThreadData::GetLocalVariables(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID, int & variable_count) throw (jvmtiError)
    {
        MethodSignature signature(jvmti, methodID);
        jint localVariableCount = 0;
        jvmtiLocalVariableEntry *variable_table;
        variable_table = NULL;
        jvmtiError error = jvmti->GetLocalVariableTable(methodID, &localVariableCount, &variable_table);
        if (error == JVMTI_ERROR_ABSENT_INFORMATION) {
		variable_count = 0;
		return NULL;
	}
        CheckJVMTIError(error);
        Variable** variables = NULL;
        if (variable_table != NULL) {
		/*log the value of each variable */
		jint ivalue = 0;
		jfloat fvalue = 0;
		jlong lvalue = 0;
		jdouble dvalue = 0;
		jint hash = 0;
		jobject ovalue = 0;
		int var = 0;
		variable_count = signature.GetNumParameters();
		if (!signature.IsStatic()) {
			variable_count += 1;
		}
		if (variable_count > localVariableCount) {
			variable_count = localVariableCount;
		}

		/* get the this pointer */
		if (!signature.IsStatic()) {
			variable_count++;
		}
		variables = new Variable*[variable_count];

		for (; var < variable_count; var++) {
			jvmtiLocalVariableEntry varEntry = variable_table[var];
			char type = varEntry.signature[0];
			stringstream output;
			switch(type) {
			case TYPE_CHAR:
				//if (true) break;
				CheckJVMTIError(jvmti->GetLocalInt(thread, 0, varEntry.slot, &ivalue));
				output << ivalue;
				break;
			case TYPE_BOOLEAN:
			case TYPE_BYTE:
			case TYPE_SHORT:
			case TYPE_INT:
				CheckJVMTIError(jvmti->GetLocalInt(thread, 0, varEntry.slot, &ivalue));
				//if (true) break;
				output << ivalue;
				break;
			case TYPE_FLOAT:
				//if (true) break;
				CheckJVMTIError(jvmti->GetLocalFloat(thread, 0, varEntry.slot, &fvalue));
				output << fvalue;
				break;
			case TYPE_LONG:
				//if (true) break;
				CheckJVMTIError(jvmti->GetLocalLong(thread, 0, varEntry.slot, &lvalue));
				output << lvalue;
				break;
			case TYPE_DOUBLE:
				//if (true) break;
				CheckJVMTIError(jvmti->GetLocalDouble(thread, 0, varEntry.slot, &dvalue));
				output << dvalue;
				break;
			default:
				//if (true) break;
				error = jvmti->GetLocalObject(thread, 0, varEntry.slot, &ovalue);
				if (error) {
					hash = 0;
				} else {
					error = jvmti->GetObjectHashCode(ovalue, &hash);
					env->DeleteLocalRef(ovalue);
					if (error) {
						hash = 0;
					}
				}
				output << hash;
				break;
			}
			variables[var] = new Variable();
			variables[var]->value_len = output.str().size();
			variables[var]->value = new char[variables[var]->value_len];
			memcpy(variables[var]->value, output.str().data(), variables[var]->value_len);
		}
		for (var = 0; var < localVariableCount; var++) {
			agent->Deallocate(jvmti, variable_table[var].generic_signature);
			agent->Deallocate(jvmti, variable_table[var].name);
			agent->Deallocate(jvmti, variable_table[var].signature);
		}
		agent->Deallocate(jvmti, (unsigned char*)variable_table);
	}
        return variables;
    }

    void JavaThreadData::FreeLocalVariables(Variable **variables, int variable_count)
    {
        if (variables == NULL) return;
        for(int i = 0;i < variable_count;i++){
            delete [] (variables[i]->value);
            delete variables[i];
        }
        delete [] variables;
    }

    void JavaThreadData::MethodExited(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID, jboolean byException, jvalue returnValue)
    {
        //can only recurse within the same thread, don't bother locking.
        if(thread_recursing)
            return;

        unique_lock<recursive_mutex> lock(mutex);
        if(!logfile.is_open()){
            return;
        }
        if(paused)
            return;

        if(agent->IsFiltering()){
            //if the last event on the stack isn't marked, then we don't
            //have to output this exit event.
            if(pending_resume){
                //wait until a method call so that the stack can be
                //rebuilt.
                return;
            }
            if(cachedStack.size() <= 0){
                std::cerr << "Uninitialised stack on method exit" << std::endl;
                exit(1);
            }
            shared_ptr<CachedMethodEvent> last = cachedStack.back();
            cachedStack.pop_back();
            if(!last->IsMarked()){
                return;
            }
        }

        /* It's possible we get here right after VmDeath event, be careful */
        if(agent->IsVMDead())
            return;

        try {
		jvmtiPhase phase;
		CheckJVMTIError(jvmti->GetPhase(&phase));

		if (phase != JVMTI_PHASE_LIVE) return;


		MethodExit exit_event;
		MethodSignature* sig = GetMethodSignature(jvmti, env, methodID);
		bool write = true;
		if (agent->IsJUnit()) {
			if (runningTest) {
				if (junitMethod == methodID) {
					junitMethod = 0;
					runningTest = false;
				}
			} else {
				write = false;
			}
			//pop off the cache
			//PopJUnitStack();
		}
		if (write) {
			exit_event.class_name = (__int8 *)sig->ReferenceClassName().c_str();
			exit_event.class_name_len = sig->ReferenceClassName().length();
			exit_event.time = (__uint32)agent->TimeFromStart();
			jlocation location;
			jmethodID method_ptr;
			jvmtiError error = jvmti->GetFrameLocation(thread, 0, &method_ptr, &location);
			exit_event.method_name = (__int8 *)sig->ReferenceMethodName().c_str();
			exit_event.method_name_len = sig->ReferenceMethodName().length();
			exit_event.method_sig_len = sig->ReferenceRawSignature().length();
			exit_event.method_sig = (__int8*)sig->ReferenceRawSignature().c_str();
			exit_event.line_number = -1;
			exit_event.is_exception = (byException) ? 1 : 0;
			if (error == JVMTI_ERROR_ABSENT_INFORMATION || error == JVMTI_ERROR_NO_MORE_FRAMES) {
			} else {
				CheckJVMTIError(error);
				if (sig != NULL) {
					exit_event.line_number = sig->LocateLine(jvmti, location);
				}
			}


			exit_event.write(logfile);
		}
	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error: " << ex << " Exiting method";
		exit (ex);
	}
    }
    void JavaThreadData::ExceptionThrown(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID, jobject exception, jlocation location, jmethodID catchMethod, jlocation catchLocation)
    {
    }

    void JavaThreadData::ExceptionCaught(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID, jobject exception, jlocation location)
    {
    }

    //Note: this method is NOT thread safe. Do not call it outside of methods
    //that aren't already locked
    void JavaThreadData::Reset(jvmtiEnv *jvmti, JNIEnv *env)
    {
        ;
        //unique_lock<recursive_mutex> lock(mutex);
        ;
        if(!agent->IsVMInited())
            return;

        ;
        try {
            jvmtiError error;
            //set the name
            jvmtiThreadInfo thrinfo;
            error = jvmti->GetThreadInfo(thread, &thrinfo);
            CheckJVMTIError(error);
            env->DeleteLocalRef(thrinfo.thread_group);
            env->DeleteLocalRef(thrinfo.context_class_loader);
            //copy the name
            name = thrinfo.name;
            if(name.empty()){
                name = "Unnamed thread";
            }
            error = jvmti->Deallocate((unsigned char*)(thrinfo.name));
            std::cout << "Initialising thread " << name << " (" << local_id << ")" << std::endl;
            CheckJVMTIError(error);
            is_inited = true;
        }
        catch(jvmtiError & ex){
            std::cerr << "JVMTI Error: " << ex << " Resetting thread";
            exit(ex);
        }
        catch(std::bad_alloc ){
            char *msg = "Error allocating memory";
            std::cerr << msg << std::endl;
            exit(-1);
        }
        ;
    }

    /*
jvmtiEnv * JavaThreadData::GetJVMTI() throw (JVMTIException) {
	thread::id current = this_thread::get_id();
	if (current == native_thread) {
		if (jvmti == NULL) {
			throw (JVMTIException(JVMTI_ERROR_NULL_POINTER));
		}
		return jvmti;
	}
	DEBUG_PRINT("Getting JVMTI from agent");
	return agent->GetJVMTI();
}

JNIEnv *JavaThreadData::GetJNI() throw (JVMTIException) {
	thread::id current = this_thread::get_id();
	if (current == native_thread) {
		return jni;
	}
	return agent->GetJNI();
}
*/
    void JavaThreadData::Pause(jvmtiEnv *jvmti, JNIEnv *env)
    {
        unique_lock<recursive_mutex> lock(mutex);
        if(pending_resume){
            //just cancel the pending resume.
            //and don't pause
            pending_pause = false;
        }else{
            //pend a pause
            pending_pause = true;
        }
        pending_resume = false;
    }

    void JavaThreadData::Resume(jvmtiEnv *jvmti, JNIEnv *env)
    {
        unique_lock<recursive_mutex> lock(mutex);
        if(pending_pause){
            //just cancel the pending pause
            //and don't resume
            pending_resume = false;
        }else{
            //pend a resume
            pending_resume = true;
        }
        pending_pause = false;
        try {
            //reset the capabilities so that we will be notified of method entries and exits.
            CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, thread));
            CheckJVMTIError(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, thread));
        }
        catch(jvmtiError ex){
            std::cerr << "JVMTI Error " << ex << ": while resuming agent." << std::endl;
        }
    }

    void JavaThreadData::PrintStackTrace(jvmtiEnv *jvmti, JNIEnv *env, ostream & output) throw (jvmtiError)
    {
        //print out the frames, from last to first
        //make sure that the thread is paused first, so that we get a correct
        //frame
        //suspend the thread if not suspended.
        //jint thread_state = 0;
        //bool restart = false;
        jvmtiFrameInfo* frameBuffer = NULL;
        try {
		//		CheckJVMTIError(jvmti->GetThreadState(thread, &thread_state));
		//		if (thread_state & JVMTI_THREAD_STATE_ALIVE) {
		//			if (!(thread_state & JVMTI_THREAD_STATE_SUSPENDED)) {
		//				CheckJVMTIError(jvmti->SuspendThread(thread));
		//				CheckJVMTIError(jvmti->GetThreadState(thread, &thread_state));
		//				restart = true;
		//			}
		//		}
		jint frameCount;
		jint numFrames;
		CheckJVMTIError(jvmti->GetFrameCount(thread, &numFrames));
		int lineNumber = -1;
		MethodSignature *lastMethod = NULL;
		if (numFrames > 0) {
			frameBuffer = new jvmtiFrameInfo[numFrames];
			CheckJVMTIError(jvmti->GetStackTrace(thread, 0, numFrames, frameBuffer, &frameCount));
			for (int frame=frameCount-1; frame >= 0; frame--) {
//				char* name, *sig, *generic;
//				jvmti->GetMethodName(frameBuffer[frame].method, &name, &sig, &generic);
//				DEBUG_PRINT(name << " " << sig << " " << generic << std::endl);
//				if (name != NULL) {
//					jvmti->Deallocate((unsigned char*)name);
//				}
//				if (name != NULL) {
//					jvmti->Deallocate((unsigned char*)sig);
//				}
//				if (name != NULL) {
//					jvmti->Deallocate((unsigned char*)generic);
//				}
				MethodSignature* sig = GetMethodSignature(jvmti, env, frameBuffer[frame].method);
				//			jint lineNumber = -1;
				//			if (lastMethod != NULL) {
				//				jint location = frameBuffer[frame].location;
				//				jint entry_count;
				//				jvmtiLineNumberEntry *line_numbers;
				//				jvmtiError error = jvmti->GetLineNumberTable(lastMethod, &entry_count, &line_numbers);
				//				if (error != JVMTI_ERROR_ABSENT_INFORMATION && error != JVMTI_ERROR_NATIVE_METHOD) {
				//					CheckJVMTIError(error);
				//					/* run a linear search to find the location... could be sped up */
				//					int index = 0;
				//					jvmtiLineNumberEntry entry;
				//					bool entry_found = false;
				//					while (index < entry_count && !entry_found) {
				//						jvmtiLineNumberEntry centry = line_numbers[index];
				//						if (centry.start_location >= location) {
				//							entry_found = true;
				//							if (centry.start_location > location && index > 0) {
				//								entry = line_numbers[index-1];
				//							} else {
				//								entry = centry;
				//							}
				//						}
				//						index++;
				//					}
				//					if (entry_found) {
				//						lineNumber = entry.line_number;
				//					}
				//				}
				//			}
				if (lastMethod == NULL) {
					lineNumber = -1;
				}

				output << "\t" << sig->ReferenceClassName() << sig->ReferenceMethodName() << sig->ReferenceRawSignature() << "[" << setbase(10) << lineNumber << "]" << std::endl;
				//			lastMethod = frameBuffer[frame].method;
				lastMethod = sig;
				lineNumber = (int)sig->LocateLine(jvmti, frameBuffer[frame].location);

			}
			if (frameBuffer != NULL) {
				delete[] frameBuffer;
			}
			//		if (restart) {
			//			restart = false;
			//			CheckJVMTIError(jvmti->ResumeThread(thread));
			//		}
		}
	} catch (jvmtiError err) {
		//		if (restart) {
		//			CheckJVMTIError(jvmti->ResumeThread(thread));
		//		}
		if (frameBuffer != NULL) {
			delete[] frameBuffer;
		}
		throw (err);
	}
    }

    void JavaThreadData::ThreadDeath(jvmtiEnv *jvmti, JNIEnv *env)
    {
        unique_lock<recursive_mutex> lock(mutex);
        std::cout << "Ending thread " << name << std::endl;
        if(cachedStack.size() > 0){
            //we have to flush out all of the method calls.
            ;
	}
	//delete the global reference
	if (thread != NULL) {
		env->DeleteGlobalRef(thread);
	}
	//kill the method cache
	unordered_map<jmethodID, MethodSignature*>::iterator it;
	for (it = methodSignatures.begin(); it != methodSignatures.end(); it++) {
		delete (*it).second;
	}
	methodSignatures.clear();
	if (logfile.is_open()) {
		logfile.flush();
		logfile.close();
	}
	paused = true;
}



}









