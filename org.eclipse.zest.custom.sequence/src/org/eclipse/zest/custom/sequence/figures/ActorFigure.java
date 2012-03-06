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
 * A simple figure used to display an actor.
 * @author Del Myers
 *
 */
public class ActorFigure extends Shape {
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Shape#fillShape(org.eclipse.draw2d.Graphics)
	 */
	protected void fillShape(Graphics graphics) {
		outlineShape(graphics);
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Shape#outlineShape(org.eclipse.draw2d.Graphics)
	 */
	protected void outlineShape(Graphics graphics) {
		Rectangle bounds = getBounds();
		int diameter = Math.min(bounds.width/3, bounds.height/3);
		Rectangle headBounds = new Rectangle(bounds.x + (bounds.width/2 - diameter/2), bounds.y, diameter, diameter);
		graphics.fillOval(headBounds);
		graphics.drawOval(headBounds);
		graphics.drawLine(bounds.x + bounds.width/2, headBounds.height+bounds.y,bounds.x +  bounds.width/2, bounds.y+(2*bounds.height/3));
		graphics.drawLine(bounds.x + bounds.width/5, bounds.y+(3*bounds.height/7), (bounds.x + 4*bounds.width/5), bounds.y+(3*bounds.height/7));
		graphics.drawLine(bounds.x + bounds.width/2, bounds.y+(2*bounds.height/3), bounds.x, bounds.y+bounds.height);
		graphics.drawLine(bounds.x + bounds.width/2, bounds.y+(2*bounds.height/3), bounds.x+bounds.width, bounds.y+bounds.height);
	}
	
}
