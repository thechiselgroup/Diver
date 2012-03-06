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
package ca.uvic.chisel.hsqldb.server;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;



/**
 * Offers an interface to EMF models and resource sets that are managed by
 * CDO.
 * @author Del Myers
 *
 */
public interface IDataPortal {
	
	
	
	/**
	 * Returns the name of the database.
	 * @return the name of the database.
	 */
	public String getDBName();
	
	/**
	 * Returns a default connection to the database. Users are not required to close this
	 * connection, and they should not retain references to it as the connection may
	 * be closed by the platform.
	 * 
	 * @return
	 */
	public Connection getDefaultConnection(boolean readOnly);
	
	/**
	 * Gets the current default connection. Convenience method for 
	 * getDefaultConnection(true);
	 * @return the connection.
	 */
	public Connection getDefaultConnection();
	
	/**
	 * Closes the open view.
	 */
	public void close();
	
	/**
	 * Prepares a statement in the default connection with the given SQL, and stores it. This
	 * is a convenience method to ensure that clients don't repeatedly prepare statements
	 * in the database.
	 * @param sql the sql to prepare the statement for.
	 * @return
	 */
	public PreparedStatement prepareStatement(String sql) throws SQLException;
	
	public CallableStatement prepareCall(String sql) throws SQLException;

	/**
	 * Makes the database either writable or read only.
	 * @param writable the new writable state.
	 * @throws IOException
	 * @throws SQLException 
	 */
	public void setWritable(boolean writable) throws IOException, SQLException;
	
	
	
}
