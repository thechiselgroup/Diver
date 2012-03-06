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
package ca.uvic.chisel.diver.sequencediagrams.sc.java.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.custom.uml.viewers.IMessageGrouper;
import org.eclipse.zest.custom.uml.viewers.IMessageGrouping;
import org.eclipse.zest.custom.uml.viewers.MessageGrouping;
import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;

import ca.uvic.chisel.javasketch.internal.ast.groups.ASTMessageGroupingTree;
import ca.uvic.chisel.javasketch.ui.ISketchColorConstants;
import ca.uvic.chisel.javasketch.ui.internal.presentation.ASTMessageGrouper.ASTMessageGrouping;

/**
 * Uses the AST of the java model to discover groups for the passed messages. 
 * @author Del Myers
 */

public class JavaMessageGrouper implements IMessageGrouper {
	
		
	private static class MappedMessageGrouping extends MessageGrouping {

		private Object key;

		/**
		 * @param activationElement
		 * @param offset
		 * @param length
		 * @param name
		 */
		public MappedMessageGrouping(Object activationElement, int offset,
				int length, String name, Object key) {
			super(activationElement, offset, length, name);
			this.key = key;
		}
		
		public Object getKey() {
			return key;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return getName() + "[" + getOffset() + "," + (getOffset()+getLength()) +"]";
		}
		
	}

