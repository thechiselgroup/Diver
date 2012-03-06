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

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * The basic data held within a trace. The starting point for all trace data.
 * @author Del Myers
 *
 */
public interface ITrace extends ITraceModel {
	
	/**
	 * Returns an identifier for the trace.
	 * @return an identifier for the trace.
	 */
	String getLaunchID();
	
	/**
	 * Returns a time-representation for when the trace was started. 
	 * @return a date representation of the time.
	 */
	Date getTraceTime();
	
	/**
	 * Returns a time-representation for when the trace was last analyzed and persisted.
	 * @return a time-representation for when the trace was last analyzed and persisted.
	 */
	Date getDataTime();
	
	/**
	 * Returns an unmodifiable list of threads in this trace.
	 * @return an unmodifiable list of threads.
	 */
	Collection<IThread> getThreads();
	
	/**
	 * Returns the static-structure list of classes that were used during the program trace.
	 * Ordered by the qualified name of the class. The list is unmodifiable.
	 * @return  static-structure list of classes that were used during the program trace.
	 */
	Collection<ITraceClass> getClasses();
	
	/**
	 * Returns an unmodifiable list of user events in the trace, ordered by the time
	 * at which they occurred. 
	 * @return an unmodifiable list of user events in the trace, ordered by the time
	 * at which they occurred.
	 */
	List<ITraceMetaEvent> getEvents();
	/**
	 * Searches for the given class by name. If it is unavailable, null is returned.
	 * @param name the name to query.
	 * @return the class for the given name, or null if it can't be found.
	 */
	ITraceClass forName(String name);
	
	
	/**
	 * Invalidates the current trace so that all of its children must be re-loaded
	 * from disk or some other source. All current children will be marked as
	 * "invalid" and should be discarded.
	 */
	void invalidate();
	
	/**
	 * Checks to see if the model element with the given identifier has been loaded
	 * into the cache, and returns it if it has. Returns null if it has not been cached.
	 * Note, that the element may still exist in the model, but it has simply not yet
	 * been loaded because it has not yet been queried in the model.
	 * @param identifier the identifier for the loaded element.
	 * @return the loaded element, or null if it has not been loaded.
	 */
	public ITraceModel findElement(String identifier);
	
	/**
	 * Creates a proxy for the element with the given identifier. The element
	 * is not guaranteed to exist in the model. In such a case, the returned
	 * proxy will supply <code>null</code> when its <code>getElement</code>
	 * method is called. This method will return null if the given identifier
	 * has an invalid form.
	 * @param identifier the identifier to create a proxy for.
	 * @return a new proxy to a model element, or null if the identifier is malformed.
	 */
	public ITraceModelProxy getElement(String identifier);
	

	/**
	 * @param listener
	 */
	void addListener(ITraceEventListener listener);

	/**
	 * @param listener
	 */
	void removeListener(ITraceEventListener listener);


}
