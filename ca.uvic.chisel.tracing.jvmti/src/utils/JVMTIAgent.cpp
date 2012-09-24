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
#include "utils/MethodSignature.hpp"
#include "utils/JVMTIUtilities.hpp"
#include "stdafx.h"
#include <ios>
#include <boost/format.hpp>
#include <boost/filesystem.hpp>
#include <boost/functional/hash.hpp>
#include <cstdlib>

//////////////////// JVMTI Agent
namespace oasis {

using namespace boost;
using namespace std;
JVMTIAgent::JVMTIAgent(JavaVM* vm) throw (jvmtiError) :
		mutex(),
		vm(vm),
		paused(false),
		num_threads(0),
		statelocation(""),
		logfile(),
		log(NULL),
		native_thread(),
		local_start_time(0),
#ifdef WIN32
	//the windows time at which the agent was started.
	start_millis(0),
#else
	//the generic time at which the agent was started.
	start_time(),
#endif


		jvmti(NULL),
//		jni(NULL),
		vm_is_dead(false),
		vm_is_started(false),
		vm_is_inited(false),
		include(NULL),
		exclude(NULL),
		max_count(0),
		port(NULL),
		start(false),
		is_storing_bundles(false),
		threads(),
		is_filtering(false),
		exclusion_filters(),
		inclusion_filters(),
		filter_mutex(),
		valid_classes(),
		junit3TestCaseClass(NULL),
		junit4TestClass(NULL)
		 {

#ifdef WIN32
	this->start_millis = GetTickCount();
#else
	DEBUG_PRINT("Getting time");
	this->start_time = posix_time::microsec_clock::local_time();
	DEBUG_PRINT("Got time");
#endif
	/* First thing we need to do is get the jvmtiEnv* or JVMTI Environment */


	jint result = vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1);
	if (result != JNI_OK) {
		jint result = vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_0);
		if (result != JNI_OK) {
			throw JVMTI_ERROR_ABSENT_INFORMATION;
		} else {
			DEBUG_PRINT("Returning JVMTI  Version 1.1")
		}
	} else {
		DEBUG_PRINT("Returning JVMTI Version 1.0");
	}
	if (jvmti == NULL) {
		throw JVMTI_ERROR_ABSENT_INFORMATION;
	}

//	res = vm->GetEnv((void **)&jni, JNI_VERSION_1_1);
//	if(res != JNI_OK) {
//		cerr << "Unable to retrieve JNI Interface 1.1." << endl;
//		throw (JVMTIException(JVMTI_ERROR_ABSENT_INFORMATION));
//	}
}

void JVMTIAgent::CloseLog() {
	unique_lock<shared_mutex> lock(mutex);
	if (logfile.is_open()) {
		logfile.close();
	}
	log = &std::cout;
}

void JVMTIAgent::Init(jvmtiEnv* jvmti, JNIEnv* jni) {
	DEBUG_PRINT("Starting init");
	unique_lock<shared_mutex> lock(mutex);
//	if (this->jvmti == NULL) {
//		this->jvmti = jvmti;
//	}
//	if (this->jni == NULL) {
//		this->jni = jni;
//	}
	DEBUG_PRINT("HERE");
	//posix_time::microsec_clock::local_time();
	DEBUG_PRINT("HERE!");
	this->vm_is_inited = true;
}

JVMTIAgent::~JVMTIAgent() {
	unique_lock<shared_mutex> lock(mutex);
	// get rid of all of the references.
	if (logfile.is_open()) {
		logfile.close();
	}
	log = &std::cout;
}

void JVMTIAgent::Deallocate(jvmtiEnv *jvmti, void *data) {
	try {
		CheckJVMTIError(jvmti->Deallocate((unsigned char *)data));
	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error: " << ex << " Deallocating jni data";
		exit(ex);
	}
}

