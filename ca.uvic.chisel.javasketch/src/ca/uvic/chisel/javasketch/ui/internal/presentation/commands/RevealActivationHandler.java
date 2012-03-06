/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.ui.internal.presentation.commands;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;
import ca.uvic.chisel.javasketch.ui.internal.presentation.IJavaSketchPresenter;
import ca.uvic.chisel.javasketch.ui.internal.presentation.JavaThreadSequenceView;

/**
 * Reveal an activation for a given event.
 * 
 * @author Del Myers
 * 
 */
public class RevealActivationHandler extends AbstractHandler {
	
	public static final String THREAD_PARAMETER = "revealThread";
	public static final String TRACE_PARAMETER = "revealTrace";
	public static final String COMMAND_ID = "ca.uvic.chisel.javasketch.command.revealActivation";
	public static final String MODEL_PARAMETER = "revealModel";

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.
	 * ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITraceModel traceModel = getTraceModel(event);
		if (traceModel != null) {
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
			if (window != null) {
				IViewPart view = window.getActivePage().findView(JavaThreadSequenceView.VIEW_ID);
				if (view == null) {
					try {
						view = window.getActivePage().showView(JavaThreadSequenceView.VIEW_ID, null, IWorkbenchPage.VIEW_VISIBLE);
					} catch (PartInitException e) {
						SketchPlugin.getDefault().log(e);
						return null;
					}
				}
				if (view != null) {
					window.getActivePage().bringToTop(view);
					//make sure that the right 
					((IJavaSketchPresenter)view).reveal(traceModel, event.getParameter(THREAD_PARAMETER));
				}
			}
		}
		return null;
	}

	
	
	/**
	 * Attempts to retrieve the trace model object for the event. First,
	 * parameters are checked to see what should be displayed. If the parameters
	 * don't contain a trace model, then the selection is queried.
	 * @param event
	 * @return
	 */
	protected ITraceModel getTraceModel(ExecutionEvent event) {
		String traceId = event.getParameter(TRACE_PARAMETER);
		String modelId = event.getParameter(MODEL_PARAMETER);
		ITraceModel tm = null;
	
		if (modelId != null) {
			IProgramSketch sketch = SketchPlugin.getDefault().getSketch(traceId);
			if (sketch != null) {
				ITrace trace = sketch.getTraceData();
				if (trace != null) {
					ITraceModelProxy proxy = trace.getElement(modelId);
					if (proxy != null) {
						tm = proxy.getElement();
					}
				}
			}
		}
		
		if (tm == null) {
			tm = getTraceModelForSelection(HandlerUtil.getCurrentSelection(event), event);
		}
		return tm;
	}
	
	protected ITraceModel getTraceModelForSelection(ISelection selection, ExecutionEvent event) {
		ITraceModel tm = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			for (Iterator<?> it = ss.iterator(); it.hasNext() && tm == null;) {
				Object o = it.next();
				if (o instanceof ITraceModel) {
					tm = (ITraceModel) o;
				} else if (o instanceof IAdaptable) {
					tm = (ITraceModel) ((IAdaptable)o).getAdapter(ITraceModel.class);
				} else {
					tm = (ITraceModel) Platform.getAdapterManager().getAdapter(o, ITraceModel.class);
				}
				if (tm != null) {
					return tm;
				}
			}
		}
		return tm;
	}

}
