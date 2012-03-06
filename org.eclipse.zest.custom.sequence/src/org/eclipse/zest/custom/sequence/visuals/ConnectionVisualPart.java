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

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.IFigure;
import org.eclipse.zest.custom.sequence.widgets.PropertyChangeListener;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;

/**
 * A widget visual part representing a connection.
 * @author Del Myers
 */

public abstract class ConnectionVisualPart extends WidgetVisualPart {
	private class NodeChangeListener implements PropertyChangeListener {

		/* (non-Javadoc)
		 * @see org.eclipse.zest.custom.sequence.widgets.PropertyChangeListener#propertyChanged(java.lang.Object, java.lang.String, java.lang.Object, java.lang.Object)
		 */
		public void propertyChanged(Object sourceObject, String property,
				Object oldValue, Object newValue) {
			boolean sourceNode = (source != null && sourceObject.equals(source.getWidget()));
			nodePropertyChanged((sourceNode) ? source : target, property, oldValue, newValue);
		}
		
	}

	private NodeVisualPart source;
	private NodeVisualPart target;
	private NodeChangeListener nodeListener;

	/**
	 * @param item
	 * @param key
	 * @param parentFigure
	 */
	public ConnectionVisualPart(UMLItem item, String key) {
		super(item, key);
		nodeListener = new NodeChangeListener();
	}
	
	/**
	 * Notifies the visual that a property has changed on the widget for the
	 * given NodeVisualPart attached to this connection visual part. Clients
	 * may override, does nothing by default.
	 * 
	 * @param nodeVisualPart
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	protected void nodePropertyChanged(NodeVisualPart nodeVisualPart,
			String property, Object oldValue, Object newValue) {
	}

	public Connection getConnection() {
		return (Connection)getFigure();
	}
	
	/**
	 * Creates the connection.
	 * @return the newly created connection.
	 */
	protected abstract Connection createConnection();

	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#createFigure()
	 */
	@Override
	public final IFigure createFigures() {
		return createConnection();
	}
	
	public NodeVisualPart getSource() {
		return this.source;
	}
	
	public NodeVisualPart getTarget() {
		return this.target;
	}

	/**
	 * @param part
	 */
	public void setSource(NodeVisualPart source) {
		if (source == this.source) return;
		NodeVisualPart oldSource = this.source;
		this.source = source;
		if (oldSource != null) {
			oldSource.removeSourceConnection(this);
			oldSource.getWidget().removePropertyChangeListener(nodeListener);
		}
		
		if (source != null) {
			getConnection().setSourceAnchor(source.getSourceAnchor(this));
			source.getWidget().addPropertyChangeListener(nodeListener);
		}
		setVisibility(getWidget().isVisible());
		getConnection().invalidate();
	}
	
	public void setTarget(NodeVisualPart target) {
		if (target == this.target) return;
		NodeVisualPart oldTarget = this.target;
		this.target = target;
		if (oldTarget != null) {
			oldTarget.removeTargetConnection(this);
			oldTarget.getWidget().removePropertyChangeListener(nodeListener);
		}
		
		if (target != null) {
			getConnection().setTargetAnchor(target.getTargetAnchor(this));
			target.getWidget().addPropertyChangeListener(nodeListener);
		}
		setVisibility(getWidget().isVisible());
		getConnection().invalidate();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#activate()
	 */
	@Override
	public void activate() {
		super.activate();
		setVisibility(true);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart#deactivate()
	 */
	@Override
	public void deactivate() {
		super.deactivate();
		setSource(null);
		setTarget(null);
	}
	
	protected void setVisibility(boolean visible) {
		IFigure figure = getFigure();
		IFigure parent = figure.getParent();
		if (parent == null) {
			parent = getLayer(LayerConstants.CONNECTION_LAYER);
		}
		visible = visible && getSource() != null && getTarget() != null;
		figure.setVisible(visible);
		if (visible) {
			if (figure.getParent() != parent) {
				if (figure.getParent() != null) {
					figure.getParent().remove(getFigure());
				}
				parent.add(figure);
			}
		} else {
			if (figure.getParent() != null) figure.getParent().remove(figure);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#installFigures()
	 */
	@Override
	protected void installFigures() {
		IFigure layer = getLayer(LayerConstants.CONNECTION_LAYER);
		if (layer != null) {
			layer.add(getFigure());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#uninstallFigures()
	 */
	@Override
	protected void uninstallFigures() {
		IFigure layer = getFigure().getParent();
		if (layer != null) {
			layer.remove(getFigure());
		}
	}
}
