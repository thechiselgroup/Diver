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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * An object that holds a set of activations. The activations on a lifeline are not ordered by default.
 * Clients can't expect that they are returned in the order that they appear visually.
 * @author Del Myers
 *
 */
public class Lifeline extends UMLTextColoredItem implements IExpandableItem {
	/**
	 * Draw as a rectangle (default style).
	 */
	public static final int CLASS = 0;
	/**
	 * Draw as a 
	 */
	public static final int COLLECTION = 1;
	/**
	 * Draw as a stick-figure.
	 */
	public static final int ACTOR = 2;
	/**
	 * Draw as a boundary.
	 */
	public static final int BOUNDARY = 3;
	/**
	 * Draw as a control (a circle with an arrow on the circumference.
	 */
	public static final int CONTROL = 4;
	/**
	 * Draw as an entity (an underlined circle).
	 */
	public static final int ENTITY = 5;
	/**
	 * Draw as a data store (a cylinder).
	 */
	public static final int DATA_STORE = 6;

	/**
	 * Draw as a package
	 */
	public static final int PACKAGE = 7;
	private LinkedList<Activation> activations;
	/**
	 * An array of child lifelines for this lifeline.
	 */
	private Lifeline[] children;
	private Lifeline parent;
	private int targetStyle;
	private int hiddenCount;
	private PropertyChangeListener hideListener;
	/**
	 * An ordered list of the visible activations.
	 */
	private Activation[] orderedActivations;
	private String stereotype;
	private boolean expanded;
	
	public Lifeline(UMLChart parent) {
		super(parent);
		activations = new LinkedList<Activation>();
		this.targetStyle = CLASS;
		this.hideListener = new PropertyChangeListener() {
			public void propertyChanged(Object source, String property,
					Object oldValue, Object newValue) {
				boolean hidden = hiddenCount == activations.size();
				if (IWidgetProperties.HIDDEN.equals(property)) {
					if (Boolean.TRUE.equals(newValue)) {
						hiddenCount++;
					} else {
						hiddenCount--;
					}
					orderedActivations = null;
				} else if (IWidgetProperties.LAYOUT.equals(property)) {
					orderedActivations = null;
				}
				firePropertyChange(IWidgetProperties.HIDDEN, hidden, hiddenCount == activations.size());
			}		
		};
		this.expanded = true;
	}
	
	/**
	 * Sets the style drawing style of the head of this lifeline may be one of
	 * ACTOR, BOUNDARY, COLLECTION, CONTROL, CLASS, DATA_STORE, ENTITY, or PACKAGE.
	 * 
	 * @param classStyle the new class style.
	 */
	public void setClassStyle(int targetStyle) {
		checkWidget();
		int oldStyle = this.targetStyle;
		this.targetStyle = targetStyle;
		firePropertyChange(IWidgetProperties.OBJECT_DRAWING_STYLE, oldStyle, targetStyle);
	}
	
	/**
	 * @return the targetStyle
	 */
	public int getTargetStyle() {
		checkWidget();
		return targetStyle;
	}
	
	/**
	 * Adds the given activation to the list of activations.
	 * @param a
	 */
	void addActivation(Activation a) {
		checkWidget();
		if (a.getLifeline() == this) {
			return;
		}
		boolean hiddenState = hiddenCount == activations.size();
		activations.add(a);
		if (a.isHidden()) {
			hiddenCount++;
		}
		a.addPropertyChangeListener(hideListener);
		orderedActivations = null;
		firePropertyChange(IWidgetProperties.HIDDEN, hiddenState, hiddenCount == activations.size());
	}
	
	void removeActivation(Activation a) {
		checkWidget();
		if (a.getLifeline() != this) {
			return;
		}
		boolean hiddenState = hiddenCount == activations.size();
		activations.remove(a);
		if (a.isHidden()) {
			hiddenCount--;
		}
		a.addPropertyChangeListener(hideListener);
		orderedActivations = null;
		firePropertyChange(IWidgetProperties.HIDDEN, hiddenState, hiddenCount == activations.size());
	}
	
	/**
	 * The hidden state of a lifeline is not controlled by the lifeline itself, but
	 * by the hidden state of all of the activations on the lifeline.
	 */
	public boolean isHidden() {
		checkWidget();
		return hiddenCount == activations.size();
	}
	
	/**
	 * This method does nothing because the hidden state is dependent on the hidden state of the
	 * activations in this life line.
	 */
	protected void hide() {
	}
	
	/**
	 * Returns the all of the activations on this lifeline, and all of its children. The order is not guaranteed.
	 * @return the activations on this lifeline. The order is not guaranteed.
	 */
	public Activation[] getAllActivations() {
		checkWidget();
		List<Activation> allActivations = collectAllActivations();
		return allActivations.toArray(new Activation[allActivations.size()]);
	}
	
	private List<Activation> collectAllActivations() {
		LinkedList<Activation> allActivations = new LinkedList<Activation>();
		allActivations.addAll(activations);
		if (children != null) { 
			for (Lifeline child : children) {
				allActivations.addAll(child.collectAllActivations());
			}
		}
		return allActivations;
	}
	/**
	 * Returns the activations that are found on this lifeline only (not on any of
	 * its children).
	 * @return the activations that are found on this lifeline.
	 */
	public Activation[] getActivations() {
		checkWidget();
		return activations.toArray(new Activation[activations.size()]);
	}
	
