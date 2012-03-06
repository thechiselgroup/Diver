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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.persistence.internal.ReanalyzeTraceWizard;

/**
 * Action to reanalyze a sketch, and reset the data according to new workbench
 * settings.
 * @author Del Myers
 *
 */
public class ReanalyzeActionCommandHandler extends AbstractHandler {
	
	
	public static final String COMMAND_ID = "ca.uvic.chisel.javasketch.command.reanalyzeTraceCommand";
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#isEnabled()
	 */
	private IProgramSketch getSelectedSketch(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			for (Iterator<?> i = ss.iterator(); i.hasNext();) {
				Object next = i.next();
				Object o = next;
				if (next instanceof IAdaptable) {
					o = ((IAdaptable)next).getAdapter(IProgramSketch.class);
				}
				if (o instanceof IProgramSketch) {
					return (IProgramSketch) o;
				}
			}
		}
		return null;
	}
	
		
		
	

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProgramSketch sketch = getSelectedSketch(event);
		Shell shell = HandlerUtil.getActiveShell(event);
		if (sketch != null && shell != null) {
			WizardDialog dialog = new WizardDialog(shell, new ReanalyzeTraceWizard(sketch));
			dialog.open();
		}
		return null;
	}

}
