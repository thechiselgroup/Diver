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

import org.eclipse.draw2d.AbstractRouter;
import org.eclipse.draw2d.Animation;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.draw2d.RotatableDecoration;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.zest.custom.sequence.figures.AligningBendpointLocator;
import org.eclipse.zest.custom.sequence.figures.CircleDecoration;
import org.eclipse.zest.custom.sequence.figures.internal.AnimatedDeferredLayoutPolylineConnection;
import org.eclipse.zest.custom.sequence.figures.internal.DeferredLayoutPolylineConnection;
import org.eclipse.zest.custom.sequence.visuals.interactions.ActivationHoverInteraction;
import org.eclipse.zest.custom.sequence.widgets.Activation;
import org.eclipse.zest.custom.sequence.widgets.Message;
import org.eclipse.zest.custom.sequence.widgets.PropertyChangeListener;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * Visual part for UML calls in sequence charts.
 * @author Del Myers
 */

public class MessageVisual extends ConnectionVisualPart implements PropertyChangeListener {

	private Label hover;
	private Label label;
	private RotatableDecoration targetDec;
	private RotatableDecoration sourceDec;
	private ActivationHoverInteraction mouseOverInteraction;
	private boolean needsRouting;

	public static final PointList DIAMOND_TIP = new PointList();
	public static final PointList SQUARE_TIP = new PointList();
	private Image localImage;
	static {
		DIAMOND_TIP.addPoint(0, 0);
		DIAMOND_TIP.addPoint(-1, 1);
		DIAMOND_TIP.addPoint(-2, 0);
		DIAMOND_TIP.addPoint(-1, -1);
		
		SQUARE_TIP.addPoint(-1, -1);
		SQUARE_TIP.addPoint(-1, 1);
		SQUARE_TIP.addPoint(1, 1);
		SQUARE_TIP.addPoint(1, -1);
	}
	
