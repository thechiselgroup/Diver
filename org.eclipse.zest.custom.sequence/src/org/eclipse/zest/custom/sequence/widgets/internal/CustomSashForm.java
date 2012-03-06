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
package org.eclipse.zest.custom.sequence.widgets.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;



/**
 * The SashForm is a composite control that lays out its children in a
 * row or column arrangement (as specified by the orientation) and places
 * a Sash between each child. One child may be maximized to occupy the
 * entire size of the SashForm.  The relative sizes of the children may
 * be specified using weights.
 * <p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>HORIZONTAL, VERTICAL, SMOOTH</dd>
 * </dl>
 * </p>
 */
public class CustomSashForm extends Composite {

	private class TypedListener implements DisposeListener {
		int type;
		Listener listener;
		private Control hookedControl;
		public TypedListener(int type, Listener listener, Control hookedControl) {
			this.type = type;
			this.listener = listener;
			this.hookedControl = hookedControl;
			hookedControl.addDisposeListener(this);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
		 */
		public void widgetDisposed(DisposeEvent e) {
			Sash sash = findSash(hookedControl);
			if (sash != null && !sash.isDisposed()) {
				sash.removeListener(type, listener);
			}
		}
	}
	
	public int SASH_WIDTH = 3;

	int sashStyle;
	Sash[] sashes = new Sash[0];
	// Remember background and foreground
	// colors to determine whether to set
	// sashes to the default color (null) or
	// a specific color
	Color background = null;
	Color foreground = null;
	Control[] controls = new Control[0];
	Control maxControl = null;
	Listener sashListener;
	private List<TypedListener> listeners;

