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
package ca.uvic.chisel.javasketch.launching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;

/**
 * A trace client represents a process that receives tracing information from an
 * instrumented program. The way that the instrumentation is done is not defined, but
 * trace client processes can pe 
 * 
 * @author Del Myers
 *
 */
public interface ITraceClient extends IProcess {
	
	public static final int MODEL_CHANGED = 0;
	public static final int TRACE_PAUSED = 1;
	public static final int TRACE_RESUMED = 2;

	/**
	 * Sets up the client. To be called before the target process is started in order to
	 * set up files and resources that may be needed to attach.
	 */
	public void initialize(ILaunchConfiguration configuration) throws CoreException;
	
	/**
	 * Attempts to attach to the given process. This process will append itself to the
	 * given process's launch. After successful attachment, isPaused() should return false,
	 * and getHost() should return the supplied process.
	 * 
	 * @param process the process to attach to.
	 * @param monitor a monitor to display progress in.
	 * @throws CoreException if there was an error attaching to the process
	 */
	public void attach(ILaunch launch, ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Used to handle process-specific events that can be interpreted by the client.
	 * @param event the event to process.
	 * @throws IllegalArgumentException if the event object is not recognized.
	 */
	public void sendEvent(Object event) throws IllegalArgumentException;
	
	/**
	 * Pauses tracing functionality in the client, if supported.
	 * @return true if the client's tracing was successfully paused.
	 */
	public void pauseTrace();
	
	/**
	 * Returns true if the tracing functionality is currently paused.
	 * @return true if the tracing functionality is currently paused.
	 */
	public boolean isPaused();
	
	/**
	 * Resumes the previously paused tracing capabilities.
	 * @return true if the tracing was successfully resumed.
	 */
	public boolean resumeTrace();
	
		
	/**
	 * Returns true if this client process supports the pausing of traces to the host process.
	 * @return true if this client process supports the pausing of traces to the host process.
	 */
	public boolean canPauseTrace();
	
	
	/**
	 * Returns the time, in milliseconds, according to the system clock, when the 
	 * process got attached to the client. May be used to identify the trace process.
	 * @return the attach time.
	 */
	public long getAttachTime();

	/**
	 * @return the launch configuration associated with this trace.
	 */
	public ILaunchConfiguration getLaunchConfiguration();
	
	/**
	 * 
	 * @return a unique identifier for this trace. It will be shared with an associated IProgramSketch
	 */
	public String getID();

	/**
	 * @return the time that this client terminated. The time will be negative if the
	 * client has not yet terminated.
	 */
	public long getTerminationTime();

}
