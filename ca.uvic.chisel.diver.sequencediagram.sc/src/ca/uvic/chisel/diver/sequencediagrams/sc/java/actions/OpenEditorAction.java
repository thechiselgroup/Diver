/*******************************************************************************
 * Copyright 2005-2008, CHISEL Group, University of Victoria, Victoria, BC, Canada.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Chisel Group, University of Victoria
 *******************************************************************************/
package ca.uvic.chisel.diver.sequencediagrams.sc.java.actions;

import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import ca.uvic.chisel.diver.sequencediagrams.sc.java.editors.MethodEditorInput;

/**
 * Opens a selected method in a sequence diagram editor.
 * @see IWorkbenchWindowActionDelegate
 */
public class OpenEditorAction implements IObjectActionDelegate {
	private IMethod method;
	private IWorkbenchPart part;
	/**
	 * The constructor.
	 */
	public OpenEditorAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		if (method != null && part != null) {
			try {
				IDE.openEditor(
						part.getSite().getPage(), 
						new MethodEditorInput(method), 
						"org.eclipse.zest.custom.sequence.examples.staticanalysis.java.ASTSequenceEditor"
				);
			} catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		action.setEnabled(false);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			this.method = null;
			for (Iterator<?> i = ss.iterator(); i.hasNext();) {
				Object o = i.next();
				if (o instanceof IAdaptable) {
					IMethod method = (IMethod)((IAdaptable)o).getAdapter(IMethod.class);
					this.method = method;
					break;
				}
			}
			action.setEnabled(method != null);
		}
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}


	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
	}
}