void JVMTIAgent::SetStateLocation(const char* location) {
	CloseLog();
	//close the log on all of the threads.
	//first lock the mutex to make sure that none of the threads
	//get interupted as we go.
	unique_lock<shared_mutex> lock(mutex);
	vector<JavaThreadData*>::iterator threadIterator;
	if (filesystem::is_directory(location)) {
		//create a file for the new location.
		filesystem::path p(location);
		filesystem::path agentlog = p / "agent.log";
		logfile.open(agentlog.string().c_str(), ios::out);
		if (!logfile.is_open()) {
			std::cerr << "Error opening " << agentlog.string() << " for writing. Switching to standard out." << endl;
			log = &std::cout;
		} else {
			log = &logfile;
		}
		statelocation = location;
		vector<JavaThreadData*>::iterator threadIterator;
		jvmtiEnv* jvmti = GetJVMTI();
		jint thread_count;
		jthread* threads;
		try {
			CheckJVMTIError(jvmti->GetAllThreads(&thread_count, &threads));
			for (int i = 0; i < thread_count; i++) {
				JavaThreadData* ptr = GetThread(jvmti, threads[i]);
				if (ptr != NULL) {
					ptr->CloseLog();
					filesystem::path threadpath = p / str(format("%d.trace") % ptr->GetID());
					ptr->SetLog(threadpath.string().c_str());
				}
			}
			Deallocate(jvmti, (void *)threads);
		} catch (jvmtiError ex) {
			std::cerr << "JVMTI Error " << ex << " setting state location";
			exit(ex);
		}
	}
}


void JVMTIAgent::MethodEntered(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID methodID) {
	//DEBUG_PRINT("method entered");
	//shared_lock<shared_mutex> lock(mutex);
	if (!IsVMInited()) return;
	//	if (paused) {
	//		return;
	//	}
	try {
		JavaThreadData* threadPtr = GetThread(jvmti, thread);

		if (threadPtr != NULL) {
			threadPtr->MethodEntered(jvmti, env, methodID);
		}
	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error: " << ex << " Agent method entry";
		exit(ex);
	}
}

void JVMTIAgent::MethodExited(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID methodID, jboolean byException, jvalue returnValue) {
	//shared_lock<shared_mutex> lock(mutex);
	if (!IsVMInited()) return;
	//	if (paused) {
	//		return;
	//	}
	try {
		JavaThreadData* threadPtr = GetThread(jvmti, thread);
		if (threadPtr != NULL) {
			threadPtr->MethodExited(jvmti, env, methodID, byException, returnValue);
		}
	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error: " << ex << " Agent method exit";
		exit(ex);
	}
}

void JVMTIAgent::ExceptionThrown(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID methodID, jobject exception, jlocation location, jmethodID catchMethod, jlocation catchLocation) {
	DEBUG_PRINT("exception thrown");
	shared_lock<shared_mutex> lock(mutex);
	if (!IsVMInited()) return;
	ostream &output = *log;
//		if (paused) {
//			return;
//		}
	try {
		JavaThreadData* threadPtr = GetThread(jvmti, thread);
		if (threadPtr) {
			threadPtr->ExceptionThrown(jvmti, env, methodID, exception, location, catchMethod, catchLocation);
		} else {
			jvmtiThreadInfo thrinfo;
			CheckJVMTIError(jvmti->GetThreadInfo(thread, &thrinfo));
			output << "Error: Unable to find thread: " << thrinfo.name << std::endl;
			Deallocate(jvmti, &thrinfo);
		}
	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error: " << ex << " Agent exception throw";
		exit(ex);
	}
}

void JVMTIAgent::ExceptionCaught(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID methodID, jobject exception, jlocation location) {
	shared_lock<shared_mutex> lock(mutex);
	if (!IsVMInited()) return;
	ostream &output = *log;
//		if (paused) {
//			return;
//		}
	try {
		JavaThreadData* threadPtr = GetThread(jvmti, thread);
		if (threadPtr) {
			threadPtr->ExceptionCaught(jvmti, env, methodID, exception, location);
		} else {
			jvmtiThreadInfo thrinfo;
			CheckJVMTIError(jvmti->GetThreadInfo(thread, &thrinfo));
			output << "Error: Unable to find thread: " << thrinfo.name << std::endl;
			Deallocate(jvmti, &thrinfo);
		}
	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error: " << ex << " Agent exception catch";
		exit(ex);
	}
}

