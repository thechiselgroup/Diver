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

import ca.uvic.chisel.javasketch.data.SketchDataPlugin;
import ca.uvic.chisel.javasketch.data.model.ITraceMetaEvent;

/**
 * @author Del Myers
 *
 */
public class TraceEventImpl extends TraceModelIDBase implements ITraceMetaEvent {

	/**
	 * @param trace
	 * @param results
	 * @throws SQLException
	 */
	public TraceEventImpl(TraceImpl trace, ResultSet results)
			throws SQLException {
		super(trace, results);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#load()
	 */
	@Override
	public void load() {
		try {
			loadFromResults(getDataUtils().getEvent(getModelID()));
		} catch (SQLException e) {
			SketchDataPlugin.getDefault().log(e);
		}
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceMetaEvent#getText()
	 */
	public String getText() {
		return getString("text");
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceMetaEvent#getTime()
	 */
	public long getTime() {
		return getLong("time");
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getKind()
	 */
	public int getKind() {
		return EVENT;
	}

}
