/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.persistence.internal.logs;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Del Myers
 *
 */
public abstract class EventReader {
	
	public static final int VERSION_0 = 0;
	private RandomAccessFile ra;
	private int version;
	
	protected EventReader(int version, RandomAccessFile file) {
		this.ra = file;
		this.version = version;
	}
	
	
	public static EventReader getReader(int version, RandomAccessFile file) {
		switch (version) {
		case 0:
			return new EventReader0(file);
		}
		throw new IllegalArgumentException("Unknown Version " + version);
	}
	
	
	public abstract TraceLogEvent nextEvent() throws IOException;
	

	/**
	 * @return the file
	 */
	final RandomAccessFile getFile() {
		return ra;
	}
	
	/**
	 * @return the version
	 */
	public final int getVersion() {
		return version;
	}
	
	public abstract boolean isComplete();
	
	public long getTraceLength() {
		if (ra == null) return 0;
		try {
			return ra.length();
		} catch (IOException e) {
		}
		return 0;
	}
	
	public long getReadLocation() {
		if (ra == null) return 0;
		try {
			return ra.getFilePointer();
		} catch (IOException e) {
		}
		return 0;
	}
	
	public boolean available() {
		if (ra == null || isComplete()) {
			return false;
		}
		//must be at least 4 bytes available to read
		try {
			long diff = (ra.length()-ra.getFilePointer());
			return diff > 4;
		} catch (IOException e) {
		}
		return false;
	}


	/**
	 * @throws IOException 
	 * 
	 */
	public void close() throws IOException {
		if (ra != null) {
			ra.close();
			ra = null;
		}
	}
	
}
