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

import static ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent.HEADER;
import static ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent.METHOD_ENTERED;
import static ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent.METHOD_EXITED;
import static ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent.PAUSED;
import static ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent.RESUMED;
import static ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent.THREAD_INIT;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Del Myers
 *
 */
public class EventReader0 extends EventReader {

	private boolean complete;
	

	/**
	 * @param file
	 */
	EventReader0(RandomAccessFile file) {
		super (VERSION_0, file);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.persistence.internal.logs.EventReader#nextEvent()
	 */
	@Override
	public TraceLogEvent nextEvent() throws IOException {
		RandomAccessFile ra = getFile();
		if (ra == null) return null;
		long eventPointer = -1;
		eventPointer = ra.getFilePointer();
		int type = -1; 
		 try {
			type = ra.readShort();
			//back-up
			ra.seek(eventPointer);
		}  catch (IOException e) {
			ra.close();
			complete = true;
			ra = null;
			return null;
		}
		TraceLogEvent event = null;
		switch (type) {
		case METHOD_ENTERED:
			event = new MethodEnterEvent();
			break;
		case METHOD_EXITED:
			event = new MethodExitEvent();
			break;
		case PAUSED:
			event = new PauseEvent();
			break;
		case RESUMED:
			event = new ResumeEvent();
			break;
		case THREAD_INIT:
			event = new ThreadInitEvent();
			break;
		case HEADER:
			event = new HeaderEvent();
			break;
		default:
			throw new IOException("Unknown event type: " + type);
		}
		if (event != null) {
			try {
				event.load(ra);
			} catch (IOException e) {
				//back up and return null...
				//no event is ready
				ra.seek(eventPointer);
				return null;
			}
		}
		return event;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.persistence.internal.logs.EventReader#isComplete()
	 */
	@Override
	public boolean isComplete() {
		return complete;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.persistence.internal.logs.EventReader#close()
	 */
	@Override
	public void close() throws IOException {
		super.close();
		complete = true;
	}

}
