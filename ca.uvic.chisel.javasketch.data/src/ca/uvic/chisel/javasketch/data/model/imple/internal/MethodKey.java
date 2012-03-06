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
package ca.uvic.chisel.javasketch.data.model.imple.internal;


public class MethodKey implements Comparable<MethodKey>{
	public final String type;
	public final String name;
	public final String signature;
	private String fullName;
	public MethodKey(String type, String name, String signature) {
		this.type = type;
		this.name = name;
		this.signature = signature;
	}
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(MethodKey o) {
		if (o == null) {
			return 1;
		}
		return toString().compareTo(o.toString());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (fullName == null) {
			fullName = type + "." + name + signature;
		}
		return fullName;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!obj.getClass().equals(this.getClass())) {
			return false;
		}
		return toString().equals(((MethodKey)obj).toString());
	}
	
}