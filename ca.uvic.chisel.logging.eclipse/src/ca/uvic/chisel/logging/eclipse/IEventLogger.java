/*******************************************************************************
 * Copyright (c) 2010 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.logging.eclipse;

/**
 * Interface for logging events in the workbench.
 * @author Del Myers
 *
 */
public interface IEventLogger {
	/**
	 * Logs an event that occurs in a particular workbench part
	 * @param logger the logger that recorded the event.
	 * @param event the name of the logged event.
	 * @param eventObject data for the logged event.
	 */
	public void logPartEvent(IPartLogger logger, String event, Object eventObject);
}
