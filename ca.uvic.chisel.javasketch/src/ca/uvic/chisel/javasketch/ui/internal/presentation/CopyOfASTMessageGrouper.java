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
package ca.uvic.chisel.javasketch.ui.internal.presentation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.swt.graphics.Color;
import org.eclipse.zest.custom.uml.viewers.IMessageGrouper;
import org.eclipse.zest.custom.uml.viewers.IMessageGrouping;
import org.eclipse.zest.custom.uml.viewers.MessageGrouping;
import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;

import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.internal.ast.groups.ASTLoopGroupCalculator;
import ca.uvic.chisel.javasketch.internal.ast.groups.ASTMessageGroupingTree;
import ca.uvic.chisel.javasketch.ui.ISketchColorConstants;

/**
 * @author Del Myers
 *
 */
public class CopyOfASTMessageGrouper implements IMessageGrouper {
	
	private static class ASTMessageGrouping extends MessageGrouping implements Comparable<ASTMessageGrouping>{

		private ASTMessageGroupingTree node;

		/**
		 * @param activationElement
		 */
		public ASTMessageGrouping(Object activationElement, ASTMessageGroupingTree node) {
			super(activationElement);
			this.node = node;
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(ASTMessageGrouping that) {
			//first, compare by indexes
			if (this.getOffset() < that.getOffset()) {
				return -1;
			} else if (this.getOffset() > that.getOffset()) {
				return 1;
			} else {
				//the offsets are equal, the smaller one is the
				//one with the smaller length
				if (this.getLength() < that.getLength()) {
					return -1;
				} else if (this.getLength() > that.getLength()){
					return 1;
				} else {
					//the only other option is that they cover the same
					//messages, use the AST to decide which one is 
					//internal
					if (this.node.getNode().getStartPosition() < 
						that.node.getNode().getStartPosition()) {
						return -1;
					} else if (this.node.getNode().getStartPosition() > 
						that.node.getNode().getStartPosition()) {
						return 1;
					} else {
						if (this.node.getNode().getLength() < 
							that.node.getNode().getLength()) {
							return -1;
						} else if (this.node.getNode().getLength() > 
							that.node.getNode().getLength()) {
							return 1;
						}
					}
				}
			}
			//can't decide which is greater.
			return 0;
		}
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.IMessageGrouper#dispose()
	 */
	@Override
	public void dispose() {
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.IMessageGrouper#calculateGroups(org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer, java.lang.Object, java.lang.Object[])
	 */
	@Override
	public IMessageGrouping[] calculateGroups(UMLSequenceViewer viewer,
			Object activationElement, Object[] children) {
		HashMap<ASTMessageGroupingTree, ASTMessageGrouping> groups =
			new HashMap<ASTMessageGroupingTree, ASTMessageGrouping>();
		if (activationElement instanceof IActivation) {
			ASTMessageGroupingTree tree = ASTLoopGroupCalculator.calculateGroups((IActivation) activationElement);
			if (tree == null) {
				return new IMessageGrouping[0];
			}
			for (int i = 0; i < children.length; i++) {
				Object child = children[i];
				if (child instanceof IMessage) {
					ASTMessageGroupingTree node = tree.getMessageContainer((IMessage) child);
					//put in the node and all of its parents.
					while (node != null && node.getParent() != null) {
						ASTMessageGrouping grouping = groups.get(node);
						if (grouping == null) {
							grouping = new ASTMessageGrouping(activationElement, node);
							grouping.setOffset(i);
							groups.put(node, grouping);
						}
						grouping.setLength(i - grouping.getOffset() + 1);
						node = node.getParent();
					}
				}
			}
		}
		for (ASTMessageGroupingTree node : groups.keySet()) {
			updateGrouping(groups.get(node), node.getNode());
		}
		ASTMessageGrouping[] result = groups.values().toArray(new ASTMessageGrouping[groups.values().size()]);
		Arrays.sort(result);
		return result;
	}
	
	/**
	 * Updates labels and colours for the grouping.
	 * @param currentGrouping
	 */
	private void updateGrouping(ASTMessageGrouping grouping, ASTNode node ) {
		String text = "";
		int i;
		Color bg = null;
		Color fg = null;
		switch (node.getNodeType()) {
		case ASTNode.IF_STATEMENT:
			IfStatement ifStatement = (IfStatement) node;
			text = "if (" + ifStatement.getExpression().toString() + ")";
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
		if (grouping.node.isLoop()) {
			ASTMessageGroupingTree[] siblings = grouping.node.getSiblings();
			text = text + "[" + grouping.node.getIteration() + " of " + (siblings.length + 1) + "]";
		}
		grouping.setName(text);
		grouping.setForeground(fg);
		grouping.setBackground(bg);
	}

}
