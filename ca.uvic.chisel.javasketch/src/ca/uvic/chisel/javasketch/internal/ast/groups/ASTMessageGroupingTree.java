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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.internal.ast.GenericVisitor;

/**
 * @author Del Myers
 *
 */
public class ASTMessageGroupingTree  {

	private ASTNode node;
	//id for the ast node
	private String nodeId;
	private ASTMessageGroupingTree parent;
	private int firstCodeLine = -2;
	private int lastCodeLine = -1;
	private int id;
	private int iteration;
	
	/**
	 * A simple class that visits a method declaration node and produces a unique identifier
	 * for an AST node within that declaration.
	 * @author Del Myers
	 *
	 */
	private class NodeIDVisitor extends GenericVisitor {
		
		private boolean methodDeclarationVisited;
		private LinkedList<Integer> identifier = new LinkedList<Integer>();
		private boolean found = false;

		protected boolean visitNode(ASTNode v) {
			if (!methodDeclarationVisited && !(v instanceof MethodDeclaration)) {
				return false;
			}
			if (!methodDeclarationVisited) {
				//this is the first method declaration, initialize everything
				identifier = new LinkedList<Integer>();
				identifier.add(0);
				methodDeclarationVisited = true;
			}
			if (v == node) {
				found = true;
				return false;
			}
			int count = identifier.removeLast();
			identifier.push(count+1);
			//get set up for the next child
			identifier.add(0);
			return true;
		}
		
		/* (non-Javadoc)
		 * @see ca.uvic.chisel.javasketch.internal.ast.GenericVisitor#endVisitNode(org.eclipse.jdt.core.dom.ASTNode)
		 */
		@Override
		protected void endVisitNode(ASTNode node) {
			if (!found) {
				if (identifier.size() > 0) {
					identifier.removeLast();
				}
			}
			super.endVisitNode(node);
		}
		
		/**
		 * @return the identifier
		 */
		public String getIdentifier() {
			StringBuilder builder = new StringBuilder();
			boolean first = true;
			for (Integer i : identifier) {
				if (!first) {
					builder.append('.');
				}
				first = false;
				builder.append(i);
				
			}
			return builder.toString();
		}
		
	}
	/**
	 * We use the sequence number of the messages because they uniquely identify
	 * a message in a thread, and they are ordered.
	 */
	private TreeSet<String> messages;
	private ArrayList<ASTMessageGroupingTree> children;
	private IOriginMessage lastMessage;

	public ASTMessageGroupingTree(ASTMessageGroupingTree parent, ASTNode node) {
		this.node = node;
		this.parent = parent;
		iteration = 1;
		messages = new TreeSet<String>();
		children = new ArrayList<ASTMessageGroupingTree>();
		id = 0;
		if (parent != null) {
			parent.addChild(this);
		}
	}

	/**
	 * @param astMessageGroupingTree
	 */
	private void addChild(ASTMessageGroupingTree child) {
		children.add(child);
		child.id = children.size();
	}
	
	/**
	 * Returns a string that uniquely identifies this tree node within
	 * the hierarchy;
	 * @return
	 */
	public String getIdentifier() {
		if (getParent() == null) {
			return "0";
		}
		return getParent().getIdentifier() + "." + id;
	}
	
	void setIteration(int iteration) {
		this.iteration = iteration;
	}
	
	/**
	 * If this group represents a loop, this method returns the iteration
	 * of that loop that this group represents within the hierarchy. To
	 * find out the actual number of times that this group iterated, one
	 * must walk up the hierarchy, to query the number of iterations on
	 * the parents as well.
	 * 
	 * If this group does not represent a loop, this method will always
	 * return 1.
	 */
	public int getIteration() {
		return iteration;
	}
	
	/**
	 * Returns the groups in the parent which are on the same AST Node.
	 * This, for example, will return all of the iterations of a loop.
	 * Returns an empty list if there are no other siblings.
	 * @return the siblings.
	 */
	public ASTMessageGroupingTree[] getSiblings() {
		LinkedList<ASTMessageGroupingTree> siblings = 
			new LinkedList<ASTMessageGroupingTree>();
		if (getParent() != null) {
			for (ASTMessageGroupingTree sibling : getParent().children) {
				if (sibling != this) {
					if (sibling.node == this.node) {
						siblings.add(sibling);
					}
				}
			}
		}
		return siblings.toArray(new ASTMessageGroupingTree[siblings.size()]);
	}
	
