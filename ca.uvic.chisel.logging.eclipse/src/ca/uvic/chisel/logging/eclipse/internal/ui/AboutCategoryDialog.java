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

import java.net.URL;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import ca.uvic.chisel.logging.eclipse.ILoggingCategory;

/**
 * A dialog for showing information about a logging category.
 * @author Del Myers
 *
 */
public class AboutCategoryDialog extends Dialog {

	private ILoggingCategory category;

	protected AboutCategoryDialog(Shell parentShell, ILoggingCategory category) {
		super(parentShell);
		this.category = category;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite page = (Composite) super.createDialogArea(parent);
		page.setLayout(new GridLayout());
		
		Group disclaimerGroup = new Group(page, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 300;
		gd.heightHint = 200;
		disclaimerGroup.setLayoutData(gd);
		disclaimerGroup.setLayout(new FillLayout());
		disclaimerGroup.setText("Disclaimer");
		Browser disclaimer = new Browser(disclaimerGroup, SWT.BORDER);
		String html = category.getDisclaimer();
		if (category.isHTML()) {
			html = "<HTML><BODY>" + html + "</BODY></HTML>";
		}
		disclaimer.setText(html);
		
		Label providerLabel = new Label(page, SWT.READ_ONLY|SWT.SINGLE);
		providerLabel.setText("Provider: " + category.getProvider());
		providerLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		Label urlLabel = new Label(page, SWT.READ_ONLY|SWT.SINGLE);
		URL url = category.getURL();
		urlLabel.setText("URL: " + ((url != null) ? category.getURL().toString() : "No upload site."));
		urlLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		getShell().setText("About");
		return page;
	}

}
