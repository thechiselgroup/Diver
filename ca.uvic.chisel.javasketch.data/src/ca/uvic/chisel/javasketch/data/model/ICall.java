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
 * Represents a call from one activation to another.
 * 
 * @author Del Myers
 *
 */
public interface ICall extends IOriginMessage {
	
	/**
	 * Returns a string which can be lexicographically ordered to indicate the sequence
	 * in which this message occurred within the parent activation. 
	 * @return a string which can be lexicographically ordered to indicate the sequence
	 * in which this message occurred within the parent activation. 
	 */
	String getSequence();
	
	/**
	 * Returns a list of numbers representing a numerically ordered tree
	 * structure for where this call occurs in the call tree.
	 * @return  a list of numbers representing a numerically ordered tree
	 * structure for where this call occurs in the call tree.
	 */
	List<Long> getOrderedSequence();
	
}
