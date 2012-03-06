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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.eclipse.zest.custom.uml.viewers.IMessageGrouper;
import org.eclipse.zest.custom.uml.viewers.IMessageGrouping;
import org.eclipse.zest.custom.uml.viewers.MessageGrouping;
import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.internal.ast.groups.ASTMessageGroupingTree;
import ca.uvic.chisel.javasketch.ui.ISketchColorConstants;
import ca.uvic.chisel.javasketch.ui.internal.preferences.ISketchPluginPreferences;
import ca.uvic.chisel.javasketch.ui.internal.presentation.metadata.PresentationData;

/**
 * @author Del Myers
 *
 */
public class ASTMessageGrouper implements IMessageGrouper {
	
	public static class ASTMessageGrouping extends MessageGrouping implements Comparable<ASTMessageGrouping>, IAdaptable{

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

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Object getAdapter(Class adapter) {
			if (adapter.isAssignableFrom(ASTMessageGroupingTree.class)) {
				return node;
			}
			return null;
		}
		
		/**
		 * @return the node
		 */
		public ASTMessageGroupingTree getNode() {
			return node;
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
//		if (true) {
//			return calculateGroups2(viewer, activationElement, children);
//		}
		HashMap<ASTMessageGroupingTree, ASTMessageGrouping> groups =
			new HashMap<ASTMessageGroupingTree, ASTMessageGrouping>();
		IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
		LinkedList<ASTMessageGroupingTree> unusedLoops = new LinkedList<ASTMessageGroupingTree>();
		LinkedList<ASTMessageGroupingTree> unusedGroups = new LinkedList<ASTMessageGroupingTree>();
		
		boolean compactLoops = store.getBoolean(ISketchPluginPreferences.COMPACT_LOOPS_PREFERENCE);
		boolean useCombinedFragments = store.getBoolean(ISketchPluginPreferences.DISPLAY_GROUPS_PREFERENCE);
		if (!useCombinedFragments) {
			return new IMessageGrouping[0];
		}
		if (activationElement instanceof IActivation) {
			IActivation parent = (IActivation) activationElement;
			IProgramSketch sketch = SketchPlugin.getDefault().getSketch(parent);
			if (sketch != null) {
				PresentationData pd = PresentationData.connect(sketch);
				if (pd != null) {
					try { 
						ASTMessageGroupingTree tree = pd.getGroups(parent);
						
						if (tree == null) {
							return new IMessageGrouping[0];
						}
						
						//search through the tree to find all loops
						if (compactLoops) {
							unusedGroups.add(tree);
							while (unusedGroups.size() > 0) {
								ASTMessageGroupingTree node = unusedGroups.removeFirst();
								if (pd.isGroupVisible(parent, node)) {
									if (node.isLoop()) {
										unusedLoops.add(node);
									}
									for (ASTMessageGroupingTree child : node.getChildren()) {
										unusedGroups.add(child);
									}
								}

							}
						}
						for (int i = 0; i < children.length; i++) {
							Object child = children[i];
							if (child instanceof IMessage) {
								ASTMessageGroupingTree node = tree.getMessageContainer((IMessage) child);
								//put in the node and all of its parents.
								
								while (node != null && node.getParent() != null) {
									ASTMessageGrouping grouping = groups.get(node);
									if (grouping == null) {
//										if (compactLoops && node.isLoop()) {
											grouping = new ASTMessageGrouping(activationElement, node);
											grouping.setOffset(i);
											groups.put(node, grouping);
//										} else if (useCombinedFragments && !node.isLoop()) {
//											grouping = new ASTMessageGrouping(activationElement, node);
//											grouping.setOffset(i);
//											groups.put(node, grouping);
//										}
									}
									if (grouping != null) {
										grouping.setLength(i - grouping.getOffset() + 1);
									}
									node = node.getParent();
								}
							}
						}
					} finally {
						pd.disconnect();
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
	
	
	

	public IMessageGrouping[] calculateGroups2(UMLSequenceViewer viewer,
			Object activationElement, Object[] children) {
		HashMap<ASTMessageGroupingTree, ASTMessageGrouping> groups =
			new HashMap<ASTMessageGroupingTree, ASTMessageGrouping>();
		IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
		ArrayList<ASTMessageGrouping> unusedGroups = new ArrayList<ASTMessageGrouper.ASTMessageGrouping>();
		boolean compactLoops = store.getBoolean(ISketchPluginPreferences.COMPACT_LOOPS_PREFERENCE);
		boolean useCombinedFragments = store.getBoolean(ISketchPluginPreferences.DISPLAY_GROUPS_PREFERENCE);
		if (!useCombinedFragments) {
			return new IMessageGrouping[0];
		}
		if (activationElement instanceof IActivation) {
			IActivation parent = (IActivation) activationElement;
			IProgramSketch sketch = SketchPlugin.getDefault().getSketch(parent);
			if (sketch != null) {
				PresentationData pd = PresentationData.connect(sketch);
				if (pd != null) {
					try { 
						ASTMessageGroupingTree tree = pd.getGroups(parent);
						
						if (tree == null) {
							return new IMessageGrouping[0];
						}
						LinkedList<ASTMessageGroupingTree> groupStack = new LinkedList<ASTMessageGroupingTree>();
						
						//search through the tree to find all loops
						groupStack.add(tree);
						while (groupStack.size() > 0) {
							ASTMessageGroupingTree node = groupStack.removeFirst();
							if (pd.isGroupVisible(parent, node) || (!compactLoops && node.isLoop())) {
								ASTMessageGrouping g = new ASTMessageGrouping(activationElement, node);
								g.setOffset(-1);
								g.setLength(0);
								groups.put(node, g);
								unusedGroups.add(g);
								for (ASTMessageGroupingTree child : node.getChildren()) {
									groupStack.add(child);
								}
							}
						}
						
						int unusedIndex = 0;
						for (int i = 0; i < children.length; i++) {
							Object child = children[i];
							if (child instanceof IMessage) {
								ASTMessageGroupingTree node = tree.getMessageContainer((IMessage) child);
								ASTMessageGrouping grouping = groups.get(node);
								int messageLineNumber = ((IMessage)child).codeLine();
								if (grouping != null) {
									if (grouping.getOffset() < 0) {
										//update all the preceding offsets to
										//be equal to this one.
										for (int u = unusedIndex; u < unusedGroups.size(); unusedIndex++, u++) {
											ASTMessageGrouping unused = unusedGroups.get(u);
											if (unused.node.getFirstCodeLine() <= messageLineNumber) {
												unused.setOffset(i);
											} else {
												unusedIndex = u;
												break;
											}
										}
									}
								}
								//update all the parents for the length
								while (node != null && node.getParent() != null) {
									grouping = groups.get(node);
									if (grouping != null) {
										grouping.setLength(i - grouping.getOffset() + 1);
									}
									node = node.getParent();
								}
							}
						}
						for (int i = unusedIndex; i < unusedGroups.size(); i++) {
							ASTMessageGrouping unused = unusedGroups.get(i);
							unused.setOffset(children.length);
						}
					} finally {
						pd.disconnect();
					}
				}
			}
		}
		for (Iterator<ASTMessageGrouping> i = unusedGroups.iterator(); i.hasNext();) {
			ASTMessageGrouping group = i.next();
			if (group.node.getNode().getNodeType() == ASTNode.METHOD_DECLARATION) {
				i.remove();
			} else if (group.getLength() <=0) {
				//remove non-loop elements
				if (!group.node.isLoop()) {
					i.remove();
				} else {
					//walk up the list and remove children of empty
					//loops
					ASTMessageGroupingTree parent = group.node.getParent();
					
					boolean remove = false;
					while (parent != null) {
						ASTMessageGrouping grouping = groups.get(parent);
						if (grouping.node.isLoop() && grouping.getLength() <= 0) {
							remove = true;
							break;
						}
						parent = parent.getParent();
					}
					if (remove) {
						i.remove();
					}
				}
			}
		}
		
		for (ASTMessageGrouping group : unusedGroups) {
			updateGrouping(group, group.node.getNode());
		}
		return unusedGroups.toArray(new ASTMessageGrouping[unusedGroups.size()]);
		//return result;
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
		if (grouping.node.isLoop()) {
			ASTMessageGroupingTree[] siblings = grouping.node.getSiblings();
			text = text + "[" + grouping.node.getIteration() + " of " + (siblings.length + 1) + "]";
		}
				
		grouping.setName(text);
		grouping.setForeground(fg);
		grouping.setBackground(bg);
	}

}
