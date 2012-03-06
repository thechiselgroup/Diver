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
import java.util.TreeMap;

import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * Represents the visuals of a draw2d widget.
 * @author Del Myers
 */

public abstract class WidgetVisualPart {
	
	private UMLItem item;
	private String key;
	private IFigure figure;
	private WidgetDisposeListener disposeListener;
	private boolean active;
	
	
	//experimental: fields to try and speed up refreshing
//	private WidgetVisualPart leftSibling;
//	private WidgetVisualPart rightSibling;
	/**
	 * Used to decorate the visual with certain properties to check different states.
	 * Use decorations on the visuals instead of on the widget when you don't want listeners
	 * to be notified of property changes due to setData() on the widget.
	 */
	private TreeMap<String, Object> decorations;
		
	private final class WidgetDisposeListener implements DisposeListener {
		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
		 */
		public void widgetDisposed(DisposeEvent e) {
			deactivate();
		}
	}

	public WidgetVisualPart(UMLItem item, String key) {
		this.item = item;
		this.active = false;
		this.key = key;
		this.disposeListener = new WidgetDisposeListener();
		this.decorations = new TreeMap<String, Object>();
		
	}
	
	/**
	 * Creates all of the figures for this visual. It is possible that the visual may install multiple
	 * figures in that case, then getFigures() will return a list of all the figures. This returns
	 * the primary figure that is created.
	 * @return the primary figure for this visual.
	 */
	public abstract IFigure createFigures();
	
	/**
	 * Returns a list of all of the figures created by this visual that have to be registered and
	 * installed. By default, the visual only has one primary figure. This method will return
	 * a list containing that figure.
	 * @return
	 */
	public List<IFigure> getFigures() {
		ArrayList<IFigure> list = new ArrayList<IFigure>(1);
		list.add(getFigure());
		return list;
	}
	
	/**
	 * Returns the main figure for this visual. 
	 * @return
	 */
	public IFigure getFigure() {
		if (this.figure == null) {
			this.figure = createFigures();
		}
		return this.figure;
	}
		
	public void activate() {
		if (isActive()) return;
		if (item.isDisposed()) return;
		item.setData(key, this);
		item.addDisposeListener(disposeListener);
		registerVisuals();
		//installFigures();
		active = true;
		item.setData(IWidgetProperties.ACTIVE, true);
		//refreshVisuals();
		
	}
	
	public void deactivate() {
		if (!isActive()) return;
		if (!item.isDisposed()) {
			item.removeDisposeListener(disposeListener);
		}
		deregisterVisuals();
		uninstallFigures();
		active = false;
		if (!item.isDisposed()) {
			item.setData(IWidgetProperties.ACTIVE, null);
		}
	}
	
	protected void deregisterVisuals() {
		MessageBasedSequenceVisuals visuals = getChartVisuals();
		if (visuals != null) {
			visuals.deregister(this);
		}
	}
	
	protected void registerVisuals() {
		MessageBasedSequenceVisuals visuals = getChartVisuals();
		if (visuals != null) {
			visuals.register(this);
		}
	}
	
	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}
	
	public UMLItem getWidget() {
		return item;
	}
	
	public abstract void refreshVisuals();
	
	public IFigure getLayer(Object key) {
		return getChartVisuals().getLayer(key);
	}
	
	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
	
	
	public MessageBasedSequenceVisuals getChartVisuals() {
		return (MessageBasedSequenceVisuals) getWidget().getChart().getData(MessageBasedSequenceVisuals.VISUAL_KEY);
	}
	
		
	/**
	 * Installs the figures for this visual part into their parent. The layers for containment of
	 * the figures can be retrieved using getLayer(key) where key is usually one of the keys defined
	 * in LayerConstants. By default, only the primary figure is installed.
	 */
	protected void installFigures() {
		IFigure layer = getLayer(LayerConstants.PRIMARY_LAYER);
		if (layer != null) {
			layer.add(getFigure());
		}
	}


	/**
	 * Removes the figures created by this visual from their parents. By default, only the primary
	 * figure is uninstalled.
	 */
	protected void uninstallFigures() {
		IFigure figure = getFigure();
		if (figure != null && figure.getParent() != null) {
			figure.getParent().remove(figure);
		}
	}
	
	/**
	 * Marks this visual with the given data, so that it can be used by other applications to
	 * check if the visual is in a particular state without the overhead of using the setData() method
	 * in the widget contained in this visual.
	 * @param key the key to store the data under.
	 * @param value the value to store there.
	 */
	public void decorate(String key, Object value) {
		decorations.put(key, value);
	}
	
	/**
	 * Returns the decoration stored under the given key, or null if it is not present.
	 * @param key the key to get the decoration from.
	 * @return the decoration, or null.
	 */
	public Object getDecoration(String key) {
		return decorations.get(key);
	}
	
	/**
	 * Removes the decoration associated with the given key, and returns what was previously stored
	 * or null, if it wasn't present.
	 * @param key the key to clear.
	 * @return the value previously stored with the key, or null.
	 */
	public Object clearDecoration(String key) {
		return decorations.remove(key);
	}
	
	
}
