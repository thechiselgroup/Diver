/*
 * MethodSignature.h
 *
 *  Created on: 4-Mar-2009
 *      Author: Del Myers
 */

#ifndef METHODSIGNATURE_H_
#define METHODSIGNATURE_H_
#include <string>
#include <jvmti.h>
#include <jni.h>
#include "utils/JVMTIErrors.hpp"
namespace oasis {
using namespace std;

const char TYPE_VOID = 'V';
const char TYPE_BOOLEAN = 'Z';
const char TYPE_BYTE = 'B';
const char TYPE_CHAR = 'C';
const char TYPE_SHORT = 'S';
const char TYPE_INT = 'I';
const char TYPE_LONG = 'J';
const char TYPE_FLOAT = 'F';
const char TYPE_DOUBLE = 'D';
const char TYPE_OBJECT = 'L';

class MethodSignature {
private:
	/**
	 * The original, raw method signature.
	 */
	string raw_signature;
	/**
	 * the number of parameters in the method signature
	 */
	int num_parameters;
	/**
	 * The name of the method.
	 */
	string method_name;

	string class_name;

	/**
	 * The modifiers for the method. See the is_* functions for more information.
	 */
	int modifiers;

	/**
	 * The return type of the method.
	 */
	char return_type;

	jmethodID methodID;

	jvmtiLineNumberEntry* line_numbers;
	jint entry_count;

	static int CountParameters(const char* signature);
public:
	MethodSignature(jvmtiEnv* jvmti, jmethodID method) throw(jvmtiError);
	virtual ~MethodSignature();
	jint LocateLine(jvmtiEnv* jvmti, jlocation location);

    string GetRawSignature() const
    {
        return raw_signature;
    }

    int GetNumParameters() const
    {
        return num_parameters;
    }

    string GetMethodName() const
    {
        return method_name;
    }

    string& ReferenceRawSignature() {
    	return raw_signature;
    }

    string& ReferenceMethodName() {
    	return method_name;
    }

    string& ReferenceClassName() {
    	return class_name;
    }

    int GetModifiers() const
    {
        return modifiers;
    }

    bool IsPublic() {
    	if ((modifiers & 0x0001) != 0) return true;
    	return false;
    }
    bool IsPrivate() {
    	if ((modifiers & 0x0002) != 0) return true;
    	return false;
    }
    bool IsProtected() {
    	if ((modifiers & 0x0004) != 0) return true;
    	return false;
    }
    bool IsStatic() {
    	if ((modifiers & 0x0008) != 0) return true;
    	return false;
    }

    bool IsNative() {
    	if ((modifiers & 0x0100) != 0) return true;
    	return false;
    }

    char GetReturnValue();

};

}

#endif /* METHODSIGNATURE_H_ */
