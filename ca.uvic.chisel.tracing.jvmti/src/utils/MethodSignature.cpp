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
 * MethodSignature.cpp
 *
 *  Created on: 4-Mar-2009
 *      Author: Del Myers
 */
#include "stdafx.h"
#include "utils/MethodSignature.hpp"
#include "utils/standard_ptrs.hpp"
namespace oasis {

MethodSignature::MethodSignature(jvmtiEnv* jvmti, jmethodID methodID) throw (jvmtiError) :
	methodID(methodID),
	line_numbers(NULL),
	entry_count(0) {
	jvmtiError error;
	jint modifiers;
	char *signature;
	char *name;
	char *className;
	char *generic;
	return_type = -1;
	try {
		error = jvmti->GetMethodName(methodID, &name, &signature, &generic);
		CheckJVMTIError(error);
		//copy the name
		method_name = string(name);
		//copy the raw signature
		raw_signature = string(signature);
		error = jvmti->Deallocate((unsigned char*)name);
		CheckJVMTIError(error);
		jclass methodClass;
		jvmti->GetMethodDeclaringClass(methodID, &methodClass);
		error = jvmti->GetClassSignature(methodClass, &className, NULL);
		CheckJVMTIError(error);
		class_name = string(className);
		error = jvmti->Deallocate((unsigned char*)className);
		CheckJVMTIError(error);
		//parse the signature
		num_parameters = CountParameters(signature);
		error = jvmti->GetMethodModifiers(methodID, &modifiers);
		CheckJVMTIError(error);
		this->modifiers = modifiers;
		error = jvmti->Deallocate((unsigned char*)signature);
		CheckJVMTIError(error);
		jvmtiLineNumberEntry* temp_entries;
		error = jvmti->GetLineNumberTable(methodID, &entry_count, &temp_entries);
		if (error != JVMTI_ERROR_NONE) {
			line_numbers = NULL;
		} else {
			CheckJVMTIError(error);
			line_numbers = new jvmtiLineNumberEntry[entry_count];
			for (int i = 0; i < entry_count; i++) {
				line_numbers[i].line_number = temp_entries[i].line_number;
				line_numbers[i].start_location = temp_entries[i].start_location;
			}
			CheckJVMTIError(jvmti->Deallocate((unsigned char*)temp_entries));
		}


	} catch (jvmtiError ex) {
		std::cerr << "JVMTIError " << ex << " while creating method signature" << std::endl;
		exit(ex);
	}
}

MethodSignature::~MethodSignature() {
	if (line_numbers != NULL) {
		delete[] line_numbers;
	}
}



jint MethodSignature::LocateLine(jvmtiEnv* jvmti, jlocation location) {
	int line_number = -1;
	if (location < 0) {
		return -1;
	}
	try {
		/* get the line number table for the method to retrieve the line numbers */
		if (line_numbers == NULL) {
			return - 1;
		}
		/* run a linear search to find the location... could be sped up */
		//perform a binary search
		int s = 0;
		int e = entry_count-1;
		int l = (e - s)+1;
		int found = 0;
		while (!found && (l > 0)) {
			l = (e - s)+1;
			if (l == 2) {
				//check the two
				if (line_numbers[e].start_location <= location) {
					line_number = line_numbers[e].line_number;
					found = 1;
				} else {
					line_number = line_numbers[s].line_number;
					found = 1;
				}
			} else if (l == 1) {
				line_number = line_numbers[s].line_number;
				found = 1;
			} else {
				int m = s + (l/2);
				if (line_numbers[m].start_location == location) {
					line_number = line_numbers[m].line_number;
					found = 1;
				} else if (line_numbers[m].start_location < location) {
					s = m;
				} else {
					e = m;
				}
			}
		}

//		while (index < entry_count && !entry_found) {
//			jvmtiLineNumberEntry centry = line_numbers[index];
//			if (centry.start_location >= location) {
//				entry_found = 1;
//				if (centry.start_location > location && index > 0) {
//					entry = line_numbers[index-1];
//				} else {
//					entry = centry;
//				}
//			}
//			index++;
//		}
//		if (entry_found) {
//			line_number = entry.line_number;
//		}
		return line_number;
	} catch (jvmtiError ex) {
		std::cerr << "JVMTI ERROR " << ex << ": Unable to retrieve line number"
				<< std::endl;
		return -1;
	}
}


int MethodSignature::CountParameters(const char* signature) {
	char *start = (char *)(signature);
	int read_object = 0;
	int count = 0;
	//go to the end of the string
	while((*start) != ')' && (*start) != 0){
		char c = *start;
		switch (c){
		case TYPE_OBJECT:
			if(!read_object){
				read_object = 1;
				count++;
			}
			break;
		case TYPE_BOOLEAN:
		case TYPE_BYTE:
		case TYPE_CHAR:
		case TYPE_DOUBLE:
		case TYPE_FLOAT:
		case TYPE_INT:
		case TYPE_LONG:
		case TYPE_SHORT:
			if(!read_object){
				count++;
			}
			break;
		case ';':
		read_object = 0;
		break;
		}

		start++;
	}

	return count;
}

char MethodSignature::GetReturnValue() {
	if (return_type < 0) {
		int index = raw_signature.find_last_of(')');
		if (index > 0 && index < (raw_signature.length()-1)) {
			return_type = raw_signature.at(index+1);
			switch(return_type) {
			case TYPE_BOOLEAN: case TYPE_BYTE: case TYPE_CHAR:
			case TYPE_DOUBLE: case TYPE_FLOAT: case TYPE_LONG:
			case TYPE_SHORT: case TYPE_OBJECT: case TYPE_VOID:
			case TYPE_INT:
				break;
			case '[':
				return_type = TYPE_OBJECT;
				break;
			default:
				return_type = -1;
				break;
			}
		}
	}
	return return_type;
}

}
