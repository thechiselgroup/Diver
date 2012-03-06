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

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * An activation group represents a range of grouped child activations on a parent. It will
 * be drawn as a box with a lable surrounding the activations.
 * @author Del Myers
 */

public class MessageGroup extends UMLItem implements IExpandableItem {

	private Activation activation;
	private int offset;
	private int length;
	private boolean expanded;
	private Color foreground;
	private Color background;
	


	/**
	 * @param parent
	 */
	public MessageGroup(UMLSequenceChart parent) {
		super(parent);
		expanded = true;
		this.foreground = parent.getForeground();
		this.background = parent.getBackground();
	}
	
	/**
	 * Sets the range for this activation group. It will surround all of the child messages
	 * on <code>a</code> starting at <code>offset</code> and ending at <code>offset+length-1</code>.
	 * The offset is zero-indexed relative to the activation <code>a</code>. If length is < 1,
	 * then the range will extend to the end of <code>a</code>
	 * @param a the activation on which to have the grouping.
	 * @param offset the start of the grouping on activation a.
	 * @param length the length of the group.
	 */
	public void setRange(Activation a, int offset, int length) {
		checkWidget();
		if (offset < 0) {
			throw new IllegalArgumentException("Offset must be non negative");
		}
		Activation oldActivation = this.activation;
		//the order that this is done is important. We have to tell the old activation
		//to remove this group and add this group to the new activation before we set the new 
		//activation on this group. Otherwise, the activations won't get added/removed.
		if (oldActivation == a && (offset != this.offset || length != this.length)) {
			if (oldActivation != null && !oldActivation.isDisposed()) {
				//remove from the activation in order to force a refresh and reordering
				oldActivation.removeGroup(this);
				//set the activation to null to make sure that it gets added again.
				this.activation = null;
				oldActivation.addGroup(this);
			}
		}
		if (oldActivation != a){ 
			if (oldActivation != null && !oldActivation.isDisposed()) {
				oldActivation.removeGroup(this);
			}
			if (a != null && !a.isDisposed()) {
				a.addGroup(this);
			}
		}
		this.offset = offset;
		this.length = length;
		this.activation = a;
		getChart().markDirty();

	}
	
	/**
	 * @return the activation
	 */
	public Activation getActivation() {
		return activation;
	}
	
	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}
	
	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}
	


	
	
	
	/**
	 * The hidden state of an activation group is dependent entirely on its activation. This method
	 * does nothing.
	 */
	@Override
	protected void open() {
		super.open();
	}


	
	
	/**
	 * @param foreground the foreground to set
	 */
	public void setForeground(Color foreground) {
		Object old = this.foreground;
		this.foreground = foreground;
		firePropertyChange(IWidgetProperties.FOREGROUND_COLOR, old, foreground);
	}
	
	@Override
	public Color getForeground() {
		return this.foreground;
	}
	
	public void setBackground(Color background) {
		Object old = this.background;
		this.background = background;
		firePropertyChange(IWidgetProperties.BACKGROUND_COLOR, old, background);
	}
	
	@Override
	public Color getBackground() {
		return this.background;
	}
	


	public boolean isExpanded() {
		return this.expanded;
	}

	public void setExpanded(boolean expanded) {
		if (expanded != this.expanded) {
			this.expanded = expanded;
			getChart().markDirty();
			firePropertyChange(IWidgetProperties.EXPANDED, !expanded, expanded);
		}
	}
	
	@Override
	public String toString() {
		return super.toString() + "[" + getOffset() + ", " + getLength() +"]";
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.UMLItem#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
	 */
	@Override
	protected void widgetDisposed(DisposeEvent e) {
		if (activation != null && !activation.isDisposed()) {
			activation.removeGroup(this);
		}
		super.widgetDisposed(e);
	}

}