JavaThreadData* JVMTIAgent::ThreadStarted(jthread thread, jvmtiEnv* jvmti, JNIEnv* jni) {
	DEBUG_PRINT("Starting thread " << thread << "on agent at " << hex << (intptr_t)this);
	JavaThreadData* jthreadData = NULL;
	//must add the thread to the list of threads in the vector.
	//first, enter a scoped (unique lock) to make sure that
	//another thread doesn't try to access the vector.

	unique_lock<shared_mutex> localLock(mutex, try_to_lock);

	try {
		CheckJVMTIError(jvmti->GetThreadLocalStorage(thread, (void**)&jthreadData));
		if (jthreadData == NULL) {
			//create new storage
			jthreadData = new JavaThreadData(num_threads++, this, thread, jvmti, jni);
			CheckJVMTIError(jvmti->SetThreadLocalStorage(thread, jthreadData));
			threads.push_back(jthreadData);
			//set up the log file
			if (!statelocation.empty() && filesystem::is_directory(statelocation)) {
				//create a file for the new location.
				filesystem::path p(statelocation);
				filesystem::path threadpath = p / str(format("%d.trace") % jthreadData->GetID());
				jthreadData->SetLog(threadpath.string().c_str());
			}
			if (paused) {
				jthreadData->Pause(jvmti, GetJNI());
			} else {
				jthreadData->Resume(jvmti, GetJNI());
			}
		}
	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error: " << ex << " Agent thread start";
		exit(ex);
	} catch (std::bad_alloc &e) {
		std::cerr << "Error in starting thread: allocation error" << std::endl;
		exit(-1);
	}
	DEBUG_PRINT("Started thread " << thread << "on agent at " << hex << (intptr_t)this);

	return jthreadData;


}

void JVMTIAgent::ThreadExited(jvmtiEnv *jvmti, JNIEnv *env, jthread thread) {
	JavaThreadData* jthreadData;
	unique_lock<shared_mutex> localLock(mutex);
	try {
		CheckJVMTIError(jvmti->GetThreadLocalStorage(thread, (void**)&jthreadData));
		if (jthreadData != NULL) {
			//remove it from the vector
			vector<JavaThreadData*>::iterator it;
			for (it = threads.begin(); it != threads.end(); it++) {
				JavaThreadData* ptr = *it;
				if (ptr->GetID() == jthreadData->GetID()) {
					threads.erase(it);
					break;
				}
			}
			CheckJVMTIError(jvmti->SetThreadLocalStorage(thread, NULL));
			jthreadData->ThreadDeath(jvmti, env);
			delete jthreadData;
		}
	} catch (jvmtiError ex) {
		std::cerr << "JVMTI Error: " << ex << " Agent thread exit";

	}
}

void JVMTIAgent::ClassPrepared(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jclass clazz) {
	//delegate the class preparation event to the thread, as we will have to shut off recursive calls,
	//which may result in us losing events from other threads.
	//JavaThreadData* jthreadData = GetThread(jvmti, thread);
	//if (jthreadData != NULL) {
	//	jthreadData->ClassPrepared(jvmti, env, clazz);
	//}
	if (IsFiltering()) {
		jint count;
		jmethodID *methods;
		jvmtiError err = jvmti->GetClassMethods(clazz, &count, &methods);
		if (err != JVMTI_ERROR_NONE) {
			//do nothing.
			return;
		}
		char* class_sig;
		try {
			CheckJVMTIError(jvmti->GetClassSignature(clazz, &class_sig, NULL));
			if (IsIncluded(class_sig) && !IsExcluded(class_sig)) {
				DEBUG_PRINT("+");
					AddInterestingClass(jvmti, clazz);
			} else {
				DEBUG_PRINT("-");
			}
			jvmti->Deallocate((unsigned char*)class_sig);
		} catch (jvmtiError &err) {
			//do nothing.
		}
		jvmti->Deallocate((unsigned char *)methods);
	}

	//check for JUnit classes
	char* signature = NULL;
	if (!jvmti->GetClassSignature(clazz, &signature, NULL)) {

		if (signature) {
			string junit3 = "Ljunit/framework/TestCase;";
			string junit4 = "Lorg/junit/Test;";
			if (junit3 == signature) {
				this->junit3TestCaseClass = (jclass)env->NewWeakGlobalRef(clazz);
				std::cerr << "found junit3" << endl;
			} else if (junit4 == signature) {
				this->junit4TestClass = (jclass)env->NewWeakGlobalRef(clazz);
				std::cerr << "found junit4" << endl;
			}
		}
		jvmti->Deallocate((unsigned char*)signature);
	}
}


