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

import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

import ca.uvic.chisel.javasketch.SketchPlugin;

/**
 * @author Del Myers
 *
 */
public class CommandAction extends Action {
	
	private String commandId;
	private Map<?, ?> parameters;

	public CommandAction(String commandId, Map<?,?> parameters) {
		this.commandId = commandId;
		this.parameters = parameters;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		runWithEvent(null);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#runWithEvent(org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void runWithEvent(Event event) {
		IHandlerService hs = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
		ICommandService cs = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		Command command = cs.getCommand(commandId);
		if (command != null) {
			try {
				ParameterizedCommand pc = ParameterizedCommand.generateCommand(command, parameters);
				hs.executeCommand(pc, event);
			} catch (CommandException e) {
				SketchPlugin.getDefault().log(e);
			} 
		}
	}

}
