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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;

public class TraceNavigatorContentProvider implements ITreeContentProvider {

	

	private TreeViewer viewer;
	private TreeNode tree;
	private TraceNavigatorEventListener navigatorEventListener = new TraceNavigatorEventListener();
	
	@Override
	public Object[] getChildren(Object parent) {
		TreeNode element = tree.findNode(parent);
		if (element != null) {
//			for (Object child : element.getAllChildElements()) {
//				if (child instanceof IProgramSketch) {
//					ITrace trace = ((IProgramSketch)child).getTraceData();
//					if (trace != null) {
//						trace.removeListener(navigatorEventListener);
//					}
//				}
//			}
			if (!element.isLoaded()) {
				element.clearChildren();
				LoadJob loader = new LoadJob(this, element);
				element.addChild(loader);
				loader.schedule();
			}
			return element.getChildElements();
		}
		return new Object[0];
		
		
	}


	@Override
	public Object getParent(Object child) {
		if (tree == null) {
			return null;
		}
		TreeNode element = tree.findNode(child);
		if (element != null && !element.isOrphaned()) {
			return element.getParent().data;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return !(element instanceof IThread);
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public synchronized void dispose() {
		SketchPlugin.getDefault().removeSketchEventListener(navigatorEventListener);
		if (tree != null) {
			for (Object element : tree.getAllChildElements()) {
				if (element instanceof IProgramSketch) {
					if (((IProgramSketch)element).isConnected()) {
						ITrace trace = ((IProgramSketch)element).getTraceData();
						if (trace != null) {
							trace.removeListener(navigatorEventListener);
						}
					}
				}
			}
		}
		viewer = null;
		tree = null;
	}

	@Override
	public synchronized void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (this.viewer == null) {
			SketchPlugin.getDefault().addSketchEventListener(navigatorEventListener);
		}
		if (tree != null) {
			for (Object element : tree.getAllChildElements()) {
				if (element instanceof IProgramSketch) {
					if (((IProgramSketch)element).isConnected()) {
						ITrace trace = ((IProgramSketch)element).getTraceData();
						if (trace != null) {
							trace.removeListener(navigatorEventListener);
						}
					}
				}
			}
		}
		if (newInput == null) {
			tree = null;
		} else {
		tree = new TreeNode(null, newInput);
		
		}
		this.viewer = (TreeViewer) viewer;
		navigatorEventListener.setViewer((TreeViewer) viewer);
	}


	/**
	 * @return
	 */
	public TreeViewer getViewer() {
		return viewer;
	}


	/**
	 * @param sketch
	 */
	public void sketchLoaded(IProgramSketch sketch) {
		sketch.getTraceData().addListener(navigatorEventListener);		
	}

	

}
