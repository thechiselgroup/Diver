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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Lays-out figures in a tree structure, using a default size for each item in the tree.
 * The constraint on each figure is assumed to be its "parent" figure. figures with
 * null parents will be laid-out in a single row on the top. Note: it is expected that
 * the constraints set will create a properly formed forest without gaps or cycles. 
 * Improper orderings may result in infinite loops, memory leaks, or deadlocks.
 * 
 * The layout has a "focal" point which will be used to determine which figure should
 * have focus and be displayed at "full size" 
 * 
 * @author Del Myers
 *
 */
public class ContainmentTreeLayout extends AbstractLayout {
	
	public static interface ContainmentTreeConstraint {
		public IFigure getParentFigure();
	}
	/**
	 * The default size of a figure in the layout. Figures will be laid out as "icons"
	 * on the screen.
	 */
	public static final int FIGURE_SIZE = 16;
	private static final int PADDING = 5;
	
	private HashMap<IFigure, ContainmentTreeConstraint> constraints = new HashMap<IFigure, ContainmentTreeConstraint>();

	private List<Map<IFigure, List<IFigure>>> tree;
	
	Set<IFigure> focalPoint = new HashSet<IFigure>();
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.AbstractLayout#calculatePreferredSize(org.eclipse.draw2d.IFigure, int, int)
	 */
	@Override
	protected Dimension calculatePreferredSize(IFigure container, int whint,
			int hhint) {
		if (tree == null) {
			tree = createLayoutTree(container);
		}
		int maxHeight = 0;
		int maxWidth = 0;
		int height = (FIGURE_SIZE+PADDING)*tree.size();
		ArrayList<IFigure> leaves = new ArrayList<IFigure>();
		//collect the leaves for the tree.
		for (int i = 0; i < tree.size(); i++) {
			Map<IFigure, List<IFigure>> row = tree.get(i);
			for (List<IFigure> figures : row.values()) {
				for (IFigure fig : figures) {
					Dimension prefferred = fig.getPreferredSize();
					if (prefferred.width > maxWidth) {
						maxWidth = prefferred.width;
					}
					if (prefferred.height > maxHeight) {
						maxHeight = prefferred.height;
					}
					boolean add = true;
					if (i < tree.size()-1) {
						Map<IFigure, List<IFigure>> nextRow = tree.get(i+1);
						add = !nextRow.containsKey(fig);
					}
					if (add) {
						leaves.add(fig);
					}
				}
			}
		}
		int width = leaves.size()*(FIGURE_SIZE+PADDING);
		Dimension result = new Dimension(
			((whint > 0) ? Math.max(whint, width) : width),
			((hhint > 0) ? Math.max(hhint, height) : height)
		);
		result.width += maxWidth;
		result.height += maxHeight;
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.LayoutManager#layout(org.eclipse.draw2d.IFigure)
	 */
	public void layout(IFigure container) {
		//Dimension preferred = getPreferredSize(container, -1, -1);
		Dimension size = container.getSize();
		int startX = size.width/2;// - preferred.width/2;
		if (tree == null) {
			tree = createLayoutTree(container);
		}
		if (tree.size() > 0) {
			layoutSubTree(null, 0, startX);
			nudge();
		}
	}
	
	/**
	 * Searches through the tree to find the figure that contains the focal point, and
	 * nudges everything else out of the way.
	 */
	private void nudge() {
		if (focalPoint == null) {
			return;
		}
		int yAdjust = 0;
		for (Map<IFigure, List<IFigure>> row : tree) {
			List<IFigure> rowFigures = new ArrayList<IFigure>();
			for (IFigure parent : row.keySet()) {
				for (IFigure child : row.get(parent)) {
					rowFigures.add(child);
				}
			}
			Collections.sort(rowFigures, new Comparator<IFigure>(){
				public int compare(IFigure o1, IFigure o2) {
					return o1.getBounds().x - o2.getBounds().x;
				}
			});
			//the amount to nudge forward
			int maxRowY = 0;
			for (int i = 0; i < rowFigures.size(); i++) {
				IFigure figure = rowFigures.get(i);
				//adjust the bounds of this figure
				if (yAdjust != 0) {
					Rectangle bounds = figure.getBounds().getCopy();
					bounds.y += yAdjust;
					figure.setBounds(bounds);
				}
				if (focalPoint.contains(figure)) {
					Dimension size = figure.getPreferredSize();
					if (size.height > FIGURE_SIZE + PADDING) {
						if (size.height-FIGURE_SIZE > maxRowY) {
							maxRowY = size.height-FIGURE_SIZE;
						}
					}
					Rectangle bounds = figure.getBounds().getCopy();
					bounds.x -= (size.width/2 - bounds.width/2);
					bounds.setSize(size);
					figure.setBounds(bounds);
					//push all the previous ones back.
					IFigure currentFigure = figure;
					for (int j = i-1; j >= 0; j--) {
						IFigure previousFigure = rowFigures.get(j);
						Rectangle previousBounds = previousFigure.getBounds().getCopy();
						Rectangle currentBounds = currentFigure.getBounds().getCopy();
						int nudge = (previousBounds.x + previousBounds.width + PADDING) - currentBounds.x;
						if (nudge > 0) {
							previousBounds.x -= nudge;
							previousFigure.setBounds(previousBounds);
							currentFigure = previousFigure;
						} else {
							break;
						}
					}
					//push all the next ones forward
					currentFigure = figure;
					for (int j = i+1; j < rowFigures.size(); j++) {
						IFigure nextFigure = rowFigures.get(j);
						Rectangle nextBounds = nextFigure.getBounds().getCopy();
						Rectangle currentBounds = currentFigure.getBounds().getCopy();
						int nudge = (currentBounds.x + currentBounds.width + PADDING) - nextBounds.x;
						if (nudge > 0) {
							nextBounds.x += nudge;
							nextFigure.setBounds(nextBounds);
							currentFigure = nextFigure;
						} else {
							break;
						}
					}
				}
			}
			yAdjust += maxRowY;
		}
	}

	private Rectangle layoutSubTree(IFigure parent, int rowIndex, int startX) {
		Rectangle containment = null;
		Map<IFigure, List<IFigure>> row = tree.get(rowIndex);
		List<IFigure> segment = row.get(parent);
		int y = rowIndex * (FIGURE_SIZE+PADDING);
		if (segment == null) return new Rectangle(startX, y, 0, 0);
		Map<IFigure, List<IFigure>> nextRow = null;
		
		if (rowIndex < tree.size()-1) {
			nextRow = tree.get(rowIndex+1);
		}
		
		for (IFigure figure : segment) {
			if (nextRow != null && nextRow.containsKey(figure)) {
				//for each segment with children, lay it out first
				Rectangle childContainment = layoutSubTree(figure, rowIndex+1, startX);
				if (containment == null) {
					containment = childContainment.getCopy();
				} else {
					containment.union(childContainment);
				}
				//set this bounds to be just above the child containment, and in its middle
				Rectangle bounds = new Rectangle(childContainment
						.getCenter().x
						- (FIGURE_SIZE/2), 
						y,
						FIGURE_SIZE,
						FIGURE_SIZE);
				containment.union(bounds);
				
				figure.setBounds(bounds);
			} else {
				Rectangle bounds = new Rectangle(startX, y, FIGURE_SIZE, FIGURE_SIZE);
				if (containment == null) {
					containment = bounds.getCopy();
				} else {
					containment.union(bounds);
				}
				figure.setBounds(bounds);
			}
			startX = containment.x+containment.width+PADDING;
		}
		return containment;
	}
	
	/**
	 * Creates a "tree" of figures sorted in rows. Each row is a map of "parent" figures
	 * which lists its children.
	 * @param container the container layout.
	 * @return the "tree" structure of figures.
	 */
	private List<Map<IFigure, List<IFigure>>> createLayoutTree(IFigure container) {
		List<Map<IFigure, List<IFigure>>> result = new ArrayList<Map<IFigure, List<IFigure>>>();
		List<?> children = container.getChildren();
		
		for (int i = 0; i < children.size(); i++) {
			IFigure child = (IFigure) children.get(i);
			Object constraint = getConstraint(child);
			int row = 0;
			IFigure parent = null;
			if (constraint instanceof ContainmentTreeConstraint) {
				parent = ((ContainmentTreeConstraint) constraint).getParentFigure();
			}
			IFigure currentParent = parent;
			while (currentParent != null) {
				row++;
				constraint = getConstraint(currentParent);
				if (!(constraint instanceof ContainmentTreeConstraint)) {
					currentParent = null;
				} else {
					currentParent = ((ContainmentTreeConstraint)constraint).getParentFigure();
				}
			}
			while (row >= result.size()) {
				result.add(new HashMap<IFigure, List<IFigure>>());
			}
			List<IFigure> segment = result.get(row).get(parent);
			if (segment == null) {
				segment = new ArrayList<IFigure>();
				result.get(row).put(parent, segment);
			}
			segment.add(child);
		}
		return result;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.AbstractLayout#setConstraint(org.eclipse.draw2d.IFigure, java.lang.Object)
	 */
	@Override
	public void setConstraint(IFigure child, Object constraint) {
		if (child == null) {
			return;
		}
		if (constraint == null || constraint instanceof ContainmentTreeConstraint) {
			constraints.put(child, (ContainmentTreeConstraint) constraint);
		}
		super.setConstraint(child, constraint);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.AbstractLayout#getConstraint(org.eclipse.draw2d.IFigure)
	 */
	@Override
	public Object getConstraint(IFigure child) {
		return constraints.get(child);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.AbstractLayout#invalidate()
	 */
	@Override
	public void invalidate() {
		tree = null;
		super.invalidate();
	}
	
	/**
	 * @param figure
	 */
	public void addFocus(IFigure container, IFigure figure) {
		if (container.getLayoutManager() == this && figure.getParent() == container) {
			focalPoint.add(figure);
		}
		super.invalidate();
		layout(container);
	}

	/**
	 * @param figure
	 */
	public void removeFocus(IFigure container, IFigure figure) {
		if (container.getLayoutManager() == this && figure.getParent() == container) {
			focalPoint.remove(figure);
		}
		super.invalidate();
		layout(container);
	}
}
