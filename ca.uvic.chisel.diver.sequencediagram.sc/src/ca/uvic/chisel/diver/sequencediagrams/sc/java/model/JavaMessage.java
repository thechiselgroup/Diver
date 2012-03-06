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

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * A class representing a message from one java object to another. This can be
 * a method call, a return, or a throw.
 * @author Del Myers
 */

public class JavaMessage implements IAdaptable, IJavaCallModel {
	
	private JavaCallTree tree;
	private ASTNode representingNode;
	private IJavaElement element;
	private IJavaActivation origin;
	private IJavaActivation target;
	private List<TryStatement> tries;
	private IType type;
	private boolean exception;
	private String typeString;

	public JavaMessage(JavaCallTree tree, ASTNode representingNode, IJavaActivation origin, IJavaActivation target) {
		this.tree = tree;
		this.representingNode = representingNode;
		this.origin = origin;
		this.target = target;
		if (!(
				(representingNode instanceof MethodInvocation) ||
				(representingNode instanceof SuperMethodInvocation) ||
				(representingNode instanceof ConstructorInvocation) ||
				(representingNode instanceof ClassInstanceCreation) ||
				(representingNode instanceof SuperConstructorInvocation) ||
				(representingNode instanceof ReturnStatement) ||
				(representingNode instanceof ThrowStatement) ||
				(representingNode instanceof MethodDeclaration)
			)) {
			throw new IllegalArgumentException();
		}
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if (ASTNode.class.isAssignableFrom(adapter)) {
			return getAST();
		}
		if (getJavaElement() != null) {
			return getJavaElement().getAdapter(adapter);
		}
		return null;
	}

	public ASTNode getAST() {
		return representingNode;
	}

	public IJavaElement getJavaElement() {
		if (element == null) {
			IMethodBinding binding = null;
			if (getAST() instanceof MethodInvocation) {
				binding = ((MethodInvocation)getAST()).resolveMethodBinding();
			} else if (getAST() instanceof SuperMethodInvocation) {
				binding = ((SuperMethodInvocation)getAST()).resolveMethodBinding();
			} else if (getAST() instanceof ConstructorInvocation) {
				binding = ((ConstructorInvocation)getAST()).resolveConstructorBinding();
			} else if (getAST() instanceof SuperConstructorInvocation) {
				binding = ((SuperConstructorInvocation)getAST()).resolveConstructorBinding();
			} else if (getAST() instanceof ClassInstanceCreation) {
				binding = ((ClassInstanceCreation)getAST()).resolveConstructorBinding();
			} else if (getAST() instanceof ReturnStatement) {
				return type;
			} else if (getAST() instanceof ThrowStatement) {
				return type;
			} else if (getAST() instanceof MethodDeclaration) {
				return target.getJavaElement();
			}
			if (binding != null) {
				element = binding.getJavaElement();
			} else {
				element = type;
			}
		}
		return element;
	}

	public JavaCallTree getTree() {
		return tree;
	}

	/**
	 * @param list
	 */
	void setTries(List<TryStatement> list) {
		this.tries = list;
	}
	
	/**
	 * Returns true if this message was passed within a try block that catches the given
	 * type.
	 * @param type
	 * @return
	 */
	public boolean catches(IType type) {
		if (tries == null) {
			return false;
		}
		for (TryStatement statement : tries) {
			for (Object o : statement.catchClauses()) {
				CatchClause catcher = (CatchClause) o;
				ITypeBinding binding = catcher.getException().getType().resolveBinding();
				if (binding != null) {
					IType caughtType = (IType) binding.getJavaElement();
					if (caughtType != null) {
						try {
							ITypeHierarchy hierarchy = caughtType.newSupertypeHierarchy(new NullProgressMonitor());
							if (caughtType.equals(type) || hierarchy.contains(type)) {
								return true;
							}
						} catch (JavaModelException e) {
							
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * @return
	 */
	public IJavaActivation getSource() {
		return origin;
	}

	/**
	 * @return
	 */
	public IJavaActivation getTarget() {
		return target;
	}

	/**
	 * @param thrown
	 */
	public void setType(IType type) {
		this.type = type;		
	}

	/**
	 * @param b
	 */
	public void setException(boolean b) {
		this.exception = b;		
	}
	
	/**
	 * @return the exception
	 */
	public boolean isException() {
		return exception;
	}

	/**
	 * Sets the type to the given string representation.
	 * @param returnType
	 */
	public void setType(String returnType) {
		this.typeString = returnType;		
	}
	
	@Override
	public String toString() {
		if (type != null) {
			return type.getElementName();
		}
		if (typeString != null) {
			return Signature.toString(typeString);
		}
		return super.toString();
	}

}
