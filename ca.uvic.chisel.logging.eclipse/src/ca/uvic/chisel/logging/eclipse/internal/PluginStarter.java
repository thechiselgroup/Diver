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

import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;
import ca.uvic.chisel.logging.eclipse.internal.network.UploadJob;
import ca.uvic.chisel.logging.eclipse.internal.ui.UploadWizard;

public class PluginStarter implements IStartup {
	InternalWindowListener windowListener = new InternalWindowListener();
	private IExecutionListener commandLogger = new CommandLogger();

	public void earlyStartup() {
		if (!WorkbenchLoggingPlugin.isEnabled()) return;
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				// check if a new upload needs to be done.
				IPreferenceStore store = WorkbenchLoggingPlugin.getDefault()
						.getPreferenceStore();
				long lastUpload = store.getLong(UploadJob.LAST_UPLOAD_KEY);
				long interval = store.getLong(UploadJob.UPLOAD_INTERVAL_KEY);
				if (interval <= 0) {
					store.setValue(UploadJob.UPLOAD_INTERVAL_KEY, UploadJob.UPLOAD_INTERVAL_WEEKLY);
					interval = UploadJob.UPLOAD_INTERVAL_WEEKLY;
				}
				if (lastUpload != 0) {
					long today = UploadJob.today();
					//see if there is anything to upload.
					boolean stale = false;
					for (ILoggingCategory category : WorkbenchLoggingPlugin.getDefault().getCategoryManager().getCategories()) {
						if (((LoggingCategory)category).isStale()) {
							stale = true;
							break;
						}
					}
					if (stale) {
						if (today - lastUpload >= interval) {
							WizardDialog dialog = new WizardDialog(PlatformUI
									.getWorkbench().getActiveWorkbenchWindow()
									.getShell(), new UploadWizard());
							dialog.open();
						}
					}
				} else {
					store.setValue(UploadJob.UPLOAD_INTERVAL_KEY, UploadJob.UPLOAD_INTERVAL_WEEKLY);
					store.setValue(UploadJob.LAST_UPLOAD_KEY, UploadJob.today());
				}
				IWorkbenchWindow[] windows = PlatformUI.getWorkbench()
						.getWorkbenchWindows();
				for (IWorkbenchWindow window : windows) {
					windowListener.windowOpened(window);
				}
				windowListener.initialize(PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow());
				ICommandService commandService = (ICommandService) PlatformUI
						.getWorkbench().getService(ICommandService.class);
				if (commandService != null) {
					commandService.addExecutionListener(commandLogger);
				}
			}

		});
	}

}
