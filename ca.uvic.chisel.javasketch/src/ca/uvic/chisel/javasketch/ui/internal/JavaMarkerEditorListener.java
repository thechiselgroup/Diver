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
package ca.uvic.chisel.javasketch.ui.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import ca.uvic.chisel.javasketch.SketchPlugin;

/**
 * @author Del Myers
 *
 */
class JavaMarkerEditorListener implements IWindowListener {
	//silly cascading windows, to make sure that the right editor is opened.
	
	private class InternalPartListener implements IPartListener {

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
		 */
		@Override
		public void partActivated(IWorkbenchPart part) {			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart)
		 */
		@Override
		public void partBroughtToTop(IWorkbenchPart part) {
			try {
				if (SketchPlugin.getDefault().getActiveSketch() == null) {
					return;
				}
				if (part instanceof IEditorPart) {
					IEditorPart editor = (IEditorPart) part;
					IEditorInput editorInput = editor.getEditorInput();
					if (editor.getEditorInput() instanceof IStorageEditorInput) {
						IStorageEditorInput input = (IStorageEditorInput) editor.getEditorInput();
						IPath path = input.getStorage().getFullPath();
						if (path != null) {
							String name = path.lastSegment();
							if (JavaCore.isJavaLikeFileName(name)) {
								//try and find the java file
								IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
								if (file != null && file.exists()) {
									IJavaElement element = JavaCore.create(file);
									if (element instanceof ITypeRoot) {
										new MarkTypeJob((ITypeRoot)element).schedule();
									}
								}
							}
						}
					} else if (editorInput instanceof IClassFileEditorInput) {
						//this isn't really allowed, but they give me no choice
						IClassFile classFile = ((IClassFileEditorInput)editorInput).getClassFile();
						new MarkTypeJob(classFile).schedule();
					}
				}
			} catch (CoreException e) {
				//just do nothing.
			}
		}
		

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
		 */
		@Override
		public void partClosed(IWorkbenchPart part) {}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
		 */
		@Override
		public void partDeactivated(IWorkbenchPart part) {}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
		 */
		@Override
		public void partOpened(IWorkbenchPart part) {}
		
	}
	
	private class InternalPageListener implements IPageListener {

		private IWorkbenchPage currentPage;
		private IPartListener partListener;

		protected InternalPageListener() {
			partListener = new InternalPartListener();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPageListener#pageActivated(org.eclipse.ui.IWorkbenchPage)
		 */
		@Override
		public void pageActivated(IWorkbenchPage page) {
			if (page == currentPage) {
				return;
			}
			if (currentPage != null) {
				currentPage.removePartListener(partListener);
			}
			currentPage = page;
			if (page != null) {
				page.addPartListener(partListener);
			}
			IEditorPart editor = page.getActiveEditor();
			if (editor != null) {
				partListener.partBroughtToTop(editor);
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPageListener#pageClosed(org.eclipse.ui.IWorkbenchPage)
		 */
		@Override
		public void pageClosed(IWorkbenchPage page) {
			if (currentPage != null) {
				currentPage.removePartListener(partListener);
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPageListener#pageOpened(org.eclipse.ui.IWorkbenchPage)
		 */
		@Override
		public void pageOpened(IWorkbenchPage page) {}
		
	}
	IPageListener pageListener;
	
	private IWorkbenchWindow currentWindow;
	
	/**
	 * 
	 */
	public JavaMarkerEditorListener() {
		pageListener = new InternalPageListener();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void windowActivated(IWorkbenchWindow window) {
		if (window == currentWindow) {
			return;
		}
		if (currentWindow != null) {
			window.removePageListener(pageListener);
		}
		pageListener.pageActivated(window.getActivePage());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void windowClosed(IWorkbenchWindow window) {
		if (currentWindow != null) {
			currentWindow.removePageListener(pageListener);
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void windowDeactivated(IWorkbenchWindow window) {
		if (currentWindow != null) {
			currentWindow.removePageListener(pageListener);
		}
	} 

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void windowOpened(IWorkbenchWindow window) {}

}
