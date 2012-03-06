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
package ca.uvic.chisel.logging.eclipse.internal.ui;

import java.util.HashSet;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;
import ca.uvic.chisel.logging.eclipse.internal.LoggingCategory;
import ca.uvic.chisel.logging.eclipse.internal.network.UploadJob;

public class WorkbenchLoggerPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private static final String EDIT_COPY = "org.eclipse.ui.edit.copy";
	private HashSet<String> enabledCategories;
	private CheckboxTableViewer viewer;
	private Button aboutButton;
	private ComboViewer intervalViewer;

	public WorkbenchLoggerPreferencePage() {
		enabledCategories = new HashSet<String>();
	}

	public WorkbenchLoggerPreferencePage(String title) {
		super(title);
	}

	public WorkbenchLoggerPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new GridLayout(2, false));

		// create a list viewer that will display all of the
		// different loggers

		viewer = CheckboxTableViewer
				.newCheckList(page, SWT.BORDER | SWT.SINGLE);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LoggingCategoryLabelProvider());
		viewer.setInput(WorkbenchLoggingPlugin.getDefault().getCategoryManager()
				.getCategories());
		// set all of the enabled categories to the checked state
		boolean stale = false;
		for (ILoggingCategory category : WorkbenchLoggingPlugin.getDefault()
				.getCategoryManager().getCategories()) {
			if (category.isEnabled()) {
				enabledCategories.add(category.getCategoryID());
				viewer.setChecked(category, true);
			}
			// also set the stale state... used for enabling the upload button.
			stale |= ((LoggingCategory) category).isStale();
		}
		viewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getElement() instanceof ILoggingCategory) {
					ILoggingCategory category = (ILoggingCategory) event
							.getElement();
					if (event.getChecked()) {
						enabledCategories.add(category.getCategoryID());
					} else {
						enabledCategories.remove(category.getCategoryID());
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

		viewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));

		// create a button area
		Composite buttonArea = new Composite(page, SWT.NONE);
		buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		buttonArea.setLayout(new GridLayout());

		GridDataFactory gdf = GridDataFactory.createFrom(new GridData(SWT.FILL,
				SWT.FILL, true, false));
		Button selectAll = new Button(buttonArea, SWT.PUSH);
		selectAll.setText("Select All");
		selectAll.setLayoutData(gdf.create());
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setAllChecked(true);
				for (ILoggingCategory category : WorkbenchLoggingPlugin.getDefault()
						.getCategoryManager().getCategories()) {
					enabledCategories.add(category.getCategoryID());
				}
			}
		});

		Button selectNone = new Button(buttonArea, SWT.PUSH);
		selectNone.setText("Select None");
		selectNone.setLayoutData(gdf.create());
		selectNone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setAllChecked(false);
				enabledCategories.clear();
			}
		});

		Control spacer = new Composite(buttonArea, SWT.NONE);
		GridData d = gdf.create();
		d.heightHint = 40;
		spacer.setLayoutData(d);

		aboutButton = new Button(buttonArea, SWT.PUSH);
		aboutButton.setText("About...");
		aboutButton.setLayoutData(gdf.create());
		aboutButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				ISelection selection = viewer.getSelection();
				if (selection instanceof IStructuredSelection) {
					IStructuredSelection ss = (IStructuredSelection) selection;
					if (!ss.isEmpty()
							&& ss.getFirstElement() instanceof ILoggingCategory) {
						AboutCategoryDialog dialog = new AboutCategoryDialog(
								getShell(), (ILoggingCategory) ss
										.getFirstElement());
						dialog.open();
					}
				}
			}
		});
		aboutButton.setEnabled(false);

		spacer = new Composite(buttonArea, SWT.NONE);
		d = gdf.create();
		d.heightHint = 40;
		spacer.setLayoutData(d);

		Button uploadButton = new Button(buttonArea, SWT.PUSH);
		uploadButton.setText("Upload Now...");
		uploadButton.setLayoutData(gdf.create());
		uploadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardDialog dialog = new WizardDialog(getShell(),
						new UploadWizard());
				dialog.open();
			}
		});
		uploadButton.setEnabled(stale);
		Composite intervalComposite = new Composite(page, SWT.NONE);
		GridData gd = gdf.create();
		gd.grabExcessVerticalSpace = false;
		gd.grabExcessHorizontalSpace = true;
		intervalComposite.setLayoutData(gd);
		intervalComposite.setLayout(new GridLayout(2, false));
		Label intervalLabel = new Label(intervalComposite, SWT.NONE);
		intervalLabel.setText("Upload Interval: ");
		gd = gdf.create();
		gd.grabExcessVerticalSpace = false;
		gd.grabExcessHorizontalSpace = false;
		intervalLabel.setLayoutData(gd);
		intervalViewer = new ComboViewer(intervalComposite, SWT.BORDER | SWT.SINGLE);
		Long[] intervals = new Long[] { UploadJob.UPLOAD_INTERVAL_DAILY,
				UploadJob.UPLOAD_INTERVAL_WEEKLY,
				UploadJob.UPLOAD_INTERVAL_MONTHLY };
		intervalViewer.setContentProvider(new ArrayContentProvider());
		intervalViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Long) {
					long interval = (Long) element;
					if (interval == UploadJob.UPLOAD_INTERVAL_DAILY) {
						return "Daily";
					} else if (interval == UploadJob.UPLOAD_INTERVAL_WEEKLY) {
						return "Every Seven Days";
					} else if (interval == UploadJob.UPLOAD_INTERVAL_MONTHLY) {
						return "Every Thirty Days";
					}
				}
				return super.getText(element);
			}
		});
		intervalViewer.setInput(intervals);
		long interval = WorkbenchLoggingPlugin
		.getDefault().getPreferenceStore().getLong(
				UploadJob.UPLOAD_INTERVAL_KEY);
		if (interval <= 0) {
			interval = WorkbenchLoggingPlugin
			.getDefault().getPreferenceStore().getDefaultLong(
					UploadJob.UPLOAD_INTERVAL_KEY);
		}
		intervalViewer.setSelection(new StructuredSelection(interval));
		intervalViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		spacer = new Composite(page, SWT.NONE);
		gd = gdf.create();
		gd.grabExcessVerticalSpace = false;
		gd.grabExcessHorizontalSpace = false;
		gd.heightHint = 2;
		spacer.setLayoutData(gd);
		Composite uidComposite = new Composite(page, SWT.NONE);
		uidComposite.setLayout(new GridLayout(2, false));
		gd = gdf.create();
		gd.grabExcessVerticalSpace = false;
		gd.grabExcessHorizontalSpace = false;
		gd.horizontalSpan = 2;
		uidComposite.setLayoutData(gd);
		Label uidLabel = new Label(uidComposite, SWT.NONE);
		uidLabel.setText("User ID:");
		gd = gdf.create();
		gd.grabExcessVerticalSpace = false;
		gd.grabExcessHorizontalSpace = false;
		uidLabel.setLayoutData(gd);
		final Text uidText = new Text(uidComposite, SWT.SINGLE | SWT.READ_ONLY);
		uidText.setText(WorkbenchLoggingPlugin.getDefault().getLocalUser());
		uidText.addMouseListener(new MouseAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.MouseAdapter#mouseUp(org.eclipse.swt.events.MouseEvent)
			 */
			@Override
			public void mouseUp(MouseEvent e) {
				uidText.selectAll();
			}
		});
		MenuManager manager = new MenuManager();
		Menu menu = manager.createContextMenu(uidText);
		uidText.setMenu(menu);
		CommandContributionItemParameter parameters = new CommandContributionItemParameter(
			WorkbenchLoggingPlugin.getDefault().getWorkbench(), null, EDIT_COPY, SWT.PUSH);
		manager.add(new CommandContributionItem(parameters));
		return page;
	}

	public void init(IWorkbench workbench) {
	}

	@Override
	protected void performApply() {
		
		super.performApply();
	}
	@Override
	public boolean performOk() {
		for (ILoggingCategory category : WorkbenchLoggingPlugin.getDefault()
				.getCategoryManager().getCategories()) {
			boolean enabled = enabledCategories.contains(category
					.getCategoryID());
			if (category.isEnabled() != enabled) {
				category.setEnabled(enabled);
			}
		}
		IPreferenceStore store = WorkbenchLoggingPlugin.getDefault().getPreferenceStore();
		IStructuredSelection selection = (IStructuredSelection) intervalViewer.getSelection();
		store.setValue(UploadJob.UPLOAD_INTERVAL_KEY, (long)((Long)selection.getFirstElement()));
		return super.performOk();
	}

}
