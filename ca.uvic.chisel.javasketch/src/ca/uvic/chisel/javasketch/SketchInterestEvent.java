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
package ca.uvic.chisel.javasketch;


/**
 * Events indicated by the degree of interest model.
 * @author Del
 *
 */
public class SketchInterestEvent {
	
	public enum SketchInterestEventType {
		SketchActivated,
		SketchDeactivated,
		SketchHidden,
		SketchShown;
	}
	
	
	private IProgramSketch sketch;
	private SketchInterestEventType type;
	
	
	public SketchInterestEvent(IProgramSketch sketch, SketchInterestEventType type) {
		this.type = type;
		this.sketch = sketch;
	}
	
	public IProgramSketch getSketch() {
		return sketch;
	}

	
	/**
	 * @return the type
	 */
	public SketchInterestEventType getType() {
		return type;
	}

}
