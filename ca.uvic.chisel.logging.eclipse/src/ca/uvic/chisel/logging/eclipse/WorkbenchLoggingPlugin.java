/*******************************************************************************
 * Copyright (c) 2010 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.logging.eclipse;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ca.uvic.chisel.logging.eclipse.internal.CategoryManager;
import ca.uvic.chisel.logging.eclipse.internal.EventLogger;

/**
 * The activator class controls the plug-in life cycle
 */
public class WorkbenchLoggingPlugin extends AbstractUIPlugin {
	//flag to say whether or not the logger is enabled or disabled.
	//set to disabled after a study is completed.
	public static final boolean ENABLED = false;

	// The plug-in ID
	public static final String PLUGIN_ID = "ca.uvic.chisel.logging.eclipse";

	// The shared instance
	private static WorkbenchLoggingPlugin plugin;
	
	private IEventLogger eventLogger;

	private ICategoryManager categoryManager;
	
	/**
	 * The constructor
	 */
	public WorkbenchLoggingPlugin() {
		eventLogger = new EventLogger();
		categoryManager = new CategoryManager();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static WorkbenchLoggingPlugin getDefault() {
		return plugin;
	}

	/**
	 * @param e
	 */
	public void log(Exception e) {
		getLog().log(createStatus(e));
	}

	/**
	 * @param e
	 * @return
	 */
	public IStatus createStatus(Exception e) {
		if (e instanceof CoreException) {
			return ((CoreException)e).getStatus();
		}
		String message = "";
		if (e.getMessage() != null) {
			message = e.getMessage();
		}
		return new Status(IStatus.ERROR, PLUGIN_ID, message, e);
	}
	
	/**
	 * @return the eventLogger
	 */
	public IEventLogger getEventLogger() {
		return eventLogger;
	}
	
	/**
	 * @return the categoryManager
	 */
	public ICategoryManager getCategoryManager() {
		return categoryManager;
	}

	/**
	 * Creates an encripted user id for this user.
	 * @return the encripted user id.
	 */
	public String getLocalUser() {
		String UID = getPreferenceStore().getString("eclipse.logger.userid");
		if (UID.isEmpty()) {
			String ip = "";
			try {
				InetAddress address = InetAddress.getLocalHost();
				ip = address.getHostAddress();
			} catch (UnknownHostException e) {
				//do nothing: keep IP empty
			}
			String instanceLocation = "";
			Location location = Platform.getInstanceLocation();
			if (location != null) {
				URL url = location.getURL();
				if (url != null) {
					instanceLocation = url.toString();
				}
			}
			UID = ip + ":" + instanceLocation;
			//encript
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				StringBuilder builder = new StringBuilder();
				byte[] digest = md.digest(UID.getBytes());
				for (byte b : digest) {
					String padded = String.format("%02x", (int)b&0xFF);
					builder.append(padded);
				}
				UID = builder.toString();
			} catch (NoSuchAlgorithmException e) {
				UID = "" + UID.hashCode();
			}
			getPreferenceStore().setValue("eclipse.logger.userid", UID);
		}
		return UID;
	}

	/**
	 * @return
	 */
	public static boolean isEnabled() {
		return ENABLED;
	}

}
