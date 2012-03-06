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
package org.eclipse.zest.custom.sequence;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Enumeration of Zest UML colors.
 * @author Del Myers
 */

public enum ZestUMLColors {
	ColorActivation(IZestUMLColors.COLOR_ACTIVATION),
	ColorCall(IZestUMLColors.COLOR_CALL),
	ColorReturn(IZestUMLColors.COLOR_RETURN),
	ColorPackage(IZestUMLColors.COLOR_PACKAGE),
	ColorClass(IZestUMLColors.COLOR_CLASS),
	ColorExpandable(IZestUMLColors.COLOR_EXPANDABLE);
	
	private String key;
	static ColorRegistry colors = new ColorRegistry(Display.getDefault());
	static {
		colors.put(IZestUMLColors.COLOR_ACTIVATION,  new RGB(216, 228, 248));
		colors.put(IZestUMLColors.COLOR_CALL,new RGB(1, 70, 122));
		colors.put(IZestUMLColors.COLOR_RETURN, new RGB(127,0,0));
		colors.put(IZestUMLColors.COLOR_PACKAGE, new RGB(255, 196, 0));
		colors.put(IZestUMLColors.COLOR_CLASS, new RGB(255,255,255));
		colors.put(IZestUMLColors.COLOR_EXPANDABLE, new RGB(0, 255, 127));
	}
	
	ZestUMLColors(String key) {
		this.key = key;
	}
	
	public Color getColor() {
		return colors.get(key);
	}
	
}
