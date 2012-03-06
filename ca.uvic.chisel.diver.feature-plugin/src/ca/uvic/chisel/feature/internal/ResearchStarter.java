/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Del Myers - initial API and implementation
 ******************************************************************************/
package ca.uvic.chisel.feature.internal;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.feature.DiverPlugin;
import ca.uvic.chisel.feature.internal.commands.ResearchBrowserHandler;
import ca.uvic.chisel.feature.research.ui.internal.ResearchDialog;

/**
 * A class that starts early in order to check to see if the user has elected to
 * fill out the survey provided for research purposes. If they have not
 * elected to complete the survey, then they will be asked one week from the
 * last time they were asked.
 * @author Del Myers
 *
 */
public class ResearchStarter implements IStartup {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	@Override
	public void earlyStartup() {
//		IPreferenceStore preferences = 
//			DiverPlugin.getDefault().getPreferenceStore();
//		if (!preferences.getBoolean(IResearchPreferences.REMIND_PARTICIPATE)) {
//			return;
//		}
//		boolean hasParticipated =
//			preferences.getBoolean(IResearchPreferences.HAS_PARTICIPATED);
//		if (hasParticipated) 
//			return;
//		long lastDate = 
//			preferences.getLong(IResearchPreferences.LAST_QUERY_DATE);
//		if (lastDate <= 0) {
//			lastDate = System.currentTimeMillis();
//			//don't ask until a week into their participation
//			preferences.setValue(IResearchPreferences.LAST_QUERY_DATE, 
//				lastDate);
//			return;
//		}
//		long today = System.currentTimeMillis();
//		long diff = today - lastDate;
//		long days = Math.round((double)diff/(1000*60*60*24));
//		if (days >= 7) {
//			preferences.setValue(IResearchPreferences.LAST_QUERY_DATE, 
//				today);
//			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable(){
//				@Override
//				public void run() {
//					askToParticipate();
//				}
//			});
//			
//		}
	}

	/**
	 * 
	 */
//	private void askToParticipate() {
//		IWorkbench workbench = PlatformUI.getWorkbench();
//		ResearchDialog dialog = new ResearchDialog(
//			workbench.getActiveWorkbenchWindow().getShell()
//		);
//		int result = dialog.open();
//		IPreferenceStore preferences = 
//			DiverPlugin.getDefault().getPreferenceStore();
//		if (result == IDialogConstants.OK_ID) {
//			//go to the research page, and set the participated value
//			Command c = ResearchBrowserHandler.getCommand();
//			if (c != null) {
//				ExecutionEvent event = new ExecutionEvent();
//				try {
//					c.executeWithChecks(event);
//				} catch (CommandException e1) {
//					DiverPlugin.getDefault().log(e1);
//				}
//			}
//		} else {
//			preferences.setValue(
//				IResearchPreferences.LAST_QUERY_DATE,
//				System.currentTimeMillis()
//			);
//		}
//	}

}
