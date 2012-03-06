/*
 * TraceEvent.hpp
 *
 *  Created on: 2009-08-13
 *      Author: Del Myers
 */

#ifndef TRACEEVENT_HPP_
#define TRACEEVENT_HPP_

#include "stdafx.h"
#include "utils/data/DataFormat.h"
#include "integers.h"


namespace oasis {
using namespace std;
const __uint16 VERSION_NUMBER = 0;

const __uint16 NONE = 0;
const __uint16 METHOD_ENTERED = 1;
const __uint16 METHOD_EXITED = 2;
const __uint16 PAUSED = 3;
const __uint16 RESUMED = 4;
const __uint16 THREAD_INIT = 5;
const __uint16 HEADER_EVENT = 6;


struct Variable {
	__uint16 value_len;
	__int8 *value;
};

class TraceEvent {
public:
	TraceEvent() : type(NONE) {}
	virtual ~TraceEvent() {};
	__uint16 type;
	virtual void write(ofstream& stream);
};

class HeaderEvent : public TraceEvent {
public:
	HeaderEvent() : TraceEvent(),
	version_number(VERSION_NUMBER),
	identifier_text("OASIS Java Sketch Data Version 0") {
		type = HEADER_EVENT;
	}
	virtual ~HeaderEvent() {};
	__uint16 version_number;
	char* identifier_text;
	virtual void write(ofstream& stream);
};


class TimedEvent : public TraceEvent {
public:
	TimedEvent() : TraceEvent(), time(0) {}
	virtual ~TimedEvent() {}
	__uint32 time;
	virtual void write(ofstream& stream);
};

class ThreadInit : public TimedEvent {
public:
	ThreadInit() : TimedEvent(),
	thread_id(0),
	thread_name_len(0),
	thread_name(NULL){
		type = THREAD_INIT;
	}
	__uint16 thread_id;
	__uint16 thread_name_len;
	__int8* thread_name;
	virtual void write(ofstream& stream);


	virtual void debug_print() {
//		char name[thread_name_len+1];
//		if (thread_name_len>0 && thread_name != NULL) {
//			memcpy(name, thread_name, thread_name_len);
//		}
//		name[thread_name_len] = 0;
	}

};

class PauseResumeEvent : public TimedEvent {
public:
	PauseResumeEvent() : TimedEvent(){}
	__uint16 stack_trace_len;
	__int8 *stack_trace;
	virtual void write(ofstream& stream);

};

class PauseEvent : public PauseResumeEvent {
public:
	PauseEvent() : PauseResumeEvent()
	{
		type = PAUSED;
	}
};

class ResumeEvent : public PauseResumeEvent {
public:
	ResumeEvent() : PauseResumeEvent()
	{
		type = RESUMED;
	}
};

class MethodEvent : public TimedEvent {
public:
	MethodEvent() : TimedEvent(),
		line_number(-1),
		class_name_len(0),
		class_name(NULL),
		method_name_len(0),
		method_name(NULL),
		method_sig_len(0),
		method_sig(NULL){
	}

	/* method enter/exit data */
	__int32 line_number;
	__uint16 class_name_len;
	__int8* class_name;
	__uint16 method_name_len;
	__int8* method_name;
	__uint16 method_sig_len;
	__int8* method_sig;
	virtual void write(ofstream& stream);

};

class MethodEnter : public MethodEvent {
public:
	MethodEnter() : MethodEvent(),
	modifiers(0),
	variables_len(0),
	variables(NULL){
		type = METHOD_ENTERED;
	}
	/* method enter */
	__int32 modifiers;
	__uint16 variables_len;
	Variable** variables;
	virtual void write(ofstream& stream);

};

class MethodExit : public MethodEvent {
public:
	MethodExit() : MethodEvent(),
		is_exception(0),
		return_value_len(0),
		return_value(NULL){
		type = METHOD_EXITED;
	}

	/* method exit */
	__int8 is_exception;
	__uint16 return_value_len;
	__int8 *return_value;

	virtual void write(ofstream& stream);
};
}

#endif /* TRACEEVENT_HPP_ */
