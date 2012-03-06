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
package ca.uvic.chisel.javasketch.data.model;

/**
 * A simple interface that represents a generic user event that was logged
 * in the trace such as pauses or resumes.
 * @author Del Myers
 *
 */
public interface ITraceMetaEvent extends ITraceModel {
	
	/**
	 * Text associated with the event.
	 * @return text associated with the event.
	 */
	String getText();
	
	
	/**
	 * The time that the event occurred.
	 * @return
	 */
	long getTime();

}
