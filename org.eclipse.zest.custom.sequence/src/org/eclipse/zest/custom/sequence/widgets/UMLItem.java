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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Item;
import org.eclipse.zest.custom.sequence.events.internal.ListenerList;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * Parent for all items in a sequence chart
 * @author Del Myers
 */

public abstract class UMLItem extends Item implements Comparable<UMLItem> {

	ListenerList listeners;
	
	
	private UMLChart chart;
	
	//private boolean visible;
	
	private boolean highlight;
	
	/**
	 * The id for an item in a chart.
	 */
	private int id;

	private boolean enabled;

	private boolean hidden;

	private String tooltip;

	/**
	 * Creates a new item on the given chart.
	 * @param parent
	 * @param style
	 */
	protected UMLItem(UMLChart parent) {
		super(parent, SWT.NONE);
		this.chart = parent;
		listeners = new ListenerList(ListenerList.EQUALITY);
		addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e) {
				UMLItem.this.widgetDisposed(e);				
			}
		});
		//visible = true;
		hidden = false;
		chart.createItem(this);
		highlight = false;
	}
	
	/**
	 * Returns the text used for the tooltip in this item.
	 */
	public String getTooltipText() {
		checkWidget();
		return tooltip;
	}
	
	/**
	 * Sets the tooltip for this item.
	 * @param tooltip the new tooltip text.
	 */
	public void setTooltipText(String tooltip) {
		checkWidget();
		String oldTip = getTooltipText();
		this.tooltip = tooltip;
		firePropertyChange(IWidgetProperties.TOOLTIP, oldTip, tooltip);
	}
	
	/**
	 * @return the visible
	 */
	public final boolean isVisible() {
		return chart.isVisible(this);
	}
	
	
	/**
	 * Handles disposal of this widget. Overriders must call super on this method.
	 * @param e the dispose event.
	 */
	protected void widgetDisposed(DisposeEvent e) {
		listeners.clear();
		chart.deleteItem(this);
	}
	
	/**
	 * Returns the parent sequence chart.
	 * @return the chart
	 */
	public UMLChart getChart() {
		return chart;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(UMLItem o) {
		if (!(o instanceof UMLItem)) return 1;
		if (((UMLItem)o).getChart() != getChart()) {
			SWT.error(SWT.ERROR_INVALID_PARENT);
		}
		return this.id - ((UMLItem)o).id;
	}
	
	public boolean equals(Object o) {
		if (o == null) return false;
		if (!o.getClass().equals(this.getClass())) return false;
		boolean result = 0 == compareTo((UMLItem)o);
		return result;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.id + chart.hashCode();
	}
	
	/**
	 * Adds the given property change listener to the list of listeners if it hasn't already been added.
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (isDisposed()) 
			return;
		listeners.add(listener);
	}
	
	/**
	 * Removes the given listener from the list of listeners.
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Fires a property change for the given property.
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	void firePropertyChange(String property, Object oldValue, Object newValue) {
		if (!shouldFire(oldValue, newValue)) return;
		for (Object listener : listeners.getListeners()) {
			((PropertyChangeListener)listener).propertyChanged(this, property, oldValue, newValue);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object data) {
		Object old = getData();
		super.setData(data);
		firePropertyChange(IWidgetProperties.DATA, old, data);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#setData(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setData(String key, Object value) {
		Object old = getData(key);
		super.setData(key, value);
		firePropertyChange(key, old, value);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Item#setText(java.lang.String)
	 */
	@Override
	public void setText(String string) {
		String old = getText();
		super.setText(string);
		firePropertyChange(IWidgetProperties.TEXT, old, string);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Item#setImage(org.eclipse.swt.graphics.Image)
	 */
	@Override
	public void setImage(Image image) {
		Image old = getImage();
		super.setImage(image);
		firePropertyChange(IWidgetProperties.IMAGE, old, image);
	}
	
	boolean shouldFire(Object oldValue, Object newValue) {
		if (oldValue != null || newValue != null) {
			return (oldValue != null) ? !oldValue.equals(newValue) : !newValue.equals(oldValue);
		}
		return false;
	}
	
	/**
	 * Ensures that the given array, and all of its elements, are not null.
	 * @param array
	 */
	protected static void checkNull(Object[] array) {
		if (array == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		for (Object o : array) {
			if (o == null) {
				SWT.error(SWT.ERROR_NULL_ARGUMENT);
			}
		}
	}
	
	void setid(int id) {
		this.id = id;
	}
	
	int getid() {
		return id;
	}
	
	/**
	 * Returns the font used for rendering this item. By default, just uses the chart's font.
	 * @return the font used for rendering this item.
	 */ 
	public Font getFont() {
		return getChart().getFont();
	}
	
	/**
	 * Returns the forground color used for rendering this item. By default, just uses the chart's
	 * color.
	 * @return the forground color used for rendering this item.
	 */
	public Color getForeground() {
		return getChart().getForeground();
	}
	
	/**
	 * Returns the background color used for rendering this item. by default, just used the chart's
	 * background color.
	 * @return the background color used for rendering this item.
	 */
	public Color getBackground() {
		return getChart().getBackground();
	}

	/**
	 * @param b
	 */
	public void setHighlight(boolean highlight) {
		boolean old = this.highlight;
		this.highlight = highlight;		
		firePropertyChange(IWidgetProperties.HIGHLIGHT, old, highlight);
	}
	
	public boolean isHighlighted() {
		return highlight;
	}
	
	public void setEnabled(boolean enabled) {
		boolean old = this.enabled;
		this.enabled = enabled;
		firePropertyChange(IWidgetProperties.ENABLED, old, enabled);
	}
	
	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Hides this item. A hidden item is different from an invisible item in that 
	 * an item becomes hidden because of a "parent" has collapsed in some fasion.
	 * Hidden items are not necessarily invisible.
	 *
	 */
	protected void hide() {
		checkWidget();
		if (isHidden()) return;
		getChart().markDirty();
		boolean hidden = this.hidden;
		this.hidden = true;
		firePropertyChange(IWidgetProperties.HIDDEN, hidden, this.hidden);
	}
	
	/**
	 * Unhides this activation due to one of its parents being expanded. The activation may
	 * still be invisible.
	 *
	 */
	protected void open() {
		if (!isHidden()) return;
		this.hidden = false;
		getChart().markDirty();
		firePropertyChange(IWidgetProperties.HIDDEN, true, false);
	}
	
	/**
	 * @return the hidden
	 */
	public boolean isHidden() {
		return hidden;
	}
	
}
