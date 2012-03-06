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
package org.eclipse.zest.custom.sequence.visuals.interactions;

import org.eclipse.draw2d.ActionEvent;
import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.custom.sequence.figures.PlusMinusFigure;
import org.eclipse.zest.custom.sequence.visuals.LayerConstants;
import org.eclipse.zest.custom.sequence.widgets.IExpandableItem;
import org.eclipse.zest.custom.sequence.widgets.PropertyChangeListener;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * An interaction that places a "+/-" icon over a visual part that can be expanded
 * and contracted.
 * @author Del Myers
 */

public class ExpandingInteraction extends AbstractInteraction {
	
	private PlusMinusFigure feedback = new PlusMinusFigure();
	
	private PropertyChangeListener expandChangeListener = new PropertyChangeListener(){
		/* (non-Javadoc)
		 * @see org.eclipse.mylar.zest.custom.sequence.widgets.PropertyChangeListener#propertyChanged(java.lang.Object, java.lang.String, java.lang.Object, java.lang.Object)
		 */
		public void propertyChanged(Object source, String property, Object oldValue, Object newValue) {
			if (IWidgetProperties.EXPANDED.equals(property)) {
				boolean value = (Boolean) newValue;
				if (value == feedback.isSelected()) {
					feedback.setSelected(!value);
				}
			}
		}
	};
	
	private ActionListener selectionChangeListener = new ActionListener() {
		public void actionPerformed(ActionEvent event) {
			IExpandableItem item = (IExpandableItem) getPart().getWidget();
			if (feedback.isSelected() == item.isExpanded()) {
				hideFeedback();
				Display.getCurrent().asyncExec(new Runnable(){
					public void run() {
						IExpandableItem item = (IExpandableItem) getPart().getWidget();
						item.setExpanded(!feedback.isSelected());
					}
				});
				
			}
		}
	};
	
	private MouseMotionListener mouseListener = new MouseMotionListener() {

		public void mouseDragged(MouseEvent me) {
		}

		public void mouseEntered(MouseEvent me) {
			showFeedback();
		}

		public void mouseExited(MouseEvent me) {
			if (feedback.getParent() != null) {
				Point p = me.getLocation().getCopy();
				((IFigure)me.getSource()).translateToAbsolute(p);
				if (!feedback.containsPoint(p)) {
					hideFeedback();
				}
			}
		}

		public void mouseHover(MouseEvent me) {
		}

		public void mouseMoved(MouseEvent me) {
			showFeedback();
		}
		
	};

	private IFigure figure;
	
	public void doHook() {
		if (getPart().getWidget() instanceof IExpandableItem) {
			IExpandableItem item = (IExpandableItem) getPart().getWidget();
			feedback.setSelected(!item.isExpanded());
			item.addPropertyChangeListener(expandChangeListener);
			feedback.addActionListener(selectionChangeListener);
			getFigure().addMouseMotionListener(mouseListener);
			feedback.addMouseMotionListener(mouseListener);
		}
	}
	
	public void doUnhook() {
		if (getPart().getWidget() instanceof IExpandableItem) {
			IExpandableItem item = (IExpandableItem) getPart().getWidget();
			item.removePropertyChangeListener(expandChangeListener);
			feedback.removeActionListener(selectionChangeListener);
			getFigure().removeMouseMotionListener(mouseListener);
			feedback.removeMouseMotionListener(mouseListener);
			hideFeedback();
		}
	}
	
	private void showFeedback() {
		if (feedback.getParent() != null) return;
		IFigure feedbackLayer = getPart().getLayer(LayerConstants.ACTIVE_FEEDBACK_LAYER);
		Rectangle figureBounds = getFigure().getClientArea();
		getFigure().getParent().translateToAbsolute(figureBounds);
		Rectangle feedbackBounds = new Rectangle(figureBounds.getCenter().x-5, figureBounds.y-5, 9,9);
		feedbackLayer.translateToRelative(feedbackBounds);
		feedbackLayer.add(feedback);
		feedback.setBounds(feedbackBounds);
		feedbackLayer.setConstraint(feedback, feedbackBounds);
	}
	
	private void hideFeedback() {
		Display.getCurrent().asyncExec(new Runnable(){
			public void run() {
				if (feedback.getParent() != null) {
					feedback.getParent().remove(feedback);
				}
			}
		});
	}
	
	public void setTargetFigure (IFigure figure) {
		this.figure = figure;
	}
	
	public IFigure getFigure() {
		if (figure == null) {
			return getPart().getFigure();
		}
		return figure;
	}

}
