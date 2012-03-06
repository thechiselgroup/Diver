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
package org.eclipse.zest.custom.sequence.visuals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * @author Del Myers
 *
 */
final class CenterFlowLayout extends FlowLayout {
	/**
	 * @param isHorizontal
	 */
	CenterFlowLayout(boolean isHorizontal) {
		super(isHorizontal);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.FlowLayout#layout(org.eclipse.draw2d.IFigure)
	 */
	@Override
	public void layout(IFigure parent) {
		//the regular flow layout doesn't take into account the 
		//condition in which the parent is not a coordinate system.
		super.layout(parent);
		
		Rectangle clientArea = parent.getClientArea();
		List<IFigure> row = new ArrayList<IFigure>();
		int y = -1;
		int startX = -1;
		for (Object o : parent.getChildren()) {
			IFigure f = (IFigure) o;
			Rectangle b = f.getBounds().getCopy();
			if (!parent.isCoordinateSystem()) {
				b.x += clientArea.x;
				b.y += clientArea.y;
			}
			f.setBounds(b);
			if (startX == -1) {
				startX = b.x;
				y = b.y;
			}
			if (y != b.y) {
				//clear the row.
				int endX = b.x + b.width;
				int nudge = (clientArea.width - (endX-startX))/2;
				if (nudge > 0) {
					while (row.size() > 0) {
						IFigure fig = row.remove(0);
						Rectangle bounds = fig.getBounds().getCopy();
						bounds.x += nudge;
						fig.setBounds(bounds);
					}
				}
				startX = -1;
			} else {
				row.add(f);
			}
		}
		if (startX != -1) {
			if (row.size() > 0) {
				Rectangle lastBounds = row.get(row.size()-1).getBounds();
				int endX = lastBounds.x + lastBounds.width;
				int nudge = (clientArea.width - (endX-startX))/2;
				if (nudge > 0) {
					while (row.size() > 0) {
						IFigure fig = row.remove(0);
						Rectangle bounds = fig.getBounds().getCopy();
						bounds.x += nudge;
						fig.setBounds(bounds);
					}
				}
			}
		}
	}
}