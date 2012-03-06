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
package org.eclipse.zest.custom.sequence.figures.internal;

import java.util.List;

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.IFigure;

/**
 * @author Del Myers
 *
 */
public class QuickClearFreeformLayer2 extends FreeformLayer {

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#removeAll()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void removeAll() {
		List children = getChildren();
		for (Object o : children) {
			IFigure next = (IFigure) o;
			next.erase();
			next.setParent(null);
			if (getParent() != null)
				next.removeNotify();
			getLayoutManager().remove(next);
		}
		children.clear();
		revalidate();
	}
	
}
