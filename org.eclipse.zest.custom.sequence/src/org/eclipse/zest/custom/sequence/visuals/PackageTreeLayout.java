/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers -- initial API and implementation
 *******************************************************************************/
package org.eclipse.zest.custom.sequence.visuals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Abstract layout for setting packages up in a package tree.
 * @author Del Myers
 */

public class PackageTreeLayout extends AbstractLayout {
	private HashMap<IFigure, Object> constraints;

	
	public static interface IPackageConstraint {
		public List<IFigure> getChildFigures();
	}
	
	private class LayoutTree {
		HashSet<LayoutNode> roots;
		HashSet<IFigure> touched;
		
		public LayoutTree() {
			this.roots = new HashSet<LayoutNode>();
			this.touched = new HashSet<IFigure>();
		}
		
		public void addToTree(IFigure figure) {
			Object constraint = getConstraint(figure);
			if (constraint instanceof IPackageConstraint) {
				createNodes(figure);
			}
		}

		/**
		 * @param figure
		 */
		private LayoutNode createNodes(IFigure figure) {
			IPackageConstraint constraint = (IPackageConstraint) getConstraint(figure);
			if (constraint == null) return null;
			LayoutNode node = new LayoutNode(figure, constraint.getChildFigures());
			if (touched.contains(figure)) {
				return node;
			}
			touched.add(figure);
			
			List<IFigure> children = constraint.getChildFigures();
			if (children.size() == 0) {
				roots.add(node);
			} else {
				for (IFigure child : children) {
					LayoutNode childNode = createNodes(child);
					if (childNode == null) continue;
					if (roots.contains(childNode)) {
						roots.remove(childNode);
						roots.add(node);
					}
				}
			}
			return node;
		}
	}
	
	private class LayoutNode {
		IFigure nodeFigure;
		List<IFigure> childFigures;
		Collection<LayoutNode> childNodes;
		public LayoutNode(IFigure data, List<IFigure> children) {
			this.nodeFigure = data;
			this.childFigures = children;
		}
		
		public boolean equals(Object that) {
			if (!(that instanceof LayoutNode)) return false;
			return (((LayoutNode)that).nodeFigure.equals(nodeFigure));
		}
		
		public int hashCode() {
			return nodeFigure.hashCode();
		}
		
		public Collection<LayoutNode> getChildNodes() {
			if (childNodes != null) {
				return childNodes;
			}
			List<LayoutNode> children = new ArrayList<LayoutNode>();
			for (IFigure child : childFigures) {
				IPackageConstraint constraint = (IPackageConstraint) getConstraint(child);
				LayoutNode node = new LayoutNode(child, constraint.getChildFigures());
				children.add(node);
			}
			childNodes = children;
			return childNodes;
		}
	}

	
	public PackageTreeLayout() {
		super();
		constraints = new HashMap<IFigure, Object>();
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.AbstractLayout#calculatePreferredSize(org.eclipse.draw2d.IFigure, int, int)
	 */
	@Override
	protected Dimension calculatePreferredSize(IFigure container, int wHint,
			int hHint) {

		LayoutTree tree = new LayoutTree();
		for (Object child : container.getChildren()) {
			tree.addToTree((IFigure)child);
		}

		Dimension d = calculateSize(tree.roots, 0, 0);
		d.height += 10;
		d.width+=10;
		return d;
	
	
	}

	/**
	 * @param roots
	 * @return
	 */
	private Dimension calculateSize(Collection<LayoutNode> nodes, int top, int left) {
		int height = 0; int width = 0; int heightCount = 0;
		for (LayoutNode node : nodes) {
			//get the maximum height.
			height = Math.max(node.nodeFigure.getPreferredSize().height, height);
		}
		heightCount = height;
		for (LayoutNode node : nodes) {
			Collection<LayoutNode> children = node.getChildNodes();
			Dimension size = node.nodeFigure.getPreferredSize();
			//node.nodeFigure.setBounds(new Rectangle(left + width, top, size.width, size.height));
			if (children.size() == 0) {
				width += size.width+10;
			} else {
				Dimension childDim = calculateSize(node.getChildNodes(), top + height + 10, left + width);
				if (height + childDim.height + 10 > heightCount)
					heightCount = height + childDim.height + 10;
				width += 10 + ((childDim.width > size.width) ? childDim.width : size.width);
			}
		}
		return new Dimension(width, heightCount);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.LayoutManager#layout(org.eclipse.draw2d.IFigure)
	 */
	public void layout(IFigure container) {
		LayoutTree tree = new LayoutTree();
		for (Object child : container.getChildren()) {
			tree.addToTree((IFigure)child);
		}
		Rectangle bounds = new Rectangle();
		layoutNodes(tree.roots, 0,10, bounds);
		container.translateToParent(bounds);
	}
	
	private Dimension layoutNodes(Collection<LayoutNode> nodes, int top, int left, Rectangle bounds) {
		int height = 0; int width = 0;
		for (LayoutNode node : nodes) {
			//get the maximum height.
			height = Math.max(node.nodeFigure.getPreferredSize().height, height);
		}
		for (LayoutNode node : nodes) {
			Collection<LayoutNode> children = node.getChildNodes();
			Dimension size = node.nodeFigure.getPreferredSize();
			Rectangle r = new Rectangle(left + width, top, size.width, size.height);
			node.nodeFigure.translateToParent(r);
			node.nodeFigure.setBounds(r);
			if (bounds.isEmpty()) {
				bounds.setBounds(r);
			} else {
				bounds = bounds.union(r);
			}
			if (children.size() == 0) {
				width += size.width+10;
			} else {
				Dimension childDim = layoutNodes(node.getChildNodes(), top + height + 10, left + width, bounds);
				if (childDim.width > size.width) {
					width += childDim.width;
				} else {
					width += size.width;
				}
				width += 10;
			}
		}
		return new Dimension(width, height);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.AbstractLayout#setConstraint(org.eclipse.draw2d.IFigure, java.lang.Object)
	 */
	@Override
	public void setConstraint(IFigure child, Object constraint) {
		constraints.put(child, constraint);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.AbstractLayout#getConstraint(org.eclipse.draw2d.IFigure)
	 */
	@Override
	public Object getConstraint(IFigure child) {
		return constraints.get(child);
	}

}
