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
package ca.uvic.chisel.javasketch.ui.internal.wizards;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import ca.uvic.chisel.javasketch.FilterSettings;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.persistence.ui.internal.FiltersComposite;
import ca.uvic.chisel.javasketch.utils.LaunchConfigurationUtilities;

/**
 * @author Del Myers
 *
 */
public class ImportTraceWizardPage1 extends WizardPage {

	private Text fileText;
	private File file;
	private FileValidator validator;
	private FiltersComposite filtersComposite;
	
	private class FileValidator implements IRunnableWithProgress, Runnable {
		private int WAITING = 0;
		private int RUNNING = 1;
		private int SCHEDULED = 2;
		private int state = WAITING;
		private Timer timer = new Timer();
		private TimerTask task = null;
		private String error = null;
		private String installLocation;
		FilterSettings filterSettings;
		public ILaunchConfiguration launchConfiguration;
		

		/* (non-Javadoc)
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			File file = getFile();
			error = null;
			filterSettings = null;
			ZipFile zFile = null;
			installLocation = null;
			try {
				zFile = new ZipFile(file);
				Enumeration<? extends ZipEntry> entries = zFile.entries();
				ZipEntry configurationEntry = null;
				ZipEntry filtersEntry = null;
				monitor.beginTask("Validating trace file", 3);
				monitor.subTask("Scanning file");
				while (entries.hasMoreElements() && ((configurationEntry == null) || (filtersEntry == null))) {
					ZipEntry entry = entries.nextElement();
					if (entry.getName().endsWith("launch.configuration")) {
						configurationEntry = entry;
					} else if (entry.getName().endsWith(".filters")) {
						filtersEntry = entry;
						int slash = entry.getName().lastIndexOf('/');
						if (slash > 0) {
							installLocation = entry.getName().substring(0, slash);
						}
					}
					if (monitor.isCanceled()) {
						throw new InterruptedException();
					}
				}
				if (configurationEntry == null || filtersEntry == null) {
					error = "Invalid trace file";
					return;
				}
				if (installLocation == null) {
					error = "Invalid trace file";
				}
				monitor.worked(2);
				monitor.subTask("Reading...");
				InputStreamReader filterReader = new InputStreamReader(zFile.getInputStream(filtersEntry));
				InputStreamReader configurationReader = new InputStreamReader(zFile.getInputStream(configurationEntry));
				StringBuilder mementoBuilder = new StringBuilder();
				char[] buf = new char[512];
				int read = -1;
				while ((read = configurationReader.read(buf)) != -1) {
					mementoBuilder.append(buf, 0, read);
				}
				launchConfiguration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(mementoBuilder.toString());
				filterSettings = FilterSettings.load(filterReader, launchConfiguration);

			} catch (ZipException e) {
				error = "Error reading trace file";
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			} catch (CoreException e) {
				error = "Error reading trace configuration";
			} finally {
				monitor.done();
				if (zFile != null) {
					try {
						zFile.close();
					} catch (IOException e) {
						throw new InvocationTargetException(e);
					}
				}
			}
			
			
		}
		
		private void schedule() {
			long time = System.currentTimeMillis();
			synchronized (timer) {
				if (state == SCHEDULED && task != null) {
					task.cancel();
					state = WAITING;
				}
				task = new TimerTask() {
					@Override
					public void run() {
						synchronized(timer) {
							if (state != SCHEDULED) return;
							state = RUNNING;
							getShell().getDisplay().syncExec(FileValidator.this);
							state = WAITING;
						}
					}
				};
				state = SCHEDULED;
				timer.schedule(task, new Date(time + 1000));
			}
			
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
			try {
				dialog.run(true, true, this);
				if (error != null) {
					setErrorMessage(error);
					setPageComplete(false);
				} else {
					setPageComplete(true);
					setErrorMessage(null);

					if (filterSettings != null) {
						filtersComposite.setEnabled(true);
						filtersComposite.setJavaContext(filterSettings.getJavaProjects());
						filtersComposite.setInclusionFilters(filterSettings.getInclusionFilters());
						filtersComposite.setExclusionFilters(filterSettings.getExclusionFilters());
						filtersComposite.setLaunchType(filterSettings.getLaunchType());
						filtersComposite.setFilterShortcut((filterSettings.isUsingProjectClassesOnly()) ? 
								FiltersComposite.SHORTCUT_PROJECT_CLASSES :
								FiltersComposite.SHORTCUT_NONE);
						setMessage(null, DialogPage.WARNING);
						//check the file name 
						IPath stateLocation = SketchPlugin.getDefault().getStateLocation();
						File stateFile = stateLocation.toFile();
						File newLocation = new File(stateFile, installLocation);
						if (newLocation.exists()) {
							setMessage("Trace already exists", DialogPage.WARNING);
						}
					} else {
						filtersComposite.setJavaContext(new IJavaProject[0]);
						filtersComposite.setInclusionFilters(new String[0]);
						filtersComposite.setExclusionFilters(new String[0]);
						filtersComposite.setLaunchType(LaunchConfigurationUtilities.JAVA_LAUNCH_TYPE);
						filtersComposite.setEnabled(false);
					}
				}
			} catch (InvocationTargetException e) {
				SketchPlugin.getDefault().log(e);
				MessageDialog.openError(getShell(), "Error validating trace file", "An error occurred validating the trace file. See the log for more information.");
				setErrorMessage("Error validating trace file.");
				setPageComplete(false);
			} catch (InterruptedException e) {
				setErrorMessage("Trace file was not validated");
				setPageComplete(false);
			}
			
			getWizard().getContainer().updateButtons();
			getWizard().getContainer().updateMessage();
		}
		
	}

	/**
	 * @param pageName
	 */
	protected ImportTraceWizardPage1() {
		super("Import a Trace", "Import a Trace", getTitleImage());
		validator = new FileValidator();
	}
	
