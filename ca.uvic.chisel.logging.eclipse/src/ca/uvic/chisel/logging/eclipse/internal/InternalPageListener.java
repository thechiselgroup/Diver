/*******************************************************************************
 * Copyright (c) 2010 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.logging.eclipse.internal;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;



/**
 * @author Del Myers
 *
 */
final class InternalPageListener implements IPageListener {
	IWorkbenchPage activePage;
	PluginPartListener partListener = new PluginPartListener();
	private ISelectionListener selectionLogger = new SelectionLogger();

	public void pageOpened(IWorkbenchPage page) {
	}

	public void pageClosed(IWorkbenchPage page) {
		if (activePage != null && activePage == page) {
			activePage.removePartListener(partListener);
			activePage.removeSelectionListener(selectionLogger);
			activePage = null;
		}
	}

	public void pageActivated(IWorkbenchPage page) {
		if (activePage != null) {
			activePage.removePartListener(partListener);
		}
		activePage = page;
		if (activePage != null) {
			page.addSelectionListener(selectionLogger );
			page.addPartListener(partListener);
		}
	}

	public void initialize(IWorkbenchPage page) {
		pageActivated(page);
		for (IViewReference reference : page.getViewReferences()) {
			IViewPart part = reference.getView(false);
			if (part != null) {
				partListener.partOpened(part);
			}
		}
		for (IEditorReference reference : page.getEditorReferences()) {
			IEditorPart part = reference.getEditor(false);
			if (part != null) {
				partListener.partOpened(part);
			}
		}
	}
}