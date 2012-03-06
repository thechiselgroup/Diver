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

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * A Wizard page that allows users to upload their usage data.
 * @author Del Myers
 *
 */
public class UploadWizardPage1 extends WizardPage {

	private Button aboutButton;
	private CheckboxTableViewer viewer;
	private HashSet<String> selectedCategories;
	private Button acceptButton;

	protected UploadWizardPage1() {
		super("Workbench Logs", "Select Logs to Upload", null);
		selectedCategories = new HashSet<String>();
		setPageComplete(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite page = new Composite(parent, SWT.BORDER);
		page.setLayout(new GridLayout());
		Browser b = new Browser(page, SWT.NONE);
		b.setText("<HTML><body>" +
				"<p>You have selected to log various user interactions in you Eclipse " +
				"workbench. The following loggers have been installed and the data gathered " +
				"by them will now be uploaded to thier servers. Please review the logs, accept " +
				"the disclaimers, and select <i>Finish</i> to upload the data.</p>" +
				"<p>You can disable the loggers at any time using the <i>Workbench Logger</i> " +
				"preference page</p>" +
				"</body></html>");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 200;
		b.setLayoutData(gd);
		Composite categoriesArea = createCategoriesArea(page);
		categoriesArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		acceptButton = new Button(page, SWT.CHECK);
		acceptButton.setText("I accept the disclaimers of the selected loggers.");
		acceptButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean complete = ((Button)e.widget).getSelection();
				if (!complete) {
					setErrorMessage("You must accept the disclaimers for the selected loggers");
				} else {
					setErrorMessage(null);
				}
				setPageComplete(complete);
			}
		});
		setControl(page);
		setErrorMessage("You must accept the disclaimers for the selected loggers");
	}

	private Composite createCategoriesArea(Composite parent) {
		Composite categoriesArea = new Composite(parent, SWT.NONE);
		categoriesArea.setLayout(new GridLayout(2, false));
		
		//create a list viewer that will display all of the 
		//different loggers
		
		viewer = CheckboxTableViewer.newCheckList(categoriesArea, SWT.BORDER|SWT.SINGLE);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LoggingCategoryLabelProvider());
		viewer.setInput(WorkbenchLoggingPlugin.getDefault().getCategoryManager().getCategories());
		//set all of the enabled categories to the checked state
		for (ILoggingCategory category : WorkbenchLoggingPlugin.getDefault().getCategoryManager().getCategories()) {
			selectedCategories.add(category.getCategoryID());
		}
		viewer.setAllChecked(true);
		viewer.addCheckStateListener(new ICheckStateListener() {
			
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getElement() instanceof ILoggingCategory) {
					ILoggingCategory category = (ILoggingCategory) event.getElement();
					if (event.getChecked()){
						selectedCategories.add(category.getCategoryID());
					} else {
						selectedCategories.remove(category.getCategoryID());
					}
				}
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (aboutButton != null && !aboutButton.isDisposed()) {
					aboutButton.setEnabled(!event.getSelection().isEmpty());
				}
			}
		});
		
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//create a button area
		Composite buttonArea = new Composite(categoriesArea, SWT.NONE);
		buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		buttonArea.setLayout(new GridLayout());
		
		GridDataFactory gdf = GridDataFactory.createFrom(new GridData(SWT.FILL, SWT.FILL, true, false));
		Button selectAll = new Button(buttonArea, SWT.PUSH);
		selectAll.setText("Select All");
		selectAll.setLayoutData(gdf.create());
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setAllChecked(true);
				for (ILoggingCategory category : WorkbenchLoggingPlugin.getDefault().getCategoryManager().getCategories()) {
					selectedCategories.add(category.getCategoryID());
				}
			}
		});
		
		Button selectNone = new Button(buttonArea, SWT.PUSH);
		selectNone.setText("Select None");
		selectNone.setLayoutData(gdf.create());
		selectNone.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setAllChecked(false);
				selectedCategories.clear();
			}
		});
		
		Control spacer = new Composite(buttonArea, SWT.NONE);
		GridData d = gdf.create();
		d.heightHint = 40;
		spacer.setLayoutData(d);
		
		aboutButton = new Button(buttonArea, SWT.PUSH);
		aboutButton.setText("Disclaimer...");
		aboutButton.setLayoutData(gdf.create());
		aboutButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISelection selection = viewer.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection ss = (IStructuredSelection) selection;
					if (!ss.isEmpty() && ss.getFirstElement() instanceof ILoggingCategory) {
						AboutCategoryDialog dialog = new AboutCategoryDialog(getShell(), (ILoggingCategory)ss.getFirstElement());
						dialog.open();
					}
				}
			}
		});
		aboutButton.setEnabled(false);
		return categoriesArea;
	}
	
	public String[] getSelectedCategories() {
		String[] categories = 
			selectedCategories.toArray(new String[selectedCategories.size()]);
		Arrays.sort(categories);
		return categories;
	}
	
	

}
