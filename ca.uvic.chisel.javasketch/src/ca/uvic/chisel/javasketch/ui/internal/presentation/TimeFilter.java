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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IArrival;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.widgets.RangeSlider;

class TimeFilter extends ViewerFilter {
	
	private final RangeSlider range;
	
	public TimeFilter(RangeSlider range) {
		this.range = range;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement,
			Object element) {
		if (element instanceof ICall) {
			ICall call = (ICall) element;
			IArrival arrival = (IArrival) call.getTarget();
			long min = range.getSelectedMinimum();
			long max = range.getSelectedMaximum();
			if (arrival != null && arrival.getActivation() != null) {
				IActivation a = arrival.getActivation();
				long start = call.getTime();
				long end = start + a.getDuration();
				return (start <= max && end >= min);
			} 
			return false;
		}
		return true;
	}
	
}