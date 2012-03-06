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
package ca.uvic.chisel.javasketch.ui.internal;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import ca.uvic.chisel.javasketch.ISketchEventListener;
import ca.uvic.chisel.javasketch.ISketchInterestListener;
import ca.uvic.chisel.javasketch.SketchEvent;
import ca.uvic.chisel.javasketch.SketchInterestEvent;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.internal.preferences.ISketchPluginPreferences;
import ca.uvic.chisel.javasketch.ui.internal.views.java.RefreshEditorsJob;
import ca.uvic.chisel.javasketch.ui.internal.views.java.RefreshPackageExplorerJob;

/**
 * Handles all of the internal UI updates for the sketch plugin.
 * @author Del Myers
 *
 */
public class SketchUI implements IPropertyChangeListener, ISketchEventListener, ISketchInterestListener {
	public static final SketchUI INSTANCE = new SketchUI();

	
	private class RefreshRunnable implements Runnable {
		@Override
		public void run() {
			IWorkbenchWindow window = SketchPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
			ICommandService service = (ICommandService) window.getService(ICommandService.class);
			if (service != null) {
				service.refreshElements("ca.uvic.chisel.javasketch.packageExplorerFilter", null);
				service.refreshElements(TraceCommandHandler.COMMAND_ID, null);
				service.refreshElements(TraceDurationCommandHandler.COMMAND_ID, null);
			}
		}
	}
	

	public static final String PREFERENCE_FILTER_PACKAGE_EXPLORER = "ca.uvic.javasketch.ui.filters.packageExplorer";
	private RefreshPackageExplorerJob refreshJavaJob;
	private IWindowListener javaMarkerListener;
	private RefreshEditorsJob javaEditorsJob;
	
	/**
	 * Refreshes the command state for the current selection in the debug UI.
	 */
	public void refreshCommands() {
		Display display = SketchPlugin.getDefault().getWorkbench().getDisplay();
		if (Display.getCurrent() != null) {
			new RefreshRunnable().run();
		} else {
			display.asyncExec(new RefreshRunnable());
		}
	}
	
	/**
	 * 
	 */
	private SketchUI() {
		refreshJavaJob = new RefreshPackageExplorerJob();
		javaMarkerListener = new JavaMarkerEditorListener();
		javaEditorsJob = new RefreshEditorsJob();
		SketchPlugin.getDefault().addSketchEventListener(this);
		SketchPlugin.getDefault().getDOI().addSketchInterestListener(this);
		PlatformUI.getWorkbench().addWindowListener(javaMarkerListener);
		javaMarkerListener.windowActivated(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
	}

	/**
	 * 
	 */
	public void refreshJavaUI() {
		//new Exception().printStackTrace();
		refreshJavaJob.cancel();
		refreshJavaJob.schedule();
		javaEditorsJob.cancel();
		javaEditorsJob.schedule();
		refreshCommands();
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (PREFERENCE_FILTER_PACKAGE_EXPLORER.equals(event.getProperty()) ||
				ISketchPluginPreferences.DISPLAY_ONLY_SOURCE_FOLDERS.equals(event.getProperty())) {
			refreshJavaUI();
		}
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.ISketchEventListener#handleSketchEvent(ca.uvic.chisel.javasketch.SketchEvent)
	 */
	@Override
	public void handleSketchEvent(SketchEvent event) {
		switch (event.getType()) {
		case SketchRefreshed:
		case SketchAnalysisEnded:
		case SketchAnalysisInterrupted:
			refreshJavaUI();
			break;
		}
		
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.ISketchInterestListener#sketchInterestChanged(ca.uvic.chisel.javasketch.SketchInterestEvent)
	 */
	@Override
	public void sketchInterestChanged(SketchInterestEvent event) {
		refreshJavaUI();
	}
	
	/**
	 * Starts the ui services for listening to workbench selection changes.
	 *
	public void start() {
		SketchPlugin.getDefault().getWorkbench().getDisplay().asyncExec(new Runnable(){
			@Override
			public void run() {
				IWorkbench workbench = SketchPlugin.getDefault().getWorkbench();
				workbench.addWindowListener(SketchUI.this);
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				if (window != null) {
					window.getSelectionService().addSelectionListener("org.eclipse.debug.ui.DebugView", SketchUI.this);
				}
			}
		});
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {
		window.getSelectionService().addSelectionListener(this);
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
		window.getSelectionService().removeSelectionListener(this);		
	}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
		window.getSelectionService().removeSelectionListener(this);	
	}

	@Override
	public void windowOpened(IWorkbenchWindow window) {
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		refreshCommands();
	}
	*/
}
