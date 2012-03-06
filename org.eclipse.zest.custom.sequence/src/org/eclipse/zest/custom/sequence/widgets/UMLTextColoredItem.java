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

import org.eclipse.swt.graphics.Color;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * 
 * A uml item that allows for a different foreground/background color to be used on its
 * text label.
 * @author Del Myers
 *
 */
public class UMLTextColoredItem extends UMLColoredItem {

	private Color textForeground;
	private Color textBackground;
	
	/**
	 * @param parent
	 */
	protected UMLTextColoredItem(UMLChart parent) {
		super(parent);
	}
	
	/**
	 * The foreground colour for the text.
	 * @return
	 */
	public Color getTextForeground() {
		checkWidget();
		if (textForeground == null) {
			return getForeground();
		}
		return textForeground;
	}

	/**
	 * The background colour for the text.
	 * @return
	 */
	public Color getTextBackground() {
		checkWidget();
		return textBackground;
	}
	
	/**
	 * Sets the text foreground color.
	 * @param textForeground the new text foreground colour.
	 */
	public void setTextForeground(Color textForeground) {
		checkWidget();
		Color old = getTextForeground();
		this.textForeground = textForeground;
		firePropertyChange(IWidgetProperties.TEXT_FOREGROUND, old, textForeground);
	}
	
	/**
	 * Sets the text background colour.
	 * @param textBackground the new text background colour.
	 */
	public void setTextBackground(Color textBackground) {
		checkWidget();
		Color old = getTextBackground();
		this.textBackground = textBackground;
		firePropertyChange(IWidgetProperties.TEXT_BACKGROUND, old, textBackground);
	}
	
	

}
