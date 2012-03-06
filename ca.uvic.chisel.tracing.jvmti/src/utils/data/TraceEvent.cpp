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
 * TraceEvent.cpp
 *
 *  Created on: 2009-08-14
 *      Author: Del Myers
 */

#include "utils/data/TraceEvent.hpp"

namespace oasis {
using namespace std;

static DataFormat formatter;


void WriteShort(short s, ofstream& stream) {
	short w = formatter.BigEndian(s);
	stream.write(reinterpret_cast<char*>(&w), sizeof(short));
}

void WriteLong(long l, ofstream& stream) {
	long w = formatter.BigEndian(l);
	stream.write(reinterpret_cast<char*>(&w), sizeof(long));
}

void TraceEvent::write(ofstream& stream) {
	if (stream.is_open()) {
		WriteShort(type, stream);
	}
}

void HeaderEvent::write(ofstream& stream) {
	if (stream.is_open()) {
		TraceEvent::write(stream);
		WriteShort(version_number, stream);
		short length = (short)strlen(identifier_text);
		WriteShort(length, stream);
		stream.write(identifier_text, length);
		stream.flush();
	}
}


void TimedEvent::write(ofstream& stream) {
	if (stream.is_open()) {
		TraceEvent::write(stream);
		WriteLong(time, stream);
	}
}



void ThreadInit::write(ofstream& stream) {
	TimedEvent::write(stream);
	if (stream.is_open()) {
		WriteShort((short)thread_id, stream);
		WriteShort((short)thread_name_len, stream);
		if (thread_name_len>0) {
			stream.write(thread_name, thread_name_len);
		}
		stream.flush();
	}
}

void PauseResumeEvent::write(ofstream& stream) {
	TimedEvent::write(stream);
	if (stream.is_open()) {
		WriteShort(stack_trace_len, stream);
		if (stack_trace_len > 0) {
			stream.write(stack_trace, stack_trace_len);
		}
		stream.flush();
	}

}

void MethodEvent::write(ofstream& stream) {
	TimedEvent::write(stream);
	if (stream.is_open()) {
		WriteLong(line_number, stream);
		WriteShort(class_name_len, stream);
		if (class_name_len > 0) {
			stream.write(class_name, class_name_len);
		}
		WriteShort(method_name_len, stream);
		if (method_name_len > 0) {
			stream.write(method_name, method_name_len);
		}
		WriteShort(method_sig_len, stream);
		if (method_sig_len > 0) {
			stream.write(method_sig, method_sig_len);
		}
	}
}

void MethodEnter::write(ofstream& stream) {
	MethodEvent::write(stream);
	if (stream.is_open()) {
		WriteLong(modifiers, stream);
		WriteShort(variables_len, stream);
		if (variables_len > 0) {
			Variable** variable = variables;
			for (int i = 0; i < variables_len; i++, variable++) {
				WriteShort((*variable)->value_len, stream);
				if ((*variable)->value_len > 0) {
					stream.write((*variable)->value, (*variable)->value_len);
				}
			}
		}
	}

}


void MethodExit::write(ofstream& stream) {
	MethodEvent::write(stream);
	if (stream.is_open()) {
		WriteShort(is_exception, stream);
		WriteShort(return_value_len, stream);
		if (return_value_len > 0) {
			stream.write(return_value, return_value_len);
		}
	}
}
}
