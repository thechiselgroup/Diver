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
package ca.uvic.chisel.diver.mylyn.logger.logging;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

import ca.uvic.chisel.diver.mylyn.logger.MylynLogger;

/**
 * @author Del
 *
 */
public class PartListener implements IPartListener {
	private ISelectionChangedListener textSelectionChangedListener = new ISelectionChangedListener() {
		private ISelectionListener partSelectionListener = new PageSelectionListener();
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (activeTextEditor != null) {
				partSelectionListener.selectionChanged(activeTextEditor, event.getSelection());		
			}
		}
	};
	private SelectionListener selectionListener = new SelectionListener() {
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			System.out.println("yeah!");
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	};
	private ITextEditor activeTextEditor = null;
	//private ISourceViewer activeSourceViewer = null;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void partActivated(IWorkbenchPart part) {
		String eventString = "event=activation\tpart=" + part.getSite().getId();
		MylynLogger.getDefault().logEvent(eventString);
		
		//log the id of the part that was activated.
		if (part instanceof ITextEditor) {
			if (activeTextEditor != null) {
				ISelectionProvider provider = activeTextEditor.getSelectionProvider();
				provider.removeSelectionChangedListener(textSelectionChangedListener);
				if (provider instanceof IPostSelectionProvider) {
					((IPostSelectionProvider)provider).removePostSelectionChangedListener(textSelectionChangedListener);
				}
			}
			activeTextEditor = (ITextEditor) part;
			ISelectionProvider provider = activeTextEditor.getSelectionProvider();
			if (provider instanceof IPostSelectionProvider) {
				((IPostSelectionProvider)provider).addPostSelectionChangedListener(textSelectionChangedListener);
			} else {
				provider.addSelectionChangedListener(textSelectionChangedListener);
			}
			IEditorInput input = activeTextEditor.getEditorInput();
			ITypeRoot typeRoot = null;
			//using internal stuff, but I don't care
			if (input instanceof IClassFileEditorInput) {
				typeRoot = ((IClassFileEditorInput) input).getClassFile();
			} else if (input instanceof IFileEditorInput){
				IFile file = ((IFileEditorInput)input).getFile();
				IJavaElement element = JavaCore.create(file);
				if (element instanceof ITypeRoot) {
					typeRoot = (ITypeRoot) element;
				}
			}
		}
		

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void partBroughtToTop(IWorkbenchPart part) {}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void partClosed(IWorkbenchPart part) {
		partDeactivated(part);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void partDeactivated(IWorkbenchPart part) {
		if (part instanceof ITextEditor) {
			if (activeTextEditor != null) {
				activeTextEditor.getSelectionProvider().removeSelectionChangedListener(textSelectionChangedListener);
			}
			activeTextEditor = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
	 */
	@Override
	public void partOpened(IWorkbenchPart part) {}

}
