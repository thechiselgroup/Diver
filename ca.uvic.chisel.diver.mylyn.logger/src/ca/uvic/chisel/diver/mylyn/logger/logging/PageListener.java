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
package ca.uvic.chisel.diver.mylyn.logger.logging;

import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;

/**
 * @author Del
 *
 */
public class PageListener implements IPageListener {
	IWorkbenchPage activePage = null;
	ISelectionListener selectionListener = new PageSelectionListener();
	IPartListener partListener = new PartListener();
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPageListener#pageActivated(org.eclipse.ui.IWorkbenchPage)
	 */
	@Override
	public void pageActivated(IWorkbenchPage page) {
		if (activePage != null) {
			activePage.removeSelectionListener(selectionListener);
			activePage.removePartListener(partListener);
		}
		activePage = page;
		if (activePage != null) {
			activePage.addSelectionListener(selectionListener);
			activePage.addPartListener(partListener);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPageListener#pageClosed(org.eclipse.ui.IWorkbenchPage)
	 */
	@Override
	public void pageClosed(IWorkbenchPage page) {
		page.removeSelectionListener(selectionListener);
		page.removePartListener(partListener);
		activePage = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPageListener#pageOpened(org.eclipse.ui.IWorkbenchPage)
	 */
	@Override
	public void pageOpened(IWorkbenchPage page) {}

}
