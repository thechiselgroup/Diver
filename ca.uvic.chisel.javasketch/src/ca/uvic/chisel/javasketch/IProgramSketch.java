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
package ca.uvic.chisel.javasketch;

import java.net.URL;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.debug.core.ILaunchConfiguration;

import ca.uvic.chisel.hsqldb.server.IDataPortal;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.launching.ITraceClient;

/**
 * Provides a portal to stored data about programs that have been traced. Note that
 * IProgramSketches aren't cached by the framework. They are built as they are needed,
 * and stored sketches will not be identical to the ones returned by the framework.
 * Clients should always use the {@link #equals(Object)} method to check for equality
 * among instances of this class.  
 * 
 * @author Del Myers
 *
 */
public interface IProgramSketch {
	
	/**
	 * 
	 * @return the name of the process that was traced.
	 */
	public String getProcessName();
	
	/**
	 * @return a label for the launch that was traced.
	 */
	public String getLabel();
	
	/**
	 * 
	 * @return the time at which the trace occurred.
	 */
	public Date getProcessTime();
	
	/**
	 * 
	 * @return the trace data associated with this trace. May be null
	 * if the trace has not yet been processed or is being processed.
	 */
	public ITrace getTraceData();
	
	/**
	 * Convenience method to check to see if the sketch has connected to
	 * its underlying data source. This may be useful for updating UI elements,
	 * as connecting to the data source may take some time.
	 * @return true if the sketch has been connected to its data source yet.
	 */
	public boolean isConnected();
	
	/**
	 * Returns a data connection to the underlying database.
	 * @return a portal to the underlying database
	 * @throws CoreException if a connection could not be made.
	 */
	public IDataPortal getPortal() throws CoreException;
	
//	/**
//	 * Returns the project associated with this trace.
//	 * @return the project associated with this trace.
//	 */
//	public IProject getTracedProject();
	
	/**
	 * Returns the launch configuration associated with this trace.
	 * @return the launch configuration associated with this trace.
	 */
	public ILaunchConfiguration getTracedLaunchConfiguration();
	
	/**
	 * Returns a trace client that is associated with the sketch. The
	 * trace client is a process within a launch which produces a sketch.
	 * If there does not exist one currently in the workbench (the process
	 * has terminated, and the workbench has been restarted, for example),
	 * then this method will return null.
	 * @param sketch the sketch to query
	 * @return an associated trace client if one is available.
	 */
	public ITraceClient getTracer();
	
	/**
	 * Returns true iff there is currently an ITraceClient running to produce
	 * this sketch.
	 * @return true iff there is currently an ITraceClient running to produce
	 * this sketch.
	 */
	public boolean isRunning();
	
	/**
	 * Returns true iff there is currently a job running that is analyzing the 
	 * sketch.
	 * @return true iff there is currently a job running that is analyzing the 
	 * sketch.
	 */
	public boolean isAnalysing();
	
	/**
	 * Returns the rule used to make sure that analysis jobs don't conflict
	 * and corrupt the indexed data.
	 * @return the scheduling rule
	 */
	public ISchedulingRule getRule();
	
	/**
	 * 
	 * @return a unique identifier for this sketch, which will be shared with its
	 * associated trace client (if one exists).
	 */
	public String getID();

	/**
	 * @return a path to the folder in which raw data has been stored for the trace.
	 */
	public URL getTracePath();
	
	/**
	 * Returns the settings used for filters for this sketch.
	 * @return
	 */
	public FilterSettings getFilterSettings();

}
