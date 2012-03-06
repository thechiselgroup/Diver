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

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTEventDispatcher;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;

/**
 * A dispatcher that dispatches events to tools that understand widgets. ToolEventDispatchers
 * can be set onto the LightweightSystem of a draw2d-based visualization. An
 * IWidgetFinder is provided in order to translate between figures in the
 * visualization and widgets inside the visualization.  Once a ToolEventDispatcher
 * is set on the draw2d based visualization, the viewer should provide a method of
 * changing the tool that is used within it (provided that it supports more than one
 * tool). Two tools (ZoomTool and ZoomToTool) are provided which will work on any draw2d based viewer, just so
 * long as it conforms to contracts outlined in the documentation of those views.
 * 
 * Currently, tools only work with mouse events that occur within the lightweight system.
 * Mouse events are handled in the following fashion:
 * 1) The figure that is under the mouse is found, and the mouse event is dispatched
 *  to it first. This ensures that any actions that must be performed regardless of
 *  the tool get performed (e.g. dragging scroll bars).
 * 2) The provided IWidgetFinder is used to find the widget that is under the mouse.
 * 3) The current tool is queried to find out if it understands the mouse event given
 *  the context of the event (the viewer that it occurred on, the widget that it
 *  occurred on (may be null), and the IFigure that it occurred on).
 * 4) If the tool understands the event, the tool is asked to perform an operation
 *  based on the event.
 * @author Del Myers
 */

public class ToolEventDispatcher extends SWTEventDispatcher {
	
	private IWidgetFinder widgetFinder;
	private Widget widgetUnderCursor;
	private IWidgetTool currentTool;
	private Cursor currentCursor;
	private boolean wasCaptured = false;
	/**
	 * The control on which the event dispatcher is running.
	 */
	private Composite control;
	/**
	 * Constructs an event dispatcher that uses the given widget finder to 
	 * dispatch to based on widgets.
	 * @param finder
	 */
	public ToolEventDispatcher(IWidgetFinder finder, Composite control) {
		this.widgetFinder = finder;
		this.control = control;
	}
	
	public void setTool(IWidgetTool tool) {
		this.currentTool = tool;
		this.currentCursor = tool.getDefaultCursor();
		tool.setCurrentControl(getControl());
	}

	public Composite getControl() {
		return control;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.EventDispatcher#dispatchMouseDoubleClicked(org.eclipse.swt.events.MouseEvent)
	 */
	@Override
	public void dispatchMouseDoubleClicked(MouseEvent me) {
		super.dispatchMouseDoubleClicked(me);
		recieve(me);
		if (getCurrentEvent() != null && getCurrentEvent().isConsumed()) {
			if (currentTool != null && currentTool.understandsEvent(me, widgetUnderCursor, getCursorTarget())) {
				currentTool.handleMouseDoubleClicked(me, widgetUnderCursor, getCursorTarget());
			}
		}
		
		
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.EventDispatcher#dispatchMouseHover(org.eclipse.swt.events.MouseEvent)
	 */
	@Override
	public void dispatchMouseHover(MouseEvent me) {
		super.dispatchMouseHover(me);
		recieve(me);
		if (getCurrentEvent() != null && getCurrentEvent().isConsumed()) {
		if (currentTool != null && currentTool.understandsEvent(me, widgetUnderCursor, getCursorTarget())) {
			currentTool.handleMouseHover(me, widgetUnderCursor, getCursorTarget());
		}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.EventDispatcher#dispatchMouseMoved(org.eclipse.swt.events.MouseEvent)
	 */
	@Override
	public void dispatchMouseMoved(MouseEvent me) {
		super.dispatchMouseMoved(me);
		recieve(me);
		if (!isCaptured()) {
		if (currentTool != null && currentTool.understandsEvent(me, widgetUnderCursor, getCursorTarget())) {
			currentTool.handleMouseMoved(me, widgetUnderCursor, getCursorTarget());
		}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.EventDispatcher#dispatchMousePressed(org.eclipse.swt.events.MouseEvent)
	 */
	@Override
	public void dispatchMousePressed(MouseEvent me) {
		super.dispatchMousePressed(me);
		recieve(me);
		wasCaptured = isCaptured();
		if (!isCaptured()) {
			if (currentTool != null && currentTool.understandsEvent(me, widgetUnderCursor, getCursorTarget())) {
				currentTool.handleMousePressed(me, widgetUnderCursor, getCursorTarget());
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.EventDispatcher#dispatchMouseReleased(org.eclipse.swt.events.MouseEvent)
	 */
	@Override
	public void dispatchMouseReleased(MouseEvent me) {
		super.dispatchMouseReleased(me);
		recieve(me);
		if (!wasCaptured) {
			if (currentTool != null && currentTool.understandsEvent(me, widgetUnderCursor, getCursorTarget())) {
				currentTool.handleMouseReleased(me, widgetUnderCursor, getCursorTarget());
			}
		}
		wasCaptured = isCaptured();
	}

	/**
	 * Updates the widget under the mouse event.
	 * @param me
	 */
	private void recieve(MouseEvent me) {
		IFigure cursorTarget = getCursorTarget();
		this.widgetUnderCursor = widgetFinder.getWidget(cursorTarget);
		if (cursorTarget != null && cursorTarget.getCursor() == null) {
			this.currentCursor = currentTool.getCursor(widgetUnderCursor != null ? widgetUnderCursor : me.widget, cursorTarget);
			if (me.widget instanceof Composite) {
				((Composite)me.widget).setCursor(this.currentCursor);
			}
		}
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.SWTEventDispatcher#setFocus(org.eclipse.draw2d.IFigure)
	 */
	@Override
	protected void setFocus(IFigure fig) {
		super.setFocus(fig);
		Widget newWidget = widgetFinder.getWidget(fig);
		if (newWidget instanceof Composite) {
			((Composite)newWidget).setFocus();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.EventDispatcher#dispatchMouseWheelScrolled(org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void dispatchMouseWheelScrolled(Event event) {
		if (currentTool != null) {
			currentTool.handleMouseWheelScrolled(event, getCursorTarget());
		}
		super.dispatchMouseWheelScrolled(event);
	}

	/**
	 * @return the current tool applied on the dispatcher.
	 */
	public IWidgetTool getTool() {
		return currentTool;
	}
	

}
