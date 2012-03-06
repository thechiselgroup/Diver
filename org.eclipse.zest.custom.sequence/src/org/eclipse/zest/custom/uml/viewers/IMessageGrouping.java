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

import org.eclipse.swt.graphics.Color;

/**
 * An abstract interface that represents a group on an activation. A message group is
 * rendered as a labelled box surrounding a list of messages. This box can be collapsed
 * and may represent things such as loops or alternate execution paths. A group is defined
 * for a parent activation, and covers a range of messages originating from that
 * activation. If a number of groups 
 * are defined for a single parent, they may be completely contained within one-another, but
 * they must not overlap otherwise.
 * @author Del Myers
 */

public interface IMessageGrouping {
	
	/**
	 * Returns the activation element that this group is on.
	 * @return the activation element that this group is on.
	 */
	public Object getActivationElement();
	
	/**
	 * Returns the starting offset of the group in the parent
	 * activation. Defines the start of the range covered by
	 * the group. Must not be negative, and must be less than
	 * the total number of sub-activations that are retrieved
	 * from the activation element. The offset is defined <i>after</i>
	 * filtering has occurred on the viewer.
	 * @return the starting offset of the group in the parent
	 * activation.
	 */
	public int getOffset();
	
	/**
	 * Returns the length of the group. The group is not allowed
	 * to extend past the end of the sub-activations that are
	 * retrieved from the activation element.
	 * @return the length of the group in the parent activation.
	 */
	public int getLength();
	
	/**
	 * Returns the name of the group. This will be the text visible
	 * inside the caption for the group.
	 * @return the name of the group.
	 */
	public String getName();
	
	
	/**
	 * Returns the foreground color for the group. May return null
	 * to use the default foreground.
	 * @return the foreground color for the group.
	 */
	public Color getForeground();
	
	/**
	 * Returns the background colour for the group. May return null
	 * to use the default background.
	 * @return the background color for the group.
	 */
	public Color getBackground();

}
