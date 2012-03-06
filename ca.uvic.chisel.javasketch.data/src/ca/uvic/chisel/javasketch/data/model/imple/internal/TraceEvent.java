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

import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceEvent;
import ca.uvic.chisel.javasketch.data.model.TraceEventType;

/**
 * An event that indicates a change in the model for a trace.
 * @author Del Myers
 *
 */
public abstract class TraceEvent implements ITraceEvent {
	private TraceEventType type;
	private TraceImpl trace;
	
	protected TraceEvent(TraceImpl trace, TraceEventType type) {
		this.type = type;
		this.trace = trace;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceEvent#getType()
	 */
	public TraceEventType getType() {
		return type;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceEvent#getTrace()
	 */
	public ITrace getTrace() {
		return trace;
	}
	
	

}