	private class CallConnectionRouter extends AbstractRouter {
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.ConnectionRouter#route(org.eclipse.draw2d.Connection)
		 */
		public void route(Connection connection) {
			try {
			if (!isActive() || (!needsRouting && !Animation.isAnimating())) 
				return;
			Activation source = getMessage().getSource();
			Activation target = getMessage().getTarget();
			Point start = getStartPoint(connection);
			Point end = getEndPoint(connection);
			connection.getPoints().removeAllPoints();
			PointList points = new PointList();
			
			if (source.getVisibleLifeline().equals(target.getVisibleLifeline())) {
				points.addPoint(start);	
				int width = 0;
				for (Iterator<?> i = connection.getChildren().iterator(); i.hasNext();) {
					IFigure child = (IFigure) i.next();
					int localWidth = child.getPreferredSize().width;
					if (localWidth > width)
						width = localWidth;
				}
				points.addPoint(new Point(end.x+width, start.y));
				points.addPoint(new Point(end.x+width, end.y));
				
			} else {
				points.addPoint(start);
			}
			points.addPoint(end);
			connection.setPoints(points);
			needsRouting = Animation.isAnimating();
			} catch (Exception e) {e.printStackTrace();}
		}
			
	}
	
	
	/**
	 * @param item
	 * @param key
	 * @param parentFigure
	 */
	public MessageVisual(UMLItem item, String key) {
		super(item, key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.ConnectionVisualPart#createConnection()
	 */
	@Override
	protected Connection createConnection() {
		PolylineConnection conn = new AnimatedDeferredLayoutPolylineConnection();
		hover = new Label();
		conn.setToolTip(hover);
		conn.setForegroundColor(getMessage().getForeground());
		label = new Label();
		label.setFont(getWidget().getFont());
		label.setForegroundColor(getMessage().getTextForeground());
		label.setBorder(new MarginBorder(1,1,1,1));
		label.setIcon(getWidget().getImage());
		conn.add(label, new AligningBendpointLocator(conn, AligningBendpointLocator.BEGINNING, AligningBendpointLocator.ABOVE));
		///conn.addRoutingListener(RoutingAnimator.getDefault());
		needsRouting = true;
		return conn;
	}

	/**
	 * @return
	 */
	private Message getMessage() {
		return (Message) getWidget();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#refreshVisuals()
	 */
	@Override
	public void refreshVisuals() {
		label.setText(getMessage().getText());
		String toolTipText = getWidget().getTooltipText();
		if (toolTipText == null) {
			toolTipText = getWidget().getText();
		}
		hover.setText(toolTipText);
		label.setForegroundColor(getMessage().getTextForeground());
		((DeferredLayoutPolylineConnection)getConnection()).setDirty(true);
		if (getMessage().getTextBackground() != null) {
			label.setOpaque(true);
			label.setBackgroundColor(getMessage().getTextBackground());
		} else {
			label.setOpaque(false);
		}		
		boolean highlight = Boolean.TRUE.equals(getWidget().getData("pin")) ||
		getWidget().isHighlighted();
		Object layout = getWidget().getData(IWidgetProperties.LAYOUT);
		Color fg = getWidget().getForeground();
		if (fg != null) {
			((PolylineConnection)getConnection()).setForegroundColor(fg);
		}
		NodeVisualPart target = getTarget();
		if (layout == null) {
			label.setVisible(false);
			getConnection().setVisible(false);
		} else if (target == null){// || !target.isActive()){
			
			label.setVisible(false);
			label.setText("");
		} else if (!target.isActive()) {
			//see if there is enough room for the label
			
			Font font = getMessage().getFont();
			String text = getMessage().getText();
			PointList points = (PointList) layout;
			int width = getConnection().getBounds().width;
			if (points.size() >=2) {
				width = Math.abs(points.getFirstPoint().x - points.getLastPoint().x);
			}
			FontMetrics metrics = FigureUtilities.getFontMetrics(font);
			int availableCharacters = width/(metrics.getAverageCharWidth()+1);
			if (availableCharacters < 3) {
				label.setVisible(false);
			} else if (availableCharacters < text.length()) {
				label.setVisible(true);
				label.setText(text.substring(0, availableCharacters-3) + "...");
			} else {
				label.setVisible(true);
				label.setText(text);
			}
			if (!(getConnection().getConnectionRouter() instanceof CallConnectionRouter))
				getConnection().setConnectionRouter(new CallConnectionRouter());
			getConnection().setVisible(true);
		} else {
			getConnection().setVisible(true);
			if (highlight) {
				label.setFont(getMessage().getChart().getFont(SWT.BOLD));
				((Shape)getFigure()).setLineWidth(2);
				if (targetDec != null) {
					((Shape)targetDec).setLineWidth(2);
				}
				if (sourceDec != null) {
					((Shape)sourceDec).setLineWidth(2);
				}
				label.setVisible(true);
			} else {
				Font f = getMessage().getChart().getFont();
				label.setFont(f);
				Dimension size = label.getTextBounds().getSize();
				label.getParent().translateToAbsolute(size);
				((Shape)getFigure()).setLineWidth(1);

				if (size.height < 8) {
					label.setVisible(false);
				} else {
					label.setVisible(true);
				}
				if (!(getConnection().getConnectionRouter() instanceof CallConnectionRouter))
					getConnection().setConnectionRouter(new CallConnectionRouter());
				if (targetDec != null) {
					((Shape)targetDec).setLineWidth(1);
				}
				if (sourceDec != null) {
					((Shape)sourceDec).setLineWidth(1);
				}
			}
		}
		((PolylineConnection)getConnection()).setLineStyle(getMessage().getLineStyle());
		((DeferredLayoutPolylineConnection)getConnection()).setDirty(true);
	}
	
	/**
	 * 
	 */
	private void resetLabelImage() {
		if (localImage != null && !localImage.isDisposed()) {
			localImage.dispose();
			localImage = null;
		}
		Image icon = getWidget().getImage();
		if (icon == null) {
			label.setIcon(null);
		} else if (icon.getImageData().height > 10) {
			//scale the image.
			ImageData iconData = icon.getImageData();
			float scale = ((float)10)/iconData.height;
			int newWidth = (int)(scale*iconData.width);
			ImageData data = icon.getImageData().scaledTo(newWidth, 10);
			localImage = new Image(icon.getDevice(), data);
			label.setIcon(localImage);
		} else {
			label.setIcon(getWidget().getImage());
		}
	}

	private void updateDecorations() {
		updateSourceDecoration();
		updateTargetDecoration();
	}
	
	
	private RotatableDecoration getDecoration(int style) {
		int unmasked = ((style | Message.FILL_MASK) ^ Message.FILL_MASK);
		boolean filled = (style & Message.FILL_MASK) != 0;
		RotatableDecoration dec = null;
		switch (unmasked) {
		case Message.NONE:
			dec = null;
			break;
		case Message.CLOSED_ARROW:
			dec = new PolygonDecoration();
			((PolygonDecoration)dec).setFill(filled);
			((PolygonDecoration)dec).setScale(4,3);
			break;
		case Message.OPEN_ARROW:
			if (filled) {
				dec = new PolygonDecoration();
				((PolygonDecoration)dec).setScale(4,3);
				((PolygonDecoration)dec).setFill(filled);
			} else {
				dec = new PolylineDecoration();
				((PolylineDecoration)dec).setScale(4,3);
			}
			break;
		case Message.CIRCLE:
			dec = new CircleDecoration();
			((CircleDecoration)dec).setFill(filled);
			((CircleDecoration)dec).setSize(new Dimension(4,4));
			break;
		case Message.DIAMOND:
			dec = new PolygonDecoration();
			((PolygonDecoration)dec).setPoints(DIAMOND_TIP);
			((PolygonDecoration)dec).setFill(filled);
			((PolygonDecoration)dec).setScale(4,3);
			break;
		case Message.SQUARE:
			dec = new PolygonDecoration();
			((PolygonDecoration)dec).setPoints(SQUARE_TIP);
			((PolygonDecoration)dec).setFill(filled);
			((PolygonDecoration)dec).setScale(3,3);
			break;
		}
		return dec;
	}
	/**
	 * 
	 */
	private void updateSourceDecoration() {
		sourceDec = getDecoration(getMessage().getSourceStyle());
		((PolylineConnection)getConnection()).setSourceDecoration(sourceDec);
	}

	/**
	 * 
	 */
	private void updateTargetDecoration() {
		targetDec = getDecoration(getMessage().getTargetStyle());
		((PolylineConnection)getConnection()).setTargetDecoration(targetDec);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.ConnectionVisualPart#activate()
	 */
	@Override
	public void activate() {
		super.activate();
		if (this.mouseOverInteraction == null) {
			this.mouseOverInteraction = new ActivationHoverInteraction();
		}
		updateDecorations();
		resetLabelImage();
		mouseOverInteraction.hookInteraction(this);
		getMessage().addPropertyChangeListener(this);
		//getMessage().getChart().addPropertyChangeListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.ConnectionVisualPart#deactivate()
	 */
	@Override
	public void deactivate() {
		if (!isActive()) return;
		super.deactivate();
		
		mouseOverInteraction.unhookInteraction();
		if (localImage != null && !localImage.isDisposed()) {
			localImage.dispose();
		}
		getMessage().removePropertyChangeListener(this);
		if (!getWidget().isDisposed())
			getWidget().setData(IWidgetProperties.LAYOUT, null);
		//getMessage().getChart().removePropertyChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.widgets.PropertyChangeListener#propertyChanged(java.lang.Object, java.lang.String, java.lang.Object, java.lang.Object)
	 */
	public void propertyChanged(Object source, String property, Object oldValue, Object newValue) {
		if (IWidgetProperties.LAYOUT.equals(property)) {
			if (getChartVisuals().refreshing) return; //early return
			needsRouting = true;
			((DeferredLayoutPolylineConnection)getConnection()).setDirty(true);
			refreshVisuals();
		} else if ("pin".equals(property) || IWidgetProperties.HIGHLIGHT.equals(property)) {
			needsRouting = true;
			((DeferredLayoutPolylineConnection)getConnection()).setDirty(true);
			refreshVisuals();
		} else if (IWidgetProperties.TOOLTIP.equals(property)) {
			Label tooltipFigure = (Label) getFigure().getToolTip();
			String tooltipText = getWidget().getTooltipText();
			if (tooltipText == null) {
				tooltipText = getWidget().getText();
			}
			tooltipFigure.setText(tooltipText);
		}  else if (IWidgetProperties.DECORATION.equals(property)) {
			updateDecorations();
		} else if (IWidgetProperties.IMAGE.equals(property)) {
			resetLabelImage();
		} else if (source == getWidget()) {
			if (IWidgetProperties.BACKGROUND_COLOR.equals(property) ||
				IWidgetProperties.FOREGROUND_COLOR.equals(property) ||
				IWidgetProperties.TEXT.equals(property) ||
				IWidgetProperties.TEXT_BACKGROUND.equals(property) ||
				IWidgetProperties.TEXT_FOREGROUND.equals(property))
			refreshVisuals();
		}
	}
	
		
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.ConnectionVisualPart#nodePropertyChanged(org.eclipse.zest.custom.sequence.visuals.NodeVisualPart, java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void nodePropertyChanged(NodeVisualPart nodeVisualPart,
			String property, Object oldValue, Object newValue) {
		if (IWidgetProperties.LAYOUT.equals(property) || IWidgetProperties.EXPANDED.equals(property)) {
			needsRouting = true;
			((DeferredLayoutPolylineConnection)getConnection()).setDirty(true);
			getConnection().invalidate();
		}
	}
}
