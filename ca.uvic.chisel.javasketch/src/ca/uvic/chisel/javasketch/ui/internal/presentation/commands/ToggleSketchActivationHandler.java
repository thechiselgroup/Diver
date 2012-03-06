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
package ca.uvic.chisel.javasketch.ui.internal.presentation.commands;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.ISketchImageConstants;

/**
 * @author Del Myers
 *
 */
public class ToggleSketchActivationHandler extends AbstractHandler implements
		IElementUpdater {

	public static final String COMMAND_ID = "ca.uvic.chisel.javasketch.commands.toggleSketchActive";
	/* (non-Javadoc)
	 * @see org.eclipse.ui.commands.IElementUpdater#updateElement(org.eclipse.ui.menus.UIElement, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		ISelectionService service = (ISelectionService) element.getServiceLocator().getService(ISelectionService.class);
		if (service != null) {
			IProgramSketch sketch = getSelectedSketch(service.getSelection());
			if (sketch != null) {
				ImageRegistry registry = SketchPlugin.getDefault().getImageRegistry();
				if (sketch.equals(SketchPlugin.getDefault().getActiveSketch())) {
					element.setText("Deactivate Trace");
					element.setIcon(registry.getDescriptor(ISketchImageConstants.ICON_TRACE_INACTIVE));
				} else {
					element.setText("Activate Trace");
					element.setIcon(registry.getDescriptor(ISketchImageConstants.ICON_TRACE_ACTIVE));
				}
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		IProgramSketch sketch = getSelectedSketch(selection);
		if (sketch == null) return null;
		if (sketch.equals(SketchPlugin.getDefault().getActiveSketch())) {
			SketchPlugin.getDefault().setActiveSketch(null);
		} else {
			SketchPlugin.getDefault().setActiveSketch(sketch);
		}
		return null;
	}

	/**
	 * @param selection
	 * @return
	 */
	private IProgramSketch getSelectedSketch(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (ss.getFirstElement() instanceof IProgramSketch) {
				return (IProgramSketch) ss.getFirstElement();
			}
		}
		return null;
	}

}
