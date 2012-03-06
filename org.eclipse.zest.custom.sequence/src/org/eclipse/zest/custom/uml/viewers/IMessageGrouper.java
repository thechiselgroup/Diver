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
 * An interface that can be used in Sequence Viewers to generate named groups
 * for messages on activations. Groups can represent things like loops or alternate paths
 * of execution.
 * @author Del Myers
 */

public interface IMessageGrouper {
	
	/**
	 * Calculates the groups on the given activation element. Groups may be contained within
	 * other groups, but they may not overlap. That is, a group may not have its starting location
	 * within another group unless its end is also contained within that other group. Behaviour is
	 * undefined for such cases.
	 * 
	 * Groups are calculated on the <i>filtered</i> elements of a sequence chart. That is, elements
	 * have to pass the viewers filters before they are passed to this method for calculation.
	 * 
	 * @param viewer the viewer in which the grouping is being performed.
	 * @param activationElement the 
	 * @param children the children of activationElement that are visible in the chart. 
	 * These children should be used when calculating the regions.
	 * @return the groups.
	 */
	public IMessageGrouping[] calculateGroups(UMLSequenceViewer viewer, Object activationElement, Object[] children);
	
	/**
	 * Called do dispose of any colors or fonts that had to be created
	 * for the grouping.
	 */
	public void dispose();
}
