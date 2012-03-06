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


import java.util.Iterator;

import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;

/**
 * A simple figure that draws a dashed line from the top of it's
 * bounds to the bottom.
 * 
 * 
 * @author Del Myers
 *
 */
public class LifeLineFigure extends Figure {
	private class RatioBorder extends AbstractBorder {
		private float h;
		private float v;

		public RatioBorder(float h, float v) {
			this.h = h; 
			this.v = v;
		}
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Border#getInsets(org.eclipse.draw2d.IFigure)
		 */
		public Insets getInsets(IFigure figure) {
			Rectangle b = figure.getBounds().getCopy();
			float vscale = 1-v;
			float hscale = 1-h;
			float height = b.height*vscale;
			float width = b.width*hscale;
			int y = Math.round((b.height-height)/2);
			int x = Math.round((b.width-width)/2);
			return new Insets(y,x,y,x);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Border#paint(org.eclipse.draw2d.IFigure, org.eclipse.draw2d.Graphics, org.eclipse.draw2d.geometry.Insets)
		 */
		public void paint(IFigure figure, Graphics graphics, Insets insets) {
		}
		
	}

	private boolean filled;
	/**
	 * 
	 */
	public LifeLineFigure() {
		//super.setLayoutManager(new LifeLineLayout());
		setBorder(new RatioBorder(0,.005f));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#paintFigure(org.eclipse.draw2d.Graphics)
	 */
	protected void paintFigure(Graphics graphics) {
		//just paint a line
		if (isFilled()) {
			int oldAlpha = graphics.getAlpha();
			graphics.setAlpha(60);
			graphics.fillRectangle(getBounds());
			graphics.setAlpha(oldAlpha);
			
		}
		int line = graphics.getLineStyle();
		graphics.setLineStyle(SWT.LINE_DASH);
		Rectangle bounds = getBounds();
		graphics.setLineWidth(2);
		graphics.drawLine(bounds.x + bounds.width/2, bounds.y, bounds.x+bounds.width/2, bounds.y+bounds.height);
		graphics.setLineStyle(line);
	}
	
	/**
	 * @return
	 */
	private boolean isFilled() {
		return filled;
	}
	
	public void setFilled(boolean filled) {
		if (this.filled != filled) {
			this.filled = filled;
			invalidate();
			repaint();
		}
	}

	/**
	 * Layouts cannot be set on this figure.
	 */
	public void setLayoutManager(LayoutManager manager) {
		return;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.IFigure#getClientArea(org.eclipse.draw2d.geometry.Rectangle)
	 */
	public Rectangle getClientArea(Rectangle rect) {
		Rectangle bounds = getBounds().getCopy();
		Iterator<?> i = getChildren().iterator();
		while(i.hasNext()) {
			IFigure child = (IFigure) i.next();
			bounds.union(child.getClientArea());
		}
		rect.setBounds(bounds);
		if (useLocalCoordinates())
			rect.setLocation(0, 0);
		return rect;
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#containsPoint(int, int)
	 */
	public boolean containsPoint(int x, int y) {
		Iterator<?> i = getChildren().iterator();
		while (i.hasNext()) {
			IFigure child = (IFigure) i.next();
			if (child.containsPoint(x, y))
				return true;
		}
		return false;
	}

}
