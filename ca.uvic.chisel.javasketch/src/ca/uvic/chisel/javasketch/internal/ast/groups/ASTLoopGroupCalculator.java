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
package ca.uvic.chisel.javasketch.internal.ast.groups;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.IReply;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;
import ca.uvic.chisel.javasketch.internal.ast.ASTMessageFinder;
import ca.uvic.chisel.javasketch.internal.ast.ASTUTils;
import ca.uvic.chisel.javasketch.internal.ast.GenericVisitor;

/**
 * @author Del Myers
 *
 */
public class ASTLoopGroupCalculator  {
	
	
	
	private static final String CURRENT_GROUPING = "ca.uvic.chisel.javasketch.activation.grouping";

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.IMessageGrouper#calculateGroups(org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer, java.lang.Object, java.lang.Object[])
	 */

	public static ASTMessageGroupingTree calculateGroups(IActivation activationElement) {
		//find the AST and define the groups
		IActivation activation = (IActivation) activationElement;
		IJavaElement element;
		try {
			HashMap<IOriginMessage, ASTNode> mappings = new HashMap<IOriginMessage, ASTNode>();
			element = JavaSearchUtils.findElement(activation, new NullProgressMonitor());
			if (element instanceof IMethod) {
				IMethod method = (IMethod) element;
				IDocument document = getDocumentFor(method);
				ASTNode root = ASTUTils.getASTFor(method.getDeclaringType());
				if (root != null) {
					MethodDeclaration methodDeclaration =ASTUTils.findMethodDeclaration(root, method);
					if (methodDeclaration != null) {
						try {
						List<IOriginMessage> children = activation.getOriginMessages();
						//a list of messages that aren't associated with an AST node:
						//they are pre-cursors to something else that must occur due to
						//reflection.
						List<IOriginMessage> unassociated = new LinkedList<IOriginMessage>();
						for (IOriginMessage child : children) {
							ASTMessageFinder finder;
							if (child instanceof ICall) {
								finder = new ASTMessageFinder((ICall)child, document);
							} else if (child instanceof IReply) {
								finder = new ASTMessageFinder((IReply)child, document);
							} else {
								return null;
							}
							methodDeclaration.accept(finder);
							ASTNode messageNode = finder.getNode();
							if (messageNode == null) {
								if (child instanceof IReply) {
									ASTMessageGroupingTree methodGroup = 
										getGrouping(methodDeclaration);
									if (methodGroup != null) {
										methodGroup.addMessage(child, unassociated);
										unassociated.clear();
									}
									//sometimes returns don't have line numbers
									continue;
								}
								unassociated.add((IOriginMessage) child);
								continue;
							} 
							mappings.put((IOriginMessage) child, messageNode);
							ASTNode blockNode = findBlockParent(messageNode);
							if (blockNode == null)
								return null;

							ASTMessageGroupingTree grouping = getGrouping(blockNode);
							if (grouping == null)
								return null;
							IOriginMessage lastMessage = grouping.getLastMessage();
							ASTNode lastNode = mappings.get(lastMessage);
							ASTNode thisNode = mappings.get(child);
							if (grouping.getLastCodeLine() <= child.codeLine()){
								//if the called methods are different, then
								//assume that they are different methods on the
								//same line of code. Otherwise, it is a loop
								if (lastNode != null && thisNode != null) {
									//first, check to see if the last node is a child of this node
									//because if it is, than the evaluation will be opposite
									if (isChild(lastNode, thisNode)) {
//										if (lastNode.getStartPosition() < thisNode.getStartPosition()) {
//											grouping = resetGrouping(blockNode);
//										}
									} else if (isChild(thisNode, lastNode)) {
										if (lastNode.getStartPosition() <= thisNode.getStartPosition()) {
											grouping = resetGrouping(blockNode);
										}
									} else if (lastNode.getStartPosition() >= thisNode.getStartPosition()) {
										grouping = resetGrouping(blockNode);
									}
								} else if (similarMessages((IOriginMessage)grouping.getLastMessage(), (IOriginMessage)child)) {
									grouping = resetGrouping(blockNode);
								}
							} else if (grouping.getLastCodeLine() > child.codeLine()) {
								if (grouping.getNode() != methodDeclaration) {
									if (lastNode != null && thisNode != null) {
										if (!isChild(lastNode, thisNode)) {
											grouping = resetGrouping(blockNode);
										}
									} else {
										grouping = resetGrouping(blockNode);
									}
								}
							}
							grouping.addMessage(child, unassociated);
							unassociated.clear();
						}
						//return the node left on the method declaration
						ASTMessageGroupingTree tree = (ASTMessageGroupingTree) methodDeclaration.getProperty(CURRENT_GROUPING);
						return tree;
						} finally {
							//make sure to clean up
							if (methodDeclaration != null) {
								methodDeclaration.accept( new GenericVisitor() {
									/* (non-Javadoc)
									 * @see ca.uvic.chisel.javasketch.internal.ast.GenericVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
									 */
									@Override
									protected boolean visitNode(ASTNode node) {
										node.setProperty(CURRENT_GROUPING, null);
										return true;
									}
								});
							}
						}
					}
				}
			}
		} catch (InterruptedException e) {
			//ignore and continue
		} catch (CoreException e) {
			SketchPlugin.getDefault().log(e);
		
		} 
		return null;
	}


