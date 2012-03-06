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
package ca.uvic.chisel.javasketch.internal.ast;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Type;

import ca.uvic.chisel.javasketch.SketchPlugin;

/**
 * Static methods for retrieving AST information.
 * @author Del Myers
 */

public class ASTUTils {
	private static ASTParser parser = ASTParser.newParser(AST.JLS3);
	private static HashMap<IType, WeakReference<ASTNode>> cache = new HashMap<IType, WeakReference<ASTNode>>();
	static {
		parser.setResolveBindings(true);
	}
	
	public static MethodDeclaration findMethodDeclaration(ASTNode rootNode, IMethod method) {
		if (rootNode != null) {
			ISourceRange range;
			try {
				if (!method.getOpenable().isOpen()) {
					method.getOpenable().open(new NullProgressMonitor());
				}
				range = method.getSourceRange();
				NodeFinder finder = new NodeFinder(rootNode, range.getOffset(), range.getLength());
				ASTNode node = finder.getCoveredNode();
				if (node instanceof MethodDeclaration) {
					return (MethodDeclaration) node;
				} else {
					return null;
				}
			} catch (JavaModelException e) {
				SketchPlugin.getDefault().log(e);
			}
		}
		return null;
	}
	
	
	public static ASTNode getASTFor(IType type) {
		try {
			synchronized (cache) {
				if (cache.containsKey(type)) {
					WeakReference<ASTNode> ref = cache.get(type);
					if (!(ref.isEnqueued() || ref.get() == null)) {
						return ref.get();
					} else {
						cache.remove(type);
					}
				}
				ICompilationUnit unit = type.getCompilationUnit();
				ASTNode node = null;
				if (unit != null) {
					node = getASTFor(unit);
				}
				IClassFile classFile = type.getClassFile();
				if (classFile != null) {
					node = getASTFor(classFile);
				}
				if (node != null) {
					cache.put(type, new WeakReference<ASTNode>(node));
				}
				return node;
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
				SketchPlugin.getDefault().log(e);
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
				SketchPlugin.getDefault().log(e);
			}
		}
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return parser.createAST(new NullProgressMonitor());
		
	}
	
	public static ASTNode getASTFor(ITypeRoot typeRoot) {
		if (typeRoot instanceof ICompilationUnit) {
			return getASTFor((ICompilationUnit)typeRoot);
		} else if (typeRoot instanceof IClassFile) {
			return getASTFor((IClassFile)typeRoot);
		}
		return null;
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
