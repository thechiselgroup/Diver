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

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.zest.custom.sequence.widgets.UMLSequenceChart;

import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.NavigateToCodeAction;

/**
 * Navigates to source code based on double click events.
 * @author Del Myers
 *
 */
public class NavigateToCodeListener extends MouseAdapter {
	

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.MouseAdapter#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
	 */
	@Override
	public void mouseDoubleClick(MouseEvent ev) {
		//get the data from the mouse event, and try to find a java editor for it
		Object o = ev.getSource();
		if (o instanceof UMLSequenceChart) {
			UMLSequenceChart chart = (UMLSequenceChart) o;
			Widget w = chart.getItemAt(ev.x, ev.y);
			if (w == null) {
				return;
			}
			final Object data = w.getData();
			NavigateToCodeAction action = new NavigateToCodeAction(chart);
			action.setFocusElement(data);
			action.run();
		}
	}

	
}
