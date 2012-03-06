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
 * DataFormat.h
 *
 *  Created on: 2009-08-17
 *      Author: Del Myers
 */

#ifndef DATAFORMAT_H_
#define DATAFORMAT_H_

namespace oasis {
class DataFormat {
public:
	DataFormat();
	virtual ~DataFormat();
	short BigEndian(short s);
	long BigEndian(long l);
	short LittleEndian(short s);
	long LittleEndian(long l);
	bool IsBigEndian();

private:
	short (*BigEndianShortDelegate)(short s);
	short (*LittleEndianShortDelegate)(short s);
	long (*BigEndianLongDelegate)(long l);
	long (*LittleEndianLongDelegate)(long l);
	bool big_endian;
};
}
#endif /* DATAFORMAT_H_ */
