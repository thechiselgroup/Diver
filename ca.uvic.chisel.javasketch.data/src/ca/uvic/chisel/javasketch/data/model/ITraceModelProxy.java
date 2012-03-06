/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.data.model;

/**
 * A proxy to a trace model element. It isn't loaded from the data source
 * until the getElement() method is called.
 * @author Del Myers
 *
 */
public interface ITraceModelProxy {
	
	/**
	 * Returns the element loaded from the data source or null if an error
	 * occurred loading it.
	 * @return the element loaded from the data source.
	 */
	public ITraceModel getElement();
	
	/**
	 * Returns the kind of trace model element that this proxy represents.
	 * Note, that some elements are unable to determine their full kind until
	 * the proxy is actually loaded. Specificially, it is not always possible
	 * to determine if a proxy with MESSAGE present in its kind flag is actually
	 * a CALL, RETURN, REPLY, THROW, ARRIVAL, or CATCH. The various is* methods
	 * can be used to check the details of the kind when such information is
	 * not available.
	 * @return the kind of trace model element that this proxy represents.
	 */
	public int getKind();
	
	/**
	 * @return a descriptive name for this element, suitable for user interfaces
	 * 
	 */
	@Override
	public String toString();
	
	public boolean isStaticElement();
	
	public boolean isDynamicElement();
	
	public boolean isMessageElement();
	
	/**
	 * 
	 * @return the identifier of the element in the trace.
	 */
	public String getElementId();

	/**
	 * Returns the trace that this proxy belongs to.
	 * @return the trace that this proxy belongs to.
	 */
	public ITraceModel getTrace();

}
