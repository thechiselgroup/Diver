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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

/**
 * Default tool that uses the system default cursor.
 * @author Del Myers
 */

public abstract class AbstractWidgetTool implements IWidgetTool {

	private Cursor cursor;
	private Control currentControl;
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#getCursor(org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public Cursor getCursor(Widget widget, IFigure cursorTarget) {
		if (widget == null) {
			return getDefaultCursor();
		}
		this.cursor = getCursor(widget.getDisplay());
		             return this.cursor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#getDefaultCursor()
	 */
	public Cursor getDefaultCursor() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		this.cursor = getCursor(display);
		return this.cursor;
	}
	
	public Cursor getCursor(Display display) {
		return display.getSystemCursor(SWT.CURSOR_ARROW);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.tools.IWidgetTool#setCurrentControl()
	 */
	public void setCurrentControl(Control currentControl) {
		this.currentControl = currentControl;
	}
	
	/**
	 * Returns the control or viewer that the tool is being run on.
	 * @return the control or viewer that the tool is being run on.
	 */
	public Control getCurrentControl() {
		return currentControl;
	}

}
