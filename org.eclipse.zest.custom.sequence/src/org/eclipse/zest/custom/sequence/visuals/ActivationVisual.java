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

import java.util.Iterator;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.AbstractHintLayout;
import org.eclipse.draw2d.ActionEvent;
import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.Animation;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.zest.custom.sequence.figures.ActivationFigure;
import org.eclipse.zest.custom.sequence.figures.PlusMinusFigure;
import org.eclipse.zest.custom.sequence.visuals.interactions.ActivationHoverInteraction;
import org.eclipse.zest.custom.sequence.widgets.Activation;
import org.eclipse.zest.custom.sequence.widgets.Message;
import org.eclipse.zest.custom.sequence.widgets.PropertyChangeListener;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;


/**
 * Visual representation of an activation.
 * @author Del Myers
 */

public class ActivationVisual extends NodeVisualPart implements PropertyChangeListener {
	private Label hover;
	//private ExpandingInteraction expander;
	private ActivationHoverInteraction highlighter;
	private PlusMinusFigure expanderFigure;
	
	private class WidgetLayoutConnectionAnchor extends AbstractConnectionAnchor {
		private boolean source;
		private Point animationHint;
		private Rectangle targetBounds;
		private ConnectionVisualPart part;
		public WidgetLayoutConnectionAnchor(ConnectionVisualPart part, IFigure figure, boolean source) {
			super(figure);
			this.part = part;
			this.source = source;
		}

		public Point getLocation(Point reference) {
			Widget widget = part.getWidget();
			if (widget instanceof Message) {
				if (isActive()) {
					Object layout = widget.getData(IWidgetProperties.LAYOUT);
					if (layout instanceof PointList) {
						if (((PointList)layout).size() < 2) {
							return new Point(0,0);
						}
						Point endPoint = (source) ? ((PointList)layout).getFirstPoint() : ((PointList)layout).getLastPoint();
						Point currentEnd = new Point(0,0);
						try {
							currentEnd = (source) ? part.getConnection().getPoints().getFirstPoint() : part.getConnection().getPoints().getLastPoint();
						} catch (IndexOutOfBoundsException e) {
							return new Point(0,0);
						}

						Point otherPoint = (source) ? ((PointList)layout).getLastPoint() : ((PointList)layout).getFirstPoint();
						Rectangle finalLayout = (Rectangle) getWidget().getData(IWidgetProperties.LAYOUT);
						Rectangle currentLayout = getFigure().getBounds();
						if (finalLayout == null) {
							animationHint =  currentLayout.getCenter();
							return animationHint;
						}
						if (!finalLayout.equals(targetBounds)) {
							//cache for animation
							targetBounds = finalLayout;
							animationHint = currentEnd;
							part.getConnection().invalidate();
							return currentEnd;
						} else {
							if (!Animation.isAnimating()) {
								return endPoint;
							}
						}

						if (currentLayout.equals(finalLayout)) {
							return endPoint;
						}
						if (!Animation.isAnimating()) {
							return endPoint;
						}
						if (currentEnd.y == endPoint.y) {
							return endPoint;
						}
						float scale = (float)currentLayout.height/(float)finalLayout.height;
						int y = (int)(currentLayout.y + ((endPoint.y-finalLayout.y)*scale));
						int x = currentLayout.x;
						if (otherPoint.x > endPoint.x) {
							x += currentLayout.width;
						}
						return new Point(x, y);
					}
				} else {
					//return a point with the same y value as the other end.
					Object layout = widget.getData(IWidgetProperties.LAYOUT);
					if (layout instanceof PointList) {
						try {
							Point otherPoint = (source) ? ((PointList)layout).getLastPoint() : ((PointList)layout).getFirstPoint();
							return new Point(0, otherPoint.y);
						} catch (IndexOutOfBoundsException e) {
							//do nothig
						}
					}
				}
			}
			return new Point(0,0);
		}
	}
	
	private class FlowingLayout extends AbstractHintLayout {
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.AbstractLayout#calculatePreferredSize(org.eclipse.draw2d.IFigure, int, int)
		 */
		@SuppressWarnings("unchecked")
		@Override
		protected Dimension calculatePreferredSize(IFigure container, int hint,
				int hint2) {
			Dimension d = new Dimension();
			for (Iterator<IFigure> i = ((Iterator<IFigure>)container.getChildren().iterator()); i.hasNext();) {
				IFigure child = i.next();
				Dimension childSize = child.getPreferredSize();
				d.height += childSize.height;
				if (d.width < childSize.width) {
					d.width = childSize.width;
				}
			}
			Dimension currentSize = container.getSize();
			return new Dimension(Math.max(currentSize.width, d.width), Math.max(currentSize.height, d.height));
			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.LayoutManager#layout(org.eclipse.draw2d.IFigure)
		 */
		@SuppressWarnings("unchecked")
		public void layout(IFigure container) {
			int top = 2;
			Rectangle containerBounds = container.getBounds();
			if (!container.isCoordinateSystem()) {
				top += containerBounds.y;
			}
			int halfWidth = containerBounds.width/2;
			for (Iterator<IFigure> i = container.getChildren().iterator(); i.hasNext();) {
				IFigure child = i.next();
				Dimension d = child.getPreferredSize();
				int x = halfWidth - (d.width/2);
				if (!container.isCoordinateSystem()) {
					x += containerBounds.x;
				}
				Rectangle newBounds = new Rectangle(x, top, d.width, d.height);
				top += d.height;
				child.setBounds(newBounds);
			}			
		}
		
	}
	
