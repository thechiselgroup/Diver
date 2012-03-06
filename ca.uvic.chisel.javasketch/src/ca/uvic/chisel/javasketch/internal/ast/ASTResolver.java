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

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;

/**
 * A public class for resolving AST nodes for calls.
 * @author Del Myers
 *
 */
public class ASTResolver {
	
	private ASTNode rootNode;

	private IDocument document;
	
	private int connectionCount;

	private ITypeRoot typeRoot;
	
	private static HashMap<ITypeRoot, ASTResolver> cache = new HashMap<ITypeRoot, ASTResolver>();

	private ASTResolver(ITypeRoot typeRoot, ASTNode rootNode, IDocument document) {
		this.rootNode = rootNode;
		this.document = document;
		this.typeRoot = typeRoot;
	}
	
	
	
	/**
	 * Connects an AST resolver to 
	 * @param modelElement
	 * @return
	 */
	public ASTResolver connect(ITraceModel modelElement, IProgressMonitor monitor) {
		try {
			IJavaElement element = JavaSearchUtils.findElement(modelElement, monitor);
			if (element instanceof IMember) {
				IMember member = (IMember) element;
				ITypeRoot root = member.getTypeRoot();
				synchronized (cache) {
					ASTResolver resolver = cache.get(root);
					if (resolver == null) {
						String source = root.getSource();
						if (source != null) {
							IDocument document = new Document(source);
							ASTNode node = ASTUTils.getASTFor(root);
							if (node != null) {
								resolver = new ASTResolver(typeRoot, rootNode, document);
								cache.put(root, resolver);
							}
						}
					}
					resolver.connectionCount++;
					return resolver;
				}
			}
		} catch (JavaModelException e) {
		} catch (InterruptedException e) {
		} catch (CoreException e) {
		}
		return null;
	}
	
	
	public ASTNode findNode(IOriginMessage message) {
		ASTMessageFinder finder = new ASTMessageFinder(message, document);
		rootNode.accept(finder);
		return finder.getNode();
	}
	
	public void disconnect() {
		synchronized (cache) {
			connectionCount--;
			if (connectionCount == 0) {
				cache.remove(typeRoot);
			}
		}
	}

}
