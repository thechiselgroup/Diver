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
package ca.uvic.chisel.javasketch.internal;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.ISketchEventListener;
import ca.uvic.chisel.javasketch.SketchAnalysisEvent;
import ca.uvic.chisel.javasketch.SketchDebugEvent;
import ca.uvic.chisel.javasketch.SketchEvent;
import ca.uvic.chisel.javasketch.SketchEvent.SketchEventType;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.launching.ITraceClient;
import ca.uvic.chisel.javasketch.persistence.internal.PersistTraceJob;

/**
 * A manager for watching debug and job events in the workspace to fire the appropriate
 * sketch events.
 * @author Del Myers
 *
 */
public class SketchEvents implements IDebugEventSetListener, IJobChangeListener {
	
	private ListenerList listeners;

	public SketchEvents() {
		this.listeners = new ListenerList();
		initialize();
	}

	/**
	 * Registers listeners with the debug and job frameworks so that events can be
	 * promulgated throughout the workbench.
	 */
	private void initialize() {
		DebugPlugin.getDefault().addDebugEventListener(this);
		Job.getJobManager().addJobChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(org.eclipse.debug.core.DebugEvent[])
	 */
	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			if (event.getSource() instanceof ITraceClient) {
				ITraceClient source = (ITraceClient) event.getSource();
				IProgramSketch associated = getAssociatedSketch(source);
				if (associated != null) {
					Job processJob = null;
					SketchEventType sketchEvent = SketchEventType.SketchEnded;
					if (event.getKind() == DebugEvent.CREATE) {
						sketchEvent = SketchEventType.SketchStarted;
						if (associated instanceof DBProgramSketch) {
							processJob = new PersistTraceJob((DBProgramSketch)associated);
						}
					}
					fireEvent(new SketchDebugEvent(associated, sketchEvent, source));
					if (processJob != null) {
						processJob.schedule();
					}
				}
			}
		}
	}

	
	public void fireEvent(SketchEvent event) {
		for (Object l : listeners.getListeners()) {
			ISketchEventListener listener = (ISketchEventListener) l;
			listener.handleSketchEvent(event);
		}
	}
	
	private IProgramSketch getAssociatedSketch(ITraceClient source) {
		IProgramSketch[] sketches = SketchPlugin.getDefault().getStoredSketches();
		for (IProgramSketch sketch : sketches) {
			if (sketch.getID().equals(source.getID())) {
				return sketch;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#aboutToRun(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void aboutToRun(IJobChangeEvent event) {
		//don't need to do anything.
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#awake(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void awake(IJobChangeEvent event) {
		//don't need to do anything
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void done(IJobChangeEvent event) {
		//fire completed event
		if (event.getJob().belongsTo(IProgramSketch.class)) {
			if (event.getJob() instanceof PersistTraceJob) {
				PersistTraceJob job = (PersistTraceJob) event.getJob();
				fireEvent(new SketchAnalysisEvent(job.getSketch(), SketchEventType.SketchAnalysisEnded));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#running(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void running(IJobChangeEvent event) {
		//fire running event
		if (event.getJob().belongsTo(IProgramSketch.class)) {
			if (event.getJob() instanceof PersistTraceJob) {
				PersistTraceJob job = (PersistTraceJob) event.getJob();
				fireEvent(new SketchAnalysisEvent(job.getSketch(), SketchEventType.SketchAnalysisStarted));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#scheduled(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void scheduled(IJobChangeEvent event) {
		//fire scheduling event
		if (event.getJob().belongsTo(IProgramSketch.class)) {
			if (event.getJob() instanceof PersistTraceJob) {
				PersistTraceJob job = (PersistTraceJob) event.getJob();
				fireEvent(new SketchAnalysisEvent(job.getSketch(), SketchEventType.SketchAnalysisScheduled));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.IJobChangeListener#sleeping(org.eclipse.core.runtime.jobs.IJobChangeEvent)
	 */
	@Override
	public void sleeping(IJobChangeEvent event) {
		//don't need to do anything
	}

	/**
	 * @param listener
	 */
	public void addListener(ISketchEventListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 */
	public void removeListener(ISketchEventListener listener) {
		listeners.remove(listener);
	}

}