JavaThreadData* JVMTIAgent::GetThread(jvmtiEnv* jvmti, jthread thread) {
	JavaThreadData* jthreadData = NULL;
	try {
		jvmtiPhase phase;
		CheckJVMTIError(jvmti->GetPhase(&phase));
		if (!IsVMInited() || phase != JVMTI_PHASE_LIVE) {
			return NULL;
		}
		CheckJVMTIError(jvmti->GetThreadLocalStorage(thread, (void**)&jthreadData));
	} catch (jvmtiError) { }
	return jthreadData;
}


void JVMTIAgent::StartTrace() {
	unique_lock<shared_mutex> lock(mutex);
	start = true;
//	paused = false;
}

bool JVMTIAgent::IsStarted() {
	shared_lock<shared_mutex> lock(mutex);
	return start;
}

bool JVMTIAgent::IsFiltering() {
	return (is_filtering && !is_junit);
}

shared_ptr<regex> GetRegEX(string filter) throw (std::exception) {
	string exp = filter;
	string::iterator it = exp.begin();
	while (it < exp.end()) {
		char c = *it;
		if (c==0) {
			//sanity-check: 0 terminated string
			break;
		}
		switch (c) {
		case '.':
			exp.replace(it, it+1, "/");
			it += 1;
			break;
		case '*':
			exp.replace(it, it+1, ".*?");
			it += 3;
			break;
		default:
			it++;
		}
	}
	exp.insert(0, "(L)?");
	DEBUG_PRINT("Created regex " << exp << std::endl);
	shared_ptr<regex> rexp(new regex(exp));
	return rexp;
}

void JVMTIAgent::SetFilter(string filter, bool exclusion) {
	unique_lock<shared_mutex> lock(mutex);
	if (filter.length() > 0) {
		try {
			shared_ptr<regex> exp = GetRegEX(filter);
			is_filtering = true;
			if (exclusion) {
				exclusion_filters.insert(exp);
			} else {
				inclusion_filters.insert(exp);
			}
		} catch (std::exception &e) {
			std::cerr << e.what();
		}
	}
}

bool CheckFilters(const char* class_name, unordered_set<shared_ptr<regex> >& filters) {
	unordered_set<shared_ptr<regex> >::iterator it;
	for (it = filters.begin();
			it != filters.end(); ++it) {
		shared_ptr<regex> rex= *it;
		if (regex_match(class_name, (*rex))) {
			return true;
		}
	}
	return false;
}

bool JVMTIAgent::IsIncluded(const char* class_name) {
	if (inclusion_filters.size() == 0) {
		return exclusion_filters.size() == 0;
	}
	return CheckFilters(class_name, inclusion_filters);

}

bool JVMTIAgent::IsExcluded(const char* class_name) {
	unordered_set<string>::iterator it;
	return CheckFilters(class_name, exclusion_filters);
}

void JVMTIAgent::AddInterestingClass(jvmtiEnv *jvmti, jclass clazz) {
	try {
			//don't block on this portion in order to speed up
			//processing as much as possible.
			char * clazzname = NULL;
			CheckJVMTIError(jvmti->GetClassSignature(clazz, &clazzname, NULL));
			boost::hash<std::string> hasher;
			string name(clazzname);
			/*critical section */ {
				unique_lock<shared_mutex> lock(filter_mutex);
				valid_classes.insert(hasher(name));
			}
			CheckJVMTIError(jvmti->Deallocate((unsigned char*)clazzname));


		} catch (jvmtiError& err) {
		}
}

bool JVMTIAgent::PassesFilter(jvmtiEnv* jvmti, jmethodID methodID) {

	try {
		//don't block on this portion in order to speed up
		//processing as much as possible.
		jclass clazz;
		char * clazzname;
		CheckJVMTIError(jvmti->GetMethodDeclaringClass(methodID, &clazz));
		CheckJVMTIError(jvmti->GetClassSignature(clazz, &clazzname, NULL));


		string name(clazzname);
		hash<std::string> hasher;
		/*critical section */ {
			shared_lock<shared_mutex> lock(filter_mutex);
			if (valid_classes.find(hasher(name)) != valid_classes.end()) {
				return true;
			}
		}
		CheckJVMTIError(jvmti->Deallocate((unsigned char*)clazzname));


	} catch (jvmtiError& err) {
	}
	return false;
}

