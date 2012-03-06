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
package ca.uvic.chisel.javasketch.data.model.imple.internal;


import ca.uvic.chisel.javasketch.data.model.ITraceModel;

/**
 * @author Del Myers
 *
 */
public abstract class TraceModelImpl implements ITraceModel {

	boolean isValid;
	/**
	 * 
	 */
	public TraceModelImpl() {
		isValid = true;
	}
		
	/**
	 * This method will load the model data from the underlying database.
	 * It should make use of the protected "setData" members. This will allow
	 * the model to be dynamically loaded and unloaded. Clients may use the
	 * getDataUtils() method to gain access to utility methods that will
	 * load values directly from the connected database. In most cases,
	 * clients can make a data-specific call to the data utility class,
	 * and then simply pass the results set to the loadFromResults() method.
	 */
	public abstract void load();
	
	public abstract void unload();
	
	protected void invalidate() {
		this.isValid = false;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#isValid()
	 */
	public boolean isValid() {
		return isValid;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!this.getClass().equals(obj.getClass())) {
			return false;
		}
		TraceModelImpl that = (TraceModelImpl) obj;
		return this.getTrace().equals(that.getTrace()) &&  getIdentifier().equals(that.getIdentifier());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getTrace().hashCode() + getIdentifier().hashCode();
	}

}
