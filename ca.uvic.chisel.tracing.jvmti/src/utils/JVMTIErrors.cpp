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
#include "utils/JVMTIErrors.hpp"

void oasis::CheckJVMTIError(jvmtiError error) throw (jvmtiError) {
	if (error != JVMTI_ERROR_NONE) {
		throw error;
	}
}

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
//void oasis::CheckJVMTIError(jvmtiError error, const char* info) throw (JVMTIException) {
//	if (error != JVMTI_ERROR_NONE) {
//		std::cerr << "JVMTI ERROR " << info;
//		throw JVMTIException(error, info);
//	}
//}
