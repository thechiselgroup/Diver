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
/**
 * 
 */
package ca.uvic.chisel.logging.eclipse.internal.ui;


import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;

import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;
import ca.uvic.chisel.logging.eclipse.internal.network.UploadJob;


/**
 * @author Del Myers
 *
 */
public class UploadWizard extends Wizard {
	private UploadWizardPage1 categoriesPage;
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#createPageControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);
		setWindowTitle("Uploading Workbench Logs");
	}
	
	@Override
	public void addPages() {
		categoriesPage = new UploadWizardPage1();
		addPage(categoriesPage);
		addPage(new UploadWizardPage2());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		Job.getJobManager().cancel(UploadJob.class);
		new UploadJob(categoriesPage.getSelectedCategories()).schedule();
		saveUpload();
		return true;
	}

	@Override
	public boolean performCancel() {
		saveUpload();
		return super.performCancel();
	}
	
	private void saveUpload() {
		WorkbenchLoggingPlugin.getDefault().getPreferenceStore().setValue(UploadJob.LAST_UPLOAD_KEY, UploadJob.today());
	}

}
