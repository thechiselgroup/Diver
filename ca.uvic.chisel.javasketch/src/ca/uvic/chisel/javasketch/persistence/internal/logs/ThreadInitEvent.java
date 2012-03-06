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
 * A thread init event.
 * @author Del Myers
 *
 */
public class ThreadInitEvent extends TimeStampedEvent {
	public int threadID;
	public String threadName;
	
	/**
	 * 
	 */
	ThreadInitEvent() {
		threadID = 0;
		threadName = "";
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.persistence.internal.logs.TimeStampedEvent#load(java.io.RandomAccessFile)
	 */
	@Override
	protected void load(RandomAccessFile file) throws IOException {
		super.load(file);
		threadID = file.readUnsignedShort();
		threadName = readShortString(file);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ThreadInitEvent [threadID=" + threadID + ", threadName="
				+ threadName + ", time=" + time + ", type=" + type + "]";
	}
	
	
}
