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
package ca.uvic.chisel.javasketch.data.model;

/**
 * Represents a method on a class.
 * @author Del Myers
 *
 */
public interface ITraceClassMethod extends ITraceModel {

	/**
	 * The name of the method.
	 * @return the name of the method.
	 */
	String getName();
	
	/**
	 * Returns a signature for the method, which can be combined with the class and
	 * name to create a unique identifier.
	 * @return a signature for the method.
	 */
	String getSignature();
	
	/**
	 * The class on which the method was defined.
	 * @return
	 */
	ITraceClass getTraceClass();
	
}
