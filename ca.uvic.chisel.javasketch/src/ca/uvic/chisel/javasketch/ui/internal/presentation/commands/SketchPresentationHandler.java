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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.ui.internal.presentation.IJavaSketchPresenter;
import ca.uvic.chisel.javasketch.ui.internal.presentation.JavaThreadSequenceView;

/**
 * @author Del Myers
 *
 */
public abstract class SketchPresentationHandler extends AbstractHandler implements IHandler2 {

	/**
	 * Returns the presenter for the workbench. Must be run in the UI thread,
	 * or null will be returned. Set <code>open</code> to true if the presenter
	 * should be opened in the workbench when it is not visible, false otherwise.
	 * If null will be returned if not called in the UI thread, or if the
	 * presenter is not available.
	 * @param open true to open the presenter if it isn't available.
	 * @return the presenter, if available.
	 */
	public IJavaSketchPresenter getPresenter(boolean open) {
		IWorkbenchPage page = null;
		try {
			page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		} catch (NullPointerException e) {
			return null;
		}
		if (page != null) {
			JavaThreadSequenceView view = (JavaThreadSequenceView) page.findView(JavaThreadSequenceView.VIEW_ID);
			if (view == null) {
				if (open) {
					try {
						view = (JavaThreadSequenceView) page.showView(JavaThreadSequenceView.VIEW_ID, null, IWorkbenchPage.VIEW_VISIBLE);
					} catch (PartInitException e) {}
				}
			}
			if (view != null) {
				page.bringToTop(view);
			}
			return view;
		}
		return null;
		
	}
	
	/**
	 * Returns the trace model in the current selection. If it is not a trace model,
	 *  null is returned.
	 * @return
	 */
	public ITraceModel getSelectedTraceModel(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if (element instanceof ITraceModel) {
				return (ITraceModel) element;
			} else if (element instanceof IAdaptable) {
				return (ITraceModel)((IAdaptable)element).getAdapter(ITraceModel.class);
			}
		}
		return null;
	}
	
	/**
	 * @return
	 */
	protected ISelection getWorkbenchSelection() {
		try {
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		} catch (NullPointerException e) {}
		
		return null;
	}

}
