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

/**
 * A {@link DeferredLayoutPolylineConnection} that remains dirty while the animator
 * is animating. This way, it will continuously be laid-out.
 * 
 * 
 * @author Del Myers
 *
 */
public class AnimatedDeferredLayoutPolylineConnection extends
		DeferredLayoutPolylineConnection {

	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.figures.internal.DeferredLayoutPolylineConnection#layout()
	 */
	@Override
	public void layout() {
		if (!isDirty()) {
			setDirty(Animation.isAnimating());
		}
		super.layout();
		setDirty(Animation.isAnimating());
	}
	
}
