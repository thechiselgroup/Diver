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

import org.eclipse.core.runtime.IProgressMonitor;

import ca.uvic.chisel.javasketch.data.model.IThread;

/**
 * A simple interface that returns the degree of interest for a particular
 * object in the workbench.
 * @author Del
 *
 */
public interface IDegreeOfInterest {
	
	/**
	 * Returns the interest of the given object from 0 as uninteresting
	 * to 1 as interesting. Negative number if it is not covered by this
	 * model.
	 * @param o
	 * @return the interest of the given object.
	 */
	public double getInterest(Object o);
	
	
	/**
	 * 
	 * @param listener
	 */
	public void addSketchInterestListener(ISketchInterestListener listener);
	
	public void removeSketchInterestListener(ISketchInterestListener listener);
	
	public boolean isSketchHidden(IProgramSketch sketch);
	
	public IProgramSketch getActiveSketch();


	/**
	 * @return
	 */
	public IProgramSketch[] getHiddenSketches();
	
	

	/**
	 * @param selected
	 * @param b
	 */
	public void setSketchHidden(IProgramSketch selected, boolean hidden, IProgressMonitor progress);


	public abstract boolean requestFiltering(IThread thread, IProgressMonitor monitor);
	
	public void setThreadSelection(IThread thread);

}
