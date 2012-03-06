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
package ca.uvic.chisel.diver.sequencediagrams.sc.java.model;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * @author Del
 *
 */
public class JavaRootActivation implements IJavaActivation, IAdaptable {

	private IMethod method;
	private JavaCallTree tree;
	private List<JavaMessage> messages;
	private MethodDeclaration astNode;
	private JavaObject lifeline;

	public JavaRootActivation(JavaCallTree tree, IMethod root) {
		this.method = root;
		this.tree = tree;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (ASTNode.class.isAssignableFrom(adapter)) {
			return getAST();
		}
		//try the java element's adapter
		return getJavaElement().getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chise.diver.sequencediagrams.sc.java.model.IJavaCallModel#getJavaElement()
	 */
	@Override
	public IJavaElement getJavaElement() {
		return method;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chise.diver.sequencediagrams.sc.java.model.IJavaCallModel#getAST()
	 */
	@Override
	public ASTNode getAST() {
		if (astNode == null) {
			ASTNode rootNode = tree.parse(method.getDeclaringType());
			this.astNode = ASTUTils.findMethodDeclaration(rootNode, method);
		}
		return astNode;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chise.diver.sequencediagrams.sc.java.model.IJavaCallModel#getTree()
	 */
	@Override
	public JavaCallTree getTree() {
		return tree;
	}
	
	public List<JavaMessage> getMessages() {
		if (messages == null) {
			this.messages = new LinkedList<JavaMessage>();
			JavaActivation target = new JavaActivation(tree, method);
			JavaMessage message = new JavaMessage(tree, getAST(), this, target);
			target.setCallingMessage(message);
			messages.add(message);
		}
		return messages;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chise.diver.sequencediagrams.sc.java.model.IJavaActivation#getCallingMessage()
	 */
	@Override
	public JavaMessage getCallingMessage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chise.diver.sequencediagrams.sc.java.model.IJavaActivation#getLifeLine()
	 */
	@Override
	public JavaObject getLifeLine() {
		if (lifeline == null) {
			lifeline = new JavaObject(tree, null, "USER");
		}
		return lifeline;
	}

}