	public IMessageGrouping[] calculateGroups2(UMLSequenceViewer viewer,
			Object activationElement, Object[] children) {
		HashMap<ASTNode, MappedMessageGrouping> groups = new HashMap<ASTNode, MappedMessageGrouping>();
		if (!(activationElement instanceof IAdaptable)) {
			return new IMessageGrouping[0];
		}
		ASTNode activationNode = (ASTNode)((IAdaptable)activationElement).getAdapter(ASTNode.class);
		if (!(activationNode instanceof MethodDeclaration)) {
			return new IMessageGrouping[0];
		}
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof IAdaptable) {
				ASTNode messageNode = (ASTNode)((IAdaptable)children[i]).getAdapter(ASTNode.class);
				if (messageNode != null) {
					ASTNode currentParent = messageNode.getParent();
					while (currentParent != null && currentParent != activationNode) {
						ASTNode block = null;
						String text = null;
						Color c = null;
						Color bc = null;
						String expressionString = "";
						switch (currentParent.getNodeType()) {
						case ASTNode.IF_STATEMENT:
							block = checkIfSide((IfStatement)currentParent, messageNode);
							if (block != null && block == ((IfStatement)currentParent).getElseStatement()) {
								text = "else";
							} else if (block == ((IfStatement)currentParent).getThenStatement()) {
								text = "if (" + ((IfStatement)currentParent).getExpression().toString() + ")";
							}
							c = ISketchColorConstants.CONDITION_FG;
							bc = ISketchColorConstants.CONDITION_FG;
							break;
						case ASTNode.WHILE_STATEMENT:
							if (((WhileStatement)currentParent).getExpression() != null) {
								expressionString = ((WhileStatement)currentParent).getExpression().toString();
							}
							text = "while (" +expressionString + ")";
							block = currentParent;
							c = ISketchColorConstants.LOOP_FG;
							bc = ISketchColorConstants.LOOP_BG;
							break;
						case ASTNode.FOR_STATEMENT:
							if (((ForStatement)currentParent).getExpression() != null) {
								expressionString = ((ForStatement)currentParent).getExpression().toString();
							} else {
								expressionString = ";;";
							}
							text = "for (" + expressionString + ")";
							block = currentParent;
							c = ISketchColorConstants.LOOP_FG;
							bc = ISketchColorConstants.LOOP_BG;
							break;
						case ASTNode.TRY_STATEMENT:
							text = "try";
							block = currentParent;
							c = ISketchColorConstants.ERROR_FG;
							bc = ISketchColorConstants.ERROR_BG;
							break;
						case ASTNode.CATCH_CLAUSE:
							text = "catch (" +((CatchClause)currentParent).getException().toString() +")";
							block = currentParent;
							c = ISketchColorConstants.ERROR_FG;
							bc = ISketchColorConstants.ERROR_BG;
							break;
						case ASTNode.DO_STATEMENT:
							text = "do while (" + ((DoStatement)currentParent).getExpression().toString() + ")";
							block = currentParent;
							c = ISketchColorConstants.LOOP_FG;
							bc = ISketchColorConstants.LOOP_BG;
							break;
						}
						if (text != null) {
							MappedMessageGrouping grouping = groups.get(block);
							if (grouping == null) {
								grouping = new MappedMessageGrouping(activationElement, i, 1, text, block);
								grouping.setBackground(bc);
								grouping.setForeground(c);
								groups.put(block, grouping);
							} else {
								int length = (i - grouping.getOffset()) + 1;
								grouping.setLength(length);
							}
						}
						currentParent = currentParent.getParent();
					}
				}
			}
		}
		ArrayList<MappedMessageGrouping> groupList = new ArrayList<MappedMessageGrouping>(groups.values());
		Collections.sort(groupList, new Comparator<MappedMessageGrouping>(){
			public int compare(MappedMessageGrouping o1, MappedMessageGrouping o2) {
				ASTNode n1 = (ASTNode) o1.getKey();
				ASTNode n2 = (ASTNode) o2.getKey();
				int diff = n1.getStartPosition() - n2.getStartPosition();
				if (diff == 0) {
					diff = (n1.getStartPosition()+n1.getLength())-(n2.getStartPosition()+n2.getLength());
				}
				return diff;
			}
		});
		return groupList.toArray(new IMessageGrouping[groupList.size()]);
	}
	
	public IMessageGrouping[] calculateGroups(UMLSequenceViewer viewer,
			Object activationElement, Object[] children) {
		HashMap<ASTNode, MappedMessageGrouping> groups = new HashMap<ASTNode, MappedMessageGrouping>();
		if (!(activationElement instanceof IAdaptable)) {
			return new IMessageGrouping[0];
		}
		ASTNode activationNode = (ASTNode)((IAdaptable)activationElement).getAdapter(ASTNode.class);
		if (!(activationNode instanceof MethodDeclaration)) {
			return new IMessageGrouping[0];
		}
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof IAdaptable) {
				ASTNode messageNode = (ASTNode)((IAdaptable)children[i]).getAdapter(ASTNode.class);
				if (messageNode != null) {
					ASTNode blockParent = findBlockParent(messageNode);
					
					List<MappedMessageGrouping> blocks = new LinkedList<MappedMessageGrouping>();
					while (blockParent != null && blockParent.getNodeType() != ASTNode.METHOD_DECLARATION) {
						
						if (blockParent!= null && blockParent.getNodeType() == ASTNode.IF_STATEMENT) {
							IfStatement ifStatement = (IfStatement) blockParent;
							ASTNode block = checkIfSide(ifStatement, messageNode);
							if (block != null && block.equals(ifStatement.getElseStatement())) {
								//add a block for the else statement as well
								MappedMessageGrouping blockNode = groups.get(block);
								if (blockNode == null) {
									blockNode = new MappedMessageGrouping(activationElement, i, 0, "", block);
									groups.put(block, blockNode);
								}
								blocks.add(blockNode);
							}
						}
						MappedMessageGrouping blockNode = groups.get(blockParent);
						if (blockNode == null) {
							blockNode = new MappedMessageGrouping(activationElement, i, 0, "", blockParent);
							groups.put(blockParent, blockNode);
						}
						blocks.add(blockNode);
						blockParent = findBlockParent(blockParent);
					}
					for (MappedMessageGrouping blockNode : blocks) {
						blockNode.setLength(blockNode.getLength()+1);
					}
				}
			}
		}
		ArrayList<MappedMessageGrouping> groupList = new ArrayList<MappedMessageGrouping>(groups.values());
		Collections.sort(groupList, new Comparator<MappedMessageGrouping>(){
			public int compare(MappedMessageGrouping o1, MappedMessageGrouping o2) {
				ASTNode n1 = (ASTNode) o1.getKey();
				ASTNode n2 = (ASTNode) o2.getKey();
				int diff = n1.getStartPosition() - n2.getStartPosition();
				if (diff == 0) {
					diff = (n1.getStartPosition()+n1.getLength())-(n2.getStartPosition()+n2.getLength());
				} if (diff == 0) {
					IfStatement ifStatement = null;
					//make sure that else statements are contained in if statements
					if (n1 instanceof IfStatement) {
						ifStatement = (IfStatement) n1;
					} else if (n2 instanceof IfStatement) {
						ifStatement = (IfStatement) n2;
					}
					if (ifStatement != null) {
						if (n2.equals(ifStatement.getElseStatement())) {
							return -1;
						} else if (n1.equals(ifStatement.getElseStatement())) {
							return 1;
						}
					}
				}
				return diff;
			}
		});
		for (MappedMessageGrouping blockNode : groupList) {
			updateGrouping(blockNode);
		}
		return groupList.toArray(new IMessageGrouping[groupList.size()]);
	}
	//[[1,10], [1,2], [2,6], [2,6], [5,6], [6,9], [6,8], [6,10]]
	/**
	 * Updates labels and colours for the grouping.
	 * @param currentGrouping
	 */
	private void updateGrouping(MappedMessageGrouping grouping) {
		ASTNode node = (ASTNode) grouping.getKey();
		String text = "";
		int i;
		Color bg = null;
		Color fg = null;
		switch (node.getNodeType()) {
		case ASTNode.IF_STATEMENT:
			IfStatement ifStatement = (IfStatement) node;
			text = "if (" + ifStatement.getExpression().toString() + ")";
			//it could be an else-if, make sure
			if (ifStatement.getParent().getNodeType() == ASTNode.IF_STATEMENT) {
				if (ifStatement.equals(((IfStatement)ifStatement.getParent()).getElseStatement())) {
					text = "else " + text;
				}
			}
			fg = ISketchColorConstants.CONDITION_FG;
			bg = ISketchColorConstants.CONDITION_BG;
			break;
		case ASTNode.WHILE_STATEMENT:
			WhileStatement whileStatement = (WhileStatement) node;
			text = "while (" + whileStatement.getExpression().toString() + ")";
			fg = ISketchColorConstants.LOOP_FG;
			bg = ISketchColorConstants.LOOP_BG;
			break;
		case ASTNode.DO_STATEMENT:
			DoStatement doStatement = (DoStatement) node;
			text = "do..while (" + doStatement.getExpression().toString() + ")";
			fg = ISketchColorConstants.LOOP_FG;
			bg = ISketchColorConstants.LOOP_BG;
			break;
		case ASTNode.FOR_STATEMENT:
			ForStatement forStatement = (ForStatement) node;
			List<?> initializers = forStatement.initializers();
			List<?> updaters = forStatement.updaters();
			text = "for (";
			for (i=0; i < initializers.size(); i++) {
				text += initializers.get(i).toString();
				if (i < initializers.size()-1) {
					text += ",";
				}
			}
			text += ";";
			if (forStatement.getExpression() != null) {
				text += forStatement.getExpression();
			}
			text += ";";
			for (i = 0; i < updaters.size(); i++) {
				text += updaters.get(i).toString();
				if (i < updaters.size()-1) {
					text += ",";
				}
			}
			text += ")";
			fg = ISketchColorConstants.LOOP_FG;
			bg = ISketchColorConstants.LOOP_BG;
			break;
		case ASTNode.ENHANCED_FOR_STATEMENT:
			EnhancedForStatement enhancedForStatement = (EnhancedForStatement) node; 
			text = "for (" + enhancedForStatement.getExpression().toString() + ")";
			fg = ISketchColorConstants.LOOP_FG;
			bg = ISketchColorConstants.LOOP_BG;
			break;
		case ASTNode.TRY_STATEMENT:
			text = "try";
			fg = ISketchColorConstants.ERROR_FG;
			bg = ISketchColorConstants.ERROR_BG;
			break;
		case ASTNode.CATCH_CLAUSE:
			CatchClause catchClause = (CatchClause) node;
			text = "catch (" + catchClause.getException().toString() + ")";
			fg = ISketchColorConstants.ERROR_FG;
			bg = ISketchColorConstants.ERROR_BG;
			break;
		default:
			//get the else blocks
			if (node instanceof Statement) {
				Statement statement = (Statement) node;
				if (statement.getParent() instanceof IfStatement) {
					if (((IfStatement)statement.getParent()).getElseStatement() == statement) {
						text = "else";
						fg = ISketchColorConstants.CONDITION_FG;
						bg = ISketchColorConstants.CONDITION_BG;
					}
				}
			}
			break;
		}
				
		grouping.setName(text);
		grouping.setForeground(fg);
		grouping.setBackground(bg);
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
	 * @param currentParent
	 * @param messageNode
	 * @return
	 */
	private ASTNode checkIfSide(IfStatement ifStatement, ASTNode messageNode) {
		ASTNode currentParent = messageNode;
		ASTNode thenStatement = ifStatement.getThenStatement();
		ASTNode elseStatement = ifStatement.getElseStatement();
		while (currentParent != null && currentParent != ifStatement) {
			if (currentParent == thenStatement) {
				return ifStatement;
			} else if (currentParent == elseStatement) {
				return elseStatement;
			}
			currentParent = currentParent.getParent();
		}
		return null;
	}
	
	private boolean contains(ASTNode parent, ASTNode child) {
		ASTNode node = child;
		while (node != null) {
			if (parent == node) {
				return true;
			}
			node = node.getParent();
		}
		return false;
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.IMessageGrouper#dispose()
	 */
	@Override
	public void dispose() {
		
	}

}
