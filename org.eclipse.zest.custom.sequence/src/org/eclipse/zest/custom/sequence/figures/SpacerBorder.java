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
package org.eclipse.zest.custom.sequence.figures;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;

/**
 * A border that draws itself as spaces around the figure.
 * @author Del Myers
 */

public class SpacerBorder extends MarginBorder {
	public SpacerBorder(Insets insets) {
		super(insets);
	}
	
	public SpacerBorder(int top, int left, int bottom, int right) {
		this(new Insets(top, left, bottom, right));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.AbstractBorder#getPreferredSize(org.eclipse.draw2d.IFigure)
	 */
	@Override
	public Dimension getPreferredSize(IFigure f) {
		return f.getBounds().getCopy().expand(getInsets(f)).getSize();
	}
}
