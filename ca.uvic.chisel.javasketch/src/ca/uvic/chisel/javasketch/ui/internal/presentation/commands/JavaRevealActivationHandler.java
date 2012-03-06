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
package ca.uvic.chisel.javasketch.ui.internal.presentation.commands;

import java.util.Iterator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;

/**
 * Reveals an activation for a Java selection
 * @author Del Myers
 *
 */
public class JavaRevealActivationHandler extends RevealActivationHandler {

	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.ui.internal.presentation.commands.RevealActivationHandler#getTraceModelForSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	protected ITraceModel getTraceModelForSelection(ISelection selection, ExecutionEvent event) {
		//get the trace to look in from the event
		String traceid = event.getParameter(TRACE_PARAMETER);
		if (traceid == null) {
			return null;
		}
		if (!(selection instanceof IStructuredSelection)) return null;
		IStructuredSelection ss = (IStructuredSelection) selection;
		IProgramSketch sketch = SketchPlugin.getDefault().getSketch(traceid);
		if (sketch == null) {
			//use the active sketch
			sketch = SketchPlugin.getDefault().getActiveSketch();
		}
		if (sketch != null && !sketch.isAnalysing() && sketch.isConnected()) {
			ITrace trace = sketch.getTraceData();
			IJavaElement element = getJEForSelection(ss);
			if (element instanceof IType) {
				return JavaSearchUtils.findSimilarType(trace, (IType) element);
			} else if (element instanceof IMethod) {
				return JavaSearchUtils.findSimilarMethod(trace, (IMethod) element);
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.ui.internal.presentation.commands.RevealActivationHandler#getTraceModel(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected ITraceModel getTraceModel(ExecutionEvent event) {
		return getTraceModelForSelection(HandlerUtil.getCurrentSelection(event), event);
	}
	
	private IJavaElement getJEForSelection(IStructuredSelection ss) {
		for (Iterator<?> it = ss.iterator(); it.hasNext();) {
			Object o = it.next();
			if (o instanceof IAdaptable) {
				return (IJavaElement) ((IAdaptable) o)
					.getAdapter(IJavaElement.class);
			} else if (o != null) {
				Object element = Platform.getAdapterManager().getAdapter(o,
					IJavaElement.class);
				if (element instanceof IJavaElement) {
					return (IJavaElement) element;
				}
			}
		}
		return null;
	}
}
