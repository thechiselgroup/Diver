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

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.ui.handlers.IHandlerService;

import ca.uvic.chisel.javasketch.SketchPlugin;

/**
 * Quick utility to execute commands
 * @author Del Myers
 *
 */
public class CommandExecuter {

	public static void execute(String commandId, Map<?,?> parameters) {
		//run a fake command to log it
		IHandlerService hs = (IHandlerService) SketchPlugin.getDefault().getWorkbench().getService(IHandlerService.class);
		if (hs != null) {
			try {
				hs.executeCommand(commandId, null);
			} catch (CommandException e) {
				SketchPlugin.getDefault().log(e);
			}
		}
	}

}
