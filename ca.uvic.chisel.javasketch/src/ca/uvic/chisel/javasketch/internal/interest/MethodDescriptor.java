/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.internal.interest;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;

/**
 * A simple descriptor for methods
 * @author Del
 *
 */
public class MethodDescriptor implements Comparable<MethodDescriptor> {
	public final String name;
	public final String type;
	public final String signature;
	
	private final String string;
	
	private static final Map<String, IType> cachedTypes;
	private static final Map<MethodDescriptor, IMethod> cachedMethods;
	static {
		cachedTypes = Collections.synchronizedMap(new TreeMap<String, IType>());
		cachedMethods = Collections.synchronizedMap(new TreeMap<MethodDescriptor, IMethod>());
	}
	
	/**
	 * 
	 */
	public MethodDescriptor(String type, String name, String signature) {
		this.type = type;
		this.name = name;
		this.signature = signature;
		this.string = type +"."+ name + signature;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return string.hashCode();
	}
	
	public boolean equals(Object that) {
		if (!that.getClass().equals(getClass())) {
			return false;
		}
		return string.equals(((MethodDescriptor)that).string);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return string;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MethodDescriptor that) {
		return this.string.compareTo(that.string);
	}
	
	protected IMethod getMethod(IJavaSearchScope scope) {
		IMethod method = cachedMethods.get(this);
		if (method == null) {
			try {
				IMethod jMethod = (IMethod) JavaSearchUtils.searchForMethod(scope, new NullProgressMonitor(), this.type, name, signature);
				if (jMethod != null) {
					cachedMethods.put(this, jMethod);
					cachedTypes.put(this.type, jMethod.getDeclaringType());
					return jMethod;
				}
			} catch (CoreException e) {
				// do nothing
			} catch (InterruptedException e) {
				//do nothing
			}
		} else {
			return method;
		}
		System.out.println("Missing method " + this);
		return null;
	}
	
	
}
