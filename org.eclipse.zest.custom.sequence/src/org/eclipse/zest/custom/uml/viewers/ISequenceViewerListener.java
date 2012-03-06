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
 * Listens for collapse and expansion events in a sequence viewer.
 * @author Del Myers
 */

public interface ISequenceViewerListener {
	
	/**
	 * Indicates when an activation or a package has been expanded in the viewer.
	 * @param event the event object.
	 */
	void elementExpanded(SequenceViewerEvent event);
	
	/**
	 * Indicates when an activation or package has been collapsed in the viewer.
	 * @param event the event object.
	 */
	void elementCollapsed(SequenceViewerEvent event);
	
	/**
	 * Indicates when an activation group has been expanded in the viewer.
	 * @param event the event object.
	 */
	void groupExpanded(SequenceViewerGroupEvent event);
	
	/**
	 * Indicates when an activation group has been collapsed in the viewer.
	 * @param event the event object.
	 */
	void groupCollapsed(SequenceViewerGroupEvent event);
	
	/**
	 * Indicates when the root activation of a sequence viewer has changed.
	 * @param event the event object.
	 */
	void rootChanged(SequenceViewerRootEvent event);

}
