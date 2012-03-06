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
package org.eclipse.zest.custom.sequence.visuals;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.custom.sequence.events.SequenceEvent;
import org.eclipse.zest.custom.sequence.events.SequenceListener;
import org.eclipse.zest.custom.sequence.widgets.Activation;
import org.eclipse.zest.custom.sequence.widgets.Call;
import org.eclipse.zest.custom.sequence.widgets.Message;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;


/**
 * 
 * A manager for highlighting selections and mouse-overs.
 * @author Del Myers
 *
 */
public class SelectionDecorationLayer implements SelectionListener, SequenceListener, FigureListener {
	private HashMap<UMLItem, IFigure> pinnedFigures; 
	private MessageBasedSequenceVisuals visuals;
	private FreeformLayer layer;

	public SelectionDecorationLayer(MessageBasedSequenceVisuals visuals) {
		this.visuals = visuals;
		pinnedFigures = new HashMap<UMLItem, IFigure>();
		visuals.getChart().addSelectionListener(this);
		visuals.getChart().addSequenceListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent e) {
		updateSelection();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent e) {
		updateSelection();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.events.SequenceListener#itemCollapsed(org.eclipse.zest.custom.sequence.events.SequenceEvent)
	 */
	public void itemCollapsed(SequenceEvent event) {
		updateSelection();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.events.SequenceListener#itemExpanded(org.eclipse.zest.custom.sequence.events.SequenceEvent)
	 */
	public void itemExpanded(SequenceEvent event) {
		updateSelection();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.events.SequenceListener#rootChanged(org.eclipse.zest.custom.sequence.events.SequenceEvent)
	 */
	public void rootChanged(SequenceEvent event) {
		updateSelection();
	}
	
	public void refresh() {
		updateSelection();
	}
	
	private void updateSelection() {
		for (UMLItem pinnedItem : pinnedFigures.keySet()) {
			IFigure itemFigure = visuals.getFigure(pinnedItem);
			IFigure pinFigure = pinnedFigures.get(pinnedItem);
			if (pinFigure.getParent() != null) {
				pinFigure.getParent().remove(pinFigure);
			}
			if (itemFigure != null) {
				itemFigure.removeFigureListener(this);
			}
		}
		pinnedFigures.clear();
		for (UMLItem item : visuals.getChart().getSelection()) {
			if (item instanceof Activation) {
				pin ((Activation)item);
			} else if (item instanceof Message) {
				pin ((Message)item);
			}
		}
	}
	
	/**
	 * @param message
	 */
	private void pin(Message item) {
		if (!item.isVisible()) return;
		if (pinnedFigures.containsKey(item)) return;
		Polyline figure = new Polyline(){
			/* (non-Javadoc)
			 * @see org.eclipse.draw2d.Figure#paint(org.eclipse.draw2d.Graphics)
			 */
			@Override
			public void paint(Graphics graphics) {
				graphics.pushState();
				graphics.setAlpha(150);
				setLineWidth(4);
				graphics.setForegroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_SELECTION));
				super.paint(graphics);
				graphics.popState();
				
			}
		};
		figure.setOpaque(true);
		IFigure itemFigure = visuals.getFigure(item);
		if (itemFigure != null) {
			pinnedFigures.put(item, figure);
			getLayer().add(figure);
			figureMoved(itemFigure);
			itemFigure.addFigureListener(this);
		}
		if (item instanceof Call) {
			pin(item.getTarget());
		}
	}

	/**
	 * @param item
	 */
	private void pin(Activation item) {
		if (!item.isVisible()) return;
		if (pinnedFigures.containsKey(item)) return;
		RectangleFigure figure = new RectangleFigure(){
			/* (non-Javadoc)
			 * @see org.eclipse.draw2d.Figure#paint(org.eclipse.draw2d.Graphics)
			 */
			@Override
			public void paint(Graphics graphics) {
				graphics.pushState();
				graphics.setAlpha(100);
				super.paint(graphics);
				graphics.popState();
				
			}
		};
		figure.setOpaque(true);
		figure.setFill(true);
		figure.setLineWidth(1);
		figure.setForegroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_SELECTION));
		figure.setBackgroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_SELECTION));
		
		IFigure itemFigure = visuals.getFigure(item);
		if (itemFigure != null) {
			pinnedFigures.put(item, figure);
			getLayer().add(figure);
			figureMoved(itemFigure);
			itemFigure.addFigureListener(this);
		}
		if (item.isExpanded()) {
			for (Message m : item.getMessages()) {
				pin(m);
			}
		}
	}

	public IFigure getLayer() {
		if (this.layer == null) {
			this.layer = new FreeformLayer();
			layer.setEnabled(false);
		}
		return layer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.FigureListener#figureMoved(org.eclipse.draw2d.IFigure)
	 */
	public void figureMoved(IFigure source) {
		UMLItem item = (UMLItem) visuals.getWidget(source);
		if (item == null) {
			//this is stale, have to remove the listener.
			source.removeFigureListener(this);
			for (Iterator<UMLItem> itemIterator = pinnedFigures.keySet().iterator(); itemIterator.hasNext();) {
				UMLItem pinnedItem = itemIterator.next();
				if (visuals.getFigure(pinnedItem) == source) {
					IFigure pinFigure = pinnedFigures.get(pinnedItem);
					if (pinFigure.getParent() != null) {
						pinFigure.getParent().remove(pinFigure);
					}
					itemIterator.remove();
				}
			}
		} else {
			if (item instanceof Activation) {
				RectangleFigure figure = (RectangleFigure) pinnedFigures.get(item);
				IFigure itemFigure = visuals.getFigure(item);
				if (figure != null && itemFigure != null) {
					figure.setBounds(itemFigure.getBounds().getCopy());
				}
			} else if (item instanceof Message) {
				Polyline line = (Polyline)pinnedFigures.get(item);
				Polyline itemLine = (Polyline)visuals.getFigure(item);
				line.setPoints(itemLine.getPoints().getCopy());
			}
		}
	}
	
	public String getLayerKey() {
		return "SelectionDecoration";
	}

}
