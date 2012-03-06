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
package ca.uvic.chisel.javasketch.ui.internal.views.java;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import ca.uvic.chisel.javasketch.ui.internal.MarkTypeJob;

/**
 * @author Del Myers
 *
 */
public class RefreshEditorsJob extends UIJob {

	/**
	 * @param name
	 */
	public RefreshEditorsJob() {
		super("Refreshing Java Editors");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		//get the top editor and refresh it. The others will be refreshed when they are opened.
		IEditorPart editor = null;
		try {
			editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		} catch (NullPointerException e) {

		}
		if (editor != null) {
			monitor.beginTask("Annotating Editors", 1);
			try {
				IEditorInput input = editor.getEditorInput();
				if (input instanceof IStorageEditorInput) {
					IPath path = ((IStorageEditorInput)input).getStorage().getFullPath();
					if (path != null && JavaCore.isJavaLikeFileName(path.lastSegment())) {
						IResource r = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
						if (r instanceof IFile) {
							IJavaElement element = JavaCore.create((IFile)r);
							if (element instanceof ITypeRoot) {
								if (monitor.isCanceled()) {
									return Status.OK_STATUS;
								}
								MarkTypeJob mtj = new MarkTypeJob((ITypeRoot) element);
								mtj.schedule();
							}
						}
					}
				}
			} catch (CoreException e) {}
			monitor.worked(1);
		}
		monitor.done();
		return Status.OK_STATUS;
	}

}
