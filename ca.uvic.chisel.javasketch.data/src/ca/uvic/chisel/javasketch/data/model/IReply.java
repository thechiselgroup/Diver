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
 * Represents the return from an activation.
 * @author Del Myers
 *
 */
public interface IReply extends IOriginMessage {

	/**
	 * The value that was returned.
	 * @return the value that was returned.
	 */
	String getReturnValue();
	
}
