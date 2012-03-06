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

import java.util.List;

/**
 * Represents work being done in an instance of a class in the running system.
 * 
 * @author Del Myers
 *
 */
public interface IActivation extends ITraceModel {
	
	/**
	 * Returns the caller that produced this activation. Convenience method
	 * for getTargetMessages().get(0);
	 * @return the call that produced this activation.
	 */
	IArrival getArrival();
	
	/**
	 * Returns an immutable, ordered list of all targets that end on this activation.
	 * @return an immutable, ordered list of all targets that end on this activation.
	 */
	List<ITargetMessage> getTargetMessages();
	
	/**
	 * Returns an immutable, ordered list of all targets that originate on this activation.
	 * @return an immutable, ordered list of all targets that originate on this activation.
	 */
	List<IOriginMessage> getOriginMessages();
	
	/**
	 * An identifier for the instance of the class that this activation occurred on.
	 * @return identifier for the instance of the class that this activation occurred on.
	 */
	String getInstanceID();
	
	/**
	 * Returns the method that this activation occurs in.
	 * @return the method that this activation occurs in.
	 */
	ITraceClassMethod getMethod();
	
	/**
	 * Returns the class that this activation occurred in. This will be the actual base
	 * class that was called.
	 * @return the class that this activation occurred in.
	 */
	ITraceClass getTraceClass();
	
	/**
	 * Returns the polymorphic class that was called for this activation. In many cases,
	 * a method call may not actually occur on the class returned by this method, because
	 * the method was defined in a parent class. To find the class that the method
	 * was actually called on, use {@link #getTraceClass()} instead
	 * @return the polymorphic class that was called for this activation.
	 */
	ITraceClass getThisClass();
	
	/**
	 * Convenience method to get the time in milliseconds from the beginning of the trace
	 * that this activation occurred.
	 * @return the time in milliseconds from the beginning of the trace
	 * that this activation occurred.
	 */
	long getTime();
	
	/**
	 * Returns the total amount of time that this activation took.
	 * @return the total amount of time that this activation took.
	 */
	long getDuration();
	
	/**
	 * Returns the thread that the activation occurred in.
	 * @return the thread that the activation occurred in.
	 */
	IThread getThread();

}