	private boolean dragging;
	static final int DRAG_MINIMUM = 0;

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a widget which will be the parent of the new instance (cannot be null)
 * @param style the style of widget to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 * </ul>
 *
 * @see SWT#HORIZONTAL
 * @see SWT#VERTICAL
 * @see #getStyle()
 */
public CustomSashForm(Composite parent, int style) {
	super(parent, checkStyle(style));
	super.setLayout(new SashFormLayout());
	sashStyle = ((style & SWT.VERTICAL) != 0) ? SWT.HORIZONTAL : SWT.VERTICAL;
	if ((style & SWT.BORDER) != 0) sashStyle |= SWT.BORDER;
	if ((style & SWT.SMOOTH) != 0) sashStyle |= SWT.SMOOTH;
	sashListener = new Listener() {
		public void handleEvent(Event e) {
			boolean wasDragging = dragging;
			if (e.type == SWT.DragDetect) {
				dragging = true;
				wasDragging = true;
			} else if ((e.type == SWT.MouseUp && e.button == 1) || (e.type == SWT.MouseExit)) {
				dragging = false;
			}
			if (dragging) {
				e.detail = SWT.DRAG; 
			}
			if (wasDragging) {
				onDragSash(e);
			}
		}
	};
}
static int checkStyle (int style) {
	int mask = SWT.BORDER | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
	return style & mask;
}
/**
 * Returns SWT.HORIZONTAL if the controls in the SashForm are laid out side by side
 * or SWT.VERTICAL   if the controls in the SashForm are laid out top to bottom.
 * 
 * @return SWT.HORIZONTAL or SWT.VERTICAL
 */
public int getOrientation() {
	//checkWidget();
	return (sashStyle & SWT.VERTICAL) != 0 ? SWT.HORIZONTAL : SWT.VERTICAL;
}
public int getStyle() {
	int style = super.getStyle();
	style |= getOrientation() == SWT.VERTICAL ? SWT.VERTICAL : SWT.HORIZONTAL;
	if ((sashStyle & SWT.SMOOTH) != 0) style |= SWT.SMOOTH;
	return style;
}
/**
 * Answer the control that currently is maximized in the SashForm.  
 * This value may be null.
 * 
 * @return the control that currently is maximized or null
 */
public Control getMaximizedControl(){
	//checkWidget();
	return this.maxControl;
}
/**
 * Answer the relative weight of each child in the SashForm.  The weight represents the
 * percent of the total width (if SashForm has Horizontal orientation) or 
 * total height (if SashForm has Vertical orientation) each control occupies.
 * The weights are returned in order of the creation of the widgets (weight[0]
 * corresponds to the weight of the first child created).
 * 
 * @return the relative weight of each child
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */

public int[] getWeights() {
	checkWidget();
	Control[] cArray = getControls(false);
	int[] ratios = new int[cArray.length];
	for (int i = 0; i < cArray.length; i++) {
		Object data = cArray[i].getLayoutData();
		if (data != null && data instanceof CustomSashFormData) {
			ratios[i] = (int)(((CustomSashFormData)data).weight * 1000 >> 16);
		} else {
			ratios[i] = 200;
		}
	}
	return ratios;
}
Control[] getControls(boolean onlyVisible) {
	Control[] children = getChildren();
	Control[] result = new Control[0];
	for (int i = 0; i < children.length; i++) {
		if (children[i] instanceof Sash) continue;
		if (onlyVisible && !children[i].getVisible()) continue;

		Control[] newResult = new Control[result.length + 1];
		System.arraycopy(result, 0, newResult, 0, result.length);
		newResult[result.length] = children[i];
		result = newResult;
	}
	return result;
}
void onDragSash(Event event) {
	Sash sash = (Sash)event.widget;
	int sashIndex = -1;
	for (int i= 0; i < sashes.length; i++) {
		if (sashes[i] == sash) {
			sashIndex = i;
			break;
		}
	}
	if (sashIndex == -1) return;

	Control c1 = controls[sashIndex];
	Control c2 = controls[sashIndex + 1];
	Rectangle b1 = c1.getBounds();
	Rectangle b2 = c2.getBounds();
	Point eventPoint = sash.toDisplay(event.x, event.y);
	eventPoint = sash.getParent().toControl(eventPoint);
	Rectangle sashBounds = sash.getBounds();
	Rectangle area = getClientArea();
	boolean correction = false;
	if (getOrientation() == SWT.HORIZONTAL) {
		correction = b1.width < DRAG_MINIMUM || b2.width < DRAG_MINIMUM;
		int totalWidth = b2.x + b2.width - b1.x; 
		int shift = eventPoint.x - sashBounds.x;
		b1.width += shift;
		b2.x += shift;
		b2.width -= shift;
		if (b1.width < DRAG_MINIMUM) {
			b1.width = DRAG_MINIMUM;
			b2.x = b1.x + b1.width + sashBounds.width;
			b2.width = totalWidth - b2.x;
			eventPoint.x = b1.x + b1.width;
			event.doit = false;
		}
		if (b2.width < DRAG_MINIMUM) {
			b1.width = totalWidth - DRAG_MINIMUM - sashBounds.width;
			b2.x = b1.x + b1.width + sashBounds.width;
			b2.width = DRAG_MINIMUM;
			eventPoint.x = b1.x + b1.width;
			event.doit = false;
		}
		Object data1 = c1.getLayoutData();
		if (data1 == null || !(data1 instanceof CustomSashFormData)) {
			data1 = new CustomSashFormData();
			c1.setLayoutData(data1);
		}
		Object data2 = c2.getLayoutData();
		if (data2 == null || !(data2 instanceof CustomSashFormData)) {
			data2 = new CustomSashFormData();
			c2.setLayoutData(data2);
		}
		((CustomSashFormData)data1).weight = (((long)b1.width << 16) + area.width - 1) / area.width;
		((CustomSashFormData)data2).weight = (((long)b2.width << 16) + area.width - 1) / area.width;
	} else {
		correction = b1.height < DRAG_MINIMUM || b2.height < DRAG_MINIMUM;
		int totalHeight = b2.y + b2.height - b1.y;
		int shift = eventPoint.y - sashBounds.y;
		b1.height += shift;
		b2.y += shift;
		b2.height -= shift;
		if (b1.height < DRAG_MINIMUM) {
			b1.height = DRAG_MINIMUM;
			b2.y = b1.y + b1.height + sashBounds.height;
			b2.height = totalHeight - b2.y;
			eventPoint.y = b1.y + b1.height;
			event.doit = false;
		}
		if (b2.height < DRAG_MINIMUM) {
			b1.height = totalHeight - DRAG_MINIMUM - sashBounds.height;
			b2.y = b1.y + b1.height + sashBounds.height;
			b2.height = DRAG_MINIMUM;
			eventPoint.y = b1.y + b1.height;
			event.doit = false;
		}
		Object data1 = c1.getLayoutData();
		if (data1 == null || !(data1 instanceof CustomSashFormData)) {
			data1 = new CustomSashFormData();
			c1.setLayoutData(data1);
		}
		Object data2 = c2.getLayoutData();
		if (data2 == null || !(data2 instanceof CustomSashFormData)) {
			data2 = new CustomSashFormData();
			c2.setLayoutData(data2);
		}
		((CustomSashFormData)data1).weight = (((long)b1.height << 16) + area.height - 1) / area.height;
		((CustomSashFormData)data2).weight = (((long)b2.height << 16) + area.height - 1) / area.height;
	}
	
	if (correction || (event.doit && event.detail != SWT.DRAG)) {
		c1.setBounds(b1);
		int x = (getOrientation() == SWT.HORIZONTAL) ? eventPoint.x : 0;
		int y = (getOrientation() == SWT.VERTICAL) ? eventPoint.y : 0;
		int width = (getOrientation() == SWT.VERTICAL) ? b1.width : SASH_WIDTH;
		int height = (getOrientation() == SWT.HORIZONTAL) ? b1.height : SASH_WIDTH;
		sash.setBounds(x, y, width, height);
		c2.setBounds(b2);
	}
	dragging = event.detail == SWT.DRAG;
}
/**
 * If orientation is SWT.HORIZONTAL, lay the controls in the SashForm 
 * out side by side.  If orientation is SWT.VERTICAL, lay the 
 * controls in the SashForm out top to bottom.
 * 
 * @param orientation SWT.HORIZONTAL or SWT.VERTICAL
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the value of orientation is not SWT.HORIZONTAL or SWT.VERTICAL
 * </ul>
 */
public void setOrientation(int orientation) {
	checkWidget();
	if (getOrientation() == orientation) return;
	if (orientation != SWT.HORIZONTAL && orientation != SWT.VERTICAL) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}
	sashStyle &= ~(SWT.HORIZONTAL | SWT.VERTICAL);
	sashStyle |= orientation == SWT.VERTICAL ? SWT.HORIZONTAL : SWT.VERTICAL;
	for (int i = 0; i < sashes.length; i++) {
		sashes[i].dispose();
		sashes[i] = createSash(controls[i]);
	}
	layout(false);
}

Sash createSash(Control forControl) {
	Sash sash = new Sash(this, sashStyle);
	sash.setBackground(background);
	sash.setForeground(foreground);
	sash.addListener(SWT.DragDetect, sashListener);
	sash.addListener(SWT.MouseMove, sashListener);
	sash.addListener(SWT.MouseUp, sashListener);
	sash.addListener(SWT.MouseExit, sashListener);
	for (TypedListener tl : listeners) {
		if (tl.hookedControl == forControl) {
			sash.addListener(tl.type, tl.listener);
		}
	}
	return sash;
}
public void setBackground (Color color) {
	super.setBackground(color);
	background = color;
	for (int i = 0; i < sashes.length; i++) {
		sashes[i].setBackground(background);
	}
}
public void setForeground (Color color) {
	super.setForeground(color);
	foreground = color;
	for (int i = 0; i < sashes.length; i++) {
		sashes[i].setForeground(foreground);
	}
}
/**
 * Sets the layout which is associated with the receiver to be
 * the argument which may be null.
 * <p>
 * Note: No Layout can be set on this Control because it already
 * manages the size and position of its children.
 * </p>
 *
 * @param layout the receiver's new layout or null
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setLayout (Layout layout) {
	checkWidget();
	return;
}
/**
 * Specify the control that should take up the entire client area of the SashForm.  
 * If one control has been maximized, and this method is called with a different control, 
 * the previous control will be minimized and the new control will be maximized.
 * If the value of control is null, the SashForm will minimize all controls and return to
 * the default layout where all controls are laid out separated by sashes.
 * 
 * @param control the control to be maximized or null
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setMaximizedControl(Control control){
	checkWidget();
	if (control == null) {
		if (maxControl != null) {
			this.maxControl = null;
			layout(false);
			for (int i= 0; i < sashes.length; i++){
				sashes[i].setVisible(true);
			}
		}
		return;
	}
	
	for (int i= 0; i < sashes.length; i++){
		sashes[i].setVisible(false);
	}
	maxControl = control;
	layout(false);
}

/**
 * Specify the relative weight of each child in the SashForm.  This will determine
 * what percent of the total width (if SashForm has Horizontal orientation) or 
 * total height (if SashForm has Vertical orientation) each control will occupy.
 * The weights must be positive values and there must be an entry for each
 * non-sash child of the SashForm.
 * 
 * @param weights the relative weight of each child
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the weights value is null or of incorrect length (must match the number of children)</li>
 * </ul>
 */
public void setWeights(int[] weights) {
	checkWidget();
	Control[] cArray = getControls(false);
	if (weights == null || weights.length != cArray.length) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}
	
