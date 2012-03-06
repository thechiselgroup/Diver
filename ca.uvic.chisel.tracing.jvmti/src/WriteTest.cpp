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
 * WriteTest.cpp
 *
 *  Created on: 2009-08-17
 *      Author: Del Myers
 */
#include <fstream>
#include <iostream>
#include "stdafx.h"
#include "utils/data/TraceEvent.hpp"

using namespace std;
using namespace oasis;
#ifdef WIN32

int WinMain(HINSTANCE,HINSTANCE,LPSTR,int)
{
	ofstream output("c:\\test.dat", ios::out | ios::binary);

	ThreadInit thinit;

	thinit.thread_name = "Thread 1";
	thinit.thread_name_len = strlen(thinit.thread_name);
	thinit.time = 28237232;
	thinit.thread_id = 5;

	thinit.write(output);


	MethodEnter enter;

	enter.class_name ="Test1";
	enter.class_name_len = strlen(enter.class_name);
	enter.line_number = 5;
	enter.method_name = "TestMethod";
	enter.method_name_len = strlen(enter.method_name);
	enter.modifiers = 0;
	enter.time = 10;
	enter.variables_len = 0;

	enter.write(output);


	MethodExit exit;

	exit.class_name = "ExitClassName";
	exit.class_name_len = strlen(exit.class_name);
	exit.line_number=-3;
	exit.method_name = "ExitMethodName";
	exit.method_name_len = strlen(exit.method_name);
	exit.method_sig = "(Lsjfdi.sdfifj.jjj;)V";
	exit.method_sig_len = strlen(exit.method_sig);
	exit.time = 34949;
	exit.is_exception=0;
	exit.write(output);

	output.close();
	return 0;


}
#endif

