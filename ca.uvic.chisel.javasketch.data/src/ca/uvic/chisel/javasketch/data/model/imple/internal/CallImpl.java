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
import java.util.List;

import ca.uvic.chisel.javasketch.data.internal.WriteDataUtils;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.javasketch.data.model.ITargetMessage;

/**
 * @author Del Myers
 *
 */
public class CallImpl extends MessageImpl implements ICall {
	
	List<Long> sequence;

	
	
	/**
	 * @param thread
	 * @param results
	 * @throws SQLException
	 */
	public CallImpl(ThreadImpl thread, ResultSet results) throws SQLException {
		super(thread, results);
		sequence = null;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IOriginMessage#getSequence()
	 */
	public String getSequence() {
		return getString("sequence");
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ICall#getOrderedSequence()
	 */
	public List<Long> getOrderedSequence() {
		if (sequence == null) {
			sequence = WriteDataUtils.fromStoredSequence(getSequence());
		}
		return sequence;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IOriginMessage#getTarget()
	 */
	public ITargetMessage getTarget() {
		return (ITargetMessage) getOpposite();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String name = "Call on ";
		try {
			IActivation target = getTarget().getActivation();
			name = name + target.getMethod().getName();
		} catch (NullPointerException e) {}
		return name;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getKind()
	 */
	public int getKind() {
		return CALL;
	}

}
