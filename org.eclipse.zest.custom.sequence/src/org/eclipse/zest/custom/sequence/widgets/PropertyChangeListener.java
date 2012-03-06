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
 * Listener for property changes.
 * @author Del Myers
 */

public interface PropertyChangeListener {
	/**
	 * The given property has changed on the source object.
	 * @param source
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	public void propertyChanged(Object source, String property, Object oldValue, Object newValue);
}
