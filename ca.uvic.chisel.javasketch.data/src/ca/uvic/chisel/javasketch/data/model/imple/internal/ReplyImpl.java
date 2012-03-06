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

import ca.uvic.chisel.javasketch.data.model.IReply;
import ca.uvic.chisel.javasketch.data.model.ITargetMessage;

/**
 * @author Del Myers
 *
 */
public class ReplyImpl extends MessageImpl implements IReply {

	/**
	 * @param thread
	 * @param results
	 * @throws SQLException
	 */
	public ReplyImpl(ThreadImpl thread, ResultSet results) throws SQLException {
		super(thread, results);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IReply#getReturnValue()
	 */
	public String getReturnValue() {
		return "?";
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IOriginMessage#getSequence()
	 */
	public String getSequence() {
		return getString("sequence");
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IOriginMessage#getTarget()
	 */
	public ITargetMessage getTarget() {
		return (ITargetMessage) getOpposite();
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getKind()
	 */
	public int getKind() {
		return REPLY;
	}

}
