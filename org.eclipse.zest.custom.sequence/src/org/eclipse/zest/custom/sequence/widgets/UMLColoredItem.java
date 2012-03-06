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
 * An item that allows the setting of foreground and background colors.
 * @author Del Myers
 *
 */
public class UMLColoredItem extends UMLItem {

	/**
	 * The foreground color
	 */
	private Color foreground;
	
	/**
	 * The background color.
	 */
	private Color background;
	
	
	/**
	 * @param parent
	 */
	protected UMLColoredItem(UMLChart parent) {
		super(parent);
	}
	
	/**
	 * @param foreground the foreground to set
	 */
	public void setForeground(Color foreground) {
		Object old = this.foreground;
		this.foreground = foreground;
		firePropertyChange(IWidgetProperties.FOREGROUND_COLOR, old, foreground);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.widgets.UMLItem#getForground()
	 */
	@Override
	public Color getForeground() {
		return foreground;
	}
	
	/**
	 * @param background the background to set
	 */
	public void setBackground(Color background) {
		Object old = this.background;
		this.background = background;
		firePropertyChange(IWidgetProperties.BACKGROUND_COLOR, old, background);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.widgets.UMLItem#getBackground()
	 */
	@Override
	public Color getBackground() {
		return background;
	}


}
