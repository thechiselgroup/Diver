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
package org.eclipse.zest.custom.sequence.widgets.internal;

import org.eclipse.swt.graphics.Image;
import org.eclipse.zest.custom.sequence.widgets.UMLChart;

/**
 * An interface that adapts a uml chart to an image.
 * @author Del Myers
 *
 */
public interface IUMLImageAdapter {
	
	/**
	 * Creates an image for the given chart in its current state.
	 * @param chart the chart to convert to an image.
	 * @return a new image.
	 */
	Image getImage(UMLChart chart);

}
