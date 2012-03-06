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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;

/**
 * Tool that handles events for widgets. Tools are very simple methods of interaction
 * with visualizations. 
 * @author Del Myers
 */

public interface IWidgetTool {

	/**
	 * @param me
	 * @param widgetUnderCursor
	 * @param cursorTarget
	 * @return
	 */
	boolean understandsEvent(MouseEvent me, Widget widget, IFigure cursorTarget);

	/**
	 * @param event
	 * @param widgetUnderCursor
	 * @param cursorTarget
	 */
	void handleMouseDoubleClicked(MouseEvent event, Widget widget, IFigure cursorTarget);

	/**
	 * @param me
	 * @param widgetUnderCursor
	 * @param cursorTarget
	 */
	void handleMouseHover(MouseEvent me, Widget widget, IFigure cursorTarget);

	/**
	 * @param me
	 * @param widgetUnderCursor
	 * @param cursorTarget
	 */
	void handleMouseMoved(MouseEvent me, Widget widget, IFigure cursorTarget);

	/**
	 * @param me
	 * @param widgetUnderCursor
	 * @param cursorTarget
	 */
	void handleMousePressed(MouseEvent me, Widget widget, IFigure cursorTarget);

	/**
	 * @param me
	 * @param widgetUnderCursor
	 * @param cursorTarget
	 */
	void handleMouseReleased(MouseEvent me, Widget widget, IFigure cursorTarget);

	/**
	 * @param event
	 */
	void handleMouseWheelScrolled(Event event, IFigure mouseTarget);
	
	/**
	 * Returns the cursor that should be used for this tool. The result must not be null.
	 * @param viewer the viewer on which to get the cursor.
	 * @param widget the widget in the viewer which the cursor is over
	 * @param cursorTarget the figure which the cursor is over.
	 * @return the default cursor used for this tool.
	 */
	public Cursor getCursor(Widget widget, IFigure cursorTarget);
	
	/**
	 * Returns the default cursor used for this tool. The result must not be null.
	 * @return the default cursor used for this tool.
	 */
	public Cursor getDefaultCursor();
	
	/**
	 * Sets the control (viewer) that this tool is working on.
	 */
	public void setCurrentControl(Control currentControl);
	
	/**
	 * Returns the control that this tool is working on.
	 * @return
	 */
	public Control getCurrentControl();

}
