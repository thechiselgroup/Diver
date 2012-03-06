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
package ca.uvic.chisel.javasketch;


import ca.uvic.chisel.javasketch.launching.ITraceClient;

/**
 * @author Del Myers
 *
 */
public class SketchDebugEvent extends SketchEvent {

	private ITraceClient trace;

	/**
	 * Constructs a new sketch debug event.
	 * @param sketch the sketch that this event corresponds to.
	 * @param originator the originating debug event that caused this sketch event.
	 * @param trace the trace that is running, or has terminated, which produced the sketch.
	 */
	public SketchDebugEvent(IProgramSketch sketch, SketchEventType type, ITraceClient trace) {
		super(sketch, type);
		this.trace = trace;
	}
	
	/**
	 * @return the trace which produced or is producing the sketch
	 */
	public ITraceClient getTrace() {
		return trace;
	}

}
