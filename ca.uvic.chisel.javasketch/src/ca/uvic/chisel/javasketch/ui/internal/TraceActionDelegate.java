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

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionDelegate;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.launching.ITraceClient;

/**
 * @author Del Myers
 *
 */
public class TraceActionDelegate extends ActionDelegate implements IViewActionDelegate {
	
	
	private IAction action;
	private IDebugEventSetListener debugListener;
	private IViewPart view;

	@Override
	public void run(IAction action) {
		IProcess process = DebugUITools.getCurrentProcess();
		ITraceClient client = SketchPlugin.getDefault().getAssociatedClient(process);
		if (client.canPauseTrace()) {
			if (!client.isPaused()) {
				client.pauseTrace();
			} else {
				client.resumeTrace();
			}
		}
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		updateAction(action);
	}

	private void updateAction(IAction action) {
		if (action == null) return;
		IProcess process = DebugUITools.getCurrentProcess();
		ITraceClient client = SketchPlugin.getDefault().getAssociatedClient(process);
		if (client == null || client.isTerminated()) {
			//set the icon to "play"
			action.setImageDescriptor(SketchPlugin.imageDescriptorFromPlugin(SketchPlugin.PLUGIN_ID, "images/etool16/play_icon.png"));
			action.setText("Resume Trace");
			action.setToolTipText("Resumes a paused trace");
			action.setChecked(false);
			action.setEnabled(false);
		} else {
			if (client.isPaused()) {
				//set the icon to "play"
				action.setImageDescriptor(SketchPlugin.imageDescriptorFromPlugin(SketchPlugin.PLUGIN_ID, "images/etool16/play_icon.png"));
				action.setText("Resume Trace");
				action.setToolTipText("Resumes a paused trace");
			} else {
				//set the icon to "play"
				action.setImageDescriptor(SketchPlugin.imageDescriptorFromPlugin(SketchPlugin.PLUGIN_ID, "images/etool16/pause_icon.png"));
				action.setText("Pause Trace");
				action.setToolTipText("Pauses a running trace");
			}
			action.setEnabled(true);
		}
	}

	@Override
	public void init(IViewPart view) {
		this.view = view;
	}
	
	@Override
	public void init(IAction action) {
		this.action = action;
		debugListener = new IDebugEventSetListener() {
			@Override
			public void handleDebugEvents(DebugEvent[] events) {
				for (DebugEvent event : events) {
					if (event.getSource() instanceof ITraceClient) {
						view.getViewSite().getWorkbenchWindow().getWorkbench().getDisplay().asyncExec(new Runnable(){
							@Override
							public void run() {
								updateAction(TraceActionDelegate.this.action);
							}
						});
						
						return;
					}
				}
			}
		};
		DebugPlugin.getDefault().addDebugEventListener(debugListener);
		updateAction(action);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		DebugPlugin.getDefault().removeDebugEventListener(debugListener);
	}

}
