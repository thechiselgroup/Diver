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
 * Concrete implementation of IMessageGroup
 * @author Del Myers
 */

public class MessageGrouping implements IMessageGrouping {
	
	private Object activationElement;
	private int offset;
	private int length;
	private String name;
	private Color foreground;
	private Color background;
	
	public MessageGrouping(Object activationElement) {
		this(activationElement, 0, 1, "", null, null);
	}

	public MessageGrouping(Object activationElement, int offset, int length, String name) {
		this(activationElement, offset, length, name, null, null);
	}
	
	public MessageGrouping(Object activationElement, int offset, int length, String name, Color foreground, Color background) {
		this.activationElement = activationElement;
		this.offset = offset;
		this.length = length;
		this.name = name;
		this.foreground = foreground;
		this.background = background;
	}

	public Object getActivationElement() {
		return activationElement;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	public String getName() {
		return name;
	}

	public Color getForeground() {
		return foreground;
	}

	public Color getBackground() {
		return background;
	}

	/**
	 * Sets the length for the grouping.
	 * @param length
	 */
	public void setLength(int length) {
		this.length = length;		
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @param background the background to set
	 */
	public void setBackground(Color background) {
		this.background = background;
	}
	
	/**
	 * @param foreground the foreground to set
	 */
	public void setForeground(Color foreground) {
		this.foreground = foreground;
	}
	
	
	/**
	 * @param offset the offset to set
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	

}
