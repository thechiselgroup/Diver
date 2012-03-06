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

import org.eclipse.jface.viewers.IStructuredContentProvider;

/**
 * A content provider to be used in sequence chart viewers.
 * @author Del Myers
 *
 */
public interface ISequenceChartContentProvider extends IStructuredContentProvider {

	/**
	 * Returns all of the messages that originate from the given activation. These may correspond
	 * to calls to other activations or returns to previous activations.
	 * @param activation the activation to retrieve the messages for.
	 * @return the the messages for the given activation--either new calls, or returns to previous
	 * activations.
	 */
	Object[] getMessages(Object activation);
	
	/**
	 * Returns an object corresponding to the lifeline that the given activation is on.
	 * @param activation the activation to place on the lifeline.
	 * @return the lifeline for the given activation.
	 */
	Object getLifeline(Object activation);
	
	/**
	 * Returns an activation that corresponds to the target activation of the given message.
	 * 
	 * @param message the message to retrieve the target for.
	 * @return an activation that corresponds to the target activation of the given message.
	 */
	Object getTarget(Object message);
	
	/**
	 * Returns true if the given message should be treated as a call. That is, it will result
	 * in a new activation being created. Otherwise, it will be treated as a return to a previous
	 * activation on the same call stack. Note that if the object returned by {@link #getTarget(Object)}
	 * is not on the call stack, and this method returns false, then a malformed sequence diagram
	 * may result, and the diagram will not be displayable.
	 * @param message the message to check.
	 * @return true if the message should be treated as a call. False if it should be a return.
	 */
	boolean isCall(Object message);
}
