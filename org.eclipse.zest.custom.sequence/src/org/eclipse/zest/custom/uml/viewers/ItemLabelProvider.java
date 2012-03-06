/*******************************************************************************
 * Copyright 2005-2006, CHISEL Group, University of Victoria, Victoria, BC, Canada.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Chisel Group, University of Victoria
 *******************************************************************************/
package org.eclipse.zest.custom.uml.viewers;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Color;

/**
 * @author Del
 *
 */
public abstract class ItemLabelProvider extends BaseLabelProvider {
	public static class ItemLabel {
		public String text;
		public Color foreground;
		public Color background;
		public int lineStyle;
		public int[] customLineStyle;
	}
	
	public abstract void style(ItemLabel label, Object element);

}
