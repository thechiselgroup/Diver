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
package ca.uvic.chisel.javasketch.ui.internal.presentation;

import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;

import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.widgets.RangeSlider;

/**
 * Represents a view/editor part that can present java sketches.
 * @author Del Myers
 *
 */
public interface IJavaSketchPresenter {

	/**
	 * @return
	 */
	RangeSlider getTimeRange();

	/**
	 * @return
	 */
	IThread getThread();

	/**
	 * @return
	 */
	UMLSequenceViewer getSequenceChartViewer();

	/**
	 * 
	 */
	public void resetExpansionStates(IActivation activation);

	/**
	 * @param traceModel
	 */
	void reveal(ITraceModel traceModel, String threadIdentifier);
	
	

}
