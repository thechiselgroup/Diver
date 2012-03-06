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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.ActionEvent;
import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.AnchorListener;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.ShortestPathConnectionRouter;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.zest.custom.sequence.figures.PlusMinusFigure;
import org.eclipse.zest.custom.sequence.figures.SequenceClassFigure;
import org.eclipse.zest.custom.sequence.visuals.ContainmentTreeLayout.ContainmentTreeConstraint;
import org.eclipse.zest.custom.sequence.visuals.interactions.AbstractInteraction;
import org.eclipse.zest.custom.sequence.widgets.Lifeline;
import org.eclipse.zest.custom.sequence.widgets.PropertyChangeListener;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * @author Del Myers
 * 
 */
public class LifelineGroupVisual extends WidgetVisualPart implements
		PropertyChangeListener, ContainmentTreeConstraint {

	private class FocusInteraction extends AbstractInteraction implements
			MouseMotionListener {
		@Override
		protected void doHook() {
			getFigure().addMouseMotionListener(this);
		}

		@Override
		protected void doUnhook() {
			getFigure().removeMouseMotionListener(this);
		}

		public void mouseEntered(MouseEvent me) {
			getWidget().setData("hover", true);
		}

		public void mouseExited(MouseEvent me) {
			if (!expanderFigure.containsPoint(me.x, me.y)) {
				getWidget().setData("hover", null);
			}
		}

		public void mouseHover(MouseEvent me) {
		}

		public void mouseMoved(MouseEvent me) {
		}

		public void mouseDragged(MouseEvent me) {
		}
	}

	private class WidgetBasedConnectionAnchor extends AbstractConnectionAnchor {

		private Lifeline lifeline;

		public WidgetBasedConnectionAnchor(Lifeline lifeline) {
			this.lifeline = lifeline;
		}

		/**
		 * This method assumes that the reference point is given in
		 * display-relative coordinates, not system relative.
		 */
		public Point getLocation(Point reference) {
			checkOwner();
			Rectangle chartRelative = getChartVisuals().getRelativeLocation(
					getConnectionWidget());
			if (chartRelative == null) {
				return new Point(0, 0);
			}
			org.eclipse.swt.graphics.Point displayPoint = getChartVisuals()
					.getChart().toDisplay(chartRelative.x, chartRelative.y);
			Rectangle systemDisplayBounds = getSystemDisplayBounds();

			displayPoint.x += chartRelative.width / 2;
			// displayPoint.y += chartRelative.height / 2;
			chartRelative.x = displayPoint.x;
			chartRelative.y = displayPoint.y;
			int locationX = displayPoint.x;
			int locationY = displayPoint.y;
			// translate to the system bounds
			locationY -= systemDisplayBounds.y;
			locationX -= systemDisplayBounds.x;

			if (reference.y > locationY) {
				locationY = displayPoint.y - systemDisplayBounds.y
						+ chartRelative.height;
			} else if (reference.y < locationY) {
				locationY = displayPoint.y - systemDisplayBounds.y;
			}
			if (locationY > systemDisplayBounds.height) {
				locationY = systemDisplayBounds.height;
			}
			return new Point((int) locationX, (int) locationY);
		}

		@Override
		public Point getReferencePoint() {
			checkOwner();
			Rectangle chartRelative = getChartVisuals().getRelativeLocation(
					getConnectionWidget());
			if (chartRelative == null) {
				return new Point(0, 0);
			}
			org.eclipse.swt.graphics.Point p = getChartVisuals().getChart()
					.toDisplay(chartRelative.x, chartRelative.y);
			p.x += chartRelative.width / 2;
			p.y += chartRelative.height / 2;
			Rectangle systemDisplayBounds = getSystemDisplayBounds();
			p.x -= systemDisplayBounds.x;
			p.y -= systemDisplayBounds.y;
			return new Point(p.x, p.y);
		}

		protected void checkOwner() {
			IFigure oldOwner = getOwner();
			IFigure figure = getChartVisuals().getFigure(getConnectionWidget());
			if (oldOwner != figure) {
				if (oldOwner != null) {
					oldOwner.removeAncestorListener(this);
				}
				setOwner(figure);
				if (figure != null) {
					figure.addAncestorListener(this);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.draw2d.AbstractConnectionAnchor#addAnchorListener(org.eclipse.draw2d.AnchorListener)
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void addAnchorListener(AnchorListener listener) {
			listeners.add(listener);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.draw2d.AbstractConnectionAnchor#removeAnchorListener(org.eclipse.draw2d.AnchorListener)
		 */
		@Override
		public void removeAnchorListener(AnchorListener listener) {
			listeners.remove(listener);
		}

		/**
		 * @return
		 */
		protected UMLItem getConnectionWidget() {
			return lifeline;
		}
	}

	private List<Connection> connections;
	//needed in order to keep track of when children are disposed.
	private Lifeline[] widgetChildren;
	private PlusMinusFigure expanderFigure;
	private FocusInteraction focusser;

	/**
	 * @param item
	 * @param key
	 */
	public LifelineGroupVisual(UMLItem item, String key) {
		super(item, key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#createFigures()
	 */
	@Override
	public IFigure createFigures() {
		int type = SequenceClassFigure.CLASS;
		switch (getCastedModel().getTargetStyle()) {
		case Lifeline.ACTOR:
			type = SequenceClassFigure.ACTOR;
			break;
		case Lifeline.BOUNDARY:
			type = SequenceClassFigure.BOUNDARY;
			break;
		case Lifeline.COLLECTION:
			type = SequenceClassFigure.COLLECTION;
			break;
		case Lifeline.CONTROL:
			type = SequenceClassFigure.CONTROL;
			break;
		case Lifeline.DATA_STORE:
			type = SequenceClassFigure.DATA_STORE;
			break;
		case Lifeline.ENTITY:
			type = SequenceClassFigure.ENTITY;
			break;
		case Lifeline.PACKAGE:
			type = SequenceClassFigure.PACKAGE;
			break;
		}
		SequenceClassFigure figure = new SequenceClassFigure(type);
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 1;
		layout.marginWidth = 1;
		figure.setLayoutManager(layout);
		Label l = new Label();
		l.setText(getWidget().getText());
		figure.setToolTip(l);
		expanderFigure = new PlusMinusFigure(9);
		expanderFigure.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Lifeline l = getCastedModel();
				l.setExpanded(!l.isExpanded());
			}
		});
		expanderFigure.addMouseMotionListener(new MouseMotionListener(){
			public void mouseDragged(MouseEvent me) {}
			public void mouseEntered(MouseEvent me) {}
			public void mouseExited(MouseEvent me) {
				if (!getFigure().containsPoint(me.x, me.y)) {
					getWidget().setData("hover", null);
				}
			}
			public void mouseHover(MouseEvent me) {}
			public void mouseMoved(MouseEvent me) {}
		});
		return figure;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#activate()
	 */
	@Override
	public void activate() {
		super.activate();
		getWidget().addPropertyChangeListener(this);
		focusser = new FocusInteraction();
		focusser.hookInteraction(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#deactivate()
	 */
	@Override
	public void deactivate() {
		super.deactivate();
		disconnect();
		if (!getWidget().isDisposed()) {
			getWidget().setData("focus", null);
			getWidget().setHighlight(false);
		}
		getWidget().removePropertyChangeListener(this);
		focusser.unhookInteraction();
	}

	/**
	 * @return
	 */
	private Lifeline getCastedModel() {
		return (Lifeline) getWidget();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#refreshVisuals()
	 */
	@Override
	public void refreshVisuals() {
		SequenceClassFigure figure = (SequenceClassFigure) getFigure();
		figure.setClassName(getCastedModel().getText());
		figure.setBackgroundColor(getCastedModel().getBackground());
		figure.setForegroundColor(getCastedModel().getForeground());
		// check to see if either the parent or the children are visible. If so,
		// update the
		// parent-child relationship in the layout
		reconnect();
		if (Boolean.TRUE.equals(getWidget().getData("pin"))) {
			figure.setLineWidth(2);
		} else {
			figure.setLineWidth(1);
		}
		IFigure layer = getLayer(LayerConstants.OBJECT_GROUP_LAYER);
		
		if (layer.getLayoutManager() instanceof ContainmentTreeLayout) {
			ContainmentTreeLayout layout = (ContainmentTreeLayout) layer
					.getLayoutManager();
			if (Boolean.TRUE.equals(getWidget().getData("hover")) || 
					Boolean.TRUE.equals(getWidget().getData("pin"))) {
				((ContainmentTreeLayout)layout).addFocus(layer, figure);
			} else {
				layout.removeFocus(layer, figure);
			}
			layout.invalidate();
		}
		if (connections != null) {
			for (Connection c : connections) {
				WidgetBasedConnectionAnchor a1 = (WidgetBasedConnectionAnchor) c
						.getSourceAnchor();
				WidgetBasedConnectionAnchor a2 = (WidgetBasedConnectionAnchor) c
						.getTargetAnchor();
				a1.checkOwner();
				a2.checkOwner();
				// c.setVisible(a1.getConnectionWidget().isVisible() &&
				// a2.getConnectionWidget().isVisible());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#installFigures()
	 */
	@Override
	protected void installFigures() {
		IFigure layer = getLayer(LayerConstants.OBJECT_GROUP_LAYER);
		layer.add(getFigure(), this);
		GridData data = new GridData(GridData.CENTER, GridData.BEGINNING,
				false, false);
		data.widthHint = 9;
		data.heightHint = 9;
		getFigure().add(expanderFigure, data);
		reconnect();
	}

	/**
	 * 
	 */
	private void reconnect() {
		disconnect();
		IFigure layer = getLayer(LayerConstants.OBJECT_GROUP_CONNECTION_LAYER);
		widgetChildren = getCastedModel().getChildren();
		connections = new ArrayList<Connection>();
		for (int i = 0; i < widgetChildren.length; i++) {
			if (!widgetChildren[i].isDisposed() && (widgetChildren[i].isExpanded() || getCastedModel().getData(
					"pin") != null || getCastedModel().getData(
					"hover") != null || widgetChildren[i].isHighlighted())
					&&widgetChildren[i].isVisible()) {
				PolylineConnection c = new PolylineConnection();
				connections.add(c);
				c.setConnectionRouter(new ShortestPathConnectionRouter(
						getLayer(LayerConstants.OBJECT_GROUP_LAYER)));
				c.setSourceAnchor(new WidgetBasedConnectionAnchor(
						getCastedModel()));
				c.setTargetAnchor(new WidgetBasedConnectionAnchor(widgetChildren[i]));
				layer.add(c);
				if (widgetChildren[i].isHighlighted()) {
					c.setLineWidth(2);
				}
			}			
			widgetChildren[i].addPropertyChangeListener(this);			
			
		}
	}

	private void disconnect() {
		if (widgetChildren != null) {
			for (Lifeline l : widgetChildren) {
				l.removePropertyChangeListener(this);
			}
			widgetChildren = null;
		}
		if (connections == null)
			return;
		while(connections.size() > 0) {
			Connection c = connections.remove(0);
			IFigure layer = c.getParent();
			if (layer != null) {
				layer.remove(c);
			}
			c.setSourceAnchor(null);
			c.setTargetAnchor(null);
		}
	}

	private Rectangle getSystemDisplayBounds() {
		IFigure layer = getLayer(LayerConstants.OBJECT_GROUP_CONNECTION_LAYER);
		Rectangle bounds = layer.getBounds().getCopy();
		layer.translateToAbsolute(bounds);
		Control c = getChartVisuals().getChart().getLifelineGroupControl();
		org.eclipse.swt.graphics.Point p = c.getParent().toDisplay(
				c.getLocation());
		int width = c.getSize().x;
		int height = c.getSize().y;
		if (c instanceof Scrollable) {
			width = ((Scrollable)c).getClientArea().width;
			height = ((Scrollable)c).getClientArea().height;
		}
		return new Rectangle(p.x, p.y, width, height);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#uninstallFigures()
	 */
	@Override
	protected void uninstallFigures() {
		disconnect();
		connections = null;
		IFigure layer = getLayer(LayerConstants.OBJECT_GROUP_LAYER);
		layer.getLayoutManager().setConstraint(getFigure(), null);
		super.uninstallFigures();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.zest.custom.sequence.widgets.PropertyChangeListener#propertyChanged(java.lang.Object,
	 *      java.lang.String, java.lang.Object, java.lang.Object)
	 */
	public void propertyChanged(Object source, String property,
			Object oldValue, Object newValue) {
		if (getChartVisuals().refreshing) return; //early return
		if (source != getCastedModel() && ("pin".equals(property))) {
			// will propogate up the hierarchy
			getWidget().setHighlight(Boolean.TRUE.equals(newValue));
		} else if (source != getCastedModel()
				&& (IWidgetProperties.HIGHLIGHT.equals(property))) {
			// will propogate up the hierarchy
			getWidget().setHighlight(Boolean.TRUE.equals(newValue));
		} else if ("child".equals(property) && newValue == null) {
			//child was removed, stop listening to it
			if (source instanceof Lifeline) {
				((Lifeline) source).removePropertyChangeListener(this);
			}
		}
		refreshVisuals();

	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.ContainmentTreeLayout.ContainmentTreeConstraint#getParentFigure()
	 */
	public IFigure getParentFigure() {
		if (getCastedModel().getParent() != null) {
			WidgetVisualPart parentPart = getChartVisuals().getVisualPart(getCastedModel().getParent());
			if (parentPart != null) {
				return parentPart.getFigure();
			}
		}
		return null;
	}

}
