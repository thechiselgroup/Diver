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

import java.util.Collection;

/**
 * A representation of a data type that was accessed during a program trace. Does not
 * take into account polymorphism. It is a simple representation of the static
 * structure of the traced program.
 * 
 * @author Del Myers
 *
 */
public interface ITraceClass extends ITraceModel {

	/**
	 * Returns the qualified and unique name of the type.
	 * @return the qualified and unique name of the type.
	 */
	String getName();
	
	/**
	 * Returns an unmodifiable list of methods on this class, ordered by method name and
	 * signature.
	 * @return an unmodifiable list of methods on this class, ordered by name and signature.
	 */
	Collection<ITraceClassMethod> getMethods();
	
	/**
	 * Finds the method for the given name and signature. Returns null if it doesn't exist.
	 * @param name the name
	 * @param signature the signature
	 * @return the found method, or null if none.
	 */
	ITraceClassMethod findMethod(String name, String signature);
	
	/**
	 * Returns the trace that this class exists in.
	 * @return the trace that this class exists in.
	 */
	ITrace getTrace();
	
	/*
	 * Returns a list of identifiers for instances of this class. It is normally
	 * not recommended that the instances be accessed directly, as there can be
	 * very many of them. Consider using instanceCount() 
	 * @return a list of identifiers for instances of this class.
	 *
	List<String> getInstances();
	
	/*
	 * Returns the number of instances of this class.
	 * @return the number of instances of this class.
	 *
	long instanceCount();
	*/
}
