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
 * An interface for providing colours to items which have text labels that are separate from
 * the other visual attributes of the element for example.
 * @author Del Myers
 */

public interface ITextColorProvider {
	
	/**
	 * Gets the foreground colour for the text.
	 * @param element the element to get the colour for.
	 * @return the text foreground.
	 */
	Color getTextForeground(Object element);
	
	/**
	 * Gets the background colour for the text.
	 * @param element the element to get the colour for.
	 * @return the text background.
	 */
	Color getTextBackground(Object element);

}
