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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * A class representing an activation on a java object.
 * @author Del Myers
 */

public class JavaActivation implements IAdaptable,  IJavaActivation {

	private JavaCallTree tree;
	private JavaMessage callingMessage;
	private IMethod method;
	private MethodDeclaration astNode;
	private ArrayList<JavaMessage> messages;
	
	private class MessageFinder extends GenericVisitor {
		private class VariableHolder {
			protected SimpleName name;
			protected Type type;

			VariableHolder(Type type, SimpleName name) {
				this.name = name;
				this.type = type;
			}
		}
		
		private class ThrowResolver extends ASTVisitor {
			protected List<ASTNode> possibilities = new ArrayList<ASTNode>();
			@Override
			public boolean visit(MethodInvocation node) {
				possibilities.add(node);
				return false;
			}
			
			@Override
			public boolean visit(SuperMethodInvocation node) {
				possibilities.add(node);
				return false;
			}
			
			@Override
			public boolean visit(ConstructorInvocation node) {
				possibilities.add(node);
				return false;
			}
			
			public boolean visit(SimpleName node) {
				possibilities.add(node);
				return false;
			}
			
			@Override
			public boolean visit(ClassInstanceCreation node) {
				possibilities.add(node);
				return false;
			}
		}
		private List<ASTNode> messages = new LinkedList<ASTNode>();
		private HashMap<Block, List<VariableHolder>> variables = new HashMap<Block, List<VariableHolder>>();
		private HashMap<ThrowStatement, List<IType>> thrownTypes = new HashMap<ThrowStatement, List<IType>>();
		private LinkedList<TryStatement> openTries = new LinkedList<TryStatement>();
		private HashMap<ASTNode, List<TryStatement>> invocationTries = new HashMap<ASTNode, List<TryStatement>>();
		@Override
		public boolean visit(TryStatement node) {
			openTries.addLast(node);
			return true;
		}
		
