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

import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.launching.ITraceClient;

public class TraceCommandHandler implements IHandler, IElementUpdater, IHandler2 {
	public static final String COMMAND_ID = "ca.uvic.chisel.javasketch.pauseResumeHandler";
	ListenerList listeners;
	private boolean enabled;
	
	public TraceCommandHandler() {
		SketchPlugin.getDefault();
		listeners = new ListenerList();
		enabled = false;
	}

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
		listeners.add(handlerListener);
		//make sure that the listener knows the enabled state.
		handlerListener.handlerChanged(new HandlerEvent(this, true, true));
	}

	@Override
	public void dispose() {
		listeners.clear();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		ICommandService service = (ICommandService) window.getService(ICommandService.class);
		IProcess process = DebugUITools.getCurrentProcess();
		ITraceClient client = SketchPlugin.getDefault().getAssociatedClient(process);
		if (client != null && client.canPauseTrace()) {
			if (!client.isPaused()) {
				client.pauseTrace();
			} else {
				client.resumeTrace();
			}
		}
		service.refreshElements(event.getCommand().getId(), null);
		return null;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isHandled() {
		return true;
	}
	
	private void setEnabled(boolean enablement) {
		if (enablement != enabled) {
			enabled = enablement;
			HandlerEvent event = new HandlerEvent(this, true, false);
			for (Object o : listeners.getListeners()) {
				IHandlerListener listener = (IHandlerListener) o;
				listener.handlerChanged(event);
			}
		}
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
		listeners.remove(handlerListener);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		IProcess process = DebugUITools.getCurrentProcess();
		ITraceClient client = SketchPlugin.getDefault().getAssociatedClient(process);
		if (client == null || client.isTerminated() || !client.canPauseTrace()) {
			//set the icon to "play"
			element.setIcon(SketchPlugin.imageDescriptorFromPlugin(SketchPlugin.PLUGIN_ID, "images/etool16/play_icon.png"));
			element.setText("Resume Trace");
			element.setTooltip("Resumes a paused trace");
			element.setChecked(false);
			setEnabled(false);
		} else {
			if (client.isPaused()) {
				//set the icon to "play"
				element.setIcon(SketchPlugin.imageDescriptorFromPlugin(SketchPlugin.PLUGIN_ID, "images/etool16/play_icon.png"));
				element.setText("Resume Trace");
				element.setTooltip("Resumes a paused trace");
			} else {
				//set the icon to "play"
				element.setIcon(SketchPlugin.imageDescriptorFromPlugin(SketchPlugin.PLUGIN_ID, "images/etool16/pause_icon.png"));
				element.setText("Pause Trace");
				element.setTooltip("Pauses a running trace");
			}
			setEnabled(true);
		}
	}


	@Override
	public void setEnabled(Object evaluationContext) {
//		System.out.println();		
	}

}
