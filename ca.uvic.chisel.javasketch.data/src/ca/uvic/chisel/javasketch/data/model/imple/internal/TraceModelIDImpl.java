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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;

import ca.uvic.chisel.javasketch.data.internal.DataUtils;

/**
 * @author Del Myers
 *
 */
public abstract class TraceModelIDImpl extends TraceModelImpl {

	private HashMap<String, Object> columns;
	/**
	 * 
	 */
	public TraceModelIDImpl() {
		
	}
		
	/**
	 * Loads attributes into this model implementation based on the column names
	 * stored within the results set.
	 * @param results
	 * @throws SQLException if there was an error retrieving the results.
	 */
	protected final void loadFromResults(ResultSet results) throws SQLException {
		if (results == null) return;
		ResultSetMetaData md = results.getMetaData();
		for (int i = 1; i <= md.getColumnCount(); i++) {
			setDataValue(md.getColumnName(i), results.getObject(i));
		}
	}
	
	/**
	 * Unloads the data stored in this portion of the model. Clients may override this
	 * method so that children can be unloaded as well, but they must call super.unload()
	 * at the end of their unload logic.
	 */
	public void unload() {
		columns = null;
	}
	
	protected void setData(String column, String value) {
		setDataValue(column, value);
	}
	
	protected void setData(String column, int value) {
		setDataValue(column, value);
	}
	
	protected void setData(String column, long value) {
		setDataValue(column, value);
	}
	
	protected void setData(String column, Timestamp value) {
		setDataValue(column, value);
	}
	
	public synchronized String getString(String column) {
		if (columns == null) {
			load();
		}
		if (columns == null) return null;
		column = column.toUpperCase();
		return (String) columns.get(column);
	}
	
	public synchronized int getInt(String column) {
		if (columns == null) {
			load();
		}
		column = column.toUpperCase();
		Integer value = (Integer) columns.get(column);
		if (value == null) {
			return -1;
		}
		return value;
	}
	
	public synchronized long getLong(String column) {
		if (columns == null) {
			load();
		}
		column = column.toUpperCase();
		Long value = (Long) columns.get(column);
		if (value == null) {
			return -1;
		}
		return value;
	}
	
	public synchronized Timestamp getDate(String column) {
		if (columns == null) {
			load();
		}
		column = column.toUpperCase();
		return (Timestamp) columns.get(column);
	}
	
	private synchronized void setDataValue(String column, Object value) {
		if (columns == null) {
			columns = new HashMap<String, Object>();
		}
		column = column.toUpperCase();
		columns.put(column, value);
	}
	
	protected DataUtils getDataUtils() {
		return ((TraceImpl)getTrace()).getDataUtils();
	}
	
	public abstract long getModelID();


}