	/**
	 * @param item
	 * @param key
	 * @param parentFigure
	 */
	public ActivationVisual(UMLItem item, String key) {
		super(item, key);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#createFigure()
	 */
	@Override
	public IFigure createFigures() {
		IFigure figure = new ActivationFigure();
		LayoutManager layout = new FlowingLayout();
		figure.setLayoutManager(layout);
		hover = new Label();
		figure.setToolTip(hover);
		return figure;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#refreshVisuals()
	 */
	@Override
	public void refreshVisuals() {
		Object layout = getWidget().getData(IWidgetProperties.LAYOUT);
		if (layout == null) {
			getFigure().setVisible(false);
		} else {
			getFigure().setVisible(true);
		}
		getFigure().setBackgroundColor(getActivation().getBackground());
		String toolTipText = getWidget().getTooltipText();
		if (toolTipText == null) {
			toolTipText = getActivation().getText();
		}
		hover.setText(toolTipText);
		if (getActivation().getImage() != null) {
			hover.setIcon(getActivation().getImage());
		}
		getFigure().setForegroundColor(getActivation().getForeground());
//		if (!getActivation().hasChildren()) {
//			
//		} else {
//			getFigure().setForegroundColor(ZestUMLColors.ColorExpandable.getColor());
//		}
		if (getFigure().getBounds() == null || getFigure().getBounds().isEmpty()) {
			if (getActivation().getSourceCall() != null) {
				Activation callingActivation = getActivation().getSourceCall().getSource();
				if (callingActivation != null) {
					WidgetVisualPart callerVisuals = (WidgetVisualPart) callingActivation.getData(MessageBasedSequenceVisuals.VISUAL_KEY);
					if (callerVisuals != null && callerVisuals.isActive()) {
						Rectangle bounds = callerVisuals.getFigure().getBounds();
						getFigure().setBounds(bounds.getCopy());
					}
				}
			}
		}
		boolean highlight = getActivation().getData("pin") == Boolean.TRUE ||
			getActivation().getData(IWidgetProperties.HIGHLIGHT) == Boolean.TRUE;
		if (highlight) {
			((ActivationFigure)getFigure()).setLineWidth(2);
		} else {
			((ActivationFigure)getFigure()).setLineWidth(1);
		}
		resetExpander();
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.widgets.PropertyChangeListener#propertyChanged(java.lang.Object, java.lang.String, java.lang.Object, java.lang.Object)
	 */
	public void propertyChanged(Object source, String property, Object oldValue, Object newValue) {
		if (getChartVisuals().refreshing) return; //early return
		if (IWidgetProperties.LAYOUT.equals(property)) {
			if (newValue == null) return;
			//getFigure().setBounds(((Rectangle)newValue).getCopy());
			getParentFigure().setConstraint(getFigure(), ((Rectangle)newValue).getCopy());
			updateExpander();
			getFigure().setVisible(true);
			//getParentFigure().getLayoutManager().layout(getParentFigure());
			//getFigure().invalidate();
		} else if (IWidgetProperties.HIGHLIGHT.equals(property) || "pin".equals(property)) {
			boolean highlight = Boolean.TRUE.equals(getActivation().getData("pin")) ||
			getActivation().isHighlighted();
			if (highlight) {
				((ActivationFigure)getFigure()).setLineWidth(2);
			} else {
				((ActivationFigure)getFigure()).setLineWidth(1);
			}
		} else if (IWidgetProperties.SUB_CALL.equals(property)) {
			//Activation a = (Activation) source;
			resetExpander();
//			if (a.hasChildren()) {
//				if (expander.getPart() == null) {
//					expander.hookInteraction(this);
//					refreshVisuals();
//				}
//			} else {
//				if (expander.getPart() != null) {
//					expander.unhookInteraction();
//					refreshVisuals();
//				}
//			}
		} else if (IWidgetProperties.BACKGROUND_COLOR.equals(property)) {
			if ((getWidget().getBackground()) != null) {
				getFigure().setBackgroundColor(((getWidget()).getBackground()));
			} else if (getParentFigure() != null){
				getFigure().setBackgroundColor(getParentFigure().getBackgroundColor());
			}
		} else if (IWidgetProperties.FOREGROUND_COLOR.equals(property)) {
			if (getWidget().getForeground() != null) {
				getFigure().setForegroundColor(((getWidget()).getForeground()));
			} else if (getParentFigure() != null){
				getFigure().setForegroundColor(getParentFigure().getForegroundColor());
			}
		} else if (IWidgetProperties.EXPANDED.equals(property)) {
			updateExpander();
		} else if (IWidgetProperties.TOOLTIP.equals(property)) {
			String toolTipText = getWidget().getTooltipText();
			if (toolTipText == null) {
				toolTipText = getActivation().getText();
			}
			hover.setText(toolTipText);
		} else if (IWidgetProperties.MESSAGE.equals(property)) {
			resetExpander();
		}
	}
	
	private IFigure getParentFigure() {
		return getFigure().getParent();
	}
	
	/**
	 * @return
	 */
	private Activation getActivation() {
		return (Activation) getWidget();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#activate()
	 */
	@Override
	public void activate() {
		super.activate();
//		if (expander == null) {
//			expander = new ExpandingInteraction();
//		}
		if (highlighter == null) {
			highlighter = new ActivationHoverInteraction();
		}
//		if (getActivation().hasChildren()) { 
//			expander.hookInteraction(this);
//		}
		highlighter.hookInteraction(this);
		//selector.hookInteraction(this);
		getActivation().addPropertyChangeListener(this);
		Message[] messages = getActivation().getMessages();
		if (messages.length > 0 && messages[0].getTarget() == getActivation()) {
			Activation source = messages[0].getSource();
			Object layout = source.getData(IWidgetProperties.LAYOUT);
			if (layout instanceof Rectangle) {
				getFigure().setBounds(((Rectangle)layout).getCopy());
			}
		}
	}
	
		
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#deactivate()
	 */
	@Override
	public void deactivate() {
		super.deactivate();
		getFigure().setSize(0,0);
		//expander.unhookInteraction();
		resetExpander();
		highlighter.unhookInteraction();
		getActivation().removePropertyChangeListener(this);
		if (!getActivation().isDisposed())
			getActivation().setData(IWidgetProperties.LAYOUT, null);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.NodeVisualPart#getSourceAnchor(org.eclipse.mylar.zest.custom.sequence.visuals.ConnectionVisualPart)
	 */
	@Override
	public ConnectionAnchor getSourceAnchor(ConnectionVisualPart part) {
		return new WidgetLayoutConnectionAnchor(part, getFigure(), true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.NodeVisualPart#getTargetAnchor(org.eclipse.mylar.zest.custom.sequence.visuals.ConnectionVisualPart)
	 */
	@Override
	public ConnectionAnchor getTargetAnchor(ConnectionVisualPart part) {
		return new WidgetLayoutConnectionAnchor(part, getFigure(), false);
	}
	
	/**
	 * Resets the expander visibility based on whether the activation in this visual has children, and the visual
	 * is active.
	 */
	private void resetExpander() {
		Activation a = getActivation();
		boolean addExpander = a.hasChildren() && isActive() && a.isVisible() && !a.isHidden();
		if (addExpander) {
			if (expanderFigure == null) {
				expanderFigure = new PlusMinusFigure(9);
				expanderFigure.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						Activation a = getActivation();
						a.setExpanded(!a.isExpanded());
					}
				});
				getFigure().add(expanderFigure);
				
			}
//			int index = getParentFigure().getChildren().indexOf(getFigure());
//			int expanderIndex = getParentFigure().getChildren().indexOf(expanderFigure);
//			//@tag zest.sequence.performance : draw2d makes this operation f^2*a where f is the total number of figures, and a is the number of activations. Not good.
//			if (expanderIndex != index+1) {
//				if (index+1>=getParentFigure().getChildren().size()) {
					
//				} else {
//					getFigure().add(expanderFigure, index+1);
//				}
//			}
			updateExpander();
		} else {
			if (expanderFigure != null && expanderFigure.getParent() != null) {
				expanderFigure.getParent().remove(expanderFigure);
			}
			expanderFigure = null;
		}
	}
	
	/**
	 * Updates the expander figure according to match the visual state of this visual. 
	 */
	private void updateExpander() {
		if (expanderFigure == null) return;
		
		Activation a = getActivation();
		expanderFigure.setSelected(!a.isExpanded());
//		Rectangle bounds = (Rectangle) getParentFigure().getLayoutManager().getConstraint(getFigure());
//		
//		if (bounds == null) {
//			bounds = getFigure().getBounds();
//		}
//		if (bounds != null) {
//			expanderFigure.getParent().setConstraint(expanderFigure, new Rectangle(
//					bounds.x-1,
//					bounds.y+2,
//					9,
//					9
//			));
//		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#installFigures()
	 */
	@Override
	protected void installFigures() {
		super.installFigures();
//		if (expanderFigure != null) {
//			if (expanderFigure.getParent() != null) {
//				IFigure eParent = expanderFigure.getParent();
//				eParent.remove(expanderFigure);
//				eParent.add(expanderFigure);
//			}
//		}
	}
}
