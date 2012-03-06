/*******************************************************************************
 * Copyright 2005-2007, CHISEL Group, University of Victoria, Victoria, BC, Canada.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Chisel Group, University of Victoria
 *******************************************************************************/
package ca.uvic.chisel.diver.sequencediagrams.sc.java.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * An object representing an object or class in a Java program.
 * @author Del Myers
 */

public class JavaObject implements IAdaptable, IJavaCallModel {

	private IType type;
	private ASTNode astNode;
	private String identifier;
	private JavaCallTree tree;

	public JavaObject(JavaCallTree tree, IType type) {
		this(tree, type, "");
	}
	
	public JavaObject(JavaCallTree tree, IType type, String identifier) {
		this.type = type;
		this.astNode = null;
		this.identifier = identifier;
		this.tree = tree;
	}
	
	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if (ASTNode.class.isAssignableFrom(adapter)) {
			return getAST();
		}
		//try the java element's adapter
		if (type != null) {
			return getJavaElement().getAdapter(adapter);
		}
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj != null && getClass() == obj.getClass()) {
			IJavaElement je = getJavaElement();
			JavaObject that = (JavaObject) obj;
			if (je != null) {
				return je.equals(that.getJavaElement());
			} else if (that.getJavaElement() == null) {
				return this.identifier.equals(that.identifier);
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		if (getJavaElement() == null) {
			return identifier.hashCode();
		}
		return getJavaElement().hashCode() * identifier.hashCode();
	}

	public ASTNode getAST() {
		if (this.astNode == null) {
			if (type != null) {
				astNode = getTree().parse(type);
			}
		}
		return astNode;
	}

	public IJavaElement getJavaElement() {
		return type;
	}

	public JavaCallTree getTree() {
		return tree;
	}
	
	

}
