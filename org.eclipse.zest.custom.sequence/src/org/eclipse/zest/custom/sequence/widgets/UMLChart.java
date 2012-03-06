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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.zest.custom.sequence.events.internal.ListenerList;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * A composite that is used to display UML charts. 
 * @author Del Myers
 */
public abstract class UMLChart extends Composite {
//	an id that is used for quick sorting
	private int nextId = 0;
	
	TreeSet<UMLItem> items;
	
	
	//registers styled fonts for universal use.
	private TreeMap<Integer, Font> fontRegistry;
	
	//holds fonts that are no longer of use.
	private List<Font> deadFonts;
	
	private ListenerList listeners;
	
	
	/**
	 * Indicates that some sort of structural change has occurred and the viewer must refresh itself.
	 */
	boolean dirty;


	private UMLItem[] selectedItems;

	private Composite contents;


	/**
	 * @param parent
	 * @param style
	 */
	public UMLChart(Composite parent, int style) {
		super(parent, style);
		this.selectedItems = new UMLItem[0];
		fontRegistry = new TreeMap<Integer, Font>();
		deadFonts = new LinkedList<Font>();
		nextId = 0;
		this.items = new TreeSet<UMLItem>(new Comparator<UMLItem>(){
			public int compare(UMLItem o1, UMLItem o2) {
				return o1.getid() - o2.getid();
			}
		});
		listeners = new ListenerList();
		dirty = false;
		this.contents = createContents(this, style);
		setLayout(new FillLayout());
		addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e) {
				UMLChart.this.widgetDisposed(e);
			}
		});
	}
	
	
	/**
	 * Creates the contents for this chart. Delegated to subclasses.
	 * @param parent the parent of the contents.
	 * @return the composite that is used to display the contents.
	 */
	protected abstract Composite createContents(Composite parent, int style);
	/**
	 * @param e
	 */
	protected void widgetDisposed(DisposeEvent e) {
		UMLItem[] items = getItems();
		//clear the items from the list so that we don't waste time deleting them
		//on the dispose.
		this.items.clear();
		for (UMLItem item : items) {
			item.dispose();
		}
	}
	
	/**
	 * Gets the next item id for an item placed on this chart.
	 * @return
	 */
	int getNextId() {
		return nextId++;
	}
	
	void createItem(UMLItem item) {
		item.setid(getNextId());
		this.items.add(item);
		this.dirty = true;
		firePropertyChange(IWidgetProperties.ITEM, null, item);
	}
	
	void deleteItem(UMLItem item) {
		if (items.size() == 0) {
			//don't waste time disposing.
			return;
		}
		this.items.remove(item);
		//check for the item in the selection and remove it.
		UMLItem[] currentSelection = getSelection();
		int i = 0;
		for (; i < currentSelection.length; i++) {
			if (currentSelection[i] == item) {
				break;
			}
		}
		if (i < currentSelection.length) {
			UMLItem[] newSelection = new UMLItem[currentSelection.length-1];
			if (i > 0) {
				System.arraycopy(currentSelection, 0, newSelection, 0, i-1);
			}
			if (i < newSelection.length) {
				System.arraycopy(currentSelection, i+1, newSelection, i, newSelection.length-i);
			}
			setSelection(newSelection);
		}
		this.dirty = true;
		firePropertyChange(IWidgetProperties.ITEM, item, null);
	}
	
	/**
	 * Convenience method for disposing all of the items on this chart and refreshing.
	 * 
	 *
	 */
	public void clearChart() {
		for (UMLItem item : getItems()) {
			item.dispose();
		}
		dirty = true;
		layout();
	}
	
	/**
	 * @return the items
	 */
	public UMLItem[] getItems() {
		return items.toArray(new UMLItem[items.size()]);
	}
	
	
	/**
	 * Returns the number of items in the chart.
	 * @return the number of items in the chart.
	 */
	public int itemCount() {
		return items.size();
	}


	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#setBackground(org.eclipse.swt.graphics.Color)
	 */
	@Override
	public void setBackground(Color color) {
		contents.setBackground(color);
	}
	
	/**
	 * Adds the given property change listener to the list of listeners if it hasn't already been added.
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
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
	 * Sets the dirty state of this chart to true. This will ensure that when the chart is next
	 * layed-out, it will be structurally refreshed.
	 *
	 */
	protected void markDirty() {
		this.dirty = true;
	}
	
	protected boolean isDirty() {
		return this.dirty;
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
	
	boolean shouldFire(Object oldValue, Object newValue) {
		if (oldValue != null || newValue != null) {
			return (oldValue != null) ? !oldValue.equals(newValue) : !newValue.equals(oldValue);
		}
		return false;
	}
	
	/**
	 * Lays out the chart. Because a chart may have a lot of children on it, an explicit refresh() or
	 * layout() must be called after a child is added or disposed in order to see the results.
	 */
	@Override
	public void layout(boolean changed) {
		if (changed) {
			refresh();
		}
		performLayout();
		super.layout(changed);
	}
	
	
	/**
	 * Refreshes the visuals of this chart.
	 */
	public abstract void refresh();
	
	
	/**
	 * Returns a universal font with the given style. Fonts will be disposed automatically. 
	 */
	public Font getFont(int style) {
		int mask = SWT.BOLD | SWT.ITALIC;
		style &= mask;
		if ((style ^ SWT.NORMAL) == 0) {
			return getFont();
		}
		Font f = fontRegistry.get(style);
		if (f == null) {
			FontData[] data = getFont().getFontData();
			data[0].setStyle(style);
			f = new Font(getDisplay(), data[0]);
			fontRegistry.put(style, f);
		}
		
		return f;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Canvas#setFont(org.eclipse.swt.graphics.Font)
	 */
	@Override
	public void setFont(Font font) {
		for (Font f : fontRegistry.values()) {
			//store the fonts until disposal just in case a child is still referencing that font.
			deadFonts.add(f);
			fontRegistry.clear();
		}
		Font old = getFont();
		super.setFont(font);
		firePropertyChange("fnt", old, font);
	}
	
	public void setSelection(UMLItem[] items) {
		checkWidget();
		if (items == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		for (UMLItem item : items) {
			if (item == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
			if (item.getChart()!=this) SWT.error(SWT.ERROR_INVALID_PARENT);
		}
		this.selectedItems = new UMLItem[items.length];
		System.arraycopy(items, 0, this.selectedItems, 0, items.length);
	}
	
	/**
	 * Sets the selection and fires an event.
	 * @param items
	 */
	protected void internalSetSelection(UMLItem[] items) {
		HashSet<UMLItem> oldSelection = new HashSet<UMLItem>(Arrays.asList(this.selectedItems));
		setSelection(items);
//		send an event for each new selection
		if (oldSelection.size() != 0 && items.length == 0) {
			//clear the selection
			Event event = new Event();
			event.widget = this;
			event.item = null;
			int type = SWT.Selection;
			event.type = type;
			notifyListeners(type, event);
		} else {
			for (UMLItem item : items) {
				Event event = new Event();
				event.widget = this;
				event.item = item;
				int type = SWT.Selection;
				if (oldSelection.contains(item)) {
					type = SWT.DefaultSelection;
				}
				event.type = type;
				notifyListeners(type, event);
			}
		}
		
	}
	
	protected void internalUpdateSelection(UMLItem[] items) {
		HashSet<UMLItem> oldSelection = new HashSet<UMLItem>(Arrays.asList(this.selectedItems));
		setSelection(items);
		for (UMLItem item : items) {
			if (!oldSelection.contains(item)) {
				Event event = new Event();
				event.widget = this;
				event.item = item;
				int type = SWT.Selection;
				event.type = type;
				notifyListeners(type, event);
			}
		}
	}
	
	protected void internalReselect(UMLItem item) {
		HashSet<UMLItem> oldSelection = new HashSet<UMLItem>(Arrays.asList(this.selectedItems));
		if (oldSelection.contains(item)) {
			Event event = new Event();
			event.widget = this;
			event.item = item;
			int type = SWT.DefaultSelection;
			event.type = type;
			notifyListeners(type, event);
		}
	}
	
	public UMLItem[] getSelection() {
		return selectedItems;
	}
	
	public void addSelectionListener(SelectionListener listener) {
		checkWidget();
		if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		TypedListener typedListener = new TypedListener (listener);
		addListener (SWT.Selection,typedListener);
	}
	
	public void removeSelectionListener(SelectionListener listener) {
		removeListener(SWT.Selection, listener);
	}
	
	protected abstract void performLayout();
	
	/**
	 * Whether an item is visible or not is decided by the chart.
	 * @param item
	 * @return
	 */
	abstract boolean isVisible(UMLItem item);
	
}
