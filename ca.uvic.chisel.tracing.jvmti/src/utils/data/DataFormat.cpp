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
 * DataFormat.cpp
 *
 *  Created on: 2009-08-17
 *      Author: Del Myers
 */

#include "utils/data/DataFormat.h"

/*
 * Functions used to swap endianness
 */

namespace oasis {
short swap_s(short s) {
	unsigned char b1, b2;
	b1 = s & 255;
	b2 = (s >> 8) & 255;
	return (b1 << 8) + b2;
}

short noswap_s(short s) {
	return s;
}

long swap_l(long l) {
	unsigned char b1, b2, b3, b4;
	b1 = l & 255;
	b2 = ( l >> 8 ) & 255;
	b3 = ( l>>16 ) & 255;
	b4 = ( l>>24 ) & 255;
	return ((int)b1 << 24) + ((int)b2 << 16) + ((int)b3 << 8) + b4;
}

long noswap_l(long l) {
	return l;
}


DataFormat::DataFormat() {
	//set up the function pointers to delegate to the correct swap functions
	char test[2] = {1,0};
	//is the first byte of the short a 1?
	if (*(reinterpret_cast<short*>(test)) == 1) {
		//yes: therefore little endian
		big_endian = false;

		BigEndianShortDelegate = swap_s;
		BigEndianLongDelegate = swap_l;
		LittleEndianShortDelegate = noswap_s;
		LittleEndianLongDelegate = noswap_l;
	} else {
		//no: therefore big endian
		big_endian = true;

		BigEndianShortDelegate = noswap_s;
		BigEndianLongDelegate = noswap_l;
		LittleEndianShortDelegate = swap_s;
		LittleEndianLongDelegate = swap_l;
	}
}

DataFormat::~DataFormat() {
}

bool DataFormat::IsBigEndian() {
	return big_endian;
}

long DataFormat::BigEndian(long l) {
	return BigEndianLongDelegate(l);
}

long DataFormat::LittleEndian(long l) {
	return LittleEndianLongDelegate(l);
}

short DataFormat::BigEndian(short s) {
	return BigEndianShortDelegate(s);
}

short DataFormat::LittleEndian(short s) {
	return LittleEndianShortDelegate(s);
}



}
