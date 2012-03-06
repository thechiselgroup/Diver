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
package ca.uvic.chisel.javasketch.ui.internal.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import ca.uvic.chisel.javasketch.data.model.IThread;

public class EmptyThreadFilter extends ViewerFilter {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement,
			Object element) {
		if (element instanceof IThread) {
			IThread thread = (IThread) element;
			try {
				long duration = thread.getRoot().getActivation().getDuration();
				if (duration > 0) {
					return true;
				}
			} catch (Exception e) {
			}
		} else {
			return true;
		}
		return false;
	}
	
}