/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.feature.internal.commands;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.commands.ICommandService;

import ca.uvic.chisel.feature.DiverPlugin;
import ca.uvic.chisel.feature.internal.IResearchPreferences;

/**
 * @author Del Myers
 * 
 */
public class ResearchBrowserHandler extends AbstractHandler {

	private static final String RESEARCH_COMMAND = 
		"ca.uvic.chisel.diver.feature.OpenResearchCommand";

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.
	 * ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			DiverPlugin.getDefault().getPreferenceStore().setValue(IResearchPreferences.HAS_PARTICIPATED, true);
			IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport()
				.createBrowser("ca.uvic.chisel.diver.browser");
			browser.openURL(new URL("http://keg.cs.uvic.ca/limesurvey/index.php?sid=26778&newtest=Y&lang=en"));
		} catch (PartInitException e1) {
			DiverPlugin.getDefault().log(e1);
		} catch (MalformedURLException e1) {
			DiverPlugin.getDefault().log(e1);
		}
		return null;
	}

	public static final Command getCommand() {
		ICommandService service = (ICommandService) PlatformUI.getWorkbench()
			.getService(ICommandService.class);
		if (service != null) {
			return service.getCommand(RESEARCH_COMMAND);
		}
		return null;
	}

}
