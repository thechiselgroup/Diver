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
package ca.uvic.chisel.javasketch.data.model;

/**
 * 
 * @author Del Myers
 *
 */
public interface IThread extends ITraceModel {

	/**
	 * Returns an identifier for the thread within the trace.
	 * @return an identifier for the thread within the trace.
	 */
	int getID();
	
	/**
	 * Returns a name for the thread.
	 * @return a name for the thread.
	 */
	String getName();
	
	/**
	 * Returns the first "arriving" activation for the thread. If null, then no 
	 * processing was performed in this thread. 
	 * @return the first "arriving" activation for the thread.
	 */
	IArrival getRoot();
	
	/**
	 * Returns the trace to which this thread belongs.
	 * @return the trace to which this thread belongs.
	 */
	ITrace getTrace();
	
	/**
	 * Returns an activation by the order identifier of its calling message, or null
	 * if none exists.
	 * @param order the order identifier of the caller that activates the activation.
	 * @return an activation by the sequence identifier of its calling message, or null
	 * if none exists.
	 */
	IActivation getByOrder(long order);
}
