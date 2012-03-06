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
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;



/**
 * Draws a collection (stacked rectangles)
 * @author Del Myers
 *
 */
public class CollectionFigure extends Shape {
	/**
	 * The number of rectangles to stack on top of one-another.
	 */
	private static final int STACKS = 3;
	
	/**
	 * The number of pixels between each stack.
	 */
	private static final int STACK_DEPTH = 2;

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Shape#fillShape(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void fillShape(Graphics graphics) {
		Rectangle rect = getReferenceRectanle();
		rect.width--;
		rect.height--;
		graphics.fillRectangle(rect);
		int x = rect.x + STACK_DEPTH;
		int y = rect.y-STACK_DEPTH;
		boolean done = (y < getBounds().y) || (x+rect.width > getBounds().x+getBounds().width);
		while (!done) {
			rect.x = x;
			rect.y = y;
			graphics.fillRectangle(rect);
			y-=STACK_DEPTH;
			x+=STACK_DEPTH;
			done = (y < getBounds().y) || (x+rect.width+STACK_DEPTH > getBounds().x+getBounds().width);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Shape#outlineShape(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void outlineShape(Graphics graphics) {
		Rectangle rect = getReferenceRectanle();
		rect.width--;
		rect.height--;
		graphics.drawRectangle(rect);
		int x = rect.x + STACK_DEPTH;
		int y = rect.y-STACK_DEPTH;
		boolean done = (y < getBounds().y) || (x+rect.width > getBounds().x+getBounds().width);
		//move up the bounds, drawing a border.
		while (!done) {
			graphics.drawLine(x, y, x, y+STACK_DEPTH);
			graphics.drawLine(x+rect.width, y+rect.height, x+rect.width-STACK_DEPTH, y+rect.height);
			graphics.drawLine(x, y, x+rect.width, y);
			graphics.drawLine(x+rect.width, y, x+rect.width, y+rect.height);
			y-=STACK_DEPTH;
			x+=STACK_DEPTH;
			done = (y < getBounds().y) || (x+rect.width+STACK_DEPTH > getBounds().x+getBounds().width);
		}
	}
	
	private Rectangle getReferenceRectanle() {
		Rectangle rect = getClientArea();
		if (getInsets() != null) {
			Insets insets = getInsets();
			rect.crop(new Insets(-insets.top, -insets.left, -insets.bottom, -insets.right));
		} if (useLocalCoordinates()) {
			rect.x += getBounds().x;
			rect.y += getBounds().y;
		}
		return rect;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#getClientArea(org.eclipse.draw2d.geometry.Rectangle)
	 */
	@Override
	public Rectangle getClientArea(Rectangle rect) {
		Rectangle bounds = getBounds();
		//three pixels between each rectangle.
		int depth = STACK_DEPTH*STACKS;
		if (bounds.height < depth || bounds.width < depth) {
			depth = 0;
		} 
		rect.setBounds(new Rectangle(bounds.x, bounds.y+depth, bounds.width-depth, bounds.height-depth));
		rect.crop(getInsets());
		if (useLocalCoordinates()) {
			rect.setLocation(0, depth);
		}
		return rect;
	}

}
