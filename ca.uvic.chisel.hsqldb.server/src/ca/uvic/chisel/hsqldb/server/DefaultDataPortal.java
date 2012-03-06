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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * @author Del Myers
 *
 */
public class DefaultDataPortal implements IDataPortal {

	private Connection connection;
	private IPath alias;
	private HashMap<String, PreparedStatement> cachedStatements;
	private HashMap<String, CallableStatement> cachedCalls;
	boolean isReadOnly;

	/**
	 * @param append
	 * @throws CoreException 
	 */
	protected DefaultDataPortal(IPath databaseAlias) throws IOException {
		checkDatabase();
		isReadOnly = true;
		this.alias = databaseAlias;
		cachedStatements = new HashMap<String, PreparedStatement>();
		cachedCalls = new HashMap<String, CallableStatement>();
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.hsqldb.server.IDataPortal#close()
	 */
	@Override
	public synchronized void close() {
		try {
			cachedStatements.clear();
			cachedCalls.clear();
			if (connection != null) {
				connection.createStatement().execute("SHUTDOWN");
				connection.close();
				connection = null;
			}
		} catch (SQLException e) {
		}
	}
	

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.hsqldb.server.IDataPortal#getDBName()
	 */
	@Override
	public String getDBName() {
		try {
			return connection.getMetaData().getURL().toString();
		} catch (SQLException e) {
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.hsqldb.server.IDataPortal#getDefaultConnection()
	 */
	@Override
	public synchronized Connection getDefaultConnection(boolean readOnly) {
		try {
			if (!readOnly) {
				//make sure to open a new connection
				setWritable(true);
				return connection;
			} else if (connection == null) {
				this.connection = openConnection(alias, true);
			}
		} catch (IOException e) {
			DBPlugin.getDefault().log(e);
			return null;
		} catch (SQLException e) {
			DBPlugin.getDefault().log(e);
			return null;
		}
		return connection;
	}
	
	public Connection getDefaultConnection() {
		return getDefaultConnection(true);
	}
	
	public synchronized void setWritable(boolean writable) throws IOException, SQLException {
		if (!writable == isReadOnly) {
			return;
		} else {
			close();
			connection = openConnection(alias, !writable);
			isReadOnly = !writable;
			cachedStatements.clear();
		}
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.hsqldb.server.IDataPortal#prepareStatement(java.lang.String)
	 */
	@Override
	public synchronized PreparedStatement prepareStatement(String sql) throws SQLException {
		if (connection == null) {
			getDefaultConnection();
		}
		PreparedStatement statement = cachedStatements.get(sql);
		if (statement == null) {
			statement = connection.prepareStatement(sql);
			cachedStatements.put(sql, statement);
		}
		return statement;
	}
	
	/**
	 * Closes the connection to the database, and deletes all of the database files.
	 * @return true if the database could be cleared.
	 * @throws SQLException 
	 */
	public boolean clear() throws SQLException {
		//reset it
		isReadOnly = true;
		if (connection != null) {
			close();
			connection = null;
		}
		File aliasFile = alias.toFile();
		File parentDirectory = aliasFile.getParentFile();
		boolean success = true;
		if (parentDirectory.isDirectory()) {
			File[] databaseFiles = parentDirectory.listFiles();
			for (File file :databaseFiles) {
				if (file.getName().startsWith(aliasFile.getName() + ".")) {
					success |= file.delete();
				}
			}
		}
		return success;
	}
	
	/**
	 * Opens a new connection to a database on the specified path. Creates the
	 * database if it doesn't already exist. It is up to the caller to close the
	 * connection when finished.
	 * 
	 * @param databaseAlias
	 *            the path to the database.
	 * @param readOnly 
	 * @return a newly open connection.
	 * @throws CoreException
	 */
	private Connection openConnection(IPath databaseAlias, boolean readOnly) throws SQLException, IOException {
		checkDatabase();
		cachedStatements.clear();
		cachedCalls.clear();
		IPath propsPath = new Path(databaseAlias.toPortableString()
				+ ".properties");
		Properties props = new Properties();
		props.setProperty("hsqldb.script_format", "0");
		props.setProperty("sql.enforce_strict_size", "false");
		props.setProperty("hsqldb.cache_size_scale", "10");
		props.setProperty("hsqldb.cache_file_scale", "8");
		props.setProperty("readonly", "false");
		props.setProperty("hsqldb.nio_data_file", "true");
		props.setProperty("hsqldb.cache_scale", "11");
		props.setProperty("version", "1.8.0");
		props.setProperty("hsqldb.default_table_type", "cached");
		props.setProperty("hsqldb.log_size", "20");
		props.setProperty("modified", "yes");
		props.setProperty("hsqldb.cache_version", "1.7.0");
		props.setProperty("hsqldb.original_version", "1.8.0");
		props.setProperty("hsqldb.compatible_version", "1.8.0");
		props.setProperty("runtime.gc_interval", "10000");
			if (!propsPath.toFile().exists()) {
				
				

				FileWriter w = new FileWriter(propsPath.toFile());
				props.store(w, null);
				w.close();
//				 create a log to set the user
				IPath logPath = new Path(databaseAlias.toPortableString()
						+ ".log");
				PrintStream ps = new PrintStream(logPath.toFile());
				ps.println("CREATE USER SA PASSWORD \"\" ADMIN");
				ps.close();

			} else {
				FileReader r = new FileReader(propsPath.toFile());
				props.load(r);
				r.close();
				props.setProperty("readonly", (readOnly) ? "true" : "false");
			}
			
			Connection c = null;



			props.setProperty("user", "SA");
			props.setProperty("password", "");
			c = DriverManager.getConnection("jdbc:hsqldb:file:"
				+ databaseAlias.toPortableString(), props);
				

		
			return c;

	}

	
	private void checkDatabase() throws IOException {
		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (Exception e) {
			throw new IOException("could not load HSQLDB JDBC driver", e);
		}
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.hsqldb.server.IDataPortal#prepareCall(java.lang.String)
	 */
	@Override
	public synchronized CallableStatement prepareCall(String sql) throws SQLException {
		if (connection == null) {
			getDefaultConnection();
		}
		CallableStatement statement = cachedCalls.get(sql);
		if (statement == null) {
			statement = connection.prepareCall(sql);
			cachedCalls.put(sql, statement);
		}
		return statement;
	}

	

}
