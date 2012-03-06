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
package ca.uvic.chisel.javasketch.ui.internal.presentation;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.ITargetMessage;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.internal.ast.groups.ASTMessageGroupingTree;
import ca.uvic.chisel.javasketch.ui.internal.presentation.metadata.PresentationData;

/**
 * A little runnable that will expand a sequence chart along a path, and optionally set the
 * end of the path to the new root.
 * @author Del Myers
 *
 */
public class ExpandToRootRunnable implements IRunnableWithProgress {

	private UMLSequenceViewer viewer;
	private LinkedList<Object> pathToRoot;
	private boolean setToRoot;

	/**
	 * @param sequenceChartViewer
	 * @param pathToRoot
	 * @param b
	 */
	public ExpandToRootRunnable(UMLSequenceViewer viewer,
			LinkedList<Object> pathToRoot, boolean setToRoot) {
		this.viewer = viewer;
		this.pathToRoot = pathToRoot;
		this.setToRoot = setToRoot;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask("Expanding chart along callpath", pathToRoot.size());
		ILabelProvider lp = (ILabelProvider) viewer.getLabelProvider();
		if (pathToRoot.size() <=0) {
			monitor.done();
			return;
		}
		//if the last element is already visible, there is no need to
		//expand, just quit early.
		if (viewer.isVisible(pathToRoot.getLast())) {
			return;
		}
		ITraceModel modelElement = (ITraceModel) pathToRoot.get(0);
		IProgramSketch sketch = SketchPlugin.getDefault().getSketch(modelElement);
		if (sketch == null) {
			monitor.done();
			return;
		}
		PresentationData pd = PresentationData.connect(sketch);
		try {
			viewer.getChart().setRedraw(false);
			for (Object o : pathToRoot) {
				IActivation a = (IActivation) o;
				
				//make sure that the activation is visible in its groups
				if (pd != null) {
					ITargetMessage arrival = a.getArrival();
					if (arrival != null) {
						IOriginMessage origin = arrival.getOrigin();
						if (origin != null) {
							IActivation pa = origin.getActivation();
							if (pa != null) {
								boolean refreshParent = false;
								ASTMessageGroupingTree groupRoot = pd.getGroups(pa);
								if (groupRoot != null) {
									ASTMessageGroupingTree targetGroup = groupRoot.getMessageContainer(origin);
									while (targetGroup != null && targetGroup != groupRoot) {
										if (targetGroup.isLoop() && !pd.isGroupVisible(pa, targetGroup)) {
											refreshParent = true;
											pd.swapLoop(pa, targetGroup, false);
										}
										targetGroup = targetGroup.getParent();
									}
								}
								if (refreshParent) {
									viewer.refresh(pa);
								}
							}
							
						}
					}
					
				}
				monitor.subTask(lp.getText(a));
				viewer.setExpanded(a, true);
				viewer.refresh(a);
				if (monitor.isCanceled()) break;
				monitor.worked(1);
				runEventLoop();
			}
			
			if (setToRoot) {
				int end = pathToRoot.size() -1;
				while (end >= 0) {
					viewer.setRootActivation(pathToRoot.get(end));
					if (viewer.getRootActivation() == pathToRoot.get(end)) {
						break;
					}
					end--;
				}
				
			} 	
		} finally {
			pd.disconnect();
			monitor.done();
			viewer.getChart().setRedraw(true);
			if (!viewer.isVisible(pathToRoot.getFirst())) {
				viewer.setRootActivation(pathToRoot.getFirst());
			}
		}
		viewer.reveal(pathToRoot.getLast());
		viewer.setSelection(new StructuredSelection(pathToRoot.getLast()));

	}

	/**
	 * 
	 */
	private void runEventLoop() {
		Display display = viewer.getChart().getDisplay();
		while (display.readAndDispatch()) {}	
	}

}
