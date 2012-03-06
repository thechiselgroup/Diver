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
 * An event in a UMLSequenceViewer that indicates that the root element in the viewer has changed. The
 * root element is different from the input element.
 * @author Del Myers
 */

public class SequenceViewerRootEvent extends EventObject {


	/**
	 * Creates an event with the given source.
	 * @param source the viewer that was the source of the event.
	 */
	public SequenceViewerRootEvent(UMLSequenceViewer source) {
		super(source);
	}
	
	public UMLSequenceViewer getSequenceViewer() {
		return (UMLSequenceViewer) getSource();
	}
	


	/**
	 * 
	 */
	private static final long serialVersionUID = -4030773947695818812L;

}
