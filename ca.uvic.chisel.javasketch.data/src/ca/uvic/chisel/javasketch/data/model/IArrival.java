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

import java.util.List;

/**
 * Represents the arrival of a call onto an activation.
 * @author Del Myers
 *
 */
public interface IArrival extends ITargetMessage {

	/**
	 * Returns string representations of the values passed as parameters.
	 * @return representations of the values passed as parameters.
	 */
	List<String> getValues();
	
	/**
	 * Returns a string which can be lexicographically ordered to indicate the sequence
	 * in which this message occurred within the parent activation. 
	 * @return a string which can be lexicographically ordered to indicate the sequence
	 * in which this message occurred within the parent activation. 
	 */
	String getSequence();
	
}
