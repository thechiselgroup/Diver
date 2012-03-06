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

import java.util.LinkedList;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.RangeModel;
import org.eclipse.draw2d.Viewport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.zest.custom.sequence.tools.AbstractWidgetTool;

/**
 * Selection tool for the sequence viewer.
 * @author Del Myers
 */

public class SelectionTool extends AbstractWidgetTool {

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
		Widget parent = getCurrentControl();
		UMLSequenceChart chart = (UMLSequenceChart) parent;
		if (((parent.getStyle() & SWT.MULTI) != 0) && ((me.stateMask & SWT.MOD1) != 0)) {
			if (widget == null) return;
			UMLItem[] oldSelection = chart.getSelection();
			LinkedList<UMLItem> newSelection = new LinkedList<UMLItem>();
			boolean reselect = false;
			for (int i = 0; i < oldSelection.length; i++) {
				UMLItem item = oldSelection[i];
				if (item == widget) {
					reselect = true;
					newSelection.addFirst(item);
				} else {
					newSelection.addLast(item);
				}
			}
			if (!reselect && widget instanceof UMLItem) {
				newSelection.addLast((UMLItem)widget);
			}
			chart.internalUpdateSelection(newSelection.toArray(new UMLItem[newSelection.size()]));			
			if (reselect) {
				//we have found the item a second time. Put it at the
				//top of the selection list, and fire a "re-select"
				chart.internalReselect(newSelection.getFirst()); 
			}			
		} else {
			if (widget == null) {
				if (chart.getSelection().length != 0) {
					chart.internalSetSelection(new UMLItem[0]);
				}
			} else if (widget != null){
				chart.internalSetSelection(new UMLItem[] {(UMLItem)widget});
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#handleMouseReleased(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public void handleMouseReleased(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#understandsEvent(org.eclipse.swt.events.MouseEvent, org.eclipse.swt.widgets.Widget, org.eclipse.draw2d.IFigure)
	 */
	public boolean understandsEvent(MouseEvent me, Widget widget,
			IFigure cursorTarget) {
		return (getCurrentControl() instanceof UMLSequenceChart); 
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.tools.IWidgetTool#handleMouseWheelScrolled(org.eclipse.swt.widgets.Event)
	 */
	public void handleMouseWheelScrolled(Event event, IFigure mouseTarget) {
		//find the nearest viewport
		while (!(mouseTarget instanceof Viewport) && mouseTarget != null) {
			mouseTarget = mouseTarget.getParent();
		}
		if (mouseTarget instanceof Viewport) {
			RangeModel rm = ((Viewport)mouseTarget).getVerticalRangeModel();
			rm.setValue(rm.getValue() - event.count*10);
		}
		
	}

}
