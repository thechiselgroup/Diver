/*
 * JVMTIErrors.h
 *
 *  Created on: 11-Mar-2009
 *      Author: Del Myers
 */

#ifndef JVMTIERRORS_H_
#define JVMTIERRORS_H_
#include <iostream>
#include <jvmti.h>


namespace oasis {
using namespace std;

/**
 * A class for the exceptions that may be thrown
 * from within the server.
 */
//class JVMTIException : public std::exception {
//private:
//	jvmtiError error;
//	char *info;
//public:
//	JVMTIException(jvmtiError error) :
//		error(error),
//		info(""){}
//	/**
//	 * Creates a JVMTI exception with the given additional
//	 * info. Note: the info pointer MUST NOT GO OUT OF SCOPE.
//	 * info should point to an immutable string, i.e. clients
//	 * should pass either strings allocated in code-space
//	 * (i.e. pass a string directly), or strings declared
//	 * as global statics. Strings allocated on the stack or
//	 * heap should NEVER be used.
//	 * @param error the error number
//	 * @param info additional info for the error
//	 */
//	JVMTIException(jvmtiError error, const char * info) :
//		error(error),
//		info((char *)info){}
//	virtual const char* what() const throw() {
//		return "JVMTI Error";
//	};
//	/**
//	 * Copy constructor for safe throwing.
//	 * @param ex
//	 * @return
//	 */
//	JVMTIException(const JVMTIException &ex) {
//		error = ex.error;
//	}
//	virtual ~JVMTIException() throw() {}
//
//	jvmtiError GetError() throw() {
//		return error;
//	}
//
//	char* GetInfo() {
//		return info;
//	}
//};


void CheckJVMTIError(jvmtiError error) throw (jvmtiError);

	/**
	 * Checks the given error number and throws an exception
	 * if it is in error. Clients may pass additional
	 * info. Note: the info pointer MUST NOT GO OUT OF SCOPE.
	 * info should point to an immutable string, i.e. clients
	 * should pass either strings allocated in code-space
	 * (i.e. pass a string directly), or strings declared
	 * as global statics. Strings allocated on the stack or
	 * heap should NEVER be used.
	 * @param error the error number
	 * @param info additional info for the error
	 */
//void CheckJVMTIError(jvmtiError error, const char* info) throw (JVMTIException);
} /*end namespace oasis */

#endif /* JVMTIERRORS_H_ */
