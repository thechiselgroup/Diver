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

import java.lang.reflect.InvocationTargetException;

/**
 * A simple interface for supplying progress service for SimpleProgressMonitors. Just requires that 
 * the service be able to handle the changing of the name of the current task, the name of the
 * current sub task and the <i>total</i> amount of work that has currently been performed. Should
 * be able to handle cases when the amount of work is unknown.
 * @author Del Myers
 *
 */
public interface IUIProgressService {
	/**
	 * Constant indicating that the amount of work done is unknown.
	 */
	public int UNKNOWN_WORK = -1;
	/**
	 * Constant indicating that the progress has completed. When a progress
	 * service gets 
	 */
	public int WORKED_DONE = -2;
	
	/**
	 * Sets the current amount of work to the given work. The progress indicator must be
	 * able to adjust by moving forward or backward relative to the total amount of work
	 * in the current task. May be set to WORKED_DONE to indicate that the job is complete.
	 * @param work the total work, or WORKED_DONE, or UNKNOWN_WORK.
	 */
	void setWorked(int work);
	
	/**
	 * Sets the current task to the given task name, with the total amount of work. If
	 * there is a current running task, then the work done is reset.
	 * @param taskName the task name.
	 * @param totalWork the total amount of work.
	 */
	void setTask(String taskName, int totalWork);
	
	/**
	 * Sets the current task name without changing the work.
	 * @param taskName the current task name.
	 */
	void setTaskName(String taskName);
	
	/**
	 * Sets the subtask of the current task.
	 * @param taskName
	 */
	void setSubTask(String taskName);
	
	/**
	 * Runs the given runnable in the UI thread.
	 * @param runnable
	 * @param enableCancelButton
	 * @throws InvocationTargetException
	 */
	void runInUIThread(final AbstractSimpleProgressRunnable runnable, boolean enableCancelButton) throws InvocationTargetException;

	/**
	 * An exception or error occurred during the execution of a job, the service must respond.
	 * @param t the exception or error.
	 */
	void handleException(Throwable t);
	

}
