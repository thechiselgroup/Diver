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

import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;

/**
 * A figure that holds children in a sash-form way.
 * @author Del Myers
 */

public class SashFigure extends Figure {
	
	private boolean isHorizontal;
	private boolean drawBars;
	private SashBarMouseListener sashBarMouseListener;
	
	protected class SashBarFigure extends Figure {
		private boolean backArrows;
		private int index;
		private IFigure connectedFigure;

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Figure#paintClientArea(org.eclipse.draw2d.Graphics)
		 */
		@Override
		protected void paintClientArea(Graphics graphics) {
			if (!drawBars) return;
			double initialRotation = (isHorizontal) ? 0 : Math.PI/2;
			PolylineDecoration d = new PolylineDecoration();
			if (getConnectedFigure() != null) {
				backArrows = getConnectedFigure().getBounds().isEmpty();
			}
			int translate = 3;
			if (!backArrows) {
				initialRotation += Math.PI;
				translate = 0;
			}
			Rectangle bounds = getClientArea().getCopy();
			graphics.pushState();
			graphics.setBackgroundColor(Display.getCurrent().getSystemColor((SWT.COLOR_WIDGET_BACKGROUND)));
			graphics.fillRectangle(bounds);
			graphics.setForegroundColor(ColorConstants.black);
			d.setScale(3, 3);
			//d.setReferencePoint(d.getBounds().getCenter());
			d.setRotation(initialRotation);
			Point center = bounds.getCenter();
			if (isHorizontal) {
				d.setLocation(new Point(bounds.x + translate, center.y -8));
			} else {
				d.setLocation(new Point(center.x-8, bounds.y + translate));
			}
			
			PointList ps = d.getPoints();
			graphics.setLineWidth(2);
			graphics.drawPolyline(ps);
			if (isHorizontal) {
				ps.translate(0,8);
			} else {
				ps.translate(8, 0);
			}
			graphics.drawPolyline(ps);
			graphics.popState();
		}
		
		protected void setBackArrows(boolean back) {
			if (this.backArrows == back) return;
			this.backArrows = back;
			repaint();
		}
		
		protected void setIndex(int index) {
			this.index = index;
		}
		
		protected int getIndex() {
			return this.index;
		}
		
		protected void setConnectedFigure(IFigure figure) {
			this.connectedFigure = figure;
		}
		
		protected IFigure getConnectedFigure() {
			return connectedFigure;
		}
	} 
	IFigure mouseSource = null;
	Point lastPoint = null;
	
