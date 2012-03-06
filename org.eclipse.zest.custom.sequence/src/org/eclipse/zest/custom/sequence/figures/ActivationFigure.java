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

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * A simple rectangle that represents an activation box.
 * Layout managers cannot be set on this
 * @author Del Myers
 *
 */
public class ActivationFigure extends RoundedRectangle {
	

	public ActivationFigure() {
		setCornerDimensions(new Dimension(5,5));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.IFigure#getClientArea(org.eclipse.draw2d.geometry.Rectangle)
	 */
	public Rectangle getClientArea(Rectangle rect) {
		Rectangle bounds = getBounds().getCopy();
		Rectangle localBounds = getBounds().getCopy();
		Iterator<?> i = getChildren().iterator();
		while(i.hasNext()) {
			IFigure child = (IFigure) i.next();
			Rectangle childArea = child.getClientArea().getCopy();
			if (!useLocalCoordinates()) {
				childArea.x += localBounds.x;
				childArea.y += localBounds.y;
			}
			bounds.union(child.getClientArea());
		}
		rect.setBounds(bounds);
		if (useLocalCoordinates())
			rect.setLocation(0, 0);
		return rect;
	}
	
	@Override
	public void addNotify() {
		super.addNotify();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#useLocalCoordinates()
	 */
	@Override
	protected boolean useLocalCoordinates() {
		return true;
	}

}
