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
public class TimeStampedEvent extends TraceLogEvent {
	
		public long time;
	
	
	TimeStampedEvent() {
		time = 0;
	}
	
	protected void load(RandomAccessFile file) throws IOException {
		super.load(file);
		time = file.readInt() & 0xffffffffL;
	}
	
	
	
	

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TimeStampedEvent [time=" + time + ", type=" + type + "]";
	}
	
	
	
	

}
