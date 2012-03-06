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

import java.sql.ResultSet;
import java.sql.SQLException;

import ca.uvic.chisel.javasketch.data.model.ITrace;

/**
 * Abstract base class for elements that are directly stored in the
 * database and can be identified by model id's within their respective
 * tables.
 * 
 * @author Del Myers
 *
 */
public abstract class TraceModelIDBase extends TraceModelIDImpl {

	private TraceImpl trace;
	private long modelID;
	private String identifier;

		
	/**
	 * Creates a new instance of the model element, and loads it directly from 
	 * the results. The results are expected to contain a column called "model_id"
	 * which uniquely identifies this element in the underlying database table.
	 * @param trace
	 * @param results
	 * @throws SQLException
	 */
	public TraceModelIDBase(TraceImpl trace, ResultSet results) throws SQLException {
		this.trace = trace;
		this.modelID = results.getLong("model_id");
		String tableName = results.getMetaData().getTableName(1);
		this.identifier = "[" + tableName.toUpperCase() + "]," + modelID;
		loadFromResults(results);
		trace.register(this);
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getTrace()
	 */
	public ITrace getTrace() {
		return trace;
	}
	
	/**
	 * @return the modelID
	 */
	public long getModelID() {
		return modelID;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getIdentifier()
	 */
	public String getIdentifier() {
		return identifier;
	}
	
	public static final String getIdentifierFromResults(ResultSet results) throws SQLException {
		long modelID = results.getLong("model_id");
		String tableName = results.getMetaData().getTableName(1);
		return "[" + tableName.toUpperCase() + "]," + modelID;
	}

}
