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
package ca.uvic.chisel.javasketch.data.model.imple.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

import ca.uvic.chisel.javasketch.data.model.IThrow;

/**
 * @author Del Myers
 *
 */
public class ThrowImpl extends ReplyImpl implements IThrow {

	/**
	 * @param thread
	 * @param results
	 * @throws SQLException
	 */
	public ThrowImpl(ThreadImpl thread, ResultSet results) throws SQLException {
		super(thread, results);
	}

}
