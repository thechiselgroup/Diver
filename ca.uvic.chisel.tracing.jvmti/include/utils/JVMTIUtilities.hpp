/*
 * AgentData.h
 *
 *  Created on: 4-Mar-2009
 *      Author: Del Myers
 */

#ifndef JVMTIUTILITIES_H_
#define JVMTIUTILITIES_H_
#include <iostream>
#include <fstream>
#include <vector>
#include <locale>
#include <set>


#include <boost/unordered_set.hpp>
#include <boost/unordered_map.hpp>
#include <boost/regex.hpp>
#include <time.h>
#include <string>

#ifdef WIN32
#define WIN32_LEAN_AND_MEAN             // Exclude rarely-used stuff from Windows headers
// Windows Header Files:
#include <windows.h>
#endif
#include "utils/MethodSignature.hpp"
#include "utils/JVMTIErrors.hpp"
#include "utils/data/TraceEvent.hpp"
#include "jni.h"
#include "jvmti.h"

//Boost.Thread needs to be referenced last. Problem from 64bit build.
#include <boost/smart_ptr.hpp>
#include <boost/thread/thread.hpp>
#include <boost/thread/shared_mutex.hpp>
#include <boost/thread/recursive_mutex.hpp>
#include <boost/thread/locks.hpp>


namespace oasis {
using namespace std;
class JVMTIAgent;

class CachedMethodEvent {
public:
	CachedMethodEvent(boost::shared_ptr<MethodEnter> event);
	virtual ~CachedMethodEvent() {};
	void Trigger();
	void Mark();
	bool IsMarked();
	bool IsTriggered();
	void Flush(ofstream &stream);
	boost::shared_ptr<MethodEvent> event;
private:
	bool triggered;
	bool marked;

};

class JavaThreadData {
public:
    JavaThreadData(unsigned int local_id, JVMTIAgent *agent, jthread localThreadReference, jvmtiEnv *jvmti, JNIEnv *jni);
    virtual ~JavaThreadData();
    ostream *GetStream();
    void SetLog(const char *filename);
    void CloseLog();
    void MethodEntered(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID);
    void MethodExited(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID, jboolean byException, jvalue returnValue);
    void ExceptionThrown(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID, jobject exception, jlocation location, jmethodID catchMethod, jlocation catchLocation);
    void ExceptionCaught(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID, jobject exception, jlocation location);
    void ClassPrepared(jvmtiEnv *jvmti, JNIEnv *env, jclass clazz);
    string GetThreadName()
    {
        return name;
    }

    unsigned int GetID()
    {
        return local_id;
    }

    void Pause(jvmtiEnv *jvmti, JNIEnv *env);
    void Resume(jvmtiEnv *jvmti, JNIEnv *env);
    void ThreadDeath(jvmtiEnv *jvmti, JNIEnv *env);
    friend class JVMTIAgent;
private:
    void RebuildStack(jvmtiEnv *jvmti, JNIEnv *env) throw (jvmtiError);
    void DoFilteredMethodEnter(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID, int stackDepth) throw (jvmtiError);
    unsigned int local_id;
    JVMTIAgent *agent;
    ofstream logfile;
    boost::thread::id native_thread;
    string name;
    unsigned long long call_num;
    boost::recursive_mutex mutex;
    bool paused;
    bool pending_resume;
    bool pending_pause;
    bool is_inited;
    jthread thread;
    boost::unordered_map<jmethodID,MethodSignature*> methodSignatures;
    std::list<boost::shared_ptr<CachedMethodEvent> > cachedStack;
    bool thread_recursing;
    unsigned long thread_recursion_count;
    jmethodID junitMethod;
    bool runningTest;
    void WriteMethodEvent(jvmtiEnv *jvmti, JNIEnv *env, jmethodID methodID, unsigned long  time);
    void PrintStackTrace(jvmtiEnv *jvmti, JNIEnv *env, ostream & output) throw (jvmtiError);
    MethodSignature *GetMethodSignature(jvmtiEnv *jvmti, JNIEnv *jni, jmethodID);
    Variable **GetLocalVariables(jvmtiEnv *jvmti, JNIEnv *jni, jmethodID methodID, int & variable_count) throw (jvmtiError);
    void FreeLocalVariables(Variable **variables, int variable_count);
    string PrependBundleName(jvmtiEnv *jvmti, JNIEnv *env, jclass cp);
    void Reset(jvmtiEnv *jvmti, JNIEnv *env);
    //void PushJUnitStack(boost::shared_ptr<MethodEnter> methodEnter);
    //void PopJUnitStack();
};

typedef boost::shared_ptr<JavaThreadData> JavaThreadDataPtr;

class JVMTIAgent {
public:
	/**
	 * Creates single point for
	 * @param logname
	 * @return
	 */
	JVMTIAgent(JavaVM *vm) throw (jvmtiError);
	virtual ~JVMTIAgent();

