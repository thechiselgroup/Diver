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

public enum TraceEventType {
	/**
	 * Indicates that a change has occurred in an activation. Clients will only be notified
	 * of changes that have occurred on activations that have already been loaded from 
	 * permanent storage into memory. This will normally occur from some sort of user
	 * interaction, or from a traversal of the model. The reason for this is that
	 * there can be many, many thousands of activations in the model and being notified
	 * of all of them will degrade system performance considerably. Events of this
	 * type may be cast to {@link IActivationEvent}
	 */
	ActivationEventType,
	/**
	 * Indicates that the threads in the trace have changed. Events of this type may be cast to 
	 * {@link IThreadEvent}
	 */
	ThreadEventType,
	/**
	 * Indicates that a change has occurred in the classes that are stored in the model.
	 * Events of this type may be cast to {@link ITypeEvent}
	 */
	TypeEventType,
	/**
	 * Indicates that the methods on a class have changed. Events of this type may be cast to
	 * {@link IMethodEvent}
	 */
	MethodEventType
}