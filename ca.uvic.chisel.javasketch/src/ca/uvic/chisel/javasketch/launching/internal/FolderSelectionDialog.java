/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.launching.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * @author Del Myers
 *
 */
public class FolderSelectionDialog extends ElementTreeSelectionDialog {
	/**
	 * @author Del Myers
	 *
	 */
	private final class CreateFolderAction extends SelectionAdapter {
		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(SelectionEvent e) {
			ISelection selection = getTreeViewer().getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				if (ss.size() == 1) {
					final IContainer element = (IContainer) ss.getFirstElement();
					final String newFolderName = folderNameText.getText();
					try {
						PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress(){
							@Override
							public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
								monitor.beginTask("Creating Folder", 3);
								IFolder folder = element.getFolder(new Path(newFolderName));
								ISchedulingRule rule = element.getWorkspace().getRuleFactory().createRule(folder);
								Job.getJobManager().beginRule(rule, new SubProgressMonitor(monitor,1));
								monitor.worked(1);
								try {
									
									if (!folder.exists()) {
										folder.create(false, true, new SubProgressMonitor(monitor,1));
										monitor.worked(1);
										element.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor,1));
										monitor.worked(1);

									}
								} catch (CoreException e1) {
									MessageDialog.openError(getShell(), "Error Creating Folder", e1.getMessage());
								} finally {
									Job.getJobManager().endRule(rule);
									monitor.done();
								}
							}
						});
					} catch (InvocationTargetException e1) {
					} catch (InterruptedException e1) {
					}
					getTreeViewer().refresh();

				}
			}
		}
	}

	private static class FolderContentProvider implements ITreeContentProvider {
		private WorkbenchContentProvider proxy;
		public FolderContentProvider() {
			proxy = new WorkbenchContentProvider();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		@Override
		public Object[] getChildren(Object parentElement) {
			Object[] children = proxy.getChildren(parentElement);
			ArrayList<Object> result = new ArrayList<Object>();
			for (Object o : children) {
				if (o instanceof IContainer) {
					result.add(o);
				}
			}
			return result.toArray();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		@Override
		public Object getParent(Object element) {
			return proxy.getParent(element);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		@Override
		public void dispose() {
			proxy.dispose();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			proxy.inputChanged(viewer, oldInput, newInput);
		}
		
	}

	private Text folderNameText;
	private Button folderNameButton;
	private ISelectionChangedListener treeListener;
	
	
	/**
	 * @param parent
	 * @param labelProvider
	 * @param contentProvider
	 */
	public FolderSelectionDialog(Shell parent) {
		super(parent, new WorkbenchLabelProvider(), new FolderContentProvider());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite page = new Composite (parent, SWT.NONE);
		page.setLayout(new GridLayout());
		Control treeHolder = super.createDialogArea(page);
		treeHolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite newFolderComposite = new Composite(page, SWT.NONE);
		newFolderComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		newFolderComposite.setLayout(new GridLayout(2, false));
		folderNameText = new Text(newFolderComposite, SWT.SINGLE | SWT.BORDER);
		folderNameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		folderNameButton = new Button(newFolderComposite, SWT.PUSH);
		folderNameButton.setText("New Folder");
		folderNameButton.setLayoutData(new GridData(SWT.END, SWT.FILL, false, false));
		
		folderNameButton.addSelectionListener(new CreateFolderAction());
		
		treeListener = new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					if (((IStructuredSelection)selection).size() == 1) {
						folderNameButton.setEnabled(true);
						folderNameText.setEnabled(true);
						return;
					}
				}
				folderNameButton.setEnabled(false);
				folderNameText.setEnabled(false);
			}
		};
		
		treeListener.selectionChanged(new SelectionChangedEvent(getTreeViewer(), getTreeViewer().getSelection()));
		getTreeViewer().addSelectionChangedListener(treeListener);
		
		return page;
	}
}
