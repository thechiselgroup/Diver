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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.IReply;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;

/**
 * An AST visitor that locates the AST Node in which a call or reply occurs.
 * @author Del Myers
 *
 */
public class ASTMessageFinder extends GenericVisitor {
	
	private IDocument document;
	private IMessage message;
	private ASTNode node;

	public ASTMessageFinder(IMessage message, IDocument document) {
		this.message = message;
		this.document = document;
	}
	
	public ASTMessageFinder(ICall call, IDocument document) {
		this((IMessage)call, document);
	}
	
	public ASTMessageFinder(IReply reply, IDocument document) {
		this((IMessage)reply, document);
	}
	
	@Override
	public boolean visit(MethodInvocation node) {
		if (!(message instanceof ICall)) return false;
		if (containsMessage(node)) {
			ICall call = (ICall) message;
			
			IMethodBinding binding = node.resolveMethodBinding();
			if (binding != null) {
				binding = binding.getMethodDeclaration();
				if (binding != null) {
					IJavaElement element = binding.getJavaElement();
					if (element instanceof IMethod) {
						try {
							IMethod jm = (IMethod) element;
							//get the target method.
							ITraceClassMethod am = call.getTarget().getActivation().getMethod();
							String types[] = Signature.getParameterTypes(am.getSignature());
							IMethod testMethod = jm.getDeclaringType().getMethod(am.getName(), types);
							if (jm.isSimilar(testMethod)) {
								this.node = node;
								try {
									if (document.getLineOfOffset(node.getStartPosition()) != (call.codeLine() - 1))
										//look for a better match.
										return true;
								} catch (BadLocationException e) {
								}
								return false;
							}
						} catch (NullPointerException e) {
							return false;
						}
					}
				}
			}
			return true;
		}
		
		return false;
	}
	
	public boolean visit(SuperMethodInvocation node) {
		if (!(message instanceof ICall)) return false;
		if (containsMessage(node)) {
			ICall call = (ICall) message;
			
			IMethodBinding binding = node.resolveMethodBinding();
			if (binding != null) {
				binding = binding.getMethodDeclaration();
				if (binding != null) {
					IJavaElement element = binding.getJavaElement();
					if (element instanceof IMethod) {
						try {
							IMethod jm = (IMethod) element;
							//get the target method.
							ITraceClassMethod am = call.getTarget().getActivation().getMethod();
							String types[] = Signature.getParameterTypes(am.getSignature());
							IMethod testMethod = jm.getDeclaringType().getMethod(am.getName(), types);
							if (jm.isSimilar(testMethod)) {
								this.node = node;
								try {
									if (document.getLineOfOffset(node.getStartPosition()) != (call.codeLine() - 1))
										//look for a better match.
										return true;
								} catch (BadLocationException e) {
								}
								return false;
							}
						} catch (NullPointerException e) {
							return false;
						}
					}
				}
			}
			return true;
		}
		
		return false;
	}
	
	public boolean visit(SuperConstructorInvocation node) {
		if (!(message instanceof ICall)) return false;
		if (containsMessage(node)) {
			ICall call = (ICall) message;
			IMethodBinding binding = node.resolveConstructorBinding();
			if (binding != null) {
				binding = binding.getMethodDeclaration();
				if (binding != null) {
					IJavaElement element = binding.getJavaElement();
					if (element instanceof IMethod) {
						try {
						IMethod jm = (IMethod) element;
						//get the target method.
						ITraceClassMethod am = call.getTarget().getActivation().getMethod();
						if (JavaSearchUtils.getFullyQualifiedName(jm.getDeclaringType(), true).equals(am.getTraceClass().getName())) {
							String types[] = Signature.getParameterTypes(am.getSignature());
							IMethod testMethod = jm.getDeclaringType().getMethod(am.getName(), types);
							if (jm.isSimilar(testMethod)) {
								this.node = node;
								try {
									if (document.getLineOfOffset(node.getStartPosition()) != (call.codeLine() - 1))
										//look for a better match.
										return true;
								} catch (BadLocationException e) {
								}
								return false;
							}
						}
						} catch (NullPointerException e) {
							return false;
						}
					}
				}
			}
			return true;
		}
		
		return false;
	}

