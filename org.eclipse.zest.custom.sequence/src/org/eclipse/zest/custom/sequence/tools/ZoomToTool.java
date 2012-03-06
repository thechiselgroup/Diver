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
package org.eclipse.zest.custom.sequence.tools;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.zest.custom.sequence.figures.RectangleZoomManager;
import org.eclipse.zest.custom.sequence.figures.internal.ZoomManager;
import org.eclipse.zest.custom.sequence.visuals.LayerConstants;

/**
 * A tool that zooms in on a particular area of the view port and draws a rectangle
 * to display that area.
 * @author Del Myers
 */

public class ZoomToTool extends ZoomTool {

	private boolean dragging;
	private IFigure feedbackFigure;

	private RectangleZoomManager zoomManager;
	private IFigure feedbackParent;
	private Point startPoint;

	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.ZoomTool#loadCursor(org.eclipse.swt.widgets.Display)
	 */
	@Override
	protected Cursor loadCursor(Display display) throws IOException {
		InputStream stream = getClass().getClassLoader().getResourceAsStream("cursors/magnifyscreen.png");
		ImageData source = new ImageData(stream);
		stream.close();
		stream = getClass().getClassLoader().getResourceAsStream("cursors/magnifyrectmask.png");
		ImageData mask = new ImageData(stream);
		stream.close();
		return new Cursor(display, source, mask, source.width/2, source.height/2);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#handleMouseDoubleClicked(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public void handleMouseDoubleClicked(MouseEvent event, Widget widget,
			IFigure cursorTarget) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#handleMouseHover(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public void handleMouseHover(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#handleMouseMoved(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public void handleMouseMoved(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		if ((me.stateMask & SWT.BUTTON1) == 0) {
			//cancel the drawing.
			clear();
		} else if (dragging) {
			updateFeedback(new Point(me.x, me.y));
		}
	}
	
	/**
	 * Clears all data to stop or cancel the zooming.
	 *
	 */
	private void clear() {
		this.dragging = false;
		this.zoomManager = null;
		this.feedbackParent = null;
		this.startPoint = null;
		eraseFeedback();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#handleMousePressed(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public void handleMousePressed(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		if (me.button != 1) return;
		this.dragging = true;
		ZoomManager m = getZoomManager(me.widget);
		if (!(m instanceof RectangleZoomManager)) {
			return;
		}
		this.zoomManager = (RectangleZoomManager) m;
		IFigure contentFigure = zoomManager.getScalableFigure();
		if (contentFigure instanceof LayeredPane) {
			this.feedbackParent = ((LayeredPane)contentFigure).getLayer(LayerConstants.FEEDBACK_LAYER);
		}
		if (this.feedbackParent == null) {
			contentFigure = zoomManager.getViewport().getContents();
			if (contentFigure instanceof LayeredPane) {
				this.feedbackParent = ((LayeredPane)contentFigure).getLayer(LayerConstants.FEEDBACK_LAYER);
			}
		}
		if (this.feedbackParent == null) {
			//finally, just use the content figure
			this.feedbackParent = zoomManager.getViewport().getContents();
		}
		Point p = new Point(me.x, me.y);
		this.startPoint = p;
		showFeedback();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#handleMouseReleased(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public void handleMouseReleased(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		if (dragging) {
			if (me.button == 1) {
				if (zoomManager != null) {
					if (getFeedbackFigure().getBounds().isEmpty()) {
						zoomManager.setZoom(1.0);
					} else {
						zoomManager.zoomTo(getFeedbackFigure().getBounds());
					}
				}
				clear();
			}
		}
	}


	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#understandsEvent(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public boolean understandsEvent(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		Object manager = getZoomManager(me.widget);
		if ((manager instanceof RectangleZoomManager)) {
			//make sure that the cursor target is a child of the zoom manager's 
			//viewport
			this.zoomManager = (RectangleZoomManager) manager;
			Viewport port = zoomManager.getViewport();
			IFigure figure = cursorTarget;
			while (figure != null) {
				if (figure == port)
					return true;
				figure = figure.getParent();
			}
		}
		return false;
	}

	
	
	private void showFeedback() {
		IFigure feedback = getFeedbackFigure();
		if (feedbackParent != null && feedback.getParent() != feedbackParent) {
			feedbackParent.add(feedback);
			Point p = startPoint.getCopy();
			feedback.translateToRelative(p);
			feedback.setLocation(p);
			feedback.setSize(new Dimension(0,0));
		}
	}
	
	private void updateFeedback(Point p) {
		int x1, x2, y1, y2;
		x1 = x2 = y1 = y2 = 0;
		if (startPoint.x < p.x) {
			x1 = startPoint.x;
			x2 = p.x;
		} else {
			x1 = p.x;
			x2 = startPoint.x;
		}
		if (startPoint.y < p.y) {
			y1 = startPoint.y;
			y2 = p.y;
		} else {
			y1 = p.y;
			y2 = startPoint.y;
		}
		Rectangle newBounds = new Rectangle(x1, y1, x2-x1, y2-y1);
		getFeedbackFigure().translateToRelative(newBounds);
		getFeedbackFigure().setBounds(newBounds);
	}
	
	private void eraseFeedback() {
		if (getFeedbackFigure().getParent() != null) {
			getFeedbackFigure().getParent().remove(getFeedbackFigure());
		}
		this.feedbackParent = null;
	}
	
	private IFigure getFeedbackFigure() {
		if (feedbackFigure == null) {
			feedbackFigure = new RectangleFigure();
			feedbackFigure.setForegroundColor(ColorConstants.blue);
			((RectangleFigure)feedbackFigure).setFill(false);
		}
		return feedbackFigure;
	}
	
}
