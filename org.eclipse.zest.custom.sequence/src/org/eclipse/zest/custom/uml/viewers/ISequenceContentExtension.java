/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers -- initial API and implementation
 *******************************************************************************/
package org.eclipse.zest.custom.uml.viewers;

/**
 * An extension for ISequenceContentProvider that allows the definition of 
 * groups for lifelines. Implementors must be careful to not introduce cycles into
 * the groupings. Otherwise, memory leaks and infinite processes will occurr.
 * @author Del Myers
 */

public interface ISequenceContentExtension {
	
	/**
	 * Returns an object that represents the grouping for the given lifeline or
	 * group. Lifeline groups allow for horizontal compaction of the chart. When a
	 * lifeline can be "grouped", a small +/- button will be overlaid on it. Grouping
	 * a lifeline by selecting the the +/- button will cause all of that lifeline's 
	 * activations to be displayed on its parent "group", and the lifeline will disappear from
	 * the main chart. The same is true for the parent groups.
	 * @param lifelineOrGroup the lifeline or group to query.
	 * @return the group that is the parent for lifelineOrGroup.
	 */
	public Object getContainingGroup(Object lifelineOrGroup);
	
	/**
	 * Returns true iff the given lifeline or group is contained in a parent group.
	 * getContainingGroup() will only be called if this method returns true.
	 * @param lifelineOrGroup the lifeline or group to query
	 * @return true iff the given object is contained in a parent group.
	 */
	public boolean hasContainingGroup(Object lifelineOrGroup);

}

