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
package org.eclipse.zest.custom.sequence.widgets;

/**
 * Represents an item that can be collapsed or expanded.
 * @author Del Myers
 */

public interface IExpandableItem {
	
	/**
	 * Sets the expanded state of this item to expanded. A property change event for EXPANDED_PROP will
	 * be fired, if the state has changed.
	 * @param expanded the new expanded state.
	 */
	public void setExpanded(boolean expanded);
	
	/**
	 * Returns the expanded state of this item.
	 * @return the expanded state of this item.
	 */
	public boolean isExpanded();
	
	
	UMLChart getChart();
	
	
	/**
	 * Adds the given property change listener to the list of listeners if it hasn't already been added.
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener);
	
	/**
	 * Removes the given listener from the list of listeners.
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener);

}
