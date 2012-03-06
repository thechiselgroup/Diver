/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.hsqldb.server;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class DBPlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ca.uvic.chisel.hsqldb.server";

	// The shared instance
	private static DBPlugin plugin;

	private Map<IPath, IDataPortal> portals;

	/**
	 * The constructor
	 */
	public DBPlugin() {
		portals = Collections
			.synchronizedMap(new HashMap<IPath, IDataPortal>());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		// make sure the databases are closed
		org.hsqldb.DatabaseManager.closeDatabases(0);
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static DBPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an object that will allow users to manipulate EMF models within a
	 * database using CDO. The database currently supported is hsqldb. EMF
	 * models must have been generated for CDO.
	 * 
	 * @param databaseAlias
	 *            the location in the file system of the database. The final
	 *            location of the files for the data base will be in
	 *            <code>databaseAlias + "/db" </code>
	 * @return the data portal.
	 * @throws CoreException
	 *             if there was a problem initialising the database
	 * @throws IOException 
	 */
	public synchronized IDataPortal getDataPortal(IPath databaseAlias)
			throws CoreException {
		IDataPortal portal = portals.get(databaseAlias);
		if (portal == null) {
			try {
				portal = new DefaultDataPortal(databaseAlias.append("db"));
			} catch (IOException e) {
				throw new CoreException(new Status(Status.ERROR, PLUGIN_ID, "Unable to open portal to data", e));
			}
			portals.put(databaseAlias, portal);
		}
		return portal;
	}



	public void log(Exception e) {
		if (e instanceof CoreException) {
			getLog().log((((CoreException) e).getStatus()));
		} else {
			String message = e.getMessage();
			if (message == null) {
				message = "";
			}
			getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, e));
		}
	}

}
