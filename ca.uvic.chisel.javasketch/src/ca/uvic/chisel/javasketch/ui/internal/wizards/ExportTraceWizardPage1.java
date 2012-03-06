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
import java.util.ArrayList;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
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
import org.eclipse.ui.model.WorkbenchLabelProvider;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;
import ca.uvic.chisel.javasketch.ui.internal.views.EmptyThreadFilter;

/**
 * @author Del Myers
 *
 */
public class ExportTraceWizardPage1 extends WizardPage {

	private CheckboxTreeViewer threadViewer;
	private Text fileText;
	private File file;
	private ITrace trace;
	private TreeSet<Integer> threads;
	private Button filterButton;

	public ExportTraceWizardPage1() {
		super("Export A Trace", "Export A Trace", getTitleImage());
	}
	
	/**
	 * @return
	 */
	private static ImageDescriptor getTitleImage() {
		return SketchPlugin.imageDescriptorFromPlugin("images/wizban/export-trace.png");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new GridLayout());
		Control traceArea = createTraceArea(page);
		traceArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Control fileArea = createFileArea(page);
		fileArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		setControl(page);
		validate();
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
				FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
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
		filterButton = new Button(page, SWT.CHECK);
		filterButton.setText("Exclude empty threads");
		boolean showThreads = getDialogSettings().getBoolean("showAllThreads");
		final ViewerFilter threadFilter = new EmptyThreadFilter();
		if (!showThreads) {
			filterButton.setSelection(true);
			threadViewer.addFilter(threadFilter);
		}
		filterButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (filterButton.getSelection()) {
					threadViewer.addFilter(threadFilter);
				} else {
					threadViewer.removeFilter(threadFilter);
				}
				threadViewer.refresh();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		filterButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		return fileLine;
	}

	/**
	 * @param page
	 * @return
	 */
	private Control createTraceArea(Composite page) {
		Group traceGroup = new Group(page, SWT.NONE);
		traceGroup.setLayout(new GridLayout());
		traceGroup.setText("Threads To Export");
		threadViewer = new CheckboxTreeViewer(traceGroup);
		threadViewer.setContentProvider(new ITreeContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
			@Override
			public void dispose() {}
			
			@Override
			public Object[] getElements(Object inputElement) {
				return getChildren(inputElement);
			}
			
			@Override
			public boolean hasChildren(Object element) {
				return (element instanceof ExportTraceWizardPage1 ||
						element instanceof ITrace);
			}
			
			@Override
			public Object getParent(Object element) {
				if (element instanceof ITrace) {
					return ExportTraceWizardPage1.this;
				} else if (element instanceof IThread) {
					return ((IThread)element).getTrace();
				}
				return null;
			}
			
			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof ExportTraceWizardPage1) {
					IProgramSketch[] sketches = SketchPlugin.getDefault().getStoredSketches();
					ITrace[] traces = new ITrace[sketches.length];
					for (int i = 0; i < sketches.length; i++) {
						traces[i] = sketches[i].getTraceData();
					}
					return traces;
				} else if (parentElement instanceof ITrace) {
					return ((ITrace)parentElement).getThreads().toArray();
				}
				return new Object[0];
			}
		});
		threadViewer.setLabelProvider(new WorkbenchLabelProvider());
		threadViewer.setInput(this);
		threadViewer.addCheckStateListener(new ICheckStateListener() {
			
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				boolean checked = event.getChecked();
				ITraceModel element = (ITraceModel) event.getElement();
				updateCheckedState(checked, element);
				
			}

		});
		threadViewer.setSorter(new ViewerSorter(){
			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
			 */
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				TreeViewer tv = (TreeViewer) viewer;
				String s1 = ((ILabelProvider)tv.getLabelProvider()).getText(e1);
				String s2 = ((ILabelProvider)tv.getLabelProvider()).getText(e2);
				return s1.compareTo(s2);
			}
		});
		threadViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//set the trace
		ITrace trace = ((ExportTraceWizard)getWizard()).getTrace();
		if (trace == null) {
			String traceID = getDialogSettings().get("trace");
			//try to get it from saved information
			String[] threads = getDialogSettings().getArray("threads");
			if (traceID != null) {
				IProgramSketch sketch = SketchPlugin.getDefault().getSketch(traceID);
				if (sketch != null) {
					trace = sketch.getTraceData();
					if (trace != null) {
						this.trace = trace;
						if (threads != null && threads.length > 0) {
							for (int i = 0; i < threads.length; i++) {
								ITraceModelProxy proxy = trace.getElement(threads[i]);
								ITraceModel element = proxy.getElement();
								if (element != null) {
									threadViewer.setChecked(element, true);
									updateCheckedState(true, element);
								}
							}

						} else {
							threadViewer.setChecked(trace, true);
							updateCheckedState(true, trace);
						}
					}
				}
				
			}
		} else {
			this.trace = trace;
			threadViewer.setChecked(trace, true);
			updateCheckedState(true, trace);
		}
		return traceGroup;
	}
	
	/**
	 * @param checked
	 * @param element
	 */
	protected void updateCheckedState(boolean checked,
			ITraceModel element) {
		Object[] ces = threadViewer.getCheckedElements();
		if (checked) {
			for (Object ce : ces) {
				ITraceModel checkedElement = (ITraceModel) ce;
				if (!checkedElement.getTrace().equals(element.getTrace())) {
					threadViewer.setChecked(checkedElement, false);
				}
			}
			if (element instanceof ITrace) {
				threadViewer.setCheckedElements(element.getTrace().getThreads().toArray());
			}
			threadViewer.setChecked(element.getTrace(), true);
			threadViewer.setGrayed(element.getTrace(), threadViewer.getCheckedElements().length < element.getTrace().getThreads().size());
		} else {
			if (element instanceof ITrace || threadViewer.getCheckedElements().length <= 1) {
				threadViewer.setGrayedElements(new Object[0]);
				threadViewer.setCheckedElements(new Object[0]);
				setTrace(null);
			} else {
				threadViewer.setGrayChecked(element.getTrace(), true);
			}
		}
		
		updateSelection();
	}
	
	/**
	 * @param object
	 */
	protected void setTrace(ITrace trace) {
		this.trace = trace;
		validate();
	}

	public void validate() {
		setErrorMessage(null);
		setMessage(null);
		setPageComplete(true);
		if (getTrace() == null) {
			setErrorMessage("Please select a trace");
			setPageComplete(false);
		} else if (getDestinationFile() == null) {
			setErrorMessage("Please select a file");
			setPageComplete(false);
		} else {
			File file = getDestinationFile();
			//check to see if the file is valid
			if (!file.exists()) {
				try {
					file.createNewFile();
					file.delete();
				} catch (IOException e) {
					setErrorMessage(file.getName() + " is an invalid file");
					setPageComplete(false);
				}
			} else {
				if (!file.isFile()) {
					setErrorMessage(file.getName() + " is a directory");
					setPageComplete(false);
				} else {
					setMessage(file.getName() + " already exists", DialogPage.WARNING);
					getContainer().updateButtons();
				}
			}
		}
		getContainer().updateButtons();
		getContainer().updateMessage();
	}

	/**
	 * @return
	 */
	public ITrace getTrace() {
		return trace;
	}

	/**
	 * @return
	 */
	public File getDestinationFile() {
		return file;
	}
	
	protected void setFileName(String fileName) {
		file = new File(fileName);
		validate();
	}
	
	protected void save() {
		IDialogSettings settings = getDialogSettings();
		settings.put("filename", fileText.getText());
		Object[] checked = threadViewer.getCheckedElements();
		ArrayList<String> threads = new ArrayList<String>();
		for (Object c : checked) {
			if (c instanceof ITrace) {
				settings.put("trace", ((ITrace)c).getLaunchID());
			} else if (c instanceof IThread) {
				threads.add(((IThread)c).getIdentifier());
			}
		}
		settings.put("showAllThreads", !filterButton.getSelection());
		//save the threads
		settings.put("threads", threads.toArray(new String[threads.size()]));
	}

	/**
	 * 
	 */
	private void updateSelection() {
		threads = new TreeSet<Integer>();
		trace = null;
		for (Object o  : threadViewer.getCheckedElements()) {
			if (o instanceof ITrace) {
				trace = (ITrace) o;
			} else if (o instanceof IThread) {
				threads.add(((IThread)o).getID());
			}
		}
		validate();
	}
	
	public int[] getThreadIDs() {
		if (threads == null) {
			return new int[0];
		}
		int[] array = new int[threads.size()];
		int i = 0;
		for (Integer t : threads) {
			array[i++] = t;
		}
		return array;
	}

}
