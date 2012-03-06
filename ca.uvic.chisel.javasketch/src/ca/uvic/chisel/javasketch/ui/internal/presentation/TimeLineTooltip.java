/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.ui.internal.presentation;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.widgets.Event;

import ca.uvic.chisel.widgets.RangeAnnotation;
import ca.uvic.chisel.widgets.TimeField;

/**
 * @author Del Myers
 *
 */
class TimeLineTooltip extends DefaultToolTip {

	
	private IJavaSketchPresenter editor;

	/**
	 * @param control
	 */
	public TimeLineTooltip(IJavaSketchPresenter editor) {
		super(editor.getTimeRange());
		this.editor = editor;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.DefaultToolTip#getText(org.eclipse.swt.widgets.Event)
	 */
	@Override
	protected String getText(Event event) {
		if (event.item instanceof RangeAnnotation) {
			return ((RangeAnnotation)event.item).getText();
		}
		//return the time
		return "Time: " + TimeField.toString(editor.getTimeRange().toRangeValue(event.x));
	}

}
