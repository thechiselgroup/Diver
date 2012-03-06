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

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * A figure for a control class (draws a circle).
 * 
 * @author Del Myers
 *
 */
public class CircleFigure extends Shape {

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Shape#fillShape(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void fillShape(Graphics graphics) {
		graphics.fillOval(getBoundingRectangle());
		outlineShape(graphics);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Shape#outlineShape(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void outlineShape(Graphics graphics) {
		Rectangle r = getBoundingRectangle();
		r.width--;
		r.height--;
		graphics.drawOval(r);
	}

	/**
	 * Gets the bounding rectangle for the circle.
	 * @return
	 */
	protected Rectangle getBoundingRectangle() {
		Point center = bounds.getCenter();
		int size = Math.min(bounds.width, bounds.height);
		return new Rectangle(center.x - size/2, center.y-size/2, size, size);
	}
}
