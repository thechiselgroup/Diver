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

import java.sql.SQLException;

import ca.uvic.chisel.javasketch.data.internal.DataUtils;
import ca.uvic.chisel.javasketch.data.model.ITrace;

/**
 * Abstract base class for elements that are built from views onto
 * tables, and don't have unique identifiers within the tables.
 * 
 * @author Del Myers
 *
 */
public abstract class TraceModelViewBase extends TraceModelImpl {

	private TraceImpl trace;
	private String identifier;

		
	/**
	 * Creates a new instance of the model element, and loads it directly from 
	 * the results. The results are expected to contain a column called "model_id"
	 * which uniquely identifies this element in the underlying database table.
	 * @param trace
	 * @param results
	 * @throws SQLException
	 */
	public TraceModelViewBase(TraceImpl trace, String identifier) {
		this.trace = trace;
		this.identifier = identifier;
		trace.register(this);
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getTrace()
	 */
	public ITrace getTrace() {
		return trace;
	}
	
		
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getIdentifier()
	 */
	public String getIdentifier() {
		return identifier;
	}
	
	protected DataUtils getDataUtils() {
		return ((TraceImpl)getTrace()).getDataUtils();
	}

}
