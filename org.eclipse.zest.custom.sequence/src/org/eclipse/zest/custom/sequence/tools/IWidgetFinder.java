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
import org.eclipse.swt.widgets.Widget;

/**
 * Helper interface for finding a widget based on an IFigure.
 * @author Del Myers
 */

public interface IWidgetFinder {
	/**
	 * Returns the widget for the given figure if it exists. Null otherwise.
	 * @param figure the figure to find the widget for.
	 * @return the widget for the given figure, or null.
	 */
	public Widget getWidget(IFigure figure);

}
