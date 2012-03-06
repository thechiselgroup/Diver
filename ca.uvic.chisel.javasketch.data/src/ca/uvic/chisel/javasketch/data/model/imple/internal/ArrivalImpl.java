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
import java.util.Collections;
import java.util.List;

import ca.uvic.chisel.javasketch.data.internal.WriteDataUtils;
import ca.uvic.chisel.javasketch.data.model.IArrival;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;

/**
 * @author Del Myers
 *
 */
public class ArrivalImpl extends MessageImpl implements IArrival {

	private String sequence;


	/**
	 * @param thread
	 * @param results
	 * @throws SQLException
	 */
	public ArrivalImpl(ThreadImpl thread, ResultSet results)
			throws SQLException {
		super(thread, results);
		sequence = null;
	}


	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IArrival#getValues()
	 */
	public List<String> getValues() {
		// TODO Auto-generated method stub
		return Collections.emptyList();
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITargetMessage#getOrigin()
	 */
	public IOriginMessage getOrigin() {
		return (IOriginMessage)getOpposite();
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IOriginMessage#getSequence()
	 */
	public String getSequence() {
		if (sequence == null) {
			String s = getString("sequence");
			sequence = WriteDataUtils.fromStoredSequenceString(s);
		}
		return sequence;
	}


	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getKind()
	 */
	public int getKind() {
		return ARRIVAL;
	}

}
