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

import java.util.HashMap;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Creates a call tree based on Java Source code.
 * @author Del Myers
 */

public class JavaCallTree {
	private HashMap<IType, ASTNode> parseCache;
	private IJavaActivation root;
	
	
	/**
	 * Creates a call tree.
	 * @param method
	 */
	private JavaCallTree() {
		 parseCache = new HashMap<IType, ASTNode>();
	}
	
	public ASTNode parse(IType type) {
		if (!parseCache.containsKey(type)) {
			if (parseCache.size() > 20) {
				//waste a little time in favour of saving space.
				clearCache();
			}
			ASTNode node = ASTUTils.getASTFor(type);
			parseCache.put(type, node);
		}
		return parseCache.get(type);
	}
	
	public void clearCache() {
		parseCache.clear();
	}

	/**
	 * @return
	 */
	public static JavaCallTree createTree(IMethod method) {
		JavaCallTree tree = new JavaCallTree();
		IJavaActivation activation = new JavaRootActivation(tree, method);
		tree.root = activation;
		return tree;
	}
	
	/**
	 * @return the root
	 */
	public IJavaActivation getRoot() {
		return root;
	}

}
