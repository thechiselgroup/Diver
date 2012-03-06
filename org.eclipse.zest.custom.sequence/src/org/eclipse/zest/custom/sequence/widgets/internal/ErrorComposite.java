/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers -- initial API and implementation
 *******************************************************************************/
package org.eclipse.zest.custom.sequence.widgets.internal;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A composite designed for displaying an error.
 * @author Del Myers
 *
 */
public class ErrorComposite extends Composite {
	private String detailsString;
	private Composite page;
	private Text details;
	private Label message;
	private boolean showingDetails;
	private Composite detailsPage;
	private Button detailsButton;

	/**
	 * Creates a composite with an error icon, a details button, and a text area to display the 
	 * error.
	 * @param parent
	 * @param style
	 */
	public ErrorComposite(Composite parent, int style) {
		super(parent, style);
		setLayout(new FillLayout());
		this.page = new Composite(this, SWT.NONE);
		page.setLayout(new GridLayout(2, false));
		message = new Label(page, SWT.READ_ONLY | SWT.WRAP);
		message.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));
		message.setImage(getDisplay().getSystemImage(SWT.ICON_ERROR));
		detailsButton = new Button(page, SWT.PUSH);
		detailsButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				showingDetails = !showingDetails;
				updateDetails();
			}
		});
		detailsButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		this.showingDetails = false;
		this.detailsPage = new Composite(page, SWT.NONE);
		GridData pageData = new GridData(SWT.FILL, SWT.FILL, true, true);
		pageData.horizontalSpan=2;
		detailsPage.setLayoutData(pageData);
		detailsPage.setLayout(new FillLayout());
		updateDetails();
	}

	/**
	 * 
	 */
	private void updateDetails() {
		if (!showingDetails) {
			detailsButton.setText(IDialogConstants.SHOW_DETAILS_LABEL);
			if (details != null && !details.isDisposed()) {
				details.dispose();
				details = null;
			}
		} else {
			detailsButton.setText(IDialogConstants.HIDE_DETAILS_LABEL);
			if (details == null || details.isDisposed()) {
				details = new Text(detailsPage, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
				details.setEditable(false);
			}
			details.setText(getDetailsText());
			detailsPage.layout();
		}
		layout(true, true);
	}

	/**
	 * @return
	 */
	private String getDetailsText() {
		if (detailsString == null) {
			return "";
		}
		return detailsString;
	}
	
	public void setError(Throwable t) {
		if (t == null) {
			message.setText("");
			detailsString = "";
		} else {
			String m = t.getMessage();
			message.setText((m != null) ? m : "Error");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.flush();
			pw.close();
			detailsString = sw.toString();
		}
		updateDetails();
		
	}

}
