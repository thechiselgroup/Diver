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
 * Event object for expansion events on the sequence viewer.
 * @author Del Myers
 */

public class SequenceViewerEvent extends EventObject {

	private Object element;

	/**
	 * @param source
	 */
	public SequenceViewerEvent(UMLSequenceViewer source, Object element) {
		super(source);
		this.element = element;
	}
	
	/**
	 * Returns the sequence viewer that the event originated on.
	 * @return the sequence viewer that the event originated on.
	 */
	public UMLSequenceViewer getViewer() {
		return (UMLSequenceViewer)getSource();
	}
	
	/**
	 * @return the element that was expanded or collapsed.
	 */
	public Object getElement() {
		return element;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3364395431122260368L;

}