	/**
	 * Checks to see if node c is a child of p
	 * @param c
	 * @param p
	 * @return
	 */
	private static boolean isChild(ASTNode c, ASTNode p) {
		ASTNode parent = c.getParent();
		while (parent != null) {
			if (parent.equals(p)) {
				return true;
			}
			parent = parent.getParent();
		}
		return false;
	}


	private static boolean similarMessages(IOriginMessage lastMessage, IOriginMessage message) {
		if (lastMessage == null && message == null) {
			return true;
		} else if (lastMessage == null || message == null) {
			return false;
		}
		//they have to be origin messages
		if (!lastMessage.getClass().equals(message.getClass())) {
			return false;
		}
		IActivation lastTarget = lastMessage.getTarget().getActivation();
		IActivation thisTarget = message.getTarget().getActivation();
		ITraceClassMethod lastMethod = lastTarget.getMethod();
		ITraceClassMethod thisMethod = thisTarget.getMethod();
		return JavaSearchUtils.areMethodsSimilar(lastMethod, thisMethod);
	}


	/**
	 * @param messageNode
	 * @return
	 */
	private static ASTNode findBlockParent(ASTNode messageNode) {
		if (messageNode.getNodeType() == ASTNode.METHOD_DECLARATION) {
			return messageNode;
		}
		//search through the tree, up through the parents to find the nearest block
		ASTNode parent = messageNode.getParent();
		while (parent != null) {
			switch (parent.getNodeType()) {
			case ASTNode.IF_STATEMENT:
			case ASTNode.WHILE_STATEMENT:
			case ASTNode.FOR_STATEMENT:
			case ASTNode.DO_STATEMENT:
			case ASTNode.ENHANCED_FOR_STATEMENT:	
			case ASTNode.TRY_STATEMENT:
			case ASTNode.CATCH_CLAUSE:
			case ASTNode.METHOD_DECLARATION:
				return parent;
			default:
				//get the else blocks
				if (parent instanceof Statement) {
					Statement statement = (Statement) parent;
					if (statement.getParent() instanceof IfStatement) {
						if (((IfStatement)statement.getParent()).getElseStatement() == statement) {
							return statement;
						}
					}
				}
				break;
			}
			parent = parent.getParent();
		}
		return null;
	}

