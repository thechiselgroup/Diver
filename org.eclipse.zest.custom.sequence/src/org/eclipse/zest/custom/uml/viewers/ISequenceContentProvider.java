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
 * A content provider for generating UML Sequence diagrams. Sequence
 * Diagrams are represented as a list of <i>activations</i>. An activation
 * is an event that occurs in time and causes a spawning of execution. Each
 * activation has a class that it is activated on, a time that it occurs,
 * and a length of execution. The times and lengths given by this content
 * provider are not absolute. They are relative, and only used to position
 * elements relatively to one another. Times and durations of sub-activations
 * are relative to the parent activation.
 * 
 * Classes, activations, and return values will be sent onto the label provider to get
 * label information about them.
 * @author Del Myers
 * @deprecated
 */
public interface ISequenceContentProvider extends IStructuredContentProvider {
	
	/**
	 * Returns the root-level activations for the sequence diagram.
	 * An entire sequence diagram can be generated from one single
	 * activation, so it is expected that this method returns a single-element
	 * array.
	 */
	public Object[] getElements(Object inputElement);
	
	/**
	 * Returns the child activations that occur on the given activation.
	 * This may spawn, again, new activations, and create new classes for the
	 * diagram.
	 * @param activation the activation for which children will be created.
	 * @return the child activations.
	 */
	public Object[] getChildren(Object activation);
	
	/**
	 * Returns true iff the given activation has child activations. Will be
	 * used to determine if the activation can be expanded/contracted.
	 * @param activation
	 * @return
	 */
	public boolean hasChildren(Object activation);
	
	/**
	 * Returns the target object for the given activation. If one is not
	 * known in the model, then new activations will create a target class.
	 * @param activation
	 * @return an object representing the class that the activation occurrs on.
	 */
	public Object getTargetObject(Object activation);
	
	/**
	 * Gets information that is returned by the end of the process that the
	 * activation spawns.
	 * @param activation the activation.
	 * @return the information that is returned by the end of the process
	 * that the activation spawns.
	 */
	public Object getReturnValue(Object activation);

}