	/**
	 * @return
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @return
	 */
	private static ImageDescriptor getTitleImage() {
		return SketchPlugin.imageDescriptorFromPlugin("images/wizban/import-trace.png");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new GridLayout());
		Composite filtersArea = createFiltersComposite(page);
		filtersArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Control fileArea = createFileArea(page);
		fileArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		setControl(page);
	}
	
	/**
	 * @param page
	 * @return
	 */
	private Control createFileArea(Composite page) {
		Composite fileLine = new Composite(page, SWT.NONE);
		fileLine.setLayout(new GridLayout(3, false));
		Label nameLabel = new Label(fileLine, SWT.NONE);
		nameLabel.setText("File name:");
		nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		fileText = new Text(fileLine, SWT.SINGLE | SWT.BORDER);
		fileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setFileName(fileText.getText().trim());			
			}
		});
		fileText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Button browseButton = new Button(fileLine, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String[] { "*.dvt" });
				String fileName = dialog.open();
				fileText.setText(fileName);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		String fileName = getDialogSettings().get("filename");
		if (fileName != null) {
			fileText.setText(fileName);
		}
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		return fileLine;
	}

	/**
	 * @param page
	 * @return
	 */
	private Composite createFiltersComposite(Composite page) {
		Group filtersGroup = new Group(page, SWT.NONE);
		filtersGroup.setText("Filters");
		filtersGroup.setLayout(new GridLayout());
		filtersComposite = new FiltersComposite(filtersGroup);
		filtersComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return filtersGroup;
	}
	
	private void setFileName(String fileName) {
		File file = new File(fileName);
		if (!file.isFile()) {
			filtersComposite.setEnabled(false);
			setErrorMessage("Please select a file to import");
			setPageComplete(false);
			getWizard().getContainer().updateButtons();
			getWizard().getContainer().updateMessage();
		} else {
			this.file = file;
			validator.schedule();
		}
	}
	
	protected FilterSettings getTempFilterSettings() {
		return validator.filterSettings;
	}
	
	protected ILaunchConfiguration getTempLaunchConfiguration() {
		return validator.launchConfiguration;
	}
	
	protected String getTempInstallLocation() {
		return validator.installLocation;
	}

}
