/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch;

/**
 * A listener for events that happen within the framework.
 * @author Del Myers
 *
 */
public interface ISketchEventListener {

	/**
	 * Indicates that an event has occurred in the sketch framework.
	 * @param event
	 */
	void handleSketchEvent(SketchEvent event);
	

}