	/**
	 * Returns a list of the non-hidden activations in the order that they appear on the screen
	 * from top to bottom. Returns all activations that will be visible on this lifeline,
	 * that is its activations and all of its children's activations.
	 * @return  a list of the non-hidden activations in the order that they appear on the screen
	 * from top to bottom.
	 */
	public Activation[] getOrderedActivations() {
		checkWidget();
		if (orderedActivations == null) {
			TreeSet<Activation> orderedSet = new TreeSet<Activation>(new Comparator<Activation>(){
				public int compare(Activation o1,Activation o2){
					Rectangle layout1 = (Rectangle) o1.getData(IWidgetProperties.LAYOUT);
					Rectangle layout2 = (Rectangle) o2.getData(IWidgetProperties.LAYOUT);
					if (layout1 == null && layout2 == null) {
						return 0;
					}
					if (layout2 == null) {
						return -1;
					}
					if (layout1 == null) {
						return 1;
					}
					return layout1.getTop().y - layout2.getTop().y;
				}
			});
			for (Activation a :activations) {
				if (a.isVisible() && !a.isHidden()) {
					orderedSet.add(a);
				}
			}
			orderedActivations = orderedSet.toArray(new Activation[orderedSet.size()]);
		}
		return orderedActivations;
	}

	
	/**
	 * Sets the parent of this lifeline to the given parent.
	 * @param parent
	 */
	private void setParent(Lifeline parent) {
		//make sure that it won't introduce a cycle
		if (parent == this.parent) {
			return;
		}
		Lifeline currentParent = parent;
		while (currentParent != null && !currentParent.isDisposed()) {
			if (currentParent == this) {
				throw new IllegalArgumentException("parent " + parent + " introduces a cycle.");
			}
			if (currentParent.isDisposed()) {
				throw new IllegalArgumentException("parent " + parent + " creates a broken hierarchy");
			}
			currentParent = currentParent.getParent();
		}
		if (this.parent != null && !this.parent.isDisposed()) {
			this.parent.removeChild(this);
		}
		this.parent = parent;
	}
	
	

	/**
	 * Removes the given lifeline from the list of children for this lifeline.
	 * @param lifeline
	 */
	private void removeChild(Lifeline lifeline) {
		checkWidget();
		if (children != null && children.length > 0) {
			Lifeline[] newchildren = new Lifeline[children.length-1];
			//int i=0, j=0;
			for (int i=0, j =0; i < children.length; i++) {
				if (j >= newchildren.length) {
					//the lifeline wasn't found. just return.
					return;
				}
				if (children[i] != lifeline) {
					newchildren[j] = children[i];
					j++;
				}
			}
			children = newchildren;
			invalidate();
			firePropertyChange(IWidgetProperties.CHILD, lifeline, null);
			getChart().markDirty();
		}
	}
	
	/**
	 * Groups the given lifeline under this lifeline.
	 * @param lifeline
	 */
	public void addChild(Lifeline lifeline) {
		checkWidget();
		if (lifeline == null) {
			return;
		}
		if (lifeline.parent == this) {
			//don't add it if it is already parented here.
			return;
		}
		try {
			lifeline.setParent(this);
		} catch (IllegalArgumentException e) {
			//just don't add it if this is an illegal parent.
			return;
		}
		if (children == null) {
			children = new Lifeline[] {lifeline};
		} else {
			Lifeline[] newchildren = new Lifeline[children.length + 1];
			System.arraycopy(children, 0, newchildren, 0, children.length);
			newchildren[children.length] = lifeline;
			children = newchildren;
		}
		invalidate();
		firePropertyChange(IWidgetProperties.CHILD, null, lifeline);
		getChart().markDirty();
	}
	
	/**
	 * Walk up the parent hierarchy, resetting the ordered activations so that
	 * the activations visible on this lifeline will be reset.
	 */
	private void invalidate() {
		this.orderedActivations = null;
		if (parent != null) {
			parent.invalidate();
		}
	}

	/**
	 * @return the stereotype text for the head of the lifeline.
	 */
	public String getStereoType() {
		return stereotype;
	}
	
	/**
	 * Sets the stereotype for the object
	 * @param stereotype the new stereotype.
	 */
	public void setStereotype(String stereotype) {
		checkWidget();
		String oldStereotype = this.stereotype;
		this.stereotype = stereotype;
		firePropertyChange(IWidgetProperties.STEREOTYPE, oldStereotype, stereotype);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.IExpandableItem#isExpanded()
	 */
	public boolean isExpanded() {
		if (this.children == null || this.children.length == 0) {
			return false;
		}
		return expanded;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.IExpandableItem#setExpanded(boolean)
	 */
	public void setExpanded(boolean expanded) {
		if (!hasChildren() || expanded == this.expanded) {
			return;
		}
		this.expanded = expanded;
		getChart().markDirty();
		firePropertyChange(IWidgetProperties.EXPANDED, !expanded, expanded);
	}
	
	boolean hasChildren() {
		return (children != null && children.length > 0);
	}
	
	/**
	 * @return the children
	 */
	public Lifeline[] getChildren() {
		if (hasChildren()) {
			return children;
		}
		return new Lifeline[0];
	}

	/**
	 * The parent lifeline for this lifeline. Allows lifelines to be grouped together.
	 * @return the parent lifeline, or null if none.
	 */
	public Lifeline getParent() {
		return parent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.UMLItem#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
	 */
	@Override
	protected void widgetDisposed(DisposeEvent e) {
		if (parent != null && !parent.isDisposed()) {
			//remove this item from its parent.
			parent.removeChild(this);
		}
		//set all the children's parents to null
		Lifeline[] oldChildren = children;
		children = null;
		if (oldChildren != null) {
			for (Lifeline child : oldChildren) {
				child.setParent(null);
			}
		}
		super.widgetDisposed(e);
	}

}