	/**
	 * @param blockNode
	 * @param child
	 * @param i
	 * @param groupings
	 * @return
	 */
	private static ASTMessageGroupingTree getGrouping(ASTNode blockNode) {
		ASTMessageGroupingTree currentGrouping = (ASTMessageGroupingTree) blockNode.getProperty(CURRENT_GROUPING);
		
		if (currentGrouping == null) {
			//first: if this blockNode is a method declaration, create a root group
			if (blockNode.getNodeType() == ASTNode.METHOD_DECLARATION) {
				currentGrouping = new ASTMessageGroupingTree(null, blockNode);
				blockNode.setProperty(CURRENT_GROUPING, currentGrouping);
				return currentGrouping;
			} else {
				ASTMessageGroupingTree parentGrouping = getGrouping(findBlockParent(blockNode));
				currentGrouping = new ASTMessageGroupingTree(parentGrouping, blockNode);
				blockNode.setProperty(CURRENT_GROUPING, currentGrouping);
				return currentGrouping;
			}
//			
//			ASTMessageGroupingTree parentGrouping = 
//			//create a new grouping for this node and all of its parents.
//			LinkedList<IMessageGrouping> created = new LinkedList<IMessageGrouping>();
//			currentGrouping = new ASTMessageGroupingTree(activation, blockNode);
//			currentGrouping.setStartIndex(i);
//			currentGrouping.setEndIndex(i);
//			blockNode.setProperty(CURRENT_GROUPING, currentGrouping);
//			updateGrouping(currentGrouping);
//			created.addFirst(currentGrouping);
//			ASTNode parent = findBlockParent(blockNode);
//			while (parent != null) {
//				ASTMessageGroupingTree newGrouping = (ASTMessageGroupingTree) parent.getProperty(CURRENT_GROUPING);
//				if (newGrouping != null) {
//					//update it to include the new index
//					if (newGrouping.getEndIndex() < i) {
//						newGrouping.setEndIndex(i);
//					}
//				} else {
//					newGrouping = new ASTMessageGroupingTree(activation, parent);
//					parent.setProperty(CURRENT_GROUPING, newGrouping);
//					newGrouping.setStartIndex(i);
//					newGrouping.setEndIndex(i);
//					updateGrouping(newGrouping);
//					created.addFirst(newGrouping);
//				}
//				parent = findBlockParent(parent);		
//			}
//			groupings.addAll(created);
		}
		return currentGrouping;
	}

	

	/**
	 * Clears the current grouping for the block node, and all of its children.
	 * @param blockNode
	 * @param child
	 * @param i
	 * @param groupings
	 * @return
	 */
	private static ASTMessageGroupingTree resetGrouping(ASTNode blockNode) {
		ASTNode loop = findLoopingParent(blockNode);
		if (loop != null) {
			ASTMessageGroupingTree loopGroup = (ASTMessageGroupingTree) loop.getProperty(CURRENT_GROUPING);
			int currentIteration = loopGroup.getIteration();
			//clear the children
			loop.accept(new GenericVisitor(){
				/* (non-Javadoc)
				 * @see ca.uvic.chisel.javasketch.internal.ast.groups.GenericVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
				 */
				@Override
				protected boolean visitNode(ASTNode node) {
					node.setProperty(CURRENT_GROUPING, null);
					return true;
				}
			});
			//create a new group for the loop
			ASTMessageGroupingTree newLoop = getGrouping(loop);
			newLoop.setIteration(currentIteration + 1);
			return getGrouping(blockNode);
		}
		return (ASTMessageGroupingTree) blockNode.getProperty(CURRENT_GROUPING);
	}

	/**
	 * @param blockNode
	 * @return
	 */
	private static ASTNode findLoopingParent(ASTNode node) {
		//search through the tree, up through the parents to find the nearest block
		ASTNode parent = node;
		ASTNode found = null;
		while (parent != null) {
			switch (parent.getNodeType()) {
			case ASTNode.WHILE_STATEMENT:
			case ASTNode.FOR_STATEMENT:
			case ASTNode.DO_STATEMENT:
			case ASTNode.ENHANCED_FOR_STATEMENT:
				found = parent;
				parent = null;
				break;
			default:
				parent = parent.getParent();
				break;
			}
			
		}
		return found;
	}

	private static IDocument getDocumentFor(IJavaElement element) {
		try {
			if (element instanceof IMethod) {
				
				IMethod method = (IMethod) element;
				String source = null;
				ITypeRoot root = method.getTypeRoot();
				
				source = root.getSource();
				if (source != null) {
					return new Document(source);
				}
			}
		} catch (CoreException e) {
			SketchPlugin.getDefault().log(e);
		} 
		return null;
	}

	

}
