/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Item;

/**
 * An annotated range for use in the range slider
 * @author Del Myers
 *
 */
public class RangeAnnotation extends Item{
	
	private Color foreground;
	private Color background;
	private long offset;
	private long length;
	private RangeSlider slider;
	
	public RangeAnnotation(RangeSlider slider) {
		super(slider, SWT.NONE);
		this.slider = slider;
		slider.createItem(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Item#setText(java.lang.String)
	 */
	@Override
	public void setText(String string) {
		super.setText(string);
		slider.redraw();
	}
	/**
	 * @param foreground the foreground to set
	 */
	public void setForeground(Color foreground) {
		checkWidget();
		this.foreground = foreground;
		slider.redraw();
	}
	
	/**
	 * @param background the background to set
	 */
	public void setBackground(Color background) {
		checkWidget();
		this.background = background;
		slider.redraw();
	}
	
	/**
	 * @param offset the offset to set
	 */
	public void setOffset(long offset) {
		checkWidget();
		this.offset = offset;
		slider.redraw();
	}
	
	/**
	 * @param length the length to set
	 */
	public void setLength(long length) {
		checkWidget();
		this.length = length;
		slider.redraw();
	}
	
	/**
	 * @return the offset
	 */
	public long getOffset() {
		checkWidget();
		return offset;
	}
	
	/**
	 * @return the length
	 */
	public long getLength() {
		checkWidget();
		return length;
	}
	
	/**
	 * @return the foreground
	 */
	public Color getForeground() {
		checkWidget();
		return foreground;
	}
	
	/**
	 * @return the background
	 */
	public Color getBackground() {
		checkWidget();
		return background;
	}
	
	
	
	

}
