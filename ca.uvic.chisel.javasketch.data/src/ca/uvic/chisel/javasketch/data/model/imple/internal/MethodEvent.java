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

import ca.uvic.chisel.javasketch.data.model.IMethodEvent;
import ca.uvic.chisel.javasketch.data.model.TraceEventType;

/**
 * @author Del Myers
 *
 */
public class MethodEvent extends TraceEvent implements IMethodEvent {

	private TreeSet<String> methods;

	/**
	 * @param trace
	 * @param type
	 */
	MethodEvent(TraceImpl trace) {
		super(trace, TraceEventType.MethodEventType);
		this.methods = new TreeSet<String>();
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.IMethodEvent#getMethodIDs()
	 */
	public String[] getMethodIDs() {
		return methods.toArray(new String[methods.size()]);
	}
	
	boolean addMethod(String id) {
		return methods.add(id);
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.IMethodEvent#getName(java.lang.String)
	 */
	public String getName(String methodID) {
		if (methodID == null) {
			return null;
		}
		String methodAndClassName = getMethodAndClassName(methodID);
		int dot = methodAndClassName.lastIndexOf('.');
		if (dot >= 0 && dot < methodAndClassName.length()-1) {
			return methodAndClassName.substring(dot+1);
		}
		return "";
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.IMethodEvent#getSignature(java.lang.String)
	 */
	public String getSignature(String methodID) {
		if (methodID == null) {
			return null;
		}
		int paren = methodID.indexOf('(');
		if (paren >= 0) {
			return methodID.substring(paren);
		}
		return "";
	}
	
	public String getTypeName(String methodID) {
		if (methodID == null) return null;
		String methodAndClassName = getMethodAndClassName(methodID);
		int dot = methodAndClassName.lastIndexOf('.');
		if (dot >= 0) {
			return methodAndClassName.substring(0, dot);
		}
		return "";
	}
	
	private String getMethodAndClassName(String methodID) {
		int paren = methodID.indexOf('(');
		if (paren >= 0) {
			return methodID.substring(0, paren);
		}
		return "";
	}
	

}
