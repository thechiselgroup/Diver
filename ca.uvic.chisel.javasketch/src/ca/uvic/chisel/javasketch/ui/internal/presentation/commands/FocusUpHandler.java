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
package ca.uvic.chisel.javasketch.ui.internal.presentation.commands;

import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.ui.internal.presentation.IJavaSketchPresenter;

/**
 * Focuses on the parent of the current root activation.
 * @author Del Myers
 */

public class FocusUpHandler extends SketchPresentationHandler implements IElementUpdater {
	
	public static String COMMAND_ID = "ca.uvic.chisel.javasketch.command.focusUpActivationCommand";
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#setEnabled(java.lang.Object)
	 */
	@Override
	public void setEnabled(Object evaluationContext) {
		boolean enabled = isEnabled();
		boolean newEnabled = enabled;
		super.setEnabled(evaluationContext);
		
		IJavaSketchPresenter presenter = getPresenter(false);
		if (presenter == null) {
			newEnabled = false;
		} else {
		UMLSequenceViewer viewer = presenter.getSequenceChartViewer();
		Object root = viewer.getRootActivation();
		newEnabled = false;
		if (root instanceof IActivation) {
			IActivation ra = (IActivation) root;
			if (ra.getArrival() != null) {
				if (ra.getArrival().getOrigin() != null) {
					newEnabled = ra.getArrival().getOrigin().getActivation() != null;
				}
			}
		}
		}
		if (newEnabled != enabled) {
			setBaseEnabled(newEnabled);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IJavaSketchPresenter presenter = getPresenter(false);
		if (presenter == null) {
			setBaseEnabled(false);
			return null;
		}
		UMLSequenceViewer viewer = presenter.getSequenceChartViewer();
		Object root = viewer.getRootActivation();
		if (root instanceof IActivation) {
			IActivation ra = (IActivation) root;
			if (ra.getArrival() != null) {
				if (ra.getArrival().getOrigin() != null) {
					if (ra.getArrival().getOrigin().getActivation() != null) {
						viewer.setRootActivation(ra.getArrival().getOrigin().getActivation());
					}
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
		element.setIcon(SketchPlugin
			.imageDescriptorFromPlugin("images/etool16/up.gif"));
	}

}
