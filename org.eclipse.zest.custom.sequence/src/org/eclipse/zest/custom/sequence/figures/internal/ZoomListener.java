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

/**
 * Listens to zoom level changes.
 * @author Eric Bordeau
 */
public interface ZoomListener {

/**
 * Called whenever the ZoomManager's zoom level changes.
 * @param zoom the new zoom level.
 */
void zoomChanged(double zoom);

}
