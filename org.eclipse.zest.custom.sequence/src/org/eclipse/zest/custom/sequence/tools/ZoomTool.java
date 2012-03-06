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

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.zest.custom.sequence.figures.RectangleZoomManager;
import org.eclipse.zest.custom.sequence.figures.internal.ZoomManager;

/**
 * Tool for zooming in and out on a Zest viewer. The viewer is required to follow the
 * following contract in order for this tool to understand mouse events: 
 * viewer.getData("ZoomManager") returns an instanceof ZoomManager. The returned
 * zoom manager will be used to perform the zooming.
 * 
 * Zooming is done in the following manner: on a left-mouse click, the viewport is
 * zoomed over the cursor position. on a right-mouse click, or a MOD1+click, the
 * viewport is zoomed out over the cursor position.
 * @author Del Myers
 */

public class ZoomTool implements IWidgetTool {
	Cursor cursor;
	private Listener cursorDisposer = new Listener() {
		public void handleEvent(Event event) {
			synchronized (ZoomTool.this) {
				if (cursor != null && !cursor.isDisposed()) {
					cursor.dispose();
					cursor = null;
				}
			}
		}
	};
	private Control currentControl;

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#getCursor(org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public Cursor getCursor(Widget widget, IFigure cursorTarget) {
		Widget viewer = getCurrentControl();
		if ((viewer instanceof Control)) {
			ZoomManager manager = getZoomManager((Control)viewer);
			if (manager instanceof RectangleZoomManager) {
				//check to see if the target is a child of the scalable figure.
				IFigure figure = cursorTarget;
				while (figure != null) {
					if (figure == manager.getViewport()) {
						return getCursor(viewer);
					}
					figure =figure.getParent();
				}
			}
		}
		if (cursorTarget != null) {
			Cursor c = cursorTarget.getCursor();
			if (c != null) {
				return c;
			}
		}
		return getDefaultCursor();
	}

	/**
	 * Loads a cursor for the given display.
	 * @param display
	 * @return
	 */
	private synchronized Cursor getCursor(Widget viewer) {
		if (cursor == null || !cursor.getDevice().equals(viewer.getDisplay())) {
			if (cursor != null) {
				((Display)cursor.getDevice()).removeListener(SWT.Dispose, cursorDisposer);
				cursor.dispose();
			}
			try {
				cursor = loadCursor(viewer.getDisplay());
				viewer.addListener(SWT.Dispose, cursorDisposer);
			} catch (IOException e) {
				return viewer.getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
			}
		}
		return cursor;
	}
	
	protected Cursor loadCursor(Display display) throws IOException {
		InputStream stream = getClass().getClassLoader().getResourceAsStream("cursors/magnify.png");
		ImageData source = new ImageData(stream);
		stream.close();
		stream = getClass().getClassLoader().getResourceAsStream("cursors/magnifymask.png");
		ImageData mask = new ImageData(stream);
		stream.close();
		return new Cursor(display, source, mask, source.width/2, source.height/2);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#getDefaultCursor()
	 */
	public Cursor getDefaultCursor() {
		if (Display.getCurrent() != null) {
			return Display.getCurrent().getSystemCursor(SWT.CURSOR_ARROW);
		} else {
			return Display.getDefault().getSystemCursor(SWT.CURSOR_ARROW);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#handleMouseDoubleClicked(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public void handleMouseDoubleClicked(MouseEvent me, Widget widget,
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
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#handleMousePressed(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public void handleMousePressed(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#handleMouseReleased(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public void handleMouseReleased(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		Widget parent = me.widget;
		if (!(parent instanceof Control)) return;
		ZoomManager manager = getZoomManager((Control) parent);
		if (manager != null) {
			setViewLocation(manager, new Point(me.x, me.y));
			if (me.button == 1) {
				if ((me.stateMask & SWT.MOD1) != 0) {
					manager.zoomOut();
				} else {
					manager.zoomIn();
				}
			} else if (me.button == 3) {
				manager.zoomOut();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#handleMouseWheelScrolled(org.eclipse.swt.widgets.Event, org.eclipse.draw2d.IFigure)
	 */
	public void handleMouseWheelScrolled(Event event, IFigure mouseTarget) {
		if (!(event.widget instanceof Control)) {
			return;
		}
		ZoomManager manager = getZoomManager((Control) event.widget);
		if (manager != null) {
			setViewLocation(manager, new Point(event.x, event.y));
			if (event.count > 0) manager.zoomIn();
			else if (event.count < 0) manager.zoomOut();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#understandsEvent(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public boolean understandsEvent(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		if (!(me.widget instanceof Control)) {
			return false;
		}
		Object manager = getZoomManager((Control) me.widget);

		if ((me.button == 1 || me.button == 3) && 
			manager instanceof ZoomManager) {
//			make sure that the cursor target is a child of the zoom manager's 
			//viewport
			ZoomManager zoomManager = (ZoomManager) manager;
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
	
	private void setViewLocation(ZoomManager zoomManager, Point p) {
		Viewport port = zoomManager.getViewport();
		port.getContents().translateToRelative(p);
		//center on the point
		Dimension d = port.getSize();
		port.setViewLocation(p.x - d.width/2, p.y - d.height/2);
	}
	
	protected ZoomManager getZoomManager(Widget viewer) {
		Object m = viewer.getData("ZoomManager");
		if (viewer instanceof Control) {
			viewer = ((Control)viewer).getParent();
		}
		while (viewer instanceof Control && m == null) {
			m = viewer.getData("ZoomManager");
			if (m instanceof ZoomManager) {
				return (ZoomManager) m;
			}
			viewer = ((Control)viewer).getParent();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.tools.IWidgetTool#getCurrentControl()
	 */
	public Control getCurrentControl() {
		return currentControl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.tools.IWidgetTool#setCurrentControl(org.eclipse.swt.widgets.Control)
	 */
	public void setCurrentControl(Control currentControl) {
		this.currentControl = currentControl;		
	}

}