	private class SashBarMouseListener implements MouseListener, MouseMotionListener {
		
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.MouseListener#mouseDoubleClicked(org.eclipse.draw2d.MouseEvent)
		 */
		public void mouseDoubleClicked(MouseEvent me) {
			if (me.getSource() instanceof SashBarFigure && me.button == 1) {
				toggle(((SashBarFigure)me.getSource()).getConnectedFigure());
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.MouseListener#mousePressed(org.eclipse.draw2d.MouseEvent)
		 */
		public void mousePressed(MouseEvent me) {
			if (me.button == 1) {
				mouseSource = (IFigure) me.getSource();
				lastPoint = me.getLocation().getCopy();
				mouseSource.translateToAbsolute(lastPoint);
				me.consume();
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.MouseListener#mouseReleased(org.eclipse.draw2d.MouseEvent)
		 */
		public void mouseReleased(MouseEvent me) {
			if (me.button == 1) {
				mouseSource = null;
				lastPoint = null;
				me.consume();
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.MouseMotionListener#mouseDragged(org.eclipse.draw2d.MouseEvent)
		 */
		public void mouseDragged(MouseEvent me) {
			if (mouseSource instanceof SashBarFigure) {
				SashBarFigure bar = (SashBarFigure) mouseSource;
				int diff = 0;
				if (isHorizontal) {
					diff = me.getLocation().x - lastPoint.x;
				} else {
					diff = me.getLocation().y - lastPoint.y;
				}
				lastPoint = me.getLocation().getCopy();
				bar.translateToAbsolute(lastPoint);
				getSashLayout().moveSashBar(SashFigure.this, bar.getIndex(), diff);
				me.consume();
				return;
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.MouseMotionListener#mouseEntered(org.eclipse.draw2d.MouseEvent)
		 */
		public void mouseEntered(MouseEvent me) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.MouseMotionListener#mouseExited(org.eclipse.draw2d.MouseEvent)
		 */
		public void mouseExited(MouseEvent me) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.MouseMotionListener#mouseHover(org.eclipse.draw2d.MouseEvent)
		 */
		public void mouseHover(MouseEvent me) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.MouseMotionListener#mouseMoved(org.eclipse.draw2d.MouseEvent)
		 */
		public void mouseMoved(MouseEvent me) {
			if (mouseSource != null && (me.getState() & SWT.BUTTON1) == 0) {
				mouseSource = null;
				lastPoint = null;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#handleMouseDragged(org.eclipse.draw2d.MouseEvent)
	 */
	@Override
	public void handleMouseDragged(MouseEvent me) {
		
		super.handleMouseDragged(me);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#handleMouseMoved(org.eclipse.draw2d.MouseEvent)
	 */
//	@Override
//	public void handleMouseMoved(MouseEvent event) {
//		 if ((event.getState() & SWT.BUTTON1) == 0) {
//			mouseSource = null;
//			lastPoint = null;
//			return;
//		} else if (mouseSource instanceof SashBarFigure) {
//			handleMouseDragged(event);
//			return;
//		}
//		super.handleMouseDragged(event);
//	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#handleMouseReleased(org.eclipse.draw2d.MouseEvent)
	 */
//	@Override
//	public void handleMouseReleased(MouseEvent event) {
//		if (event.button == 1 && mouseSource instanceof SashBarFigure) {
//			mouseSource = null;
//			lastPoint = null;
//			return;
//		}
//		super.handleMouseReleased(event);
//	}
	
		
	/**
	 * Creates a sash form with a horizontal layout.
	 * Same as SashFigure(true, true, false).
	 */
	public SashFigure() {
		this (true, false);
	}
	/**
	 * @param horizontal true for horizontal layout. False for vertical layout.
	 * @param drawBorder draw a border surrounding the sashes.
	 * @param drawBars draw sash bars.
	 */
	public SashFigure(boolean horizontal, boolean drawBars) {
		this.isHorizontal = horizontal;
		super.setLayoutManager(new SashLayout(horizontal));
		this.drawBars = true;
		this.sashBarMouseListener = new SashBarMouseListener();
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#add(org.eclipse.draw2d.IFigure, java.lang.Object, int)
	 */
	@Override
	public void add(IFigure figure, Object constraint, int index) {
		boolean addEnd = index == -1;
		if (addEnd) index = getChildren().size();
		boolean addBarBefore = (index > 0);
		IFigure connected = (index > 0) ? (IFigure)getChildren().get(index-1) : figure;
		if (connected instanceof SashBarFigure) {
			connected = figure;
			addBarBefore = false;
		}
		SashBarFigure bar = null;
		if (getChildren().size() != 0) {
			bar = new SashBarFigure();
			bar.setConnectedFigure(connected);
			bar.addMouseListener(sashBarMouseListener);
			bar.addMouseMotionListener(sashBarMouseListener);
			Cursor c = (isHorizontal) ?
					Display.getCurrent().getSystemCursor(SWT.CURSOR_SIZEW) :
					Display.getCurrent().getSystemCursor(SWT.CURSOR_SIZENS);
			bar.setCursor(c);
		}
		if (addBarBefore && bar != null) {
			super.add(bar, null, addEnd ? -1 : index);
			index++;
		}
		super.add(figure, constraint, addEnd ? -1 : index);
		index++;
		if (!addBarBefore && bar!= null) {
			super.add(bar, null, addEnd ? -1 : index);
		}
		reorderBars();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#remove(org.eclipse.draw2d.IFigure)
	 */
	@Override
	public void remove(IFigure figure) {
		if (figure instanceof SashBarFigure) return;
		int index = getChildren().indexOf(figure);
		if (index > 0) {
			//remove the sash
			IFigure sash = (IFigure) getChildren().get(index-1);
			super.remove(sash);
		}
		super.remove(figure);
		reorderBars();
	}
	
	
	private void reorderBars() {
		for (int i = 0; i < getChildren().size(); i++) {
			IFigure child = (IFigure) getChildren().get(i);
			if (child instanceof SashBarFigure) {
				((SashBarFigure)child).setIndex(i/2);
			}
		}
	}
	

	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#paintChildren(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void paintChildren(Graphics graphics) {
		super.paintChildren(graphics);
	}

	
	/**
	 * Layout managers are not allowed in sash figures.
	 */
	@Override
	public void setLayoutManager(LayoutManager manager) {
	}


	
	/**
	 * Toggles the child figure between the "open" (size maximized) and "closed"
	 * (size 0) state. 
	 */
	public void toggle(IFigure child) {
		List<?> children = getChildren();
		int i;
		for (i = 0; i < children.size(); i++) {
			IFigure next = (IFigure) children.get(i);
			if (next == child)
				break;
		}
		if (i >= children.size()) return;
		int size = getSashLayout().getSashBarSize(this, i);
		if (size > 0) {
			getSashLayout().moveSashBar(this, i, -size);
		} else {
			Dimension psize = child.getPreferredSize();
			int newSize = (isHorizontal) ? psize.width : psize.height;
			getSashLayout().moveSashBar(this, i, newSize);
		}
		
		
	}
	
	private SashLayout getSashLayout() {
		return ((SashLayout)getLayoutManager());
	}



	/**
	 * @param figure
	 */
	public void maximize(IFigure child) {
		List<?> children = getChildren();
		int i;
		for (i = 0; i < children.size(); i++) {
			IFigure next = (IFigure) children.get(i);
			if (next == child)
				break;
		}
		if (i >= children.size()) return;
		Dimension psize = child.getPreferredSize();
		int newSize = (isHorizontal) ? psize.width : psize.height;
		newSize -= getSashLayout().getSashBarSize(this, i/2);
		if (newSize > 0) {
			getSashLayout().moveSashBar(this, i/2, newSize);
		}
	}
	
	public void minimize (IFigure child) {
		List<?> children = getChildren();
		int i;
		for (i = 0; i < children.size(); i++) {
			IFigure next = (IFigure)children.get(i);
			if (next == child)
				break;
		}
		if (i >= children.size()) return;
		int size = getSashLayout().getSashBarSize(this, i/2);
		if (size > 0) {
			getSashLayout().moveSashBar(this, i/2, -size);
		}
	}

}
