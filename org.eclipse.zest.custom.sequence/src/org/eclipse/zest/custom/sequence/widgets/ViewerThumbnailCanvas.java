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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.zest.custom.sequence.figures.internal.ZoomManager;

/**
 * A canvas that allows tries to create a thumbnail based on a Zest viewer. The thumbnail
 * will only be set if the viewer is zoomable.
 * @author Del Myers
 */

public class ViewerThumbnailCanvas extends ThumbnailCanvas {

	/**
	 * @param parent
	 * @param style
	 */
	public ViewerThumbnailCanvas(Composite parent, int style) {
		super(parent, style);
	}
	
	
	/**
	 * Tries to set the thumnail to the given viewer.
	 * @param viewer
	 */
	public void setViewer(Composite viewer) {
		ZoomManager manager = (ZoomManager)viewer.getData("ZoomManager");
		if (manager != null) {
			setFigure(manager.getViewport(), viewer);
		}
	}

}
