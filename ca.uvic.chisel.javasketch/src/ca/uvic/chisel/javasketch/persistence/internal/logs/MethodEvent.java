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

/**
 * @author Del Myers
 *
 */
public class MethodEvent extends TimeStampedEvent {
	public int lineNumber;
	public String className;
	public String methodName;
	public String methodSignature;
		
	public MethodEvent() {
		lineNumber = -1;
		className = "";
		methodName = "";
		methodSignature = "";
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.persistence.internal.logs.TimeStampedEvent#load(java.io.RandomAccessFile)
	 */
	@Override
	protected void load(RandomAccessFile file) throws IOException {
		super.load(file);
		lineNumber = file.readInt();
		className = readShortString(file);
		methodName = readShortString(file);
		methodSignature = readShortString(file);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MethodEvent [className=" + className + ", lineNumber="
				+ lineNumber + ", methodName=" + methodName
				+ ", methodSignature=" + methodSignature + ", time=" + time
				+ ", type=" + type + "]";
	}
	
	

}