	int total = 0;
	for (int i = 0; i < weights.length; i++) {
		if (weights[i] < 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		total += weights[i];
	}
	if (total == 0) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}
	for (int i = 0; i < cArray.length; i++) {
		Object data = cArray[i].getLayoutData();
		if (data == null || !(data instanceof CustomSashFormData)) {
			data = new CustomSashFormData();
			cArray[i].setLayoutData(data);
		}
		((CustomSashFormData)data).weight = (((long)weights[i] << 16) + total - 1) / total;
	}

	layout(false);
}

/**
 * Adds the given listener to the Sash which comes after the given child in the CustomSashForm
 * @param child the child before the sash that this listener will be added to. 
 * @param listener
 */
public void addSashListener(int type, Control child, Listener listener) {
	if (listeners == null) {
		listeners = new ArrayList<TypedListener>();
	}
	if (child == null || child.isDisposed() || listener == null) {
		return;
	}
	Control[] controls = getControls(false);
	for (int i = 0; i < controls.length; i++) {
		if  (controls[i] == child) {
			//make sure that the listener doesn't already exist here.
			for (TypedListener tl : listeners) {
				if (tl.hookedControl == child && tl.type == type && tl.listener.equals(listener)) {
					return;
				}
			}
			//add it to the sash
			Sash s = findSash(child);
			TypedListener tl = new TypedListener(type, listener, child);
			listeners.add(tl);
			if (s != null && !s.isDisposed()) {
				s.addListener(type, tl.listener);
			}
			
		}
	}
}

