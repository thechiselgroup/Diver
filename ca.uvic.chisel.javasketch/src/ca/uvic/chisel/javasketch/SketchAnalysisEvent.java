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
 * @author Del Myers
 *
 */
public class SketchAnalysisEvent extends SketchEvent {

	/**
	 * 
	 * @param sketch
	 * @param type
	 */
	public SketchAnalysisEvent(IProgramSketch sketch, SketchEventType type) {
		super(sketch, type);
	}

}
