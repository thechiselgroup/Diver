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
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * 
 * Draw as a control (a circle with an arrow on the circumference.
 * @author Del Myers
 *
 */
public class ControlFigure extends CircleFigure {
	@Override
	protected void outlineShape(Graphics graphics) {
		super.outlineShape(graphics);
		Rectangle circleBounds = getBoundingRectangle();
		graphics.drawLine(
				circleBounds.getCenter().x-2, 
				circleBounds.y,
				circleBounds.getCenter().x+2,
				circleBounds.y-3
			);
		graphics.drawLine(
				circleBounds.getCenter().x-2, 
				circleBounds.y,
				circleBounds.getCenter().x+2,
				circleBounds.y+3
			);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.figures.CircleFigure#getBoundingRectangle()
	 */
	@Override
	protected Rectangle getBoundingRectangle() {
		return super.getBoundingRectangle().shrink(3, 3);
	}
}
