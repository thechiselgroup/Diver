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

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Type;

import ca.uvic.chisel.diver.sequencediagrams.sc.java.StaticSequenceEditorPlugin;

/**
 * Static methods for retrieving AST information.
 * @author Del Myers
 */

public class ASTUTils {
	private static ASTParser parser = ASTParser.newParser(AST.JLS3);
	
	public static MethodDeclaration findMethodDeclaration(ASTNode rootNode, IMethod method) {
		if (rootNode != null) {
			ISourceRange range;
			try {
				range = method.getSourceRange();
				NodeFinder finder = new NodeFinder(range.getOffset(), range.getLength());
				rootNode.accept(finder);
				return (MethodDeclaration) finder.getCoveredNode();
			} catch (JavaModelException e) {
			}
		}
		return null;
	}
	
	
	public static ASTNode getASTFor(IType type) {
		try {
			ICompilationUnit unit = type.getCompilationUnit();
			if (unit != null) {
				return getASTFor(unit);
			}
			IClassFile classFile = type.getClassFile();
			if (classFile != null) {
				return getASTFor(classFile);
			}
		} catch (Exception e) {}
		return null;
	}
	
	
	/**
	 * @param classFile
	 * @return
	 */
	public static ASTNode getASTFor(IClassFile unit) {
		if (!unit.isOpen()) {
			try {
				unit.open(new NullProgressMonitor());
			} catch (JavaModelException e) {
				StaticSequenceEditorPlugin.getDefault().getLog().log(e.getStatus());
			}
		}
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return parser.createAST(new NullProgressMonitor());
	}

	/**
	 * @param unit
	 * @return
	 */
	public static ASTNode getASTFor(ICompilationUnit unit) {
		if (!unit.isOpen()) {
			try {
				unit.open(new NullProgressMonitor());
			} catch (JavaModelException e) {
				StaticSequenceEditorPlugin.getDefault().getLog().log(e.getStatus());
			}
		}
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return parser.createAST(new NullProgressMonitor());
		
	}


	/**
	 * Returns the corresponding return type for the given method invocation, or
	 * null if it could not be found.
	 * @param unresolved
	 */
	public static Type findReturnType(MethodInvocation invocation) {
		MethodDeclaration declaration = findDeclarationFor(invocation);
		if (declaration != null) {
			return declaration.getReturnType2();
		}
		return null;
	}
	
	public static MethodDeclaration findDeclarationFor(MethodInvocation invocation) {
		IMethodBinding binding = invocation.resolveMethodBinding();
		if (binding != null) {
			IMethod method = (IMethod) binding.getJavaElement();
			IType declaringType = method.getDeclaringType();
			ASTNode typeNode = getASTFor(declaringType);
			if (typeNode != null) {
				return findMethodDeclaration(typeNode, method);
			}
		}
		return null;
	}

}
