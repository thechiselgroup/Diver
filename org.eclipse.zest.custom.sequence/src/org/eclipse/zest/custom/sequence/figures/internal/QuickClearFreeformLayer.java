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

import java.util.Iterator;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformFigure;
import org.eclipse.draw2d.FreeformListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.TreeSearch;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * @author Del Myers
 *
 */
public class QuickClearFreeformLayer extends QuickClearingFigure implements FreeformFigure {
	private FreeformHelper helper = new FreeformHelper(this);

	
	/**
	 * @see FreeformFigure#addFreeformListener(FreeformListener)
	 */
	public void addFreeformListener(FreeformListener listener) {
		addListener(FreeformListener.class, listener);
	}

	/**
	 * @see FreeformFigure#fireExtentChanged()
	 */
	public void fireExtentChanged() {
		Iterator<?> iter = getListeners(FreeformListener.class);
		while (iter.hasNext())
			((FreeformListener)iter.next())
				.notifyFreeformExtentChanged();
	}

	/**
	 * Overrides to do nothing.
	 * @see Figure#fireMoved()
	 */
	protected void fireMoved() { }

	/**
	 * @see FreeformFigure#getFreeformExtent()
	 */
	public Rectangle getFreeformExtent() {
		return helper.getFreeformExtent();
	}

	/**
	 * @see Figure#primTranslate(int, int)
	 */
	public void primTranslate(int dx, int dy) {
		bounds.x += dx;
		bounds.y += dy;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#add(org.eclipse.draw2d.IFigure, java.lang.Object, int)
	 */
	@Override
	public void add(IFigure figure, Object constraint, int index) {
		super.add(figure, constraint, index);
		helper.hookChild(figure);
	}
	
	/**
	 * @see IFigure#remove(IFigure)
	 */
	public void remove(IFigure child) {
		helper.unhookChild(child);
		super.remove(child);
	}

	/**
	 * @see FreeformFigure#removeFreeformListener(FreeformListener)
	 */
	public void removeFreeformListener(FreeformListener listener) {
		removeListener(FreeformListener.class, listener);
	}

	/**
	 * @see FreeformFigure#setFreeformBounds(Rectangle)
	 */
	public void setFreeformBounds(Rectangle bounds) {
		helper.setFreeformBounds(bounds);
	}
	
	/**
	 * Overridden to implement transparent behavior.
	 * @see IFigure#containsPoint(int, int)
	 * 
	 */
	public boolean containsPoint(int x, int y) {
		if (isOpaque())
			return super.containsPoint(x, y);
		Point pt = new Point(x, y);
		translateFromParent(pt);
		for (int i = 0; i < getChildren().size(); i++) {
			IFigure child = (IFigure)getChildren().get(i);
			if (child.containsPoint(pt.x, pt.y))
				return true;
		}
		return false;
	}

	/**
	 * Overridden to implement transparency.
	 * @see IFigure#findFigureAt(int, int, TreeSearch)
	 */
	public IFigure findFigureAt(int x, int y, TreeSearch search) {
		if (!isEnabled())
			return null;
		if (isOpaque())
			return super.findFigureAt(x, y, search);

		IFigure f = super.findFigureAt(x, y, search);
		if (f == this)
			return null;
		return f;
	}
	
}
