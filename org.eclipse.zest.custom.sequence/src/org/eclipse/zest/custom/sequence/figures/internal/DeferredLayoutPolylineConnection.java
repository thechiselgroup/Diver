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
package org.eclipse.zest.custom.sequence.figures.internal;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.SWT;

/**
 * A polyline connection that allows the user to set whether or not the line should be layed out
 * by toggling a "dirty" state.
 * @author Del Myers
 *
 */
public class DeferredLayoutPolylineConnection extends PolylineConnection {
	
	private boolean dirty = true;
	
	/**
	 * Sets the dirty state. If the line is not dirty, it will not layout.
	 * @param dirty
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	@Override
	public void layout() {
		if (!dirty) return;
		super.layout();
		dirty = false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Polyline#outlineShape(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void outlineShape(Graphics g) {
		if (g.getAntialias() == SWT.ON) {
			//use custom dots-dashes for antialiasing, otherwise we can't see them.
			g.pushState();
			switch (g.getLineStyle()) {
			case SWT.LINE_DOT:
				g.setLineDash(new int[] {g.getLineWidth(), g.getLineWidth()*4});
				break;
			case SWT.LINE_DASH:
				g.setLineDash(new int[] {g.getLineWidth()*4, g.getLineWidth()*4});
				break;
			case SWT.LINE_DASHDOT:
				g.setLineDash(new int[] {g.getLineWidth()*4, g.getLineWidth()*4, g.getLineWidth(), g.getLineWidth()*4});
				break;
			case SWT.LINE_DASHDOTDOT:
				g.setLineDash(new int[] {g.getLineWidth()*4, g.getLineWidth()*4, g.getLineWidth(), g.getLineWidth()*4, g.getLineWidth(), g.getLineWidth()*4});
				break;
			}
			g.drawPolyline(getPoints());
			g.popState();
		} else {
			super.outlineShape(g);
		}
	}
	
	/**
	 * @return the dirty
	 */
	public boolean isDirty() {
		return dirty;
	}

}
