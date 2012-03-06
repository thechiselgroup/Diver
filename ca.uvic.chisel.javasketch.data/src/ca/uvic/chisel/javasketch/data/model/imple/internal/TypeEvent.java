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

import java.util.TreeSet;

import ca.uvic.chisel.javasketch.data.model.ITypeEvent;
import ca.uvic.chisel.javasketch.data.model.TraceEventType;

/**
 * An event that indicates that the types in a trace have changed.
 * @author Del Myers
 *
 */
public class TypeEvent extends TraceEvent implements ITypeEvent {

	private TreeSet<String> classNames;

	/**
	 * @param trace
	 * @param type
	 */
	public TypeEvent(TraceImpl trace) {
		super(trace, TraceEventType.TypeEventType);
		this.classNames = new TreeSet<String>();
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITypeEvent#getClassNames()
	 */
	public String[] getClassNames() {
		return classNames.toArray(new String[classNames.size()]);
	}
	
	boolean addClassName(String name) {
		return classNames.add(name);
	}

}
