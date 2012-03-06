/*******************************************************************************
 * Copyright (c) 2010 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.logging.eclipse;

import org.eclipse.swt.widgets.Event;


/**
 * @author Del Myers
 *
 */
public class DefaultSWTInterpreter implements
		ILogObjectInterpreter {
	public String toString(Object object) {
		if (object == null) return "null";
		Event event = (Event) object;
		String widget = event.widget.getClass().getCanonicalName();
		String result = "widget:" + widget;
		if (event.item != null) {
			result += ",item:" + event.item.getClass().getCanonicalName();
			if (event.item.getData() != null) {
				result += ",object:" + event.item.getData().getClass().getCanonicalName() + "@" + 
					System.identityHashCode(event.item.getData());
			}
		} else if (event.data != null) {
			result += ",object:" + event.data.getClass().getCanonicalName() +
				"@" + System.identityHashCode(event.data);
		}
		
		return result;
	}
}