void JVMTIAgent::PauseAgent() {
	//lock it so that we can pause
	unique_lock<shared_mutex> lock(mutex);
	paused = true;
	//tell all of the threads to pause
	if (IsVMInited()) {
		jthread* threads;
		jint thread_count;
		jvmtiEnv* jvmti = GetJVMTI();
		JNIEnv* env = GetJNI();
		try {
			CheckJVMTIError(jvmti->GetAllThreads(&thread_count, &threads));
			for (int i = 0; i < thread_count; i++) {

				JavaThreadData* ptr = GetThread(jvmti, threads[i]);
				if (ptr != NULL) {
					//note, to try and have compatibility with different JVMS
					//if the thread is not already paused, we have to wait
					ptr->Pause(jvmti, env);
				}
				//no more need for the local reference, free it
				env->DeleteLocalRef(threads[i]);

			}
			jvmti->Deallocate((unsigned char*)threads);
		} catch (jvmtiError ex) {
			std::cerr << "JVMTI Error " << ex << ": while pausing agent." << std::endl;
		}
		//	vector<JavaThreadData*>::iterator threadIterator;
		//	for (threadIterator = threads.begin(); threadIterator != threads.end(); threadIterator++) {
		//		JavaThreadData* ptr = *(threadIterator);
		//		if (ptr) {
		//			ptr->Pause(jvmti);
		//		}
		//	}

	}


}

void JVMTIAgent::ResumeAgent() {
	//tell all of the threads to resume
	unique_lock<shared_mutex> lock(mutex);
	jthread* threads;
	jint thread_count;
	jvmtiEnv* jvmti = GetJVMTI();
	JNIEnv* env = GetJNI();
	jvmti->GetAllThreads(&thread_count, &threads);
	for (int i = 0; i < thread_count; i++) {
		JavaThreadData* ptr = GetThread(jvmti, threads[i]);
		if (ptr != NULL) {
			ptr->Resume(jvmti, env);
		}
	}
	jvmti->Deallocate((unsigned char*)threads);

//	vector<JavaThreadData*>::iterator threadIterator;
//	for (threadIterator = threads.begin(); threadIterator != threads.end(); threadIterator++) {
//		JavaThreadData* ptr = *(threadIterator);
//		if (ptr) {
//			ptr->Resume(jvmti);
//		}
//	}
	//lock it so that we can pause
	paused = false;
}

void JVMTIAgent::VMStart(jvmtiEnv *jvmti, JNIEnv *env) {
	unique_lock<shared_mutex> lock(mutex);
	vm_is_dead = false;
	vm_is_started = true;
}

void JVMTIAgent::VMDeath(jvmtiEnv *jvmti, JNIEnv *env) {
	unique_lock<shared_mutex> lock(mutex);
	vm_is_dead = true;
}

void JVMTIAgent::Unload() {
	unique_lock<shared_mutex> lock(mutex);
	//kill all of the remaining threads
	vector<JavaThreadData*>::iterator it;
	for (it = threads.begin(); it != threads.end(); it++) {
		(*it)->ThreadDeath(GetJVMTI(), GetJNI());
		delete (*it);
	}
	threads.clear();
	//unload junit stuff
	JNIEnv* env = GetJNI();
	if (env) {
		if (junit3TestCaseClass != NULL) {
			env->DeleteWeakGlobalRef(junit3TestCaseClass);
		}
		if (junit4TestClass != NULL) {
			env->DeleteWeakGlobalRef(junit4TestClass);
		}
	}


}

jvmtiEnv * JVMTIAgent::GetJVMTI() throw (jvmtiError) {
//	thread::id current = this_thread::get_id();
//	if (current == native_thread) {
//		if (jvmti == NULL) {
//			throw (JVMTIException(JVMTI_ERROR_NULL_POINTER));
//		} else {
//			return jvmti;
//		}
//	}
	return jvmti;
}

JNIEnv* JVMTIAgent::GetJNI() throw (jvmtiError) {
	JNIEnv* env = NULL;
	jint result = vm->GetEnv((void**)&env, JNI_VERSION_1_6);
	if (result != JNI_OK) {
		throw JVMTI_ERROR_ABSENT_INFORMATION;
	}
	return env;
}

