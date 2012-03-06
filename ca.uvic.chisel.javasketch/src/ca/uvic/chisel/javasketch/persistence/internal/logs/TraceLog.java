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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A file representing a trace log.
 * @author Del Myers
 *
 */
public class TraceLog {
	
	public static final int VERSION_0 = 0;
	
	private long eventsRead;

	private EventReader reader;

	/**
	 * Creates a new trace log for reading
	 * @param file
	 * @throws FileNotFoundException
	 */
	public TraceLog(File file) throws IOException {
		if (reader == null) {
			RandomAccessFile ra = new RandomAccessFile(file, "r");
			int count = 0;
			try {
				while (count < 3 && ra.length() <= 4) {
					try {
						//try to wait until there is data available
						Thread.sleep(200);
					} catch (InterruptedException e) {
						Thread.interrupted();
					}
					count++;
				}
				if (count >= 3) {
					throw new IOException("No data in file " + file.getAbsolutePath());
				}
				//read the first 16 bits, plus a version number to find out what reader we should use.
				int type = ra.readShort();
				if (type != TraceLogEvent.HEADER) {
					throw new IOException("Unknown file format.");
				}
				int version = ra.readShort();
				ra.seek(0);

				this.reader = EventReader.getReader(version, ra);
			} catch (IllegalArgumentException e) {
				ra.close();
				throw new IOException("Bad file version", e);
			} catch (IOException e) {
				ra.close();
				throw e;
			}
		}
	}
		
	/**
	 * Returns the next event in the log file, or null if no more exist. Blocks the current thread to
	 * wait for more data if necessary.
	 * @return
	 * @throws IOException 
	 */
	public TraceLogEvent nextEvent() throws IOException {
		
		TraceLogEvent event = reader.nextEvent();
		if (event != null) {
			eventsRead++;
		}
		return event;
	}
	
	public long getTraceLength() {
		return reader.getTraceLength();
	}
	
	public long getReadLocation() {
		return reader.getReadLocation();
	}
	
	public boolean available() {
		return reader.available();
	}
	
	public long getNumberOfEventsRead() {
		return eventsRead;
	}
	
	/**
	 * Returns true if the log has been read completely.
	 * @return
	 */
	public boolean isComplete() {
		return reader.isComplete();
	}

	/**
	 * @throws IOException 
	 * 
	 */
	public void close() throws IOException {
		reader.close();
	}

}
