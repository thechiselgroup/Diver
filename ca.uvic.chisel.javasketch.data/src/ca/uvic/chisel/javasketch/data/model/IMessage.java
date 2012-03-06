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
 * A representation of a message passed between instances.
 * @author Del Myers
 *
 */
public interface IMessage extends ITraceModel {
	
	/**
	 * Returns the activation that this message is attached to. Depending on context, this
	 * may be where the message starts, or where it ends.
	 * @return the activation.
	 */
	IActivation getActivation();
	
	/**
	 * Returns the number of milliseconds from the start of the trace at which this
	 * message occurred.
	 * @return the number of milliseconds from the start of the trace at which this
	 * message occurred.
	 */
	long getTime();
	
	/**
	 * Returns the line of code at which this message occurred.
	 * @return the line of code at which this message occurred.
	 */
	int codeLine();
	
	/**
	 * Returns the order in which this message occurred within the trace. 
	 * @return the order in which this message occurred within the trace. 
	 */
	long getOrder();
	
	IThread getThread();

}