unsigned long JVMTIAgent::TimeFromStart() {
#ifdef WIN32
	long current_time = GetTickCount();
	return current_time - start_millis;
#else
	posix_time::ptime current_time = posix_time::microsec_clock::local_time();
	posix_time::time_duration td = current_time - start_time;
	return td.total_milliseconds();
#endif
}

/*
JNIEnv * JVMTIAgent::GetJNI() throw (JVMTIException) {
	thread::id current = this_thread::get_id();
	DEBUG_PRINT("Getting JNI");
	if (current == native_thread) {
		if (jni == NULL) {
			throw (JVMTIException(JVMTI_ERROR_NULL_POINTER));
		} else {
			return jni;
		}
	}
	JNIEnv *env;
	jint result = vm->GetEnv((void **)&env, JNI_VERSION_1_4);
	if (result != JNI_OK) {
		throw (JVMTIException(JVMTI_ERROR_ABSENT_INFORMATION));
	}
	return env;
}
*/


bool JVMTIAgent::IsJUnitMethod(jvmtiEnv* jvmti, JNIEnv* env, jmethodID method)  {
	bool isJunitMethod = false;
	try {
		//if (cachedStack.size() > 0) {
		//get the top one on the stack, and see if it is a test
		/*
	shared_ptr<CachedMethodEvent> cached = cachedStack.back();
		shared_ptr<MethodEvent> event = cached->event;
		string methodName = event->method_name;
		if (methodName.length() > 4) {
			string testStr = methodName.substr(0, 4);
			//std::cout << testStr << std::endl;
			if(testStr == "test") {
				//std::cerr << event->class_name << event->method_name << " is a test" << std::endl;
				isJunitMethod = true;
			}
			//std::cerr << event->class_name << event->method_name << " is a not test" << std::endl;
		}
		 */
		//check JUnit 3 first
		jclass clazz = NULL;
		jvmtiError err = jvmti->GetMethodDeclaringClass(method, &clazz);
		CheckJVMTIError(err);

		if (junit3TestCaseClass != NULL) {
			bool assignable = env->IsAssignableFrom(clazz, junit3TestCaseClass);
			if (env->ExceptionCheck()) {
			} else 	if (assignable) {
				//this is a unit test, check the method name.
				char* methodNameChars = NULL;
				jvmti->GetMethodName(method, &methodNameChars, NULL, NULL);
				string methodName = methodNameChars;
				jvmti->Deallocate((unsigned char*)methodNameChars);
				string testStr = methodName.substr(0, 4);
				if(testStr == "test") {
					//std::cerr << event->class_name << event->method_name << " is a test" << std::endl;
					isJunitMethod = true;
				}
			}
		}

		if (!isJunitMethod && junit4TestClass != NULL) {
			//check for JUnit 4 annotations.
			jobject methodObj = env->ToReflectedMethod(clazz, method, false);
			if (env->ExceptionCheck()) {
				env->ExceptionDescribe();
				env->ExceptionClear();
			} else if (methodObj) {
				jclass methodClass = env->GetObjectClass(methodObj);
				if (env->ExceptionCheck()) {
					env->ExceptionDescribe();
					env->ExceptionClear();
				} else if (methodClass) {

					jmethodID getAnnotation = env->GetMethodID(methodClass, "getAnnotation", "(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;");
					if (env->ExceptionCheck()) {
						env->ExceptionDescribe();
						env->ExceptionClear();
					} else if (getAnnotation) {
						jobject annotation = env->CallObjectMethod(methodObj, getAnnotation, junit4TestClass);
						if (env->ExceptionCheck()) {
							env->ExceptionDescribe();
							env->ExceptionClear();
						} else if (annotation) {

							env->DeleteLocalRef(annotation);
							isJunitMethod = true;

						}

					}

					env->DeleteLocalRef(methodClass);
				}

				env->DeleteLocalRef(methodObj);

			}

		}
		env->DeleteLocalRef(clazz);
	} catch (jvmtiError err) {
		std::cerr << "JVMTI Error " << err << std::endl;
	}
	return isJunitMethod;
}
}