	@Override
	public boolean visit(ConstructorInvocation node) {
		if (!(message instanceof ICall)) return false;
		if (containsMessage(node)) {
			ICall call = (ICall) message;
			IMethodBinding binding = node.resolveConstructorBinding();
			if (binding != null) {
				binding = binding.getMethodDeclaration();
				if (binding != null) {
					IJavaElement element = binding.getJavaElement();
					if (element instanceof IMethod) {
						try {
						IMethod jm = (IMethod) element;
						//get the target method.
						ITraceClassMethod am = call.getTarget().getActivation().getMethod();
						if (JavaSearchUtils.getFullyQualifiedName(jm.getDeclaringType(), true).equals(am.getTraceClass().getName())) {
							String types[] = Signature.getParameterTypes(am.getSignature());
							IMethod testMethod = jm.getDeclaringType().getMethod(am.getName(), types);
							if (jm.isSimilar(testMethod)) {
								this.node = node;
								try {
									if (document.getLineOfOffset(node.getStartPosition()) != (call.codeLine() - 1))
										//look for a better match.
										return true;
								} catch (BadLocationException e) {
								}
								return false;
							}
						}
						} catch (NullPointerException e) {
							return false;
						}
					}
				}
			}
			return true;
		}
		
		return false;
	}
	
	public boolean visit(ClassInstanceCreation node) {
		if (!(message instanceof ICall)) return false;
		if (containsMessage(node)) {
			ICall call = (ICall) message;
			IMethodBinding binding = node.resolveConstructorBinding();
			if (binding != null) {
				binding = binding.getMethodDeclaration();
				if (binding != null) {
					IJavaElement element = binding.getJavaElement();
					if (element instanceof IMethod) {
						try {
						IMethod jm = (IMethod) element;
						//get the target method.
						ITraceClassMethod am = call.getTarget().getActivation().getMethod();
						if (JavaSearchUtils.getFullyQualifiedName(jm.getDeclaringType(), true).equals(am.getTraceClass().getName())) {
							String types[] = Signature.getParameterTypes(am.getSignature());
							IMethod testMethod = jm.getDeclaringType().getMethod(am.getName(), types);
							if (jm.isSimilar(testMethod)) {
								this.node = node;
								try {
									if (document.getLineOfOffset(node.getStartPosition()) != (call.codeLine() - 1))
										//look for a better match.
										return true;
								} catch (BadLocationException e) {
								}
								return false;
							}
						}
						} catch (NullPointerException e) {
							return true;
						}
					} else {
						//try to match just on the class name
						ITypeBinding typeBinding = binding.getDeclaringClass();
						IJavaElement je = typeBinding.getJavaElement();
						if (je instanceof IType) {
							IType type = (IType) je;
							try {
								ITraceClassMethod am = call.getTarget().getActivation().getMethod();
								if (JavaSearchUtils.getFullyQualifiedName(type, true).equals(am.getTraceClass().getName())) {
									this.node = node;
									try {
										if (document.getLineOfOffset(node.getStartPosition()) != (call.codeLine() - 1))
											//look for a better match.
											return true;
									} catch (BadLocationException e) {
									}
									return false;
								}
							} catch (NullPointerException e) {
								return true;
							}
						}
					}
				}
			}
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean visit(ReturnStatement node) {
		if (!(message instanceof IReply)) return false;
		if (containsMessage(node)) {
			//we can only assume that it is the correct one at this point.
			this.node = node;
			return true;
		}
		return false;
	}

	@Override
	public boolean visit(ThrowStatement node) {
		if (!(message instanceof IReply)) return false;
		if (containsMessage(node)) {
			//we can only assume that it is the correct one at this point.
			this.node = node;
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.internal.ast.groups.GenericVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		if (message.codeLine() < 0) {
			this.node = node;
			return false;
		}
		return super.visit(node);
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.internal.ast.groups.GenericVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	@Override
	protected boolean visitNode(ASTNode node) {
		if (containsMessage(node)) {
			return true;
		}
		return false;
	}
	
	boolean containsMessage(ASTNode node) {
		try {
			int start = node.getStartPosition();
			int end = start + node.getLength();
			int lineStart = document.getLineOfOffset(start);
			int lineEnd = document.getLineOfOffset(end);
			if (lineStart <= message.codeLine()-1) {
				if (lineEnd >= message.codeLine()-1) {
					return true;
				}
			}
		} catch (BadLocationException e) {
			SketchPlugin.getDefault().log(e);
		}
		return false;
	}
	
	
	/**
	 * @return the node that was found by this visitor, or null if none.
	 */
	public ASTNode getNode() {
		return node;
	}
	

}
