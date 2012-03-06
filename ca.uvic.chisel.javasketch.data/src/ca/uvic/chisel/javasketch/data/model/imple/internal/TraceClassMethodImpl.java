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

import java.sql.SQLException;

import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;

/**
 * @author Del Myers
 *
 */
public class TraceClassMethodImpl extends TraceModelViewBase implements
		ITraceClassMethod {

	private TraceClassImpl traceClass;
	private String name;
	private String signature;

	/**
	 * @throws SQLException 
	 * 
	 */
	public TraceClassMethodImpl(TraceClassImpl tc, String methodName, String methodSignature)  {
		super((TraceImpl)tc.getTrace(), getIdentifier(tc.getName(), methodName, methodSignature));
		this.traceClass = tc;
		this.name = methodName;
		this.signature = methodSignature;
	}
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#load()
	 */
	@Override
	public void load() {
		//nothing to do
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#unload()
	 */
	@Override
	public void unload() {
		//nothing to do
	}
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceClassMethod#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceClassMethod#getSignature()
	 */
	public String getSignature() {
		return signature;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceClassMethod#getTraceClass()
	 */
	public ITraceClass getTraceClass() {
		return traceClass;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getTraceClass().toString() + "." + getName();
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getKind()
	 */
	public int getKind() {
		return TRACE_CLASS_METHOD;
	}
	
	public static final String getIdentifier(String className, String methodName, String methodSignature) {
		return "[METHOD],"+className+"."+methodName+methodSignature;
	}
}
