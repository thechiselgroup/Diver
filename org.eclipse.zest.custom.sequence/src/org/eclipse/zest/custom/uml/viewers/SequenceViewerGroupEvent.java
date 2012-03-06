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

import java.util.EventObject;

/**
* Event object for expansion events on activation groups in the sequence viewer.
* @author Del Myers
*/

public class SequenceViewerGroupEvent extends EventObject {

	private IMessageGrouping element;

	/**
	 * @param source
	 */
	public SequenceViewerGroupEvent(UMLSequenceViewer source, IMessageGrouping group) {
		super(source);
		this.element = group;
	}
	
	/**
	 * Returns the sequence viewer that the event originated on.
	 * @return the sequence viewer that the event originated on.
	 */
	public UMLSequenceViewer getViewer() {
		return (UMLSequenceViewer)getSource();
	}
	
	/**
	 * @return the group that was expanded or collapsed.
	 */
	public IMessageGrouping getGroup() {
		return element;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3364395431122260368L;

}