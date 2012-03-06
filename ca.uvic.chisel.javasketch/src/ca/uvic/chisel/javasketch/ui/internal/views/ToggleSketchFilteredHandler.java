/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.ui.internal.views;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import ca.uvic.chisel.javasketch.IDegreeOfInterest;
import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.ISketchImageConstants;

/**
 * @author Del Myers
 * 
 */
public class ToggleSketchFilteredHandler extends AbstractHandler implements
		IElementUpdater {

	
	public static final String COMMAND_ID = "ca.uvic.chisel.javasketch.commands.toggleSketchFilter";
	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.commands.IElementUpdater#updateElement(org.eclipse.ui.
	 * menus.UIElement, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		IProgramSketch sketch = getSelectedSketch();
		ImageRegistry registry = SketchPlugin.getDefault().getImageRegistry();
		IDegreeOfInterest doi = SketchPlugin.getDefault().getDOI();
		if (doi != null && sketch != null) {
			if (doi.isSketchHidden(sketch)) {
				element.setIcon(registry.getDescriptor(ISketchImageConstants.ICON_ELEMENT_VISIBLE));
				element.setText("View Trace");
			} else {
				element.setIcon(registry.getDescriptor(ISketchImageConstants.ICON_ELEMENT_FILTERED));
				element.setText("Filter Trace");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.
	 * ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IProgramSketch selected = getSelectedSketch();
		if (selected == null) {
			return null;
		}
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					IDegreeOfInterest doi = SketchPlugin.getDefault().getDOI();
					boolean hidden = doi.isSketchHidden(selected);
					doi.setSketchHidden(selected, !hidden, monitor);
					
				}
			});
		} catch (InvocationTargetException e) {
			throw new ExecutionException(e.getMessage(), e);
		} catch (InterruptedException e) {
		}
		
		return null;
	}

	private TraceNavigator getNagivator() {
		IWorkbenchPage page;
		try {
			page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage();
		} catch (NullPointerException e) {
			return null;
		}

		if (page != null) {
			return (TraceNavigator) page.findView(TraceNavigator.VIEW_ID);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.commands.AbstractHandler#setEnabled(java.lang.Object)
	 */
	@Override
	public void setEnabled(Object evaluationContext) {
		super.setEnabled(evaluationContext);
		boolean enabled = isEnabled();
		IProgramSketch sketch = SketchPlugin.getDefault().getActiveSketch();
		IProgramSketch selected = getSelectedSketch();
		if (sketch == null || selected == null) {
			enabled = false;
		} else {
			enabled = !sketch.equals(selected) && selected.getTracedLaunchConfiguration().getName().equals(
				sketch.getTracedLaunchConfiguration().getName());
		}
		if (enabled != isEnabled()) {
			setBaseEnabled(enabled);
		}
	}

	/**
	 * @return
	 */
	private IProgramSketch getSelectedSketch() {
		TraceNavigator navigator = getNagivator();
		if (navigator != null) {
			ISelection selection = navigator.getViewSite()
				.getSelectionProvider().getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				if (ss.getFirstElement() instanceof IProgramSketch) {
					return (IProgramSketch) ss.getFirstElement();
				}
			}
		}
		return null;
	}

}
