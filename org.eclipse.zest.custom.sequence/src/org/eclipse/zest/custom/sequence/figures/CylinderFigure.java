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
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * 
 * Draw as a cylinder.
 * @author Del Myers
 *
 */
public class CylinderFigure extends Shape {
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Shape#fillShape(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void fillShape(Graphics graphics) {
		Rectangle bounds = getBounds();
		int elipseHeight = bounds.height/4;
		Rectangle elipse = new Rectangle(bounds.x, bounds.y, bounds.width, elipseHeight);
		Rectangle bottom = new Rectangle(bounds.x, bounds.y+elipseHeight*3, bounds.width, elipseHeight);
		graphics.fillOval(bottom);
		graphics.fillRectangle(bounds.x, elipse.getCenter().y, bounds.width, elipseHeight*3);
		graphics.drawLine(bounds.x, elipse.getCenter().y, bounds.x, bottom.getCenter().y);
		graphics.drawLine(bounds.x+bounds.width, elipse.getCenter().y, bounds.x+bounds.width, bottom.getCenter().y);
		graphics.fillOval(elipse);
		
	}
	@Override
	protected void outlineShape(Graphics graphics) {
		Rectangle bounds = getBounds().getCopy();
		bounds.width--;
		bounds.height--;
		int elipseHeight = bounds.height/4;
		Rectangle elipse = new Rectangle(bounds.x, bounds.y, bounds.width, elipseHeight);
		Rectangle bottom = new Rectangle(bounds.x, bounds.y+elipseHeight*3, bounds.width, elipseHeight-1);
		graphics.drawArc(bottom, 180, 180);
		graphics.drawLine(bounds.x, elipse.getCenter().y, bounds.x, bottom.getCenter().y);
		graphics.drawLine(bounds.x+bounds.width, elipse.getCenter().y, bounds.x+bounds.width, bottom.getCenter().y);
		graphics.drawOval(elipse);
	}
	
}
