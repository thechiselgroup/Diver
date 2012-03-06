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
package org.eclipse.zest.custom.sequence.events;

/**
 * Listener for sequence events. These events include expanding/collapsing of activations or packages, and
 * changes in the root activation of the chart.
 * @author Del Myers
 */

public interface SequenceListener {
	
	/**
	 * A package or activation has been expanded in the chart. The <code>item</code> field of the given
	 * event will be set to the IExpandableItem that changed. The <code>widget</code> field will be set to
	 * the chart that the event occurred on.
	 * @param event the sequence event.
	 */
	public void itemExpanded(SequenceEvent event);
	/**
	 * A package or activation has been collapsed in the chart. The <code>item</code> field of the given
	 * event will be set to the IExpandableItem that changed. The <code>widget</code> field will be set to
	 * the chart that the event occurred on.
	 * @param event the sequence event.
	 */
	public void itemCollapsed(SequenceEvent event);
	/**
	 * The root activation has been changed in the chart. The <code>item</code> field of the given
	 * event will be set to the new root. The <code>widget</code> field will be set to
	 * the chart that the event occurred on.
	 * @param event the sequence event.
	 */
	public void rootChanged(SequenceEvent event);

}
