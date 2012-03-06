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

/**
 * Converts an object to a string for logging.
 * 
 * @author Del Myers
 *
 */
public interface ILogObjectInterpreter {
	
	/**
	 * Converts the given object to a string for logging. Tabs, newlines, and
	 * equals (=) signs are all illegal, and will be transformed into single
	 * spaces for the final output.
	 * @param object the object to interpret.
	 * @return a string representing the object for logging purposes.
	 */
	public String toString(Object object);

}