		@Override
		public void endVisit(TryStatement node) {
			openTries.removeLast();
		}
		@Override
		protected void endVisitNode(ASTNode node) {
			if ((node instanceof MethodInvocation)||
				(node instanceof SuperMethodInvocation)||
				(node instanceof ConstructorInvocation)||
				(node instanceof SuperConstructorInvocation)||
				(node instanceof ThrowStatement)||
				(node instanceof ReturnStatement) ||
				(node instanceof ClassInstanceCreation)) {
				messages.add(node);
				if (openTries.size() > 0) {
					invocationTries.put(node, new ArrayList<TryStatement>(openTries));
				}
			}
			if (node instanceof ThrowStatement) {
				//try to resolve the throw statements.
				ThrowResolver resolver = new ThrowResolver();
				node.accept(resolver);
				List<IType> thrown = new ArrayList<IType>();
				thrownTypes.put((ThrowStatement)node, thrown);
				for (ASTNode unresolved : resolver.possibilities) {
					try {
						switch (unresolved.getNodeType()) {
						case ASTNode.METHOD_INVOCATION:
							//have to try and find the type for the return value.
							Type rtype = ASTUTils.findReturnType((MethodInvocation)unresolved);
							if (rtype != null) {
								//try and resolve it to an exception
								ITypeBinding binding = rtype.resolveBinding();
								IType type = (IType) binding.getJavaElement();
								ITypeHierarchy hierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());
								IJavaProject project = type.getJavaProject();
								IType throwable = project.findType("java.lang.Throwable");
								if (hierarchy.contains(throwable)) {
									thrown.add(type);
								}

							}
							break;
						case ASTNode.CONSTRUCTOR_INVOCATION:
							IMethodBinding methodBinding = ((ConstructorInvocation)unresolved).resolveConstructorBinding();
							if (methodBinding != null) {
								IType type = (IType) methodBinding.getDeclaringClass().getJavaElement();
								if (type != null) {
									ITypeHierarchy hierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());
									IJavaProject project = type.getJavaProject();
									IType throwable = project.findType("java.lang.Throwable");
									if (hierarchy.contains(throwable)) {
										thrown.add(type);
									}
								}
							}
							break;
						case ASTNode.SIMPLE_NAME:
							for (List<VariableHolder> localVariables : variables.values()) {
								for (VariableHolder variable : localVariables) {
									if (variable.name.equals(node)) {
										ITypeBinding binding = variable.type.resolveBinding();
										if (binding != null) {
											IType type = (IType) binding.getJavaElement();
											ITypeHierarchy hierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());
											IJavaProject project = type.getJavaProject();
											IType throwable = project.findType("java.lang.Throwable");
											if (hierarchy.contains(throwable)) {
												thrown.add(type);
											}
										}
										break;
									}
								}
							}
							break;
						case ASTNode.CLASS_INSTANCE_CREATION:
							ClassInstanceCreation ci = (ClassInstanceCreation) unresolved;
							ITypeBinding binding = ci.getType().resolveBinding();
							if (binding != null) {
								IType type = (IType) binding.getJavaElement();
								ITypeHierarchy hierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());
								IJavaProject project = type.getJavaProject();
								IType throwable = project.findType("java.lang.Throwable");
								if (hierarchy.contains(throwable)) {
									thrown.add(type);
								}
							}
							break;
						}
					} catch (JavaModelException e) {

					}
				}
			}
			super.endVisitNode(node);
		}
		
		//keep track of variables to resolve throw statements.
		@Override
		public boolean visit(VariableDeclarationExpression node) {
			Type type = node.getType();
			ASTNode scope = node.getParent();
			while (scope != null && !(scope instanceof Block)) {
				scope = scope.getParent();
			}
			if (scope instanceof Block) {
				List<VariableHolder> exps = variables.get(scope);
				if (exps == null) {
					exps = new LinkedList<VariableHolder>();
					variables.put((Block)scope, exps);
				}
				for (Object o : node.fragments()) {
					exps.add(new VariableHolder(type, ((VariableDeclarationFragment)o).getName()));
				}
			}
			return super.visit(node);
		}
		
		//keep track of variables to resolve throw statements.
		@SuppressWarnings("unchecked")
		@Override
		public boolean visit(VariableDeclarationStatement node) {
			Type type = node.getType();
			ASTNode scope = node.getParent();
			while (scope != null && !(scope instanceof Block)) {
				scope = scope.getParent();
			}
			if (scope instanceof Block) {
				List<VariableHolder> exps = variables.get(scope);
				if (exps == null) {
					exps = new LinkedList<VariableHolder>();
					variables.put((Block)scope, exps);
				}
				for (Object o : node.fragments()) {
					exps.add(new VariableHolder(type, ((VariableDeclarationFragment)o).getName()));
				}
			}
			return super.visit(node);
		}
		
		@Override
		public void endVisit(Block node) {
			//unscope all variables.
			variables.remove(node);
			super.endVisit(node);
		}
		
	}

	public JavaActivation(JavaCallTree tree, IMethod method) {
		this.tree = tree;
		this.callingMessage = null;
		this.method = method;
	}
	
	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if (ASTNode.class.isAssignableFrom(adapter)) {
			return getAST();
		}
		//try the java element's adapter
		return getJavaElement().getAdapter(adapter);
	}
	
	public JavaMessage getCallingMessage() {
		return this.callingMessage;
	}

	public ASTNode getAST() {
		if (astNode == null) {
			ASTNode rootNode = tree.parse(method.getDeclaringType());
			this.astNode = ASTUTils.findMethodDeclaration(rootNode, method);
		}
		return astNode;
	}

	public IJavaElement getJavaElement() {
		return method;
	}
	
	public JavaObject getLifeLine() {
		return new JavaObject(tree, method.getDeclaringType());
	}

	public JavaCallTree getTree() {
		return tree;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chise.diver.sequencediagrams.sc.java.model.IJavaActivation#getMessages()
	 */
	@Override
	public List<JavaMessage> getMessages() {
		if (this.messages == null) {
			this.messages = new ArrayList<JavaMessage>();
			if (getAST() == null) {
				return messages;
			}
			MessageFinder finder = new MessageFinder();
			getAST().accept(finder);
			for (ASTNode node : finder.messages) {
				IMethodBinding binding = null;
				switch (node.getNodeType()) {
				case ASTNode.METHOD_INVOCATION:
					binding = ((MethodInvocation)node).resolveMethodBinding();
					break;
				case ASTNode.SUPER_METHOD_INVOCATION:
					binding = ((SuperMethodInvocation)node).resolveMethodBinding();
					break;
				case ASTNode.CONSTRUCTOR_INVOCATION:
					binding = ((ConstructorInvocation)node).resolveConstructorBinding();
					break;
				case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
					binding = ((SuperConstructorInvocation)node).resolveConstructorBinding();
					break;
				case ASTNode.CLASS_INSTANCE_CREATION:
					binding = ((ClassInstanceCreation)node).resolveConstructorBinding();
					break;
				}
				if (binding != null) {
					IMethod target = (IMethod) binding.getJavaElement();
					if (target != null)
					{
						JavaActivation targetActivation = new JavaActivation(tree, target);
						JavaMessage message = new JavaMessage(tree, node, this, targetActivation);
						//add catches
						targetActivation.setCallingMessage(message);
						message.setTries(finder.invocationTries.get(node));
						messages.add(message);
					}
				} else if (node instanceof ThrowStatement){
					ThrowStatement statement = (ThrowStatement) node;
					List<IType> throwns = finder.thrownTypes.get(statement);
					for (IType thrown : throwns) {
						JavaMessage parentMessage = getCallingMessage();
						IJavaActivation root = this;
						JavaMessage message = null;
						while (parentMessage != null) {
							root = parentMessage.getSource();
							if (parentMessage.catches(thrown)) {
								break;
							}
							parentMessage = root.getCallingMessage();
						}
						if (root != null && root != this) {
							message = new JavaMessage(tree, statement, this, root);
							message.setType(thrown);
							message.setException(true);
							messages.add(message);
						}
					}
				} else if (node instanceof ReturnStatement) {
					if (getCallingMessage() != null) {
						JavaMessage message = new JavaMessage(tree, node, this, getCallingMessage().getSource());
						IJavaProject project = getJavaElement().getJavaProject();
						if (project != null) {
							String returnType;
							try {
								returnType = ((IMethod)getJavaElement()).getReturnType();
								if (returnType != null) {
									message.setType(returnType);
								}
							} catch (JavaModelException e) {
							}
							
						}
						messages.add(message);
						
					}
				}
			}
		}
		return messages;
	}
	
	protected void setCallingMessage(JavaMessage message) {
		this.callingMessage = message;
	}

}
