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

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Event;

/**
 * Instances of this event are sent as a result of items in a sequence chart being expanded or collapsed,
 * or when the root activation of a sequence chart changes.
 * 
 * In the case of an item being expanded or collapsed, the <code>item</code> field will contain the IExpandableItem
 * that changed. In the case of the root of the graph changing, the <code>item</code> field will contain
 * the new root.
 * 
 * @author Del Myers
 */

public class SequenceEvent extends SelectionEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8351452971299329707L;

	
	public SequenceEvent(Event e) {
		super(e);
	}

}
