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
package org.eclipse.zest.custom.sequence.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;

/**
 * An event filter for UMLSequenceCharts that forwards events in its canvases to
 * the parent control. This is used to make it look like the event occurred within
 * the chart, rather than within its children.
 * @author Del Myers
 *
 */
public class SequenceChartEventFilter implements Listener {
	
	private UMLSequenceChart chart;
	private Display display;
	private boolean in;
	/**
	 * Creates a filter for the chart, and hooks it to the display.
	 * @param chart
	 */
	public SequenceChartEventFilter(UMLSequenceChart chart) {
		this.chart = chart;
		this.display = chart.getDisplay();
	}

	/**
	 * 
	 */
	public void hookToDisplay() {
		display.addFilter(SWT.MouseDoubleClick, this);
		display.addFilter(SWT.MouseDown, this);
		display.addFilter(SWT.MouseHover, this);
		display.addFilter(SWT.MouseMove, this);
		display.addFilter(SWT.MouseUp, this);
		display.addFilter(SWT.MouseWheel, this);
		display.addFilter(SWT.DragDetect, this);
		display.addFilter(SWT.KeyDown, this);
		display.addFilter(SWT.KeyUp, this);
		display.addFilter(SWT.MenuDetect, this);
		display.addFilter(SWT.Show, this);
	}
	
	public void unhookFromDisplay() {
		display.removeFilter(SWT.MouseDoubleClick, this);
		display.removeFilter(SWT.MouseDown, this);
		display.removeFilter(SWT.MouseHover, this);
		display.removeFilter(SWT.MouseMove, this);
		display.removeFilter(SWT.MouseUp, this);
		display.removeFilter(SWT.MouseWheel, this);
		display.removeFilter(SWT.DragDetect, this);
		display.removeFilter(SWT.KeyDown, this);
		display.removeFilter(SWT.KeyUp, this);
		display.removeFilter(SWT.MenuDetect, this);
		display.removeFilter(SWT.Show, this);
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	public void handleEvent(Event event) {
		Point point = null;
		Point displayPoint = null;
		Event e = new Event();
		if (event.type == SWT.MouseMove) {
			if (chart.isVisible()) {
				handleMouseMove(event);
			}
			return;
		}
		if (event.widget == chart.getSequenceControl()) {
			displayPoint = chart.getSequenceControl().toDisplay(event.x, event.y);
			point = chart.toControl(displayPoint);
		} else if (event.widget == chart.getLifelineControl()) {
			displayPoint = chart.getLifelineControl().toDisplay(event.x, event.y);
			point = chart.toControl(displayPoint);
		} else {
			return;
		}
		e.keyCode = event.keyCode;
		e.stateMask = event.stateMask;
		e.text = event.text;
		e.type = event.type;
		e.time = event.time;
		e.widget = chart;
		e.button = event.button;
		e.character = event.character;
		e.count = event.count;
		e.data = event.data;
		e.detail = event.detail;
		e.display = event.display;
		e.doit = event.doit;
		e.end = event.end;
		e.gc = event.gc;
		e.height = event.height;
		e.index = event.index;
		e.x = point.x;
		e.y = point.y;
		switch (event.type) {
		case SWT.MenuDetect:
			event.doit = false;
			e.doit = true;
			if (point != null) {
				e.item = chart.getItemAt(point);
				chart.notifyListeners(e.type, e);
				Menu menu = chart.getMenu();
				if (menu != null && !menu.isDisposed()) {
					menu.setLocation (event.x, event.y);
					menu.setVisible(true);
				}
			}
		case SWT.MouseDoubleClick:
		case SWT.MouseDown:
		case SWT.MouseHover:
		case SWT.MouseUp:
		case SWT.MouseWheel:
			if (point != null) {
				e.item = chart.getItemAt(point);
				chart.notifyListeners(e.type, e);
			}
			break;
		case SWT.KeyUp:
		case SWT.KeyDown:
			UMLItem[] selected = chart.getSelection();
			if (selected.length > 0) {
				e.item = selected[0];
			}
		}
		chart.notifyListeners(e.type, e);
	}

	/**
	 * @param e
	 * @param point
	 */
	private void handleMouseMove(Event event) {
		if (event.widget instanceof Control) {
			if (event.widget == chart) return;
			Rectangle bounds = chart.getBounds();
			Point displayPoint = ((Control)event.widget).toDisplay(event.x, event.y);
			Point parentPoint = chart.getParent().toControl(displayPoint);
			Event e = new Event();
			e.keyCode = event.keyCode;
			e.stateMask = event.stateMask;
			e.text = event.text;
			e.type = event.type;
			e.time = event.time;
			e.widget = chart;
			e.button = event.button;
			e.character = event.character;
			e.count = event.count;
			e.data = event.data;
			e.detail = event.detail;
			e.display = event.display;
			e.doit = event.doit;
			e.end = event.end;
			e.gc = event.gc;
			e.height = event.height;
			e.index = event.index;
			
			if (bounds.contains(parentPoint)) {
				Point point = chart.toControl(displayPoint);
				e.x = point.x;
				e.y = point.y;
				if (!in) {
					in = true;
					chart.notifyListeners(SWT.MouseEnter, e);
				}
				chart.notifyListeners(SWT.MouseMove, e);
			} else {
				if (in) {
					Point point = chart.toControl(displayPoint);
					e.x = point.x;
					e.y = point.y;
					in = false;
					chart.notifyListeners(SWT.MouseExit, e);
				}
			}
			
		}
		
	}
	
	

}
