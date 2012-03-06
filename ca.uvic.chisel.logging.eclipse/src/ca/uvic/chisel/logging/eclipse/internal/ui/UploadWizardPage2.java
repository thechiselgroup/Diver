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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;
import ca.uvic.chisel.logging.eclipse.internal.LoggingCategory;

public class UploadWizardPage2 extends WizardPage {

	private TabFolder folder;
	private Group group;
	
	private class LogFilesReader {
		private LoggingCategory category;
		private File[] uploadFiles;
		private int uploadIndex;
		int rawLine = 0;
		public LogFilesReader(LoggingCategory category) {
			this.category = category;
			uploadIndex = -1;
			uploadFiles = null;
			
		}
		public String readNextFile() {
			StringWriter writer = new StringWriter();
			String separator = System.getProperty("line.separator");
			if (separator == null) {
				separator = "\n";
			}
			if (uploadFiles == null) {
				
				File logFile = category.getLog().getLogFile();
				int linesRead = 0;
				if (logFile.exists()) {
					BufferedReader reader = null;
					try {
						reader = new BufferedReader(new FileReader(logFile));
						String line = null;
						//read 50 lines at a time.
						while ((line = reader.readLine()) != null && (linesRead - rawLine < 50)) {
							linesRead++;
							if (linesRead > rawLine) {
								writer.append(line);
								writer.append(separator);
							}
						}
						if (line == null) {
							//get the upload files ready
							rawLine = 0;
							uploadFiles = category.getFilesToUpload();
							uploadIndex = 0;
						} else {
							rawLine = linesRead;
						}
					} catch (IOException e) {
					} finally {
						if (reader != null) {
							try {
								reader.close();
							} catch (IOException e) {
							}
						}
					}
					try {
						writer.close();
					} catch (IOException e) {
					}
					
					return writer.toString();
				} else {
					//start reading the backup files
					rawLine = 0;
					uploadFiles = category.getFilesToUpload();
					uploadIndex = 0;
				}
			}
			if (uploadIndex >= 0 && uploadIndex < uploadFiles.length) {
				File file = uploadFiles[uploadIndex];
				ZipFile zipFile = null;
				try {
					zipFile = new ZipFile(file);
					for (Enumeration<? extends ZipEntry> entries=zipFile.entries(); entries.hasMoreElements();) {
						ZipEntry entry = entries.nextElement();
						InputStream is = zipFile.getInputStream(entry);
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						String line = null;
						int linesRead = 0;
						//read 50 lines at a time
						while ((line = reader.readLine()) != null && (linesRead - rawLine < 50)) {
							linesRead++;
							if (linesRead > rawLine) {
								writer.append(line);
								writer.append(separator);
							}
						}
						if (line == null) {
							//get the upload files ready
							rawLine = 0;
							uploadIndex++;
						} else {
							rawLine = linesRead;
						}
						//there should be only one entry in the zip file.
						break;
					}
					
					return writer.toString();
				} catch (IOException e) {
					WorkbenchLoggingPlugin.getDefault().log(e);
				} finally {
					if (zipFile != null) {
					try {
						zipFile.close();
					} catch (IOException e) {
					}
					}
					try {
						writer.close();
					} catch (IOException e) {}
				}
				
			}
			
			return "";
		}
	}
	
	

	protected UploadWizardPage2() {
		super("Review Upload", "Review Upload", null);
		setPageComplete(true);
	}

	public void createControl(Composite parent) {
		group = new Group(parent, SWT.NONE);
		group.setText("Upload Data");
		group.setLayout(new GridLayout());
		
		
		setControl(group);
	}

	private void createTab(TabFolder folder, String categoryID) {
		ILoggingCategory category = WorkbenchLoggingPlugin.getDefault().getCategoryManager().getCategory(categoryID);
		if (category instanceof LoggingCategory) {
			final LoggingCategory lc = (LoggingCategory) category;
			TabItem tab = new TabItem(folder, SWT.NONE);
			tab.setText(category.getName());
			Composite page = new Composite(folder, SWT.NONE);
			page.setLayout(new GridLayout());
			
			final Text text = new Text(page, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
			final LogFilesReader reader = new LogFilesReader(lc);
			text.getVerticalBar().addSelectionListener(new SelectionListener() {
				int uploadIndex;
				private File[] uploadFiles;
				public void widgetSelected(SelectionEvent e) {
					//lazy load the text so that we don't take up more memory
					//than is needed.
					final int selection = text.getVerticalBar().getSelection();
					int max = text.getVerticalBar().getMaximum();
					int extent = text.getVerticalBar().getThumb();
					
					if (selection >= max - extent) {
						final String nextFile = reader.readNextFile();
						if (!nextFile.isEmpty()) {
							text.getDisplay().asyncExec(new Runnable(){
								public void run() {
									text.setText(text.getText() + nextFile);
								}
							});
							text.getDisplay().asyncExec(new Runnable(){
								public void run() {
									text.setTopIndex(selection);
								}
							});
							
							uploadIndex++;
						}
					}
				}
				
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
			String uploadData = reader.readNextFile();
			text.setText(uploadData);
			text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			tab.setControl(page);
		}
	}

	
	
	@Override
	public void setVisible(boolean visible) {
		if (visible){
			if (folder != null && !folder.isDisposed()) folder.dispose();
			folder = new TabFolder(group, SWT.BOTTOM);
			folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			IWizardPage previous = getWizard().getPreviousPage(this);
			if (previous instanceof UploadWizardPage1){
				String[] categories = ((UploadWizardPage1)previous).getSelectedCategories();
				for (String category : categories) {
					createTab(folder, category);
				}
			}
		}
		group.layout();
		super.setVisible(visible);
	}

}
