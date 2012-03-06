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
package ca.uvic.chisel.javasketch.ui.internal.views;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.ui.internal.presentation.JavaThreadSequenceView;

/**
 * @author Del Myers
 *
 */
public class OpenIThreadCommandHandler extends AbstractHandler implements IElementUpdater {


	public static final String COMMAND_ID = "ca.uvic.chisel.javasketch.command.openThreadCommand";
		
		
	private IThread getSelectedThread(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			for (Iterator<?> i = ss.iterator(); i.hasNext();) {
				Object next = i.next();
				Object o = next;
				if (next instanceof IAdaptable) {
					o = ((IAdaptable)next).getAdapter(IThread.class);
				}
				if (o instanceof IThread) {
					return (IThread) o;
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.commands.IElementUpdater#updateElement(org.eclipse.ui.menus.UIElement, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void updateElement(UIElement element, Map parameters) {
			
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		//must execute in the ui thread
		IWorkbenchSite site = HandlerUtil.getActiveSite(event);
		if (site == null) {
			return null;
		}
		IWorkbenchPage page = site.getWorkbenchWindow().getActivePage();
		if (page == null) {
			return null;
		}
		IThread selectedThread = getSelectedThread(event);
		if (selectedThread != null) {
			try {
				IViewPart part = page.showView(JavaThreadSequenceView.VIEW_ID, null, IWorkbenchPage.VIEW_VISIBLE);
				if (part instanceof JavaThreadSequenceView) {
					((JavaThreadSequenceView)part).setInput(selectedThread);
				}
				
				//IDE.openEditor(page, new ProgramSketchEditorInput(selectedThread), "ca.uvic.chisel.javasketch.threadEditor");
			} catch (PartInitException e) {
				SketchPlugin.getDefault().log(e);
			}
		}
		return null;
	}

}
