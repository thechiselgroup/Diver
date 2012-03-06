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
package ca.uvic.chisel.javasketch.data.model.imple.internal;

import java.util.HashSet;

import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IActivationEvent;
import ca.uvic.chisel.javasketch.data.model.TraceEventType;

/**
 * An event indicating that changes have occurred to activations in the trace model. Clients
 * are only made aware of events that occur on activations that have already been loaded in
 * the model from persistent storage in order to save on processing time.
 * @author Del Myers
 *
 */
public class ActivationEvent extends TraceEvent implements IActivationEvent {

	private HashSet<IActivation> activations;

	/**
	 * @param type
	 */
	public ActivationEvent(TraceImpl trace) {
		super(trace, TraceEventType.ActivationEventType);
		this.activations = new HashSet<IActivation>();
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IActivationEvent#getActivation()
	 */
	public IActivation[] getActivations() {
		return activations.toArray(new IActivation[activations.size()]);
	}
	
	boolean addActivation(IActivation activation) {
		return activations.add(activation);
	}

}
