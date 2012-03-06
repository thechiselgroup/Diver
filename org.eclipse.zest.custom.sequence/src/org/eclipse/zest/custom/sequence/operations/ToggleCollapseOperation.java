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
package org.eclipse.zest.custom.sequence.operations;

import org.eclipse.zest.custom.sequence.widgets.IExpandableItem;

/**
 * Toggles the expanded/collapsed state of an activation and causes the graph to be updated.
 * @author Del Myers
 */

public class ToggleCollapseOperation implements Runnable {
	IExpandableItem item;
	public ToggleCollapseOperation(IExpandableItem item) {
		this.item = item;
	}
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		item.setExpanded(!item.isExpanded());
	}

}
