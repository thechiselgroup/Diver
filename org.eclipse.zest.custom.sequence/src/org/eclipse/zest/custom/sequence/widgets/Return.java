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
 * A concrete implementation of Message that represents a return to a previous activation.
 * @author Del Myers
 *
 */
public class Return extends Message {

	/**
	 * @param chart
	 */
	public Return(UMLSequenceChart chart) {
		super(chart);
	}

}
