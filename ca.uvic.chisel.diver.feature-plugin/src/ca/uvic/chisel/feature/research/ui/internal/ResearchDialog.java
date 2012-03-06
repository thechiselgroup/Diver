/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.feature.research.ui.internal;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.ISketchImageConstants;

/**
 * A dialog inviting users to participate in a study.
 * 
 * @author Del Myers
 * 
 */
public class ResearchDialog extends FormDialog {
	static final String TEXT = "<form><p>Diver is a free set of tools given to you by the CHISEL "
			+ "group at the University of Victoria. It is part of a "
			+ "research project entitled Reverse Engineered Sequence "
			+ "Diagrams to Support Software Evolution. It is designed to "
			+ "help us understand ways to make programming and "
			+ "reverse engineering tasks easier for you. So, we invite you "
			+ "to participate in our studies. It is easy. "
			+ "Simply select <a href='participate'>Participate &gt;</a> to go to our web site and "
			+ "fill out our survey, or file a bug, or make a feature "
			+ "request. It won’t take much time, and it will help us to "
			+ "make better tools for you in the future.</p>"
			+ "<p>If you haven't had much opportunity to use Diver, select <b>Later</b>"
			+ "You will be reminded to participate in one week's time.</p>"
			+ "<p>You can stop this dialog from displaying again by going to"
			+ "the Diver Research preference page.</p>"
			+ "<p>Thank you for your participation.</p></form>";

	/**
	 * @param parentShell
	 */
	public ResearchDialog(Shell parentShell) {
		super(parentShell);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.forms.FormDialog#createFormContent(org.eclipse.ui.forms
	 * .IManagedForm)
	 */
	@Override
	protected void createFormContent(IManagedForm mform) {
		FormToolkit tk = mform.getToolkit();
		ScrolledForm form = mform.getForm();
		form.setText("Thank You For Using Diver");
		form.getBody().setLayout(new GridLayout());
		FormText text = tk.createFormText(form.getBody(), false);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		text.setText(TEXT, true, false);
		FontData fd = text.getFont().getFontData()[0];
		final Font largefont = new Font(text.getDisplay(), fd.getName(), 11,
			SWT.NORMAL);
		text.setFont(largefont);
		text.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				largefont.dispose();
			}
		});
		text.addHyperlinkListener(new IHyperlinkListener() {

			@Override
			public void linkExited(HyperlinkEvent e) {
			}

			@Override
			public void linkEntered(HyperlinkEvent e) {
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				Object o = e.getHref();
				if ("participate".equals(o)) {
					setReturnCode(IDialogConstants.OK_ID);
					close();
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create the standard OK and cancel buttons, but reset their text
		super.createButtonsForButtonBar(parent);
		Button b = getButton(IDialogConstants.OK_ID);
		b.setText("Participate >");
		b = getButton(IDialogConstants.CANCEL_ID);
		b.setText("Later");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets
	 * .Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setImage(SketchPlugin.getDefault().getImageRegistry().get(
			ISketchImageConstants.ICON_LOGO));
		newShell.setText("Diver Research");

		newShell.setSize(500, 500);
	}
	
	

}
