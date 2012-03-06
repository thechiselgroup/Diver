/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.persistence.internal.logs;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;


/**
 * @author Del Myers
 *
 */
public class MethodEnterEvent extends MethodEvent {

	public long modifiers;
	public String[] variableValues;
	/**
	 * 
	 */
	MethodEnterEvent() {
		modifiers = 0;
		variableValues = new String[0];
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.persistence.internal.logs.MethodEvent#load(java.io.RandomAccessFile)
	 */
	@Override
	protected void load(RandomAccessFile file) throws IOException {
		super.load(file);
		modifiers = file.readInt();
		int variablesCount = file.readUnsignedShort();
		if (variablesCount > 0) {
			variableValues = new String[variablesCount];
			for (int i = 0; i < variablesCount; i++) {
				variableValues[i] = readShortString(file);
			}
		}
	}

	/**
	 * Creates and returns a method exit event that simulates an exit from this enter.
	 * @return
	 */
	public MethodExitEvent simulateExit(long time) {
		MethodExitEvent exitEvent = new MethodExitEvent();
		exitEvent.className = className;
		exitEvent.methodName = methodName;
		exitEvent.methodSignature = methodSignature;
		exitEvent.time = time;
		return exitEvent;
	}

	/**
	 * @param className
	 * @param methodName
	 * @param classSignature
	 * @param lineNumber
	 * @param time
	 * @return
	 */
	public static MethodEnterEvent simulateEnter(String className,
			String methodName, String methodSignature, int lineNumber, long time) {
		MethodEnterEvent methodEnter = new MethodEnterEvent();
		methodEnter.className = className;
		methodEnter.methodName = methodName;
		methodEnter.lineNumber = lineNumber;
		methodEnter.methodSignature = methodSignature;
		methodEnter.time = time;
		return methodEnter;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MethodEnterEvent [modifiers=" + modifiers + ", variableValues="
				+ Arrays.toString(variableValues) + ", className=" + className
				+ ", lineNumber=" + lineNumber + ", methodName=" + methodName
				+ ", methodSignature=" + methodSignature + ", time=" + time
				+ ", type=" + type + "]";
	}
	
	
}
