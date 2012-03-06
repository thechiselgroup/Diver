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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.launching.ITraceClient;

/**
 * @author Del Myers
 *
 */

public class TraceDurationCommandHandler extends AbstractHandler implements IElementUpdater {
	
	
	protected static final String COMMAND_ID = "ca.uvic.chisel.javasketch.commands.time";
	private TimerDialog dialog;

	
		
	@Override
	public void dispose() {
		super.dispose();
		if (dialog != null && !dialog.getShell().isDisposed()) {
			dialog.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (dialog == null) {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			dialog = new TimerDialog(shell);
			dialog.open();
			dialog.getShell().addDisposeListener(new DisposeListener() {

				@Override
				public void widgetDisposed(DisposeEvent e) {
					forceUpdate();
					
				}
				
			});
		} else {
			forceUpdate();
			if (dialog.isOpen()) {
				dialog.close();
			} else {
				dialog.open();
			}
		}
		return null;
	}

	void forceUpdate() {
		IWorkbenchWindow window = SketchPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		ICommandService service = (ICommandService) window.getService(ICommandService.class);
		if (service != null) {
			service.refreshElements(COMMAND_ID, null);
		}
	}


	/* (non-Javadoc)
	 * @see org.eclipse.ui.commands.IElementUpdater#updateElement(org.eclipse.ui.menus.UIElement, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		IProcess process = DebugUITools.getCurrentProcess();
		ITraceClient client = SketchPlugin.getDefault().getAssociatedClient(process);
		//set the checked state
		element.setChecked(dialog != null && dialog.isOpen());
		if (dialog != null) {
			if (client == null) {
				dialog.setStartTime(0);
				dialog.setEndTime(0);
			} else {
				dialog.setStartTime(client.getAttachTime());
				if (client.isTerminated()) {
					//set the end time
					dialog.setEndTime(client.getTerminationTime());
				}
			}
		}
	}

}
