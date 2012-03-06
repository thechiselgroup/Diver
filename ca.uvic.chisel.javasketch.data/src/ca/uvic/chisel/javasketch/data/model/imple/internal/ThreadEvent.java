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
package ca.uvic.chisel.javasketch.data.model.imple.internal;

import ca.uvic.chisel.javasketch.data.model.IThreadEvent;
import ca.uvic.chisel.javasketch.data.model.TraceEventType;

/**
 * Event that indicates that the threads in the trace have changed. Users
 * can simply query the trace to find the threads.
 * @author Del Myers
 *
 */
public class ThreadEvent extends TraceEvent implements IThreadEvent {

	/**
	 * @param trace
	 * @param type
	 */
	protected ThreadEvent(TraceImpl trace) {
		super(trace, TraceEventType.ThreadEventType);
	}

}
