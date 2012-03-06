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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.progress.UIJob;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.ISketchEventListener;
import ca.uvic.chisel.javasketch.SketchEvent;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.ITraceEvent;
import ca.uvic.chisel.javasketch.data.model.ITraceEventListener;
import ca.uvic.chisel.javasketch.data.model.TraceEventType;

/**
 * @author Del Myers
 *
 */
public class TraceNavigatorEventListener implements ISketchEventListener,
		ITraceEventListener {
	
	private TreeViewer viewer;
	private long lastRefresh;

	/**
	 * @author Del Myers
	 *
	 */
	private final class UIJobExtension extends UIJob {
		private Object[] elements;

		/**
		 * @param name
		 */
		private UIJobExtension(String name) {
			this(name, null);
		}
		
		private UIJobExtension(String name, Object[] elements) {
			super(name);
			this.elements = elements;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			monitor.beginTask("Refreshing Sketches", IProgressMonitor.UNKNOWN);
			performRefresh(elements);
			monitor.done();
			return Status.OK_STATUS;
		}
	}
	
	private UIJob refreshJob = new UIJobExtension("Refreshing Sketches");
	
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.ISketchEventListener#handleSketchEvent(ca.uvic.chisel.javasketch.SketchEvent)
	 */
	@Override
	public void handleSketchEvent(SketchEvent event) {
		ITreeContentProvider provider = (ITreeContentProvider) viewer.getContentProvider();
		Object parent = null;
		Object root = null;
		IProgramSketch sketch =event.getSketch();
		switch (event.getType()) {
		case SketchAnalysisStarted:
		case SketchAnalysisEnded:
		case SketchAnalysisInterrupted:
			parent = provider.getParent(sketch);
			root = provider.getParent(sketch.getTracedLaunchConfiguration());
		case SketchDeleted:
			if (sketch != null) {
				//the sketch is already in the viewer, just update it.
				if (parent != null) {
					new UIJobExtension("Refreshing Sketches", new Object[]{sketch}).schedule();
				} else if (root != null){
					//the sketch is not in the viewer, must update its project
					new UIJobExtension("Refreshing Sketches", new Object[]{sketch.getTracedLaunchConfiguration()}).schedule();
				} else {
					refreshJob.schedule();
				}
				
			}
			break;
		case SketchRefreshed:
			refreshJob.schedule();
		}
	}

	/**
	 * @param elements 
	 * 
	 */
	protected synchronized void performRefresh(Object[] elements) {
		if (viewer != null) {
			if (elements == null) {
				Object inutElement = viewer.getInput();
				//force a full refresh
				viewer.setInput(null);
				viewer.setInput(inutElement);
				viewer.refresh();
			} else {
				for (Object element : elements) {
					if (element instanceof TreeNode) {
						((TreeNode)element).clearChildren();
						if (!((TreeNode)element).isOrphaned()) {
							viewer.refresh(element, true);
						}
					}
					
					
				}
			}
		}
	}

	public void setViewer(TreeViewer viewer) {
		this.viewer = viewer;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceEventListener#handleEvents(ca.uvic.chisel.javasketch.data.model.ITraceEvent[])
	 */
	@Override
	public void handleEvents(ITraceEvent[] events) {
		for (ITraceEvent event : events) {
			if (event.getType() == TraceEventType.ThreadEventType) {
				//refresh the sketch
				IProgramSketch associated = SketchPlugin.getDefault().getSketch(event.getTrace());
				if (associated != null) {
					long currentTime = System.currentTimeMillis();
					if (currentTime - lastRefresh >= 3000) {
						new UIJobExtension("Refreshing Sketch", new Object[]{associated}).schedule();
						lastRefresh = currentTime;
					}
				}
			}
		}
	}

}
