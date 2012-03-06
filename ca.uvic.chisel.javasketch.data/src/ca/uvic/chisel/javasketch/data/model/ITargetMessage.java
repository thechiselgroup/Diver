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
 * Represents a message that has its target in an activation.
 * @author Del Myers
 *
 */
public interface ITargetMessage extends IMessage {
	
	/**
	 * Returns the activation that this message targets.
	 * @return the activation that this message targets.
	 */
	public IActivation getActivation();
	
	/**
	 * Returns the message that originates this target. May be null.
	 * @return the message that originates this target.
	 */
	IOriginMessage getOrigin();

}