	JavaVM* GetJVM() { return vm; };

	void Init(jvmtiEnv* env, JNIEnv* jni);

	/**
	* Sets the state location to the given directory. The directory will
	* hold files for the general log, as well as logs for each thread. The
	* logs for the threads will be identified by the name [thread_num].log.
	* The log for the agent will be called agent.log. The state location must
	* exist, or else all the logs will be set to standard output.
	*
	* @param location a file-system directory. Must exist.
	*/
	void SetStateLocation(const char* location);
	/**
	 * Closes the log file and sets the logging to standard out.
	 */
	void CloseLog();

	//redirects for the agent call-backs. All logging is handled here.
	void MethodEntered(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID methodID);
	void MethodExited(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID methodID, jboolean byException, jvalue returnValue);
	void ExceptionThrown(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID methodID, jobject exception, jlocation location, jmethodID catchMethod, jlocation catchLocation);
	void ExceptionCaught(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jmethodID methodID, jobject exception, jlocation location);
	JavaThreadData* ThreadStarted(jthread thread, jvmtiEnv* jvmti, JNIEnv* jni);
	void ThreadExited(jvmtiEnv *jvmti, JNIEnv *env, jthread thread);
	void ClassPrepared(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jclass clazz);
	int GetNumThreads();
	void StartTrace();
	bool IsStarted();

	bool IsFiltering();
	bool PassesFilter(jvmtiEnv* jvmti, jmethodID methodID);
	void PauseAgent();
	void ResumeAgent();
	void VMDeath(jvmtiEnv *jvmti, JNIEnv *env);
	void VMStart(jvmtiEnv *jvmti, JNIEnv *env);
	void Unload();
	jvmtiEnv *GetJVMTI() throw (jvmtiError);
	JNIEnv *GetJNI() throw (jvmtiError);
	bool IsVMInited() {return vm_is_inited;}
	bool IsVMDead() {return vm_is_dead;}
	bool IsVMStarted() {return vm_is_started;}
	bool IsStoringVariables() { return false; }
	bool IsStoringBundles() { return is_storing_bundles; }
	/**
	 *
	 * @return the number of milliseconds since the agent was initialized.
	 */
	unsigned long TimeFromStart();
	void SetJUnit(bool junit) {is_junit = junit;}
	bool IsJUnit() {return is_junit;}
	 bool IsJUnitMethod(jvmtiEnv* jvmti, JNIEnv* env, jmethodID method);

	//so that it can access deallocate
	friend class JavaThreadData;
protected:
	void SetFilter(string filter, bool exclusion);
	void AddInterestingClass(jvmtiEnv *jvmti, jclass clazz);
	bool IsIncluded(const char* class_name);
	bool IsExcluded(const char* class_name);

	JavaThreadData* GetThread(jvmtiEnv* jvmti, jthread thread);
	/**
	 * Deallocates the given data in the jvmti interface.
	 * @param data the data to deallocate
	 */
	void Deallocate(jvmtiEnv *jvmti, void *data);
	/**
	 * Mutex for locking important data.
	 */
	boost::shared_mutex mutex;
private:
	JavaVM			*vm;
	bool paused;
	unsigned int num_threads;
	string statelocation;
	ofstream logfile;
	ostream *log;
	boost::thread::id native_thread;
	//the time that the agent was initialized.
	time_t local_start_time;
#ifdef WIN32
	//the windows time at which the agent was started.
	unsigned long start_millis;
#else
	//the generic time at which the agent was started.
	boost::posix_time::ptime start_time;
#endif
	jvmtiEnv*		jvmti;
	/* JVMTI Environment */
	jboolean		vm_is_dead;
	jboolean		vm_is_started;
	jboolean		vm_is_inited;
	/* Options */
	char			*include;
	char			*exclude;
	int				max_count;
	char 			*port;
	bool			start;
	/**
	 * Used to tell whether or not to store the bundle identifier for the class loader (for tracing osgi-enabled platforms)
	 */
	bool			is_storing_bundles;

	std::vector<JavaThreadData*> threads;
	bool is_filtering;
	boost::unordered_set<boost::shared_ptr<boost::regex> > exclusion_filters;
	boost::unordered_set<boost::shared_ptr<boost::regex> > inclusion_filters;
	//stores a hash of each valid class.
	boost::shared_mutex filter_mutex;
	std::set<size_t> valid_classes;
	//indicates that we are running in junit mode. Only trace while in a junit
	//method.
	bool			is_junit;
	jclass			junit3TestCaseClass;
	jclass			junit4TestClass;
	//boost::unordered_set<jmethodID> included_methods;

};

}
#endif /* AGENTDATA_H_ */
