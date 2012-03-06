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
package ca.uvic.chisel.javasketch.ui.internal.views;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;

import org.eclipse.core.runtime.IAdaptable;


public class TreeNode implements IAdaptable {
	private TreeNode parent;
	public final Object data;
	private LinkedList<TreeNode> children;

	public TreeNode(TreeNode parent, Object data) {
		this.parent = parent;
		this.data = data;
		this.children = null;
	}
	
	public synchronized TreeNode addChild(Object data) {
		if (children == null) {
			children = new LinkedList<TreeNode>();
		}
		TreeNode node = new TreeNode(this, data);
		children.add(node);
		return node;
	}
	
	public synchronized void clearChildren() {
		if (children == null) return;
		//set the parent to each child to null
		LinkedList<TreeNode> childList = new LinkedList<TreeNode>(children);
		while (childList.size() > 0) {
			TreeNode child = childList.removeFirst();
			if (child.children != null) {
				childList.addAll(child.children);
			}
			child.parent = null;
		}
		children = null;
	}
	
	public synchronized boolean isOrphaned() {
		return parent != null;
	}
	
	public synchronized TreeNode getParent() {
		return parent;
	}

	/**
	 * @return
	 */
	public synchronized TreeNode[] getChildren() {
		if (children == null) {
			return new TreeNode[0];
		}
		return children.toArray(new TreeNode[children.size()]);
	}
	
	public synchronized Object[] getChildElements() {
		if (children == null) {
			return new Object[0];
		}
		Object[] array = new Object[children.size()];
		int i = 0;
		for (TreeNode child : children) {
			array[i] = child.data;
			i++;
		}
		return array;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter != null && data != null && adapter.isInstance(data)) {
			return data;
		}
		return null;
	}
	
	/**
	 * Finds a child in this node that contains the given data.
	 * @param parent
	 * @return
	 */
	public synchronized TreeNode findNode(Object data) {
		return recursiveFindNode(this, data);
	}

	/**
	 * @param tree2
	 * @param parent
	 * @return
	 */
	private TreeNode recursiveFindNode(TreeNode node, Object data) {
		if (node.data.equals(data)) {
			//we must use the identity of a Calendar because many runs may
			//occur on the same day
			if (node.data instanceof Calendar) {
				if (node.data == data) {
					return node;
				}
			} else {
				return node;
			}
		}
		for (TreeNode child : node.getChildren()) {
			TreeNode found = recursiveFindNode(child, data);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/**
	 * Returns all of the child elements of this node.
	 * @return
	 */
	public synchronized Object[] getAllChildElements() {
		if (children == null) return new Object[0];
		LinkedList<TreeNode> allChildren = new LinkedList<TreeNode>(children);
		ArrayList<Object> elements = new ArrayList<Object>();
		while (allChildren.size() > 0) {
			TreeNode child = allChildren.removeFirst();
			elements.add(child.data);
			if (child.children != null) {
				allChildren.addAll(child.children);
			}
		}
		return elements.toArray();
	}

	public synchronized boolean isLoaded() {
		return children != null;
	}
	
	/**
	 * Used to set the node to an empty node that is loaded, but has no
	 * children.
	 */
	public void setEmpty() {
		children = new LinkedList<TreeNode>();
	}

}