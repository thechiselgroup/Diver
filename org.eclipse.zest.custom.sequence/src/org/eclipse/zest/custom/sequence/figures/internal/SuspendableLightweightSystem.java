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

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.TreeSearch;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.graphics.GC;

/**
 * @author Del Myers
 *
 */
public class SuspendableLightweightSystem extends LightweightSystem {
	
	protected class SearchableRootFigure extends org.eclipse.draw2d.LightweightSystem.RootFigure {
		
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Figure#findFigureAt(int, int, org.eclipse.draw2d.TreeSearch)
		 */
		@Override
		public IFigure findFigureAt(int x, int y, TreeSearch search) {
			if (containsPoint(x, y)) {
				return super.findFigureAt(x, y, search);
			}
			if (search.prune(this))
				return null;
			List<?> children = getChildren();
			for (int i = children.size(); i > 0;) {
				i--;
				IFigure child = (IFigure)children.get(i);
				if (child.isVisible() && child instanceof Viewport) {
					Point copy = new Point(x, y);
					child.translateFromParent(copy);
					if (((Viewport)child).getContents().containsPoint(copy)) {
						IFigure fig = ((Viewport)child).getContents().findFigureAt(copy.x, copy.y, search);
						if (fig != null)
							return fig;
					}
				}
			}
			if (search.accept(this))
				return this;
			return null;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Figure#findMouseEventTargetAt(int, int)
		 */
		@Override
		public IFigure findMouseEventTargetAt(int x, int y) {
			if (containsPoint(x,y)) {
				return super.findMouseEventTargetAt(x, y);
			}
			List<?> children = getChildren();
			for (int i = children.size(); i > 0;) {
				i--;
				IFigure child = (IFigure)children.get(i);
				if (child.isVisible() && child instanceof Viewport) {
					//try and find it in the viewport
					Point copy = new Point(x, y);
					child.translateFromParent(copy);
					if (((Viewport)child).getContents().containsPoint(copy)) {
						IFigure fig =((Viewport)child).getContents().findMouseEventTargetAt(copy.x, copy.y);
						if (fig != null)
							return fig;
					}
				}
			}
			if (isMouseEventTarget())
				return this;
			return null;
		}
	}

	private boolean suspend;
	private GC lastGC;

	public SuspendableLightweightSystem() {
		this.suspend = false;
		this.lastGC = null;
	}
	
	@Override
	public void paint(GC gc) {
		if (suspend) {
			lastGC = gc;
			return;
		}
		super.paint(gc);
	}
	
	public void suspend() {
		this.suspend = true;
	}
	
	/**
	 * Resumes painting.
	 */
	public void resume() {
		this.suspend = false;
		if (lastGC != null) {
			paint(lastGC);
			lastGC = null;
		}
	}
	
	protected RootFigure createRootFigure() {
		RootFigure f = new SearchableRootFigure();
		f.addNotify();
		f.setOpaque(true);
		f.setLayoutManager(new StackLayout());
		return f;
	}
	
}
