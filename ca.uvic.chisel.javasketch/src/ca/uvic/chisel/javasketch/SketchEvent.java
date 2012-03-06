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

/**
 * An event that occurs within the framework, indicating different parts of a sketch's lifecycle.
 * 
 * @author Del Myers
 *
 */
public class SketchEvent {
	/**
	 * The different event types for sketches.
	 * @author Del Myers
	 *
	 */
	public static enum SketchEventType {
		/**
		 * Indicates that a sketch has started within the debug
		 * framework. The returned event may be safely cast to
		 * a SketchDebugEvent.
		 */
		SketchStarted,
		/**
		 * Indicates that a sketch has ended within the debug
		 * framework. The returned event may be safely cast to
		 * a SketchDebugEvent.
		 */
		SketchEnded,
		/**
		 * Indicates that a sketch is scheduled for "analysis"
		 * by the framework. "Analysis" is a long-running process
		 * in which traced information is indexed so that it
		 * can be used efficiently. The returned event may
		 * be safely cast to a SketchAnalysisEvent.
		 */
		SketchAnalysisScheduled,
		/**
		 * Indicates that a sketch is currently under "analysis"
		 * by the framework. "Analysis" is a long-running process
		 * in which traced information is indexed so that it
		 * can be used efficiently. The returned event may
		 * be safely cast to a SketchAnalysisEvent.
		 */
		SketchAnalysisStarted,
		/**
		 * Indicates that a sketch has completed its analysis
		 * phase. "Analysis" is a long-running process
		 * in which traced information is indexed so that it
		 * can be used efficiently. The returned event may
		 * be safely cast to a SketchAnalysisEvent.
		 */
		SketchAnalysisEnded,
		/**
		 * Indicates that sketch analysis has abnormally
		 * terminated, either by a user interruption, or
		 * by an error. Indexing did not complete, and
		 * the sketch must be re-analyzed. "Analysis" is a long-running process
		 * in which traced information is indexed so that it
		 * can be used efficiently. The returned event may
		 * be safely cast to a SketchAnalysisEvent.
		 */
		SketchAnalysisInterrupted, 
		
		/**
		 * Indicates to clients that a major change has occurred in
		 * the framework, and that all sketches should be refreshed. 
		 */
		SketchRefreshed,
		
		/**
		 * The given sketch has been deleted
		 */
		SketchDeleted,
		
	}

	/**
	 * The sketch associated with this event.
	 */
	private IProgramSketch sketch;
	/**
	 * The event type
	 */
	private SketchEventType type;
	
	/**
	 * Constructs a new sketch event.
	 * @param sketch a new sketch event.
	 */
	public SketchEvent(IProgramSketch sketch, SketchEventType type) {
		this.type = type;
		this.sketch = sketch;
	}
	
	/**
	 * @return the sketch
	 */
	public IProgramSketch getSketch() {
		return sketch;
	}
	
	/**
	 * @return the type
	 */
	public SketchEventType getType() {
		return type;
	}
}
