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
package ca.uvic.chisel.feature.research.ui.internal;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;

import ca.uvic.chisel.feature.DiverPlugin;
import ca.uvic.chisel.feature.internal.IResearchPreferences;
import ca.uvic.chisel.feature.internal.commands.ResearchBrowserHandler;

/**
 * @author Del Myers
 *
 */
public class DiverResearchPreferencePage extends PreferencePage
		implements IWorkbenchPreferencePage {
	boolean remindStatus = false;
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		setPreferenceStore(DiverPlugin.getDefault().getPreferenceStore());
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new GridLayout());
		Form form = new Form(page, SWT.NONE);
		form.getBody().setLayout(new GridLayout());
		FormText text = new FormText(form.getBody(), SWT.MULTI);
		text.setText( 
			 "<form><p>Thank you for using Diver. Diver is a research project " +
				"of the University of Victoria CHISEL group. This page " +
				"makes it possible for you to be a part of our research.</p>" +
				"<p>Visit our <a href='participate'>website</a> to participate.</p></form>",
				true, false);
		text.setBackground(page.getBackground());
		GridData gd = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, true);
		gd.widthHint = 200;
		text.setLayoutData(gd);
		form.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, true));
		text.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkExited(HyperlinkEvent e) {}
			@Override
			public void linkEntered(HyperlinkEvent e) {}
			@Override
			public void linkActivated(HyperlinkEvent e) {
				participate();
			}
		});
		Button remindButton = new Button(page, SWT.CHECK);
		remindStatus = getPreferenceStore().getBoolean(IResearchPreferences.REMIND_PARTICIPATE);
		remindButton.setSelection(remindStatus);
		remindButton.setText("Remind me to participate");
		remindButton.setEnabled(!getPreferenceStore().getBoolean(IResearchPreferences.HAS_PARTICIPATED));
		remindButton.setFocus();
		remindButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				remindStatus = !remindStatus;
				setValid(false);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		return page;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(DiverPlugin.getDefault().getPreferenceStore());		
	}
	
	private void participate() {
		//go to the research page, and set the participated value
		Command c = ResearchBrowserHandler.getCommand();
		if (c != null) {
			ExecutionEvent event = new ExecutionEvent();
			try {
				c.executeWithChecks(event);
			} catch (CommandException e1) {
				DiverPlugin.getDefault().log(e1);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	@Override
	protected void performApply() {
		super.performApply();
		getPreferenceStore().setValue(IResearchPreferences.REMIND_PARTICIPATE, remindStatus);
	}

}
