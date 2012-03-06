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

import java.util.Set;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

class InternalWindowListener implements IWindowListener {
	IWorkbenchWindow activeWindow;
	InternalPageListener pageListener = new InternalPageListener();
	IPerspectiveListener perspectiveListener = new IPerspectiveListener() {
		
		public void perspectiveChanged(IWorkbenchPage page,
				IPerspectiveDescriptor perspective, String changeId) {}
		
		public void perspectiveActivated(IWorkbenchPage page,
				IPerspectiveDescriptor perspective) {
			if (page == null || perspective == null) return;
			Set<LoggingCategory> categories = 
				WorkbenchLogger.getMatchingCategories(perspective.getId());
			for (LoggingCategory c : categories) {
				c.getLog().logLine(System.currentTimeMillis() + "\tworkbenchEvent\tperspective=" +
					perspective.getId() + "\tactivated");
			}
			
		}
	};

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void windowActivated(IWorkbenchWindow window) {
		if (activeWindow != null) {
			activeWindow.removePageListener(pageListener);
			activeWindow.removePerspectiveListener(perspectiveListener);
		}
		activeWindow = window;
		activeWindow.addPageListener(pageListener);
		activeWindow.addPerspectiveListener(perspectiveListener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void windowClosed(IWorkbenchWindow window) {
		activeWindow = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void windowDeactivated(IWorkbenchWindow window) {
		if (activeWindow != null && activeWindow == window) {
			activeWindow.removePageListener(pageListener);
			activeWindow.removePerspectiveListener(perspectiveListener);
			activeWindow = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void windowOpened(IWorkbenchWindow window) {
	}

	public void initialize(IWorkbenchWindow activeWorkbenchWindow) {
		windowActivated(activeWorkbenchWindow);
		perspectiveListener.perspectiveActivated(activeWorkbenchWindow.getActivePage(), activeWorkbenchWindow.getActivePage().getPerspective());
		pageListener.initialize(activeWorkbenchWindow.getActivePage());
	}		
}