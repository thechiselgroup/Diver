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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.ActionEvent;
import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.custom.sequence.figures.LifeLineFigure;
import org.eclipse.zest.custom.sequence.figures.PlusMinusFigure;
import org.eclipse.zest.custom.sequence.figures.SequenceClassFigure;
import org.eclipse.zest.custom.sequence.figures.internal.ZoomListener;
import org.eclipse.zest.custom.sequence.figures.internal.ZoomManager;
import org.eclipse.zest.custom.sequence.widgets.Lifeline;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * Visual representation of a life line.
 * @author Del Myers
 */

public class LifelineVisual extends NodeVisualPart implements org.eclipse.zest.custom.sequence.widgets.PropertyChangeListener, ZoomListener {
	
	private class InternalFigure extends Figure {
		private SequenceClassFigure classFigure;
		private LifeLineFigure lifeLineFigure;
		InternalFigure(int type) {
			classFigure = new SequenceClassFigure(type);
			lifeLineFigure = new LifeLineFigure();
			super.add(lifeLineFigure, null, -1);
			super.add(classFigure, null, -1);
			setLayoutManager(new AbstractLayout(){
				protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint) {
					Rectangle bounds = container.getBounds();
					Rectangle parentB = container.getParent().getBounds();
					Dimension classSize = classFigure.getPreferredSize(wHint, 0);
					Dimension lifeSize = lifeLineFigure.getPreferredSize(wHint, hHint-classSize.height);
					int width = classSize.width;
					int height = Math.max(lifeSize.height + classSize.height, parentB.height-bounds.y);
					return new Dimension(width,height);
				}
				public void layout(IFigure container) {
					//stack one on top of the other
					Rectangle bounds = container.getBounds();
					Rectangle classBounds = new Rectangle(bounds.x, bounds.y, bounds.width, 0);
					lifeLineFigure.setBounds(new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height));
					classFigure.setBounds(classBounds);
				}
				
			});
		}
		public void add(IFigure figure, Object constraint, int index) {
			lifeLineFigure.add(figure, constraint, index);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Figure#remove(org.eclipse.draw2d.IFigure)
		 */
		public void remove(IFigure figure) {
			if (figure.getParent() == lifeLineFigure) {
				lifeLineFigure.remove(figure);
			} else {
				super.remove(figure);
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Figure#setBounds(org.eclipse.draw2d.geometry.Rectangle)
		 */
		public void setBounds(Rectangle rect) {
			super.setBounds(rect);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Figure#getBounds()
		 */
		public Rectangle getBounds() {
			return super.getBounds();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Figure#getClientArea(org.eclipse.draw2d.geometry.Rectangle)
		 */
		public Rectangle getClientArea(Rectangle rect) {
			Rectangle area = classFigure.getBounds().getCopy();
			area.union(lifeLineFigure.getBounds());
			return area;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Figure#containsPoint(int, int)
		 */
		public boolean containsPoint(int x, int y) {
			return lifeLineFigure.containsPoint(x, y) || classFigure.containsPoint(x, y);
		}
	}


	private ConnectionAnchor anchor;

	private PlusMinusFigure expander;
	
	private SequenceClassFigure classFigure;

	private InternalFigure internalFigure;
	
	/**
	 * @param item
	 * @param key
	 * @param parentFigure
	 */
	public LifelineVisual(UMLItem item, String key) {
		super(item, key);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#createFigure()
	 */
	@Override
	public IFigure createFigures() {
		int type = SequenceClassFigure.CLASS;
		switch(getCastedModel().getTargetStyle()) {
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
		classFigure = new SequenceClassFigure(type);
		internalFigure = new InternalFigure(type);
		internalFigure.setFont(Display.getCurrent().getSystemFont());
		anchor = new AbstractConnectionAnchor(internalFigure.classFigure){
			public Point getLocation(Point reference) {
				Point point = getOwner().getBounds().getCenter();
				point.y = getOwner().getBounds().y;
				getOwner().translateToAbsolute(point);
				return point;
			}
		};
		classFigure.getLabel().setIcon(getWidget().getImage());
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 1;
		layout.marginWidth = 1;
		classFigure.setLayoutManager(layout);
		internalFigure.classFigure.getLabel().setIcon(getWidget().getImage());
		internalFigure.lifeLineFigure.setFilled(false);
		internalFigure.lifeLineFigure.setBackgroundColor(getWidget().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
//		feedbackFigure = new SequenceClassFigure(type);
//		feedbackFigure.setEnabled(false);
		Label toolTip = new Label();
		toolTip.setText(getCastedModel().getText());
		internalFigure.setToolTip(toolTip);
		expander = new PlusMinusFigure(9);
		expander.setSelected(false);
		expander.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event) {
				getCastedModel().setExpanded(!getCastedModel().isExpanded());
			}
		});
		return classFigure;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#getFigures()
	 */
	@Override
	public List<IFigure> getFigures() {
		LinkedList<IFigure> figures = new LinkedList<IFigure>();
		figures.add(getFigure());
		figures.add(classFigure);
		return figures;
	}
	
	private Lifeline getCastedModel() {
		return (Lifeline)getWidget();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#refreshVisuals()
	 */
	@Override
	public void refreshVisuals() {
		Lifeline model = getCastedModel();
		String stereotype = 
			(String)model.getStereoType();
		String text = model.getText();
		internalFigure.classFigure.setClassName(text);
		classFigure.setClassName(text);
		internalFigure.classFigure.setStereoType(stereotype);
		classFigure.setStereoType(stereotype);
		classFigure.getLabel().setIcon(getWidget().getImage());
		internalFigure.classFigure.getLabel().setIcon(getWidget().getImage());
		Label tooltipFigure = (Label) internalFigure.getToolTip();
		String tooltipText = getWidget().getTooltipText();
		if (tooltipText == null) {
			tooltipText = getWidget().getText();
		}
		tooltipFigure.setText(tooltipText);
		if (model.getData("pin") != null) {
			internalFigure.classFigure.setLineWidth(2);
			classFigure.setLineWidth(2);
		} else {
			internalFigure.classFigure.setLineWidth(1);
			internalFigure.lifeLineFigure.setFilled(false);
			classFigure.setLineWidth(1);
		}
		internalFigure.lifeLineFigure.setFilled(model.isHighlighted());
		if ((getWidget().getBackground()) != null) {
			internalFigure.classFigure.setBackgroundColor(((getWidget()).getBackground()));
			classFigure.setBackgroundColor(((getWidget()).getBackground()));
		} else if (internalFigure.getParent() != null){
			internalFigure.classFigure.setBackgroundColor(internalFigure.getParent().getBackgroundColor());
			classFigure.setBackgroundColor(internalFigure.getParent().getBackgroundColor());
		}
		Label l = internalFigure.classFigure.getLabel();
		Label cl = classFigure.getLabel();
		if (getWidget().getForeground() != null) {
			internalFigure.classFigure.setForegroundColor(((getWidget()).getForeground()));
			classFigure.setForegroundColor(((getWidget()).getForeground()));
		} else if (internalFigure.getParent() != null){
			internalFigure.classFigure.setForegroundColor(internalFigure.getParent().getForegroundColor());
			classFigure.setForegroundColor(internalFigure.getParent().getForegroundColor());
		}
		if (getCastedModel().getTextBackground() != null) {
			l.setBackgroundColor(getCastedModel().getTextBackground());
			l.setOpaque(true);
			cl.setBackgroundColor(getCastedModel().getTextBackground());
			cl.setOpaque(true);
		} else {
			l.setOpaque(false);
			cl.setOpaque(false);
		}
		if (getCastedModel().getTextForeground() != null) {
			l.setForegroundColor(getCastedModel().getTextForeground());
			cl.setForegroundColor(getCastedModel().getTextForeground());
		}
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#activate()
	 */
	@Override
	public void activate() {
		super.activate();
		hookWidget();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#getFigure()
	 */
	@Override
	public IFigure getFigure() {
		// TODO Auto-generated method stub
		return super.getFigure();
	}
	
	/**
	 * 
	 */
	private void hookWidget() {
		getCastedModel().addPropertyChangeListener(this);
		ZoomManager manager = (ZoomManager) getChartVisuals().getChart().getData("ZoomManager");
		if (manager != null) {
			manager.addZoomListener(this);
		}
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#deactivate()
	 */
	@Override
	public void deactivate() {
		unhookWidget();
		super.deactivate();
	}

	/**
	 * 
	 */
	private void unhookWidget() {
		ZoomManager manager = (ZoomManager) getChartVisuals().getChart().getData("ZoomManager");
		if (manager != null) {
			manager.removeZoomListener(this);
		}
		getCastedModel().removePropertyChangeListener(this);
		if (!getWidget().isDisposed())
			getCastedModel().setData(IWidgetProperties.LAYOUT, null);
	}

	

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.widgets.PropertyChangeListener#propertyChanged(java.lang.Object, java.lang.String, java.lang.Object, java.lang.Object)
	 */
	public void propertyChanged(Object source, String property, Object oldValue, Object newValue) {
		if (getChartVisuals().refreshing) return; //early return
		if (IWidgetProperties.LAYOUT.equals(property)) {
			if (newValue == null) return;
			internalFigure.getParent().setConstraint(internalFigure, newValue);
			internalFigure.invalidate();
			classFigure.invalidateTree();
			if (classFigure.getParent() != null) {
				Rectangle r = (Rectangle) newValue;
				r = r.getCopy().setSize(r.width, MessageBasedSequenceVisuals.OBJECT_HEIGHT);
				r.y=0;
				//scale the x position
				ZoomManager manager = (ZoomManager) getChartVisuals().getChart().getData("ZoomManager");
				if (manager != null) {
					r.x *= manager.getZoom();
					int zoomedWidth = (int) (r.width*manager.getZoom());
					if (zoomedWidth > r.width) {
						r.x += (zoomedWidth/2-r.width/2);
					} else {
						r.width = zoomedWidth;
					}
				}
				classFigure.getParent().setConstraint(classFigure, r);
				
			}
		} else if (IWidgetProperties.HIGHLIGHT.equals(property)) {
			internalFigure.lifeLineFigure.setFilled(getWidget().isHighlighted());
		} else if ("pin".equals(property)) {
			if (newValue != null) {
				internalFigure.classFigure.setLineWidth(2);
				classFigure.setLineWidth(2);
				
			} else {
				internalFigure.classFigure.setLineWidth(1);
				classFigure.setLineWidth(1);
			}
		} else if (IWidgetProperties.BACKGROUND_COLOR.equals(property)) {
			if ((getWidget().getBackground()) != null) {
				internalFigure.classFigure.setBackgroundColor(((getWidget()).getBackground()));
				classFigure.setBackgroundColor(((getWidget()).getBackground()));
			} else if (internalFigure.getParent() != null){
				internalFigure.classFigure.setBackgroundColor(internalFigure.getParent().getBackgroundColor());
				classFigure.setBackgroundColor(internalFigure.getParent().getBackgroundColor());
			}
		} else if (IWidgetProperties.FOREGROUND_COLOR.equals(property)) {
			if (getWidget().getForeground() != null) {
				internalFigure.classFigure.setForegroundColor(((getWidget()).getForeground()));
				classFigure.setForegroundColor(((getWidget()).getForeground()));
			} else if (internalFigure.getParent() != null){
				internalFigure.classFigure.setForegroundColor(internalFigure.getParent().getForegroundColor());
				classFigure.setForegroundColor(internalFigure.getParent().getForegroundColor());
			}
		} else if (IWidgetProperties.TEXT_BACKGROUND.equals(property)) {
			Label l = ((InternalFigure)internalFigure).classFigure.getLabel();
			if (getCastedModel().getTextBackground() != null) {
				l.setBackgroundColor(getCastedModel().getTextBackground());
				l.setOpaque(true);
			} else {
				l.setOpaque(false);
			}
			l = classFigure.getLabel();
			if (getCastedModel().getTextBackground() != null) {
				l.setBackgroundColor(getCastedModel().getTextBackground());
				l.setOpaque(true);
			} else {
				l.setOpaque(false);
			}
		} else if (IWidgetProperties.TEXT_FOREGROUND.equals(property)) {
			Label l = internalFigure.classFigure.getLabel();
			if (getCastedModel().getTextForeground() != null) {
				l.setForegroundColor(getCastedModel().getTextForeground());
			}
			l = classFigure.getLabel();
			if (getCastedModel().getTextForeground() != null) {
				l.setForegroundColor(getCastedModel().getTextForeground());
			}
		} else if (IWidgetProperties.TOOLTIP.equals(property)) {
			Label tooltipFigure = (Label) internalFigure.getToolTip();
			String tooltipText = getWidget().getTooltipText();
			if (tooltipText == null) {
				tooltipText = getWidget().getText();
			}
			tooltipFigure.setText(tooltipText);
		} else if (IWidgetProperties.CHILD.equals(property)) {
			updateExpander();
		} else if (IWidgetProperties.EXPANDED.equals(property)) {
			updateExpander();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.figures.internal.ZoomListener#zoomChanged(double)
	 */
	public void zoomChanged(double zoom) {
		ZoomManager manager = (ZoomManager) getChartVisuals().getChart().getData("ZoomManager");
		if (manager == null) return;
		if (classFigure.getParent() != null) {
			Rectangle r = (Rectangle) internalFigure.getBounds();
			r = r.getCopy().setSize(r.width, MessageBasedSequenceVisuals.OBJECT_HEIGHT);
			r.y=0;
			//scale the x position
			r.x *= manager.getZoom();
			int zoomedWidth = (int) (r.width*manager.getZoom());
			if (zoomedWidth > r.width) {
				r.x += (zoomedWidth/2-r.width/2);
			} else {
				r.width = zoomedWidth;
			}
			classFigure.getParent().setConstraint(classFigure, r);
		}
		
	}
	
	
	/**
	 * 
	 */
	private void updateExpander() {
		if (getCastedModel().getChildren().length > 0) {
			if (expander.getParent() != classFigure) {
				GridData data = new GridData(GridData.CENTER, GridData.BEGINNING, false, false);
				data.widthHint = 9;
				data.heightHint = 9;
				classFigure.add(expander, data);
				expander.setSelected(!getCastedModel().isExpanded());
			}
		} else {
			if (expander.getParent() == classFigure) {
				classFigure.remove(expander);
			}
		}
	}


	public IFigure getClassFigure() {
		return classFigure;
	}
	
		
	/* (non-JavinternalFigure.getParent()rg.eclipse.mylar.zest.custom.sequence.visuals.NodeVisualPart#getSourceAnchor(org.eclipse.mylar.zest.custom.sequence.visuals.ConnectionVisualPart)
	 */
	@Override
	public ConnectionAnchor getSourceAnchor(ConnectionVisualPart part) {
		return anchor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.NodeVisualPart#getTargetAnchor(org.eclipse.mylar.zest.custom.sequence.visuals.ConnectionVisualPart)
	 */
	@Override
	public ConnectionAnchor getTargetAnchor(ConnectionVisualPart part) {
		return anchor;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#installFigures()
	 */
	@Override
	protected void installFigures() {
		IFigure layer = getLayer(LayerConstants.LIFELINE_LAYER);
		layer.add(internalFigure);
		layer = getLayer(LayerConstants.OBJECT_LAYER);
		if (layer != null) {
			layer.add(classFigure);
		}
		updateExpander();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#uninstallFigures()
	 */
	@Override
	protected void uninstallFigures() {
		super.uninstallFigures();
		if (internalFigure != null && internalFigure.getParent() != null) {
			internalFigure.getParent().remove(internalFigure);
		}
	}


	
	
}
