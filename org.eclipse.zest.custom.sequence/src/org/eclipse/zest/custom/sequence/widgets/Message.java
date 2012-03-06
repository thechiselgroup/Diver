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
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * Represents a message between two objects on a life line. This could be
 * another Activation, or it could be a return to a previously created 
 * Activation. A message is hidden if either its source or target activation
 * is hidden, or non-existent.
 * 
 * @author Del Myers
 *
 */
public class Message extends UMLTextColoredItem {
	/**
	 * Endpoint style indicating no decoration.
	 */
	public static final int NONE = 0;
	/**
	 * Endpoint style indicating an open arrow.
	 */
	public static final int OPEN_ARROW = 1;
	/**
	 * Endpoint style indicating a closed arrow.
	 */
	public static final int CLOSED_ARROW = 2;
	/**
	 * Endpoint style indicating a circle.
	 */
	public static final int CIRCLE = 3;
	/**
	 * Endpoint style indicating a square.
	 */
	public static final int SQUARE = 4;
	/**
	 * Endpoint style indicating a diamond.
	 */
	public static final int DIAMOND = 5;
	/**
	 * Mask for endpoint styles indicating that the endpoint should be filled.
	 */
	public static final int FILL_MASK = 8;
	
	
	private Activation source;
	private Activation target;
	private int lineStyle;
	private int targetDecoration;
	private int sourceDecoration;

	private int index;
	
	protected Message(UMLSequenceChart chart) {
		super(chart);
		lineStyle = SWT.LINE_SOLID;
		targetDecoration = CLOSED_ARROW | FILL_MASK;
		index = -1;
	}
	
	/**
	 * Sets the source of this message to the given activation. If this 
	 * message currently has a source, then it is disconnected from that source
	 * first.
	 * @param activation
	 */
	void setSource(Activation activation) {
		if (activation == source) {
			return;
		}
		if (source != null && !source.isDisposed()) {
			source.removeMessage(this);
		}
		this.source = activation;
	}
	
	/**
	 * Sets the target of this message to the given activation. If this message
	 * currently has a target, it is disconnected from that target first.
	 * @param activation
	 */
	void setTarget(Activation activation) {
		if (activation == target) {
			return;
		}
		if (target != null && !target.isDisposed()) {
			target.removeTargetMessage(this);
		}
		if (activation != null) {
			activation.addTargetMessage(this);
		}
		this.target = activation;
		
	}
	
	/**
	 * The message is hidden iff its source and/or target is hidden.
	 */
	public boolean isHidden() {
		if (source == null || target == null) {
			return true;
		}
		return source.isHidden() || target.isHidden();
	}
	
	/**
	 * This implementation does nothing. The hidden state of the
	 * message is determined by its source and target.
	 */
	@Override
	protected void hide() {
	}

	/**
	 * 
	 * @return the source activation.
	 */
	public Activation getSource() {
		return source;
	}
	
	/**
	 * 
	 * @return the target activation.
	 */
	public Activation getTarget() {
		return target;
	}
	
	/**
	 * Sets the line style according to SWT line styles: <code>SWT.LINE_SOLID</code>, <code>SWT.LINE_DASH</code>, 
	 * <code>SWT.LINE_DOT</code>, <code>SWT.LINE_DASHDOT</code> or <code>SWT.LINE_DASHDOTDOT</code>. 
	 * By default, the style is SWT.LINE_SOLID.
	 * @param style the new style.
	 */
	public void setLineStyle(int style) {
		checkWidget();
		switch (style) {
		case SWT.LINE_SOLID:
		case SWT.LINE_DASH:
		case SWT.LINE_DASHDOT:
		case SWT.LINE_DASHDOTDOT:
		case SWT.LINE_DOT:
			break;
		default:
			SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, "Line style must be one of SWT.LINE_SOLID, SWT.LINE_DASH, SWT.LINE_DASHDOT, SWT.LINE_DASHDOTO, or SWT.LINE_DOT");
		}
		Integer oldStyle = (Integer)getLineStyle();
		this.lineStyle = style;
		firePropertyChange(IWidgetProperties.LINE_STYLE, oldStyle, style);
	}
	
	/**
	 * Sets the decoration for the "source" end of the message to the given style. Must be one of
	 * NONE, OPEN_ARROW, CLOSED_ARROW, CIRCLE, SQUARE, or DIAMOND, optionally bitwise-OR'd with
	 * FILL_MASK to indicate that the endpoint should be filled.
	 * @param style
	 */
	public void setSourceStyle(int style) {
		checkWidget();
		int clearMask = style & FILL_MASK;
		switch (clearMask) {
		case NONE:
		case OPEN_ARROW:
		case CLOSED_ARROW:
		case CIRCLE:
		case SQUARE:
		case DIAMOND:
			break;
		default:
			SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, "Endpoint style must be one of NONE, OPEN_ARROW, CLOSED_ARROW, CIRCLE, SQUARE, or, DIAMOND");	
		}
		int oldStyle = sourceDecoration;
		this.sourceDecoration = style;
		firePropertyChange(IWidgetProperties.DECORATION, oldStyle, style);
	}
	
	/**
	 * @return the style for the source decoration.
	 */
	public int getSourceStyle() {
		return sourceDecoration;
	}
	
	
	/**
	 * Sets the decoration for the "target" end of the message to the given style. Must be one of
	 * NONE, OPEN_ARROW, CLOSED_ARROW, CIRCLE, SQUARE, or DIAMOND, optionally bitwise-OR'd with
	 * FILL_MASK to indicate that the endpoint should be filled.
	 * @param style
	 */
	public void setTargetStyle(int style) {
		checkWidget();
		int clearMask = (style | FILL_MASK) ^ FILL_MASK;
		switch (clearMask) {
		case NONE:
		case OPEN_ARROW:
		case CLOSED_ARROW:
		case CIRCLE:
		case SQUARE:
		case DIAMOND:
			break;
		default:
			SWT.error(SWT.ERROR_INVALID_ARGUMENT, null, "Endpoint style must be one of NONE, OPEN_ARROW, CLOSED_ARROW, CIRCLE, SQUARE, or, DIAMOND");	
		}
		int oldStyle = targetDecoration;
		this.targetDecoration = style;
		firePropertyChange(IWidgetProperties.DECORATION, oldStyle, style);
	}
	
	/**
	 * @return the style for the target decoration.
	 */
	public int getTargetStyle() {
		return targetDecoration;
	}
	
	public int getLineStyle() {
		return lineStyle;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.UMLItem#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
	 */
	@Override
	protected void widgetDisposed(DisposeEvent e) {
		setSource(null);
		setTarget(null);
		super.widgetDisposed(e);
	}



	/**
	 * @param i
	 */
	void setIndexInActivation(int i) {
		this.index = i;		
	}
	
	int getIndexInActivation() {
		return index;
	}
	
	
	
}
