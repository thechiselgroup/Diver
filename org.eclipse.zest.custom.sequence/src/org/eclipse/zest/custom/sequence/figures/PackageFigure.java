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
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Figure representing a package.
 * @author Del Myers
 */

public class PackageFigure extends Shape {
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Shape#fillShape(org.eclipse.draw2d.Graphics)
	 */
	protected void fillShape(Graphics graphics) {
		//draw two rectangles in the bounds.
		graphics.fillRectangle(getTabBounds());
		graphics.fillRectangle(getFolderBounds());
		outlineShape(graphics);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Shape#outlineShape(org.eclipse.draw2d.Graphics)
	 */
	protected void outlineShape(Graphics graphics) {
		Rectangle folderBounds = getFolderBounds();
		folderBounds = new Rectangle(folderBounds.x, folderBounds.y, folderBounds.width-1, folderBounds.height-1);
		Rectangle tabBounds = getTabBounds();
		tabBounds = new Rectangle(tabBounds.x, tabBounds.y, tabBounds.width, tabBounds.height);
		graphics.drawRectangle(tabBounds);
		graphics.drawRectangle(folderBounds);

	}
	
	public Rectangle getFolderBounds() {
		//the folder is 3/4ths of the height
		Rectangle bounds = getBounds();
		
		Rectangle fBounds = new Rectangle(bounds.x, bounds.y + bounds.height/4, bounds.width, (3*bounds.height)/4);
		
		return fBounds;
	}
	
	public Rectangle getTabBounds() {
		Rectangle bounds = getBounds();
		return new Rectangle(bounds.x, bounds.y, bounds.width/4, bounds.height/4);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#getClientArea(org.eclipse.draw2d.geometry.Rectangle)
	 */
	public Rectangle getClientArea(Rectangle rect) {
		Rectangle fBounds = getFolderBounds();
		rect.setBounds(fBounds);
		return rect;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#setLayoutManager(org.eclipse.draw2d.LayoutManager)
	 */
	public void setLayoutManager(LayoutManager manager) {
		super.setLayoutManager(manager);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#getPreferredSize(int, int)
	 */
	@Override
	public Dimension getPreferredSize(int wHint, int hHint) {

		return super.getPreferredSize(wHint, hHint);
	}

}
