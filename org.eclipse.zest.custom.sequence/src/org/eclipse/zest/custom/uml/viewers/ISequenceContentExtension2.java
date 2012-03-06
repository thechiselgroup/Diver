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
package org.eclipse.zest.custom.uml.viewers;

/**
 * An extension to sequence chart content providers which allows callers to traverse
 * backward along the call graph.
 * @author Del Myers
 *
 */
public interface ISequenceContentExtension2 {
	
	/**
	 * Returns the message that resulted in the given activation.
	 * @param activation the activation to query.
	 * @return the message that resulted in the given activation.
	 */
	Object getCall(Object activation);
	
	/**
	 * Returns the activation at which the given message originated.
	 * @param message the message to query.
	 * @return the activation at which the given message originated.
	 */
	Object getOriginActivation(Object message);

}
