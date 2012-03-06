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
 * The super-type of all trace model elements.
 * @author Del Myers
 *
 */
public interface ITraceModel {
	//the different kinds of model elements
	public static final int STATIC_ELEMENT = 1;
	public static final int DYNAMIC_ELEMENT =  1 << 1;
	public static final int MESSAGE  = 1 << 2 | DYNAMIC_ELEMENT;
	public static final int ORIGIN_MESSAGE = (1 << 3) | MESSAGE;
	public static final int TARGET_MESSAGE = (1 << 4) | MESSAGE;
	public static final int TRACE_CLASS = (1 << 5) | STATIC_ELEMENT;
	public static final int TRACE_CLASS_METHOD = (1 << 6) | STATIC_ELEMENT;
	public static final int CALL = (1 << 5) | ORIGIN_MESSAGE;
	public static final int ARRIVAL = (1 << 5) | TARGET_MESSAGE;
	public static final int REPLY = (1 << 6) | ORIGIN_MESSAGE;
	public static final int RETURN = (1 << 6) | TARGET_MESSAGE;
	public static final int THROW = (1 << 7) | ORIGIN_MESSAGE;
	public static final int CATCH = (1 << 7) | TARGET_MESSAGE;
	public static final int ACTIVATION = (1 << 6) | DYNAMIC_ELEMENT;
	public static final int THREAD = (1 << 5) | DYNAMIC_ELEMENT;
	public static final int TRACE = (1 << 5);
	public static final int EVENT = (1 << 6);

	/**
	 * Returns the trace that is the root of this model.
	 * @return the trace that is the root of this model.
	 */
	ITrace getTrace();
	
	/**
	 * Returns a unique identifier for this model element within the trace.
	 * @return
	 */
	public String getIdentifier();
	
	/**
	 * Returns true if and only if the current model element is valid. An
	 * Equivalent model element may no longer exist in the model. Therefore
	 * if this element is not valid, clients should query the parent trace to 
	 * find a fresh copy of the new equivalent model element, if it exists.
	 */
	public boolean isValid();
	
	/**
	 * Returns the element kind represented by this model element.
	 * @return
	 */
	public int getKind();
}
