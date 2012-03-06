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
 * Command.cpp
 *
 *  Created on: 3-Mar-2009
 *      Author: Del Myers
 */

#include "communication/Command.hpp"
#include "integers.h"
namespace oasis {
Command::Command(__int8 c, __uint16 l, array_8 d) :
	command(c),
	length(l),
	data(d) {
}

Command::~Command() {
}

__int8 Command::GetCommand() {
	return command;
}

__uint16 Command::GetLength() {
	return length;
}

array_8 Command::GetData() {
	return data;
}
}
