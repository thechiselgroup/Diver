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

import org.eclipse.draw2d.Animation;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.swt.SWT;

/**
 * A freeform layered pane that draws with antialiasing when not animating.
 * @author Del Myers
 *
 */
public class AntialiasingScalableFreeformLayeredPane extends
		ScalableFreeformLayeredPane {
	
	private int antialiasing;

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#paint(org.eclipse.draw2d.Graphics)
	 */
	@Override
	public void paint(Graphics graphics) {
		int oldAntialias = graphics.getAntialias();
		if (Animation.isAnimating() && Animation.getProgress() < .9) {
			graphics.setAntialias(SWT.OFF);
		} else {
			graphics.setAntialias(antialiasing);
		}
		super.paint(graphics);
		graphics.setAntialias(oldAntialias);
	}
	
	/**
	 * Sets the antialiasing must be one of SWT.ON, SWT.OFF, or SWT.DEFAULT
	 * @param antialiasing one of SWT.ON, SWT.OFF, or SWT.DEFAULT.
	 */
	public void setAntialiasing(int antialiasing) {
		this.antialiasing = antialiasing;
	}

}
