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

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.zest.custom.uml.viewers.BreadCrumbViewer;
import org.eclipse.zest.custom.uml.viewers.ISequenceViewerListener;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerEvent;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerGroupEvent;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerRootEvent;
import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;

import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.javasketch.data.model.IThread;

/**
 * A hook between a breadcrumb viewer and 
 * @author Del Myers
 *
 */
class BreadCrumbHook implements ISelectionChangedListener,
		ISequenceViewerListener {
	
	

	private UMLSequenceViewer viewer;
	private BreadCrumbViewer breadcrumb;

	/**
	 * @param breadcrumb
	 * @param viewer
	 */
	public BreadCrumbHook(BreadCrumbViewer breadcrumb, UMLSequenceViewer viewer) {
		this.viewer = viewer;
		this.breadcrumb = breadcrumb;
		breadcrumb.addSelectionChangedListener(this);
		viewer.addSequenceListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (event.getSelection() instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) event.getSelection();
			Object element = ss.getFirstElement();
			IActivation newRoot = null;
			if (element instanceof ICall) {
				try {
					if (((ICall)element).getActivation() != null) {
						newRoot = ((ICall)element).getTarget().getActivation();
					}
				} catch (NullPointerException e) {}
			}
			if (newRoot == null) {
				newRoot = ((IThread)viewer.getInput()).getRoot().getActivation();
			}
			viewer.setRootActivation(newRoot);
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceViewerListener#elementCollapsed(org.eclipse.zest.custom.uml.viewers.SequenceViewerEvent)
	 */
	@Override
	public void elementCollapsed(SequenceViewerEvent event) {}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceViewerListener#elementExpanded(org.eclipse.zest.custom.uml.viewers.SequenceViewerEvent)
	 */
	@Override
	public void elementExpanded(SequenceViewerEvent event) {}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceViewerListener#groupCollapsed(org.eclipse.zest.custom.uml.viewers.SequenceViewerGroupEvent)
	 */
	@Override
	public void groupCollapsed(SequenceViewerGroupEvent event) {}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceViewerListener#groupExpanded(org.eclipse.zest.custom.uml.viewers.SequenceViewerGroupEvent)
	 */
	@Override
	public void groupExpanded(SequenceViewerGroupEvent event) {}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceViewerListener#rootChanged(org.eclipse.zest.custom.uml.viewers.SequenceViewerRootEvent)
	 */
	@Override
	public void rootChanged(SequenceViewerRootEvent event) {
		if (viewer.getRootActivation() != null && !viewer.getRootActivation().equals(breadcrumb.getInput())) {
			try {
				breadcrumb.setInput(((IActivation)viewer.getRootActivation()).getArrival().getOrigin());
			} catch (NullPointerException e) {
				breadcrumb.setInput(null);
			}
		}
	}

}
