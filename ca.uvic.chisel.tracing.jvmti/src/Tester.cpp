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
#include <boost/asio.hpp>
#include <boost/thread.hpp>
#include <boost/thread/shared_mutex.hpp>
#include <boost/thread/locks.hpp>

#include "jni.h"
#include "jvmti.h"


/** all callbacks must be extern C */
extern "C" {

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
	std::cout << "Agent loaded..." << std::endl;

	return JNI_OK;
}
JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
	std::cout << "Agent unloaded..." << std::endl;
}
}

#ifdef WIN32

int WinMain(HINSTANCE,HINSTANCE,LPSTR,int)
{
	boost::recursive_mutex mutex;
	MessageBox(0,"Hello, Windows","MinGW Test Program",MB_OK);
	boost::lock_guard<boost::recursive_mutex> sharedLock(mutex);
	MessageBox(0,"Shared lock","MinGW Test Program",MB_OK);
	boost::lock_guard<boost::recursive_mutex> uniqueLock(mutex);
	MessageBox(0,"Unique lock","MinGW Test Program",MB_OK);
  //MessageBox(0,"Hello, Windows","MinGW Test Program",MB_OK);
  return 0;
}

#endif
