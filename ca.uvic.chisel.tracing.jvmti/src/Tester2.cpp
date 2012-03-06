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
 * A test to see if we can get the agent to start.
 * Tester.cpp
 *
 *  Created on: 16-Mar-2009
 *      Author: Del Myers
 */

#include "stdafx.h"
#include <iostream>
#include <sys/types.h>
#include <boost/date_time/posix_time/posix_time.hpp>
#include "jni.h"
#include "jvmti.h"


/** all callbacks must be extern C *
extern "C" {

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
	std::cout << "Agent loaded..." << std::endl;

	return JNI_OK;
}
JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
	std::cout << "Agent unloaded..." << std::endl;
}
}
*/
#ifdef WIN32
using namespace boost::posix_time;
int WinMain(HINSTANCE,HINSTANCE,LPSTR,int)
{
	//ptime time = microsec_clock::local_time();;
	//time = ;
	MessageBox(0,"Hello, Windows","MinGW Test Program",MB_OK);
	//std::cout << "The current time is " << time << std::endl;

  return 0;
}

#endif
