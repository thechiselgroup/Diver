/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers -- initial API and implementation
 *******************************************************************************/
package org.eclipse.zest.custom.sequence.internal;

/**
 * A custom progress monitor used specifically for this kind of dialog. 
 * @author Del Myers
 *
 */
public class SimpleProgressMonitor {
	
	private IUIProgressService dialog;
	
	private int work;
	private double worked;
	private int workInParent;
	private String task;
	private SimpleProgressMonitor parent;
	private double scale;
	private final boolean isCancelable;

	private volatile boolean cancelled;
	
	
	public SimpleProgressMonitor(IUIProgressService service, boolean isCancelable) {
		this(service, null, isCancelable);
	}
	
	SimpleProgressMonitor(IUIProgressService dialog, SimpleProgressMonitor parent, boolean isCancelable) {
		this.dialog = dialog;
		this.parent = parent;
		workInParent = 0;
		work = -1;
		scale = 1;
		worked = 0;
		this.isCancelable = isCancelable;
	}
	
	/**
	 * Creates a new sub-monitor for this monitor. Work is the total amount of work
	 * that this monitor will do in the parent monitor.
	 * @param task the task name.
	 * @param workInParent the work that will be done in the parent monitor.
	 * @return a new progress monitor.
	 */
	public SimpleProgressMonitor createSubMonitor(String task, int  workInParent) {
		SimpleProgressMonitor subMonitor = new SimpleProgressMonitor(dialog, this, isCancelable);
		subMonitor.workInParent = workInParent;
		return subMonitor;
	}
	
	public void beginTask(String taskName, int work) {
		if (parent != null) {
			scale = (((double)workInParent)/parent.work) * parent.scale;
			if (scale < 0) {
				scale = 0;
			}
		} else {
			dialog.setTask(taskName, work);
		}
		this.work = work;
		this.task = taskName;
		dialog.setSubTask("");
		updateTaskLabel();
	}
	
	public void setSubTask(String taskName) {
		dialog.setSubTask(taskName);
	}
	
	private void updateTaskLabel() {
		if (parent == null) {
			dialog.setTaskName(task);
		} else {
			dialog.setSubTask(task);
		}
	}
	
	public void done() {
		if (isDone()) {
			return;
		} 
		
		worked = IUIProgressService.WORKED_DONE;
		if (parent == null) {
			dialog.setWorked(IUIProgressService.WORKED_DONE);
		}
	}
	
	public void worked(int work) {
		if (isDone()) {
			return;
		}
		worked += work;
		increment(work*scale);
	}
	
	public boolean isDone() {
		return (worked == IUIProgressService.WORKED_DONE);
	}
	
	private void increment(double increase) {
		if (parent != null) {
			parent.increment(increase);
		} else {
			worked += increase;
			dialog.setWorked((int)Math.round(worked));
			if (work != IUIProgressService.UNKNOWN_WORK && worked > work) {
				done();
			}
		}
	}
	
	
	public boolean isCancelled() {
		if (parent != null) {
			return parent.isCancelled();
		}
		return this.cancelled;
	}
	
	public void cancel() {
		if (!isCancelable) return;
		if (parent != null) {
			parent.cancel();
		}
		this.cancelled = true;
	}
	
	
}