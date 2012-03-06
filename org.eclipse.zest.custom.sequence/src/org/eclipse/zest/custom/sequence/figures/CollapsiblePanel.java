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
package org.eclipse.zest.custom.sequence.figures;

import org.eclipse.draw2d.Panel;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * A panel that can be collapsed horizontally or vertically. When collapsed, the preferred size, as well as the
 * bounds, will always return a 0 width or height depending on the collapse mode.
 * @author Del Myers
 */

public class CollapsiblePanel extends Panel {
	
	private boolean isHorizontal;
	
	private boolean isCollapsed;

	private boolean useLocalCoordinates;
	
	public CollapsiblePanel(boolean isHorizontal) {
		this.isHorizontal = isHorizontal;
		this.isCollapsed = true;
	}
	
	public CollapsiblePanel() {
		this(true);
	}
	
	/**
	 * @return the isCollapsed
	 */
	public boolean isCollapsed() {
		return isCollapsed;
	}
	
	/**
	 * @param isCollapsed the isCollapsed to set
	 */
	public void setCollapsed(boolean isCollapsed) {
		this.isCollapsed = isCollapsed;
		revalidate();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#getPreferredSize(int, int)
	 */
	@Override
	public Dimension getPreferredSize(int wHint, int hHint) {
		Dimension d = super.getPreferredSize(wHint, hHint);
		if (isCollapsed) {
			if (isHorizontal) {
				d.height = 0;
			} else {
				d.width = 0;
			}
		}
		return d;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#getBounds()
	 */
	@Override
	public Rectangle getBounds() {
		Rectangle b = super.getBounds().getCopy();
		if (isCollapsed) {
			if (isHorizontal) {
				b.height = 0;
			} else {
				b.width = 0;
			}
		}
		return b;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#setBounds(org.eclipse.draw2d.geometry.Rectangle)
	 */
	@Override
	public void setBounds(Rectangle rect) {
//		Rectangle b = rect.getCopy();
//		if (isCollapsed) {
//			if (isHorizontal) {
//				b.height = 0;
//			} else {
//				b.width = 0;
//			}
//		}
		super.setBounds(rect);
	}
	
	/**
	 * @return the isHorizontal
	 */
	public boolean isHorizontal() {
		return isHorizontal;
	}
	
	public void setCoordinateSystem(boolean isCoordinateSystem) {
		this.useLocalCoordinates = isCoordinateSystem;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#useLocalCoordinates()
	 */
	@Override
	protected boolean useLocalCoordinates() {
		return this.useLocalCoordinates;
	}

}
