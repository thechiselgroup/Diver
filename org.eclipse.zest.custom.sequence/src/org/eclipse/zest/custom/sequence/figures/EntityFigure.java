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
 * Draws a figure for an entity (an underlined Circle).
 * @author Del Myers
 *
 */
public class EntityFigure extends CircleFigure {
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.figures.CircleFigure#outlineShape(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void outlineShape(Graphics graphics) {
		super.outlineShape(graphics);
		Rectangle circleBounds = getBoundingRectangle();
		graphics.drawLine(circleBounds.x, circleBounds.y + circleBounds.height,
				circleBounds.x + circleBounds.width, circleBounds.y
						+ circleBounds.height);
	}
}
