#include "VMServices.hpp"

namespace oasis {

void ServerAgent::InitReceiveThread() {
	DEBUG_PRINT("Starting server receive thread");
	AttachToJVM();
}
void ServerAgent::EndReceiveThread() {
	DEBUG_PRINT("Ending server receive thread");
	DetachFromJVM();
}
void ServerAgent::InitSendThread() {
	DEBUG_PRINT("Starting server send thread");
	AttachToJVM();
}
void ServerAgent::EndSendThread() {
	DEBUG_PRINT("Ending server send thread");
	DetachFromJVM();
}
void ServerAgent::AttachToJVM() throw(int) {
	DEBUG_PRINT("Attaching current thread to JVM");
	JNIEnv *env;
	JavaVM* jvm = GetJVM();
	jint result = jvm->AttachCurrentThreadAsDaemon((void **)&env, NULL);
	if (result != JNI_OK) {
		throw (result);
	}
}

void ServerAgent::DetachFromJVM() throw(int) {
	JavaVM* jvm = GetJVM();
	jint result = jvm->DetachCurrentThread();
	if (result != JNI_OK) {
		throw (result);
	}
}



ServerAgent::ServerAgent(JavaVM* vm) throw (jvmtiError) : JVMTIAgent(vm), VServerCallbacks() {
	//do nothing.
}
ServerAgent::~ServerAgent() {} //do nothing
void ServerAgent::CommandReceived(Command &cmd) {
	switch (cmd.GetCommand()) {
	case START_COMMAND:
		DEBUG_PRINT("Got Start command");
		if (cmd.GetLength() > 0) {
			char* filename = new char[cmd.GetLength()];
			memcpy(filename, cmd.GetData().get(), cmd.GetLength()*sizeof(char));
			SetStateLocation(filename);
			delete[] filename;
		}
		StartTrace();
		break;
	case FILE_COMMAND:
		DEBUG_PRINT("Got File command");
		if (cmd.GetLength() > 0) {
			char* filename = new char[cmd.GetLength()];
			memcpy(filename, cmd.GetData().get(), cmd.GetLength()*sizeof(char));
			SetStateLocation(filename);
			delete[] filename;
		}
		break;
	case PAUSE_COMMAND:
		DEBUG_PRINT("Got Pause command");
		/* pause tracing command */
		//			(*env)->SetStaticIntField(env, klass, field, 0);
		/* Change the tracing flag */
		//only respond to pause commands if the agent is not in JUnit mode.
		if (!IsJUnit()) {
			PauseAgent();
		}
		break;
	case RESUME_COMMAND:
		DEBUG_PRINT("Got Resume command");
		/* resume tracing command */
		//			(*env)->SetStaticIntField(env, klass, field, 1);
		/* Change the tracing flag */
		if (!IsJUnit()) {
			ResumeAgent();
		}
		break;
	case FILTER_COMMAND:
		//must make sure that the agent hasn't started yet.
		DEBUG_PRINT("Got filter command ");
		if (!IsStarted()) {
			if (cmd.GetLength() > 1) {
				array_8 data = cmd.GetData();
				char * ptr = data.get();
				char is_exclusion = ptr[0];
				char* filter_string = new char[cmd.GetLength()+1];
				memset(filter_string, 0, (cmd.GetLength()+1)*sizeof(char));
				memcpy(filter_string, (char*)(ptr+1), (cmd.GetLength()-1)*sizeof(char));
				string filter = string(filter_string);
				DEBUG_PRINT("Filtering on string " << filter);
				if (is_exclusion) {
					SetFilter(filter, true);
				} else {
					SetFilter(filter, false);
				}
				delete[] filter_string;
			}
		} else {
			DEBUG_PRINT("Cannot filter, host already started");
		}
		break;

	}
}
}
