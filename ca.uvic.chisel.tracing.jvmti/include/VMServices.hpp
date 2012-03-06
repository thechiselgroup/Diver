#ifndef CJVMTRACER_H
#define CJVMTRACER_H

/* Standard C functions used throughout. */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <stdarg.h>

/* General JVM/Java functions, types and macros. */

#include <sys/types.h>
#include "jni.h"
#include "jvmti.h"


#include "utils/MethodSignature.hpp"
#include "utils/JVMTIUtilities.hpp"
#include "communication/Server.hpp"

namespace oasis {

/**
 * An extension to the JVMTI agent that supports callbacks for network interfacing.
 */
class ServerAgent : public JVMTIAgent, public VServerCallbacks  {
public:
	ServerAgent(JavaVM* vm) throw (jvmtiError);
	virtual ~ServerAgent();
	void CommandReceived(Command &cmd);
protected:
	virtual void InitReceiveThread();
	virtual void EndReceiveThread();
	virtual void InitSendThread();
	virtual void EndSendThread();
private:
	/**
	 * Attach the current thread to the JVM so that we can get the correct
	 * environment.
	 * @throw an error code if the JVM couldn't attach.
	 *
	 */
	void AttachToJVM() throw(int);

	/**
	 * Detach the current thread from the JVM so that the environments are
	 * freed.
	 * @throw an error code if the JVM couldn't detach.
	 */
	void DetachFromJVM() throw (int);
};

}
/* Agent library externals to export. */
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved);
JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm);

#ifdef __cplusplus
}
#endif
#endif
