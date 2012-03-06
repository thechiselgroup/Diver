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

import org.eclipse.draw2d.ScalableFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.zest.custom.sequence.figures.internal.ZoomManager;



/**
 * A zoom manager that has support for the zoomTo method.
 * @author Del Myers
 *
 */
public class RectangleZoomManager extends ZoomManager {

	/**
	 * @param pane
	 * @param viewport
	 */
	public RectangleZoomManager(ScalableFigure pane, Viewport viewport) {
		super(pane, viewport);
	}
	
	/**
	 * Takes a rectangle in the viewport coordinates, that is not scaled according
	 * to the current viewport scale. Scales the viewport to which ever axis of the
	 * rectangle holds the most information.
	 */
	public void zoomTo(Rectangle rect) {
		//figure out the scale.
		Rectangle vbounds = getViewport().getBounds().getCopy();
		Point center = rect.getCenter();
		Rectangle copy = rect.getCopy();
		
		double scale = 1;
		if (rect.isEmpty()) {
			//do nothing
		} else if (rect.width < rect.height) {
			scale = ((double)vbounds.height)/copy.height;
			copy.scale(scale,1);
		} else {
			scale = ((double)vbounds.width)/copy.width;
			copy.scale(1,scale);
		}
		center.scale(scale);
		primSetZoom(scale);
		Rectangle clientArea = getViewport().getClientArea();
		setViewLocation(new Point(center.x - clientArea.width/2, center.y - clientArea.height/2));
	}

	

}