public void removeSashListener(int type, Listener listener) {
	for (Iterator<TypedListener> i = listeners.iterator(); i.hasNext();) {
		TypedListener tl = i.next();
		if (tl.type == type && tl.listener.equals(listener)) {
			i.remove();
			Sash s = findSash(tl.hookedControl);
			if (s != null && !s.isDisposed()) {
				s.removeListener(tl.type, tl.listener);
			}
			tl.hookedControl.removeDisposeListener(tl);
		}
	}
}

/**
 * Returns the sash after the given control, if it can be found. Note, that clients should not
 * depend on the reference returned remaining consistent. The Sashes in the form are constantly updated
 * due to layout events, and the returned reference may become stale. For this reason, this method
 * may only be called from within the display thread.
 * @return the Sash after the given control, if it exists. Null otherwise.
 */
public Sash findSash(Control control) {
	checkWidget();
	for (int i = 0; i < controls.length; i++) {
		if (controls[i] == control) {
			if (i < sashes.length) {
				return sashes[i];
			}
		}
	}
	return null;
}

/**
 * Returns the Control before the given Sash if it exists in this form. Null otherwise. Must be called
 * within the display thread.
 * @param sash the Sash to look for.
 * @return the Control before the given Sash, if it exists.
 */
public Control findControl(Sash sash) {
	checkWidget();
	for (int i = 0; i < sashes.length; i++) {
		if (sashes[i] == sash) {
			return controls[i];
		}
	}
	return null;
}

