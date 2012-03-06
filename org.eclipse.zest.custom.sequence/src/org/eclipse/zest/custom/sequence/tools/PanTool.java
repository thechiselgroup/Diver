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
import java.util.HashMap;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;

/**
 * A tool that supports panning in a viewport.
 * @author Del Myers
 */

public class PanTool extends AbstractWidgetTool {

	private Viewport viewport;
	private Point initialPoint;
	
	private Listener disposeListener = new Listener() {
		public void handleEvent(Event event) {
			if (event.type != SWT.Dispose) return;
 			synchronized (cursorMap) {
				Cursor[] cursors = cursorMap.get(event.display);
				if (cursors != null) {
					if (!cursors[0].isDisposed()) {
						cursors[0].dispose();
					}
					if (!cursors[1].isDisposed()) {
						cursors[1].dispose();
					}
					cursorMap.remove(event.display);
				}
			}
		}
	};
	
	private static final HashMap<Display, Cursor[]> cursorMap =
		new HashMap<Display, Cursor[]>();

	public void handleMouseDoubleClicked(MouseEvent event, Widget widget,
			IFigure cursorTarget) {
		if (event.button == 1) {
			viewport = getViewport(cursorTarget);
		}
	}

	public void handleMouseHover(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
	}

	public void handleMouseMoved(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		if ((me.stateMask & SWT.BUTTON1) == 0) {
			viewport = null;
		} else if (viewport == null) {
			viewport = getViewport(cursorTarget);
			if (viewport != null) {
				this.initialPoint = new Point(me.x, me.y);
				viewport.translateToRelative(initialPoint);
			}
		}
		if (viewport != null && initialPoint != null) {
			Point currentPoint = new Point(me.x, me.y);
			viewport.translateToRelative(currentPoint);
			Point vp = viewport.getViewLocation();
			viewport.setViewLocation(
				vp.x + (initialPoint.x - currentPoint.x), 
				vp.y + (initialPoint.y - currentPoint.y)
			);
			initialPoint = currentPoint;
		}
	}

	public void handleMousePressed(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		if (me.button != 1) return;
		if (me.widget instanceof Composite) {
			((Composite)me.widget).setCursor(getCursors(me.display)[1]);
		}
		this.viewport = getViewport(cursorTarget);
		if (viewport != null) {
			this.initialPoint = new Point(me.x, me.y);
			viewport.translateToRelative(initialPoint);
		}
	}

	public void handleMouseReleased(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		if (me.button == 1) {
			if (me.widget instanceof Composite) {
				((Composite)me.widget).setCursor(getCursors(me.display)[0]);
			}
			viewport = null;
		}
	}

	public void handleMouseWheelScrolled(Event event, IFigure mouseTarget) {
	}

	public boolean understandsEvent(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		return getViewport(cursorTarget) != null;
	}
	
	/**
	 * @return the nearest viewport for the given figure or null if the
	 * figure isn't a viewport or a child of a viewport.
	 */
	private Viewport getViewport(IFigure figure) {
		while (figure != null) {
			if (figure instanceof Viewport) return (Viewport)figure;
			figure = figure.getParent();
		}
		return null;
	}
	
		
	public Cursor getCursor(Display display) {
		Cursor[] cursors = getCursors(display);
		if (viewport == null) {
			return cursors[0];
		}
		return cursors[1];                           
	}

	/**
	 * @param display
	 * @return
	 * @throws IOException 
	 */
	private Cursor[] getCursors(Display display)  {
		Cursor[] cursors;
		synchronized (cursorMap) {
			cursors = cursorMap.get(display);
			if (cursors == null) {
				try {
					cursors = new Cursor[2];
					InputStream stream = getClass().getClassLoader().getResourceAsStream("cursors/handopenscreen.png");
					ImageData source = new ImageData(stream);
					stream.close();
					stream = getClass().getClassLoader().getResourceAsStream("cursors/handopenmask.png");
					ImageData mask = new ImageData(stream);
					stream.close();
					cursors[0] =  new Cursor(display, source, mask, source.width/2, source.height/2);
					stream = getClass().getClassLoader().getResourceAsStream("cursors/handclosedscreen.png");
					source = new ImageData(stream);
					stream.close();
					stream = getClass().getClassLoader().getResourceAsStream("cursors/handclosedmask.png");
					mask = new ImageData(stream);
					stream.close();
					cursors[1] =  new Cursor(display, source, mask, source.width/2, source.height/2);
					display.addListener(SWT.Dispose, disposeListener);
					cursorMap.put(display, cursors);
				} catch (IOException e) {
					cursors[0] = super.getCursor(display);
					cursors[1] = super.getCursor(display);
				}
			}
		}
		return cursors;
	}
		
}
