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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeSet;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.utils.LaunchConfigurationUtilities;

class LoadJob extends Job implements ISchedulingRule {

	private TraceNavigatorContentProvider provider;
	private TreeNode parent;

	/**
	 * @param parent 
	 * @param traceNavigatorContentProvider 
	 * @param name
	 */
	public LoadJob(TraceNavigatorContentProvider provider, TreeNode parent) {
		super("Loading children...");
		this.provider = provider;
		this.parent = parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Loading children", IProgressMonitor.UNKNOWN);
		loadChildren();
		provider.getViewer().getControl().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				//the load job will have been added as an element 
				provider.getViewer().remove(LoadJob.this);
				//add all of the new children for the parent
				provider.getViewer().add(parent.data, parent.getChildElements());
				provider.getViewer().setExpandedState(parent.data, true);
			}
		});
		return Status.OK_STATUS;
	}
	
	private void loadChildren() {
		Object parentElement = parent.data;
		parent.setEmpty();
		if (parentElement instanceof IWorkspaceRoot) {
			IProgramSketch[] storedSketches =  SketchPlugin.getDefault().getStoredSketches();
			TreeSet<ILaunchConfiguration> configs = new TreeSet<ILaunchConfiguration>(new Comparator<ILaunchConfiguration>() {
				@Override
				public int compare(ILaunchConfiguration o1,
						ILaunchConfiguration o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			for (IProgramSketch sketch : storedSketches) {
				ILaunchConfiguration slc = sketch.getTracedLaunchConfiguration();
				//get the configuration in the workspace that matches the name first
				ILaunchConfiguration lc = LaunchConfigurationUtilities.getLaunchConfiguration(slc.getName());
				if (lc == null) {
					lc = slc;
				}
				configs.add(lc);
			}
			for (Object o : configs) {
				parent.addChild(o);
			}
		} else if (parentElement instanceof ILaunchConfiguration) {
			IProgramSketch[] storedSketches = SketchPlugin.getDefault().getStoredSketches(((ILaunchConfiguration) parentElement).getName());
			//check the dates for each of them and return calendars.
			TreeSet<Calendar> days = new TreeSet<Calendar>();
			for (IProgramSketch sketch : storedSketches) {
				Calendar calendar = Calendar.getInstance();
				Date time = sketch.getProcessTime();
				calendar.setTime(time);
				int day = calendar.get(Calendar.DAY_OF_YEAR);
				int year = calendar.get(Calendar.YEAR);
				Calendar dayCalendar = Calendar.getInstance();
				dayCalendar.setTimeInMillis(0);
				dayCalendar.set(Calendar.YEAR, year);
				dayCalendar.set(Calendar.DAY_OF_YEAR, day);
				days.add(dayCalendar);
			}
			for (Calendar day : days) {
				parent.addChild(new ParentedCalendar(parentElement, day));
			}
		} else if (parentElement instanceof ParentedCalendar) {
			//do the above in reverse
			ILaunchConfiguration lc = (ILaunchConfiguration) parent.getParent().data;
			Calendar day = ((ParentedCalendar)parent.data).getCalendar();

			IProgramSketch[] storedSketches = SketchPlugin.getDefault().getStoredSketches(lc.getName());
			ArrayList<IProgramSketch> sketches = new ArrayList<IProgramSketch>();
			
			for (IProgramSketch sketch : storedSketches) {
				Date time = sketch.getProcessTime();
				Calendar sketchDay = Calendar.getInstance();
				sketchDay.setTime(time);
				if (sketchDay.get(Calendar.YEAR) == day.get(Calendar.YEAR)) {
					if (sketchDay.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)) {
						sketches.add(sketch);
					}
				}
			}
			//make sure that they always come in the same order.
			Collections.sort(sketches, new Comparator<IProgramSketch>() {
				@Override
				public int compare(IProgramSketch o1, IProgramSketch o2) {
					return o1.getID().compareTo(o2.getID());
				}
			});
			for (IProgramSketch sketch : sketches) {
				parent.addChild(sketch);
			}
		} else if (parentElement instanceof IProgramSketch) {
			//try to return the threads
			IProgramSketch sketch = (IProgramSketch) parentElement;
			ITrace trace = sketch.getTraceData();
			if (trace != null && !sketch.isAnalysing()) {
				for (IThread thread : trace.getThreads()) {
					parent.addChild(thread);
				}
				provider.sketchLoaded(sketch);
			}
			
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	@Override
	public boolean contains(ISchedulingRule rule) {
		return this.equals(rule);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		if (rule instanceof LoadJob) {
			return parent.equals(((LoadJob)rule).parent);
		}
		return false;
	}
	
}