	/**
	 * Gets all of the iterations of the node equivalent to this one.
	 * @return
	 */
	public ASTMessageGroupingTree[] getIterations() {
		if (!isLoop()) {
			return new ASTMessageGroupingTree[0];
		}
		LinkedList<ASTMessageGroupingTree> loops = 
			new LinkedList<ASTMessageGroupingTree>();
		if (getParent() != null) {
			for (ASTMessageGroupingTree sibling : getParent().children) {
				if (sibling == this || sibling.getNodeID().equals(getNodeID())) {
					loops.add(sibling);
				}
			}
		}
		return loops.toArray(new ASTMessageGroupingTree[loops.size()]);
	}
	
	/**
	 * Returns true iff the AST node for this group represents a loop.
	 * @return true iff the AST node for this group represents a loop.
	 */
	public boolean isLoop() {
		switch (node.getNodeType()) {
		case ASTNode.ENHANCED_FOR_STATEMENT:
		case ASTNode.FOR_STATEMENT:
		case ASTNode.WHILE_STATEMENT:
		case ASTNode.DO_STATEMENT:
			return true;
		}
		return false;
	}
	
	public void addMessage(IOriginMessage message, List<IOriginMessage> associated) {
		if (message.codeLine() < firstCodeLine || firstCodeLine == -2) {
			firstCodeLine = message.codeLine();
		}
		if (message.codeLine() > lastCodeLine) {
			lastCodeLine = message.codeLine();
		}
		if (messages.size() == 0 || lastCodeLine == message.codeLine()) {
			lastMessage = message;
		}
		for (IOriginMessage m :associated) {
			messages.add(m.getIdentifier());
		}
		messages.add(message.getIdentifier());
	
	}
	
	/**
	 * Returns the identifiers for the messages contained in this group.
	 * @return
	 */
	public Set<String> getMessageIdentifiers() {
		TreeSet<String> identifiers = new TreeSet<String>();
		LinkedList<ASTMessageGroupingTree> children = new LinkedList<ASTMessageGroupingTree>();
		children.add(this);
		while(children.size() > 0) {
			ASTMessageGroupingTree child = children.removeFirst();
			identifiers.addAll(child.messages);
			children.addAll(child.getChildren());
		}
		return Collections.unmodifiableSet(identifiers);
	}
	
	/**
	 * Returns the first line of code covered by this group
	 * @return the firstCodeLine
	 */
	public int getFirstCodeLine() {
		return firstCodeLine;
	}
	
	/**
	 * Returns the last line of code covered by this group
	 * @return the lastCodeLine
	 */
	public int getLastCodeLine() {
		return lastCodeLine;
	}
	
	/**
	 * Convenience method for querying while setting up loops
	 * @return
	 */
	public IOriginMessage getLastMessage() {
		return lastMessage;
	}
	
	public boolean containsMessage(IMessage message) {
		return messages.contains(message.getIdentifier());
	}
	
	/**
	 * Finds the container in this tree for the given message, or null if it coud not be found.
	 * @param message
	 * @return
	 */
	public ASTMessageGroupingTree getMessageContainer(IMessage message) {
		if (containsMessage(message)) {
			return this;
		}
		for (ASTMessageGroupingTree child : children) {
			ASTMessageGroupingTree found = child.getMessageContainer(message);
			if (found != null) {
				return found;
			}
		}
		return null;
	}
	
	public List<ASTMessageGroupingTree> getChildren() {
		return Collections.unmodifiableList(children);
	}

	/**
	 * @return the parent
	 */
	public ASTMessageGroupingTree getParent() {
		return parent;
	}

	/**
	 * @return
	 */
	public ASTNode getNode() {
		return node;
	}
	
	/**
	 * Returns an identifier for the ast node for this tree,
	 * @return
	 */
	public String getNodeID() {
		if (nodeId == null) {
			//find the declaring method
			ASTNode declaringMethod = node;
			while (declaringMethod != null && !(declaringMethod instanceof MethodDeclaration)) {
				declaringMethod = declaringMethod.getParent();
			}
			if (declaringMethod != null) {
				NodeIDVisitor v = new NodeIDVisitor();
				declaringMethod.accept(v);
				nodeId = v.getIdentifier();
			}
		}
		return nodeId;
	} 
}
