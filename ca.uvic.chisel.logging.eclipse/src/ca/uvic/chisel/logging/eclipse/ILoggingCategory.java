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

import java.net.URL;

/**
 * Represents a category for logging.
 * @author Del Myers
 *
 */
public interface ILoggingCategory {
	/**
	 * The category id.
	 * @return the category id.
	 */
	public String getCategoryID();
	
	/**
	 * A human-readable name for the logging category
	 * @return a human readable name for the logging category
	 */
	public String getName();
	
	/**
	 * The disclaimer for the category.
	 * @return the disclaimer.
	 */
	public String getDisclaimer();
	
	/**
	 * True if the disclaimer should be read as HTML.
	 * @return if the disclaimer should be read as HTML.
	 */
	public boolean isHTML();
	
	/**
	 * The url for uploading logs to.
	 * @return the url for uploading logs to.
	 */
	public URL getURL();
	
	/**
	 * Returns an interpreter for the given class.
	 * @param clazz the class to interpret.
	 * @return an interpreter. Will not be null.
	 */
	public ILogObjectInterpreter getInterpreter(Class<?> clazz);
	
	/**
	 * Returns true if logging is enabled for this category.
	 * @return true iff logging is enabled for this category.
	 */
	public boolean isEnabled();
	
	/**
	 * Sets the enabled state of this logging category.
	 * @param enabled the new enabled state.
	 */
	public void setEnabled(boolean enabled);

	/**
	 * Returns the name of the provider of this logger.
	 * @return the name of the provider of this logger.
	 */
	public String getProvider();
	
}
