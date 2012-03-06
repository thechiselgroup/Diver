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

/**
 * Contains all of the properties used to update widget visuals, and tools when
 * properties change on widgets.
 * 
 * @author Del Myers
 *
 */
public interface IWidgetProperties {

	/**
	 * Property indicating that a new item was created in a composite. If an item
	 * was created, then the new value in the property change will be a reference to
	 * that item. If it has been deleted, then the old value will be a reference to the
	 * item. The other value, in either case will be null.
	 */
	public static final String ITEM= "itm";
		
	/**
	 * Property indicating that the data has changed on this item.
	 */
	public static final String DATA = "data";
	public static final String ENABLED = "enable";
	public static final String HIDDEN = "hid";
	/**
	 * Property indicating that highlighting has changed on the item.
	 */
	public static final String HIGHLIGHT = "hlt";
	/**
	 * Property indicating that the image has changed on this item.
	 */
	public static final String IMAGE = "img";
	/**
	 * The text property for this item.
	 */
	public static final String TEXT = "txt";
	public static final String TOOLTIP = "tltp";
	public static final String BACKGROUND_COLOR = "bgnd";
	public static final String FOREGROUND_COLOR = "fgnd";
	/**
	 * Property indicating that the life line has changed on this activation.
	 */
	public static final String LIFELINE = "lfln";
	/**
	 * Property indicating that the sub calls have changed for this activation.
	 */
	public static final String SUB_CALL = "sbcls";
	public static final String TEXT_FOREGROUND = "tfgnd";
	/**
	 * Property indicating that the expansion state of this item has changed.
	 */
	public static final String EXPANDED = "exp";
	
	public static final String LAYOUT = "layout";
	public static final String SOURCE_RELATIONSHIP = "scrln";
	public static final String TARGET_RELATIONSHIP = "tgrln";
	public static final String TEXT_BACKGROUND = "tbgnd";
	public static final String LINE_STYLE = "lnstl";
	/**
	 * Indicates that a message has either been added or removed from an activation. If the
	 * message has been added, then the "new" value for the property will be the 
	 */
	public static final String MESSAGE = "msg";

	/**
	 * The drawing style of a lifeline or object has changed. 
	 */
	public static final String OBJECT_DRAWING_STYLE = "drstl";
	/**
	 * Property indicating that the "owner" of a widget has changed. For example, the lifeline for
	 * an activation.
	 */
	public static final String OWNER = "own";
	/**
	 * A decoration on a widget has changed.
	 */
	public static final String DECORATION = "dec";

	public static final String STEREOTYPE = "stype";

	public static final String ACTIVE = "active";

	public static final String CHILD = "child";

}
