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
 * Represents a message that originates at an activation.
 * @author Del Myers
 *
 */
public interface IOriginMessage extends IMessage {
	
	/**
	 * Returns the activation that originated this message.
	 * @return the activation that originated this message.
	 */
	public IActivation getActivation();
	
	/**
	 * The target of this message.
	 * @return the target of this message.
	 */
	ITargetMessage getTarget();
	
}
