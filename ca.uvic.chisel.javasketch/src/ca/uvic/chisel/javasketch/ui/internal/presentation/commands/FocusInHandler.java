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

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.internal.operations.TimeTriggeredProgressMonitorDialog;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.zest.custom.uml.viewers.ISequenceContentExtension2;
import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.ui.internal.presentation.ExpandToRootRunnable;
import ca.uvic.chisel.javasketch.ui.internal.presentation.IJavaSketchPresenter;

/**
 * Focuses on an element in a sequence viewer.
 * @author Del Myers
 */
@SuppressWarnings("restriction")
public class FocusInHandler extends SketchPresentationHandler implements IElementUpdater {
	public static String COMMAND_ID = "ca.uvic.chisel.javasketch.command.focusActivationCommand";
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IJavaSketchPresenter presenter = getPresenter(false);
		ITraceModel element = getSelectedTraceModel(getWorkbenchSelection());
		UMLSequenceViewer viewer = presenter.getSequenceChartViewer();
		if (viewer.isVisible(element)) {
			if (element instanceof IActivation) {
				viewer.setRootActivation(element);
				return null;
			}
		}
		LinkedList<Object> pathToRoot = new LinkedList<Object>();
		Object input = viewer.getInput();
		if (input == null) return null;
		ISequenceContentExtension2 provider = (ISequenceContentExtension2) viewer.getContentProvider();
		Object[] roots = ((IStructuredContentProvider)provider).getElements(input);
		if (roots.length <= 0) return null;
		Object currentRoot = roots[0];
		Object currentParent = element;
		while (currentParent != null && !currentParent.equals(currentRoot)) {
			pathToRoot.addFirst(currentParent);
			Object call = provider.getCall(currentParent);
			if (call != null) {
				currentParent = provider.getOriginActivation(call);
			} else {
				currentParent = null;
			}
		}
		pathToRoot.addFirst(currentRoot);
		TimeTriggeredProgressMonitorDialog progress = new TimeTriggeredProgressMonitorDialog(viewer.getControl().getShell(), 1000);
		try {
			progress.run(false, true, new ExpandToRootRunnable(viewer, pathToRoot, true));
		} catch (InvocationTargetException ex) {
			SketchPlugin.getDefault().log(ex);
		} catch (InterruptedException e1) {}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.commands.IElementUpdater#updateElement(org.eclipse.ui.menus.UIElement, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		String aname = "Activation";
		ITraceModel traceModel = getSelectedTraceModel(getWorkbenchSelection());
		if (traceModel != null) {
			IJavaSketchPresenter presenter = getPresenter(false);
			if (presenter != null) {
				aname = ((ILabelProvider)presenter.getSequenceChartViewer().getLabelProvider()).getText(traceModel);
			}
		}
		element.setText("Focus On " + aname);
		
		element.setIcon(SketchPlugin
			.imageDescriptorFromPlugin("images/etool16/in.gif"));
	}

	@Override
	public void setEnabled(Object evaluationContext) {
		super.setEnabled(evaluationContext);
		ITraceModel model = getSelectedTraceModel(getWorkbenchSelection());
		boolean newEnabled = (model instanceof IActivation);
		if (newEnabled != isEnabled()) {
			setBaseEnabled(newEnabled);
		}
	}
}