public void extendControl(Control control) {
	checkWidget();
	if (this.maxControl != null) {
		return;
	}
	int controlIndex = 0;
	for (; controlIndex < controls.length; controlIndex++) {
		if (controls[controlIndex] == control) {
			break;
		}
	}
	if (controlIndex >= controls.length) {
		//didn't find the control
		return;
	}
	Control[] allControls = getControls(false);
	int[] weights = getWeights();
	Point size = getSize();
	int wHint = (getOrientation() == SWT.HORIZONTAL) ? -1 : size.x;
	int hHint = (getOrientation() == SWT.VERTICAL) ? -1 : size.y;
	Point controlSize = control.computeSize(wHint, hHint);
	float controlWeight = (getOrientation() == SWT.HORIZONTAL) ? (float)controlSize.x/size.x : (float)controlSize.y/size.y;
	controlWeight *= 1000;
	if (controlWeight > 1000) {
		controlWeight = 1000f;
	}
	float availableWeight = 0;
	//calculate everything available.
	for (int i = 0;  i < allControls.length; i++) {
		for (int j = controlIndex+1; j < controls.length; j++) {
			if (controls[j] == allControls[i]) {
				availableWeight += weights[i];
			}
		}
	}
	float weightLeft = availableWeight - controlWeight;
	if (weightLeft < 0) {
		controlWeight = availableWeight;
		weightLeft = 0;
	}
	float scale = (availableWeight > 0 && weightLeft > 0) ? weightLeft/availableWeight : 0;
	
	//scale the weights
	for (int i = 0;  i < allControls.length; i++) {
		for (int j = controlIndex; j < controls.length; j++) {
			if (controls[j] == allControls[i]) {
				if (j == controlIndex) {
					weights[i] = (int) controlWeight;
				} else {
					weights[i] = (int)(weights[i]*scale);
				}
			}
		}
	}
	setWeights(weights);
	
}

public void collapseControl(Control control) {
	int controlWeight = getWeight(control);
	int currentWeight = 0;
	boolean startCount = false;
	int controlIndex = 0;
	for (int i = 0; i < controls.length; i++) {
		if (startCount) {
			currentWeight += getWeight(controls[i]);
		}
		if (controls[i] == control) {
			startCount = true;
			controlIndex = i;
		}
	}
	float scale = (controlWeight + currentWeight)/(float)currentWeight;
	if (Float.isInfinite(scale)) {
		scale = 1000;
	}
	int[] weights = getWeights();
	Control[] allControls = getControls(false);
	for (int i = 0; i < allControls.length; i++) {
		for (int j = controlIndex; j < controls.length; j++) {
			if (allControls[i] == controls[j]) {
				if (j == controlIndex) {
					weights[i] = 0;
				} else {
					weights[i] = (int)(weights[i]*scale);
					if (weights[i]==0) {
						weights[i] = 1000;
					}
				}
			}
		}
	}
	setWeights(weights);
}

public int getWeight(Control control) {
	Object o = control.getLayoutData();
	if (!(o instanceof CustomSashFormData)) {
		return 200;
	}
	CustomSashFormData data = (CustomSashFormData) o;
	return (int)(data.weight * 1000 >> 16);
}
}
