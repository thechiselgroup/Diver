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

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

/**
 * 
 * A thumbnail that overrides the paint method in order to speed up drawing. If the
 * scale is too small for any useful viewing.
 * 
 * @author Del Myers
 *
 */
public class FastThumbnail extends Thumbnail {
	private final float MIN_SCALE  = .01f;
	
	@Override
	protected void paintFigure(Graphics graphics) {
		Dimension targetSize = getPreferredSize();
		targetSize.expand(new Dimension(getInsets().getWidth(), 
										getInsets().getHeight()).negate());
		setScales(targetSize.width / (float)getSourceRectangle().width,
			     targetSize.height / (float)getSourceRectangle().height);
		
		float scaleX = getScaleX();
		float scaleY = getScaleY();
		if (scaleX < MIN_SCALE || scaleY < MIN_SCALE) {
			//just draw a rectangle.
			graphics.setBackgroundColor(ColorConstants.lightGray);
			Point p = getLocation();
			int width = (int)(getSourceRectangle().width*scaleX);
			int height = (int)(getSourceRectangle().height*scaleY);
			graphics.fillRectangle(p.x, p.y, width, height);
			graphics.setForegroundColor(ColorConstants.darkBlue);
			graphics.drawRectangle(p.x, p.y, width, height);
		} else {
			super.paintFigure(graphics);
		}
	}

}
