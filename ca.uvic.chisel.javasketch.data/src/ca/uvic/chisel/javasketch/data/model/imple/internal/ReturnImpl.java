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

import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.IReturn;

/**
 * @author Del Myers
 *
 */
public class ReturnImpl extends MessageImpl implements IReturn {

	/**
	 * @param thread
	 * @param results
	 * @throws SQLException
	 */
	public ReturnImpl(ThreadImpl thread, ResultSet results) throws SQLException {
		super(thread, results);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITargetMessage#getOrigin()
	 */
	public IOriginMessage getOrigin() {
		return (IOriginMessage) getOpposite();
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getKind()
	 */
	public int getKind() {
		return RETURN;
	}

}
