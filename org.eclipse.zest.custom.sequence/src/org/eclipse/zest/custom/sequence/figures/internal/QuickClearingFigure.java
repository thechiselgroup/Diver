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
package org.eclipse.zest.custom.sequence.figures.internal;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.draw2d.TreeSearch;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * The base implementation for graphical figures.
 */
public class QuickClearingFigure
extends Figure
{

	private static final Rectangle PRIVATE_RECT = new Rectangle();
	private static final Point PRIVATE_POINT = new Point();
	private static final int
	FLAG_VISIBLE = new Integer(1 << 2).intValue(),
	FLAG_ENABLED = new Integer(1 << 4).intValue(),
	FLAG_FOCUS_TRAVERSABLE = new Integer(1 << 5).intValue();

	static final int
	FLAG_REALIZED = 1 << 31;

	/**
	 * The largest flag defined in this class.  If subclasses define flags, they should
	 * declare them as larger than this value and redefine MAX_FLAG to be their largest flag
	 * value.
	 * <P>
	 * This constant is evaluated at runtime and will not be inlined by the compiler.
	 */
	protected static int MAX_FLAG = FLAG_FOCUS_TRAVERSABLE;

	/**
	 * The rectangular area that this Figure occupies.
	 */
	protected Rectangle bounds = new Rectangle(0, 0, 0, 0);

	private LayoutManager layoutManager;

	/**
	 * The flags for this Figure.
	 */
	protected int flags = FLAG_VISIBLE | FLAG_ENABLED;


	private List<IFigure> children = new LinkedList<IFigure>();;



	/**
	 * @see IFigure#add(IFigure, Object, int)
	 */
	public void add(IFigure figure, Object constraint, int index) {
		if (index < -1 || index > children.size())
			throw new IndexOutOfBoundsException("Index does not exist"); //$NON-NLS-1$

		//Check for Cycle in hierarchy
		for (IFigure f = this; f != null; f = f.getParent())
			if (figure == f)
				throw new IllegalArgumentException(
				"Figure being added introduces cycle"); //$NON-NLS-1$

		//Detach the child from previous parent
		if (figure.getParent() != null)
			figure.getParent().remove(figure);

		if (index == -1)
			children.add(figure);
		else
			children.add(index, figure);
		figure.setParent(this);

		if (layoutManager != null)
			layoutManager.setConstraint(figure, constraint);

		revalidate();

		if (getFlag(FLAG_REALIZED))
			figure.addNotify();
		figure.repaint();
	}



	/**
	 * Called after the receiver's parent has been set and it has been added to its parent.
	 * 
	 * @since 2.0
	 */
	public void addNotify() {
		if (getFlag(FLAG_REALIZED))
			throw new RuntimeException("addNotify() should not be called multiple times"); //$NON-NLS-1$
		setFlag(FLAG_REALIZED, true);
		for (Iterator<IFigure> i = (Iterator<IFigure>)children.iterator();i.hasNext();) {
			IFigure child = i.next();
			child.addNotify();
		}
	}


	/**
	 * Returns a descendant of this Figure such that the Figure returned contains the point
	 * (x, y), and is accepted by the given TreeSearch. Returns <code>null</code> if none 
	 * found.
	 * @param x The X coordinate
	 * @param y The Y coordinate
	 * @param search the TreeSearch
	 * @return The descendant Figure at (x,y)
	 */
	protected IFigure findDescendantAtExcluding(int x, int y, TreeSearch search) {
		PRIVATE_POINT.setLocation(x, y);
		translateFromParent(PRIVATE_POINT);
		if (!getClientArea(Rectangle.SINGLETON).contains(PRIVATE_POINT))
			return null;

		x = PRIVATE_POINT.x;
		y = PRIVATE_POINT.y;
		IFigure fig;
		ListIterator<IFigure> i = children.listIterator(children.size());
		while (i.hasPrevious()) {
			fig = i.previous();
			if (fig.isVisible()) {
				fig = fig.findFigureAt(x, y, search);
				if (fig != null)
					return fig;
			}
		}
		//No descendants were found
		return null;
	}


	/**
	 * Searches this Figure's children for the deepest descendant for which 
	 * {@link #isMouseEventTarget()} returns <code>true</code> and returns that descendant or
	 * <code>null</code> if none found.
	 * @see #findMouseEventTargetAt(int, int)
	 * @param x The X coordinate
	 * @param y The Y coordinate
	 * @return The deepest descendant for which isMouseEventTarget() returns true
	 */
	protected IFigure findMouseEventTargetInDescendantsAt(int x, int y) {
		PRIVATE_POINT.setLocation(x, y);
		translateFromParent(PRIVATE_POINT);

		if (!getClientArea(Rectangle.SINGLETON).contains(PRIVATE_POINT))
			return null;

		IFigure fig;
		for (int i = children.size(); i > 0;) {
			i--;
			fig = (IFigure)children.get(i);
			if (fig.isVisible() && fig.isEnabled()) {
				if (fig.containsPoint(PRIVATE_POINT.x, PRIVATE_POINT.y)) {
					fig = fig.findMouseEventTargetAt(PRIVATE_POINT.x, PRIVATE_POINT.y);
					return fig;
				}
			}
		}
		return null;
	}


	/**
	 * @see IFigure#getChildren()
	 */
	@SuppressWarnings("unchecked")
	public List getChildren() {
		return children;
	}



	/**
	 * @see IFigure#invalidateTree()
	 */
	public void invalidateTree() {
		invalidate();
		for (Iterator<IFigure> iter = children.iterator(); iter.hasNext();) {
			IFigure child = (IFigure) iter.next();
			child.invalidateTree();
		}
	}



	/**
	 * Paints this Figure's children. The caller must save the state of the graphics prior to
	 * calling this method, such that <code>graphics.restoreState()</code> may be called
	 * safely, and doing so will return the graphics to its original state when the method was
	 * entered.
	 * <P>
	 * This method must leave the Graphics in its original state upon return.
	 * @param graphics the graphics used to paint
	 * @since 2.0
	 */
	protected void paintChildren(Graphics graphics) {
		IFigure child;

		Rectangle clip = Rectangle.SINGLETON;
		for (Iterator<IFigure> i = children.iterator(); i.hasNext();) {
			child = i.next();
			if (child.isVisible() && child.intersects(graphics.getClip(clip))) {
				graphics.clipRect(child.getBounds());
				child.paint(graphics);
				graphics.restoreState();
			}
		}
	}

	/**
	 * Paints this Figure's client area. The client area is typically defined as the anything
	 * inside the Figure's {@link Border} or {@link Insets}, and by default includes the
	 * children of this Figure. On return, this method must leave the given Graphics in its
	 * initial state.
	 * @param graphics The Graphics used to paint
	 * @since 2.0
	 */
	protected void paintClientArea(Graphics graphics) {
		if (children.isEmpty())
			return;

		boolean optimizeClip = getBorder() == null || getBorder().isOpaque();

		if (useLocalCoordinates()) {
			graphics.translate(
					getBounds().x + getInsets().left,
					getBounds().y + getInsets().top);
			if (!optimizeClip)
				graphics.clipRect(getClientArea(PRIVATE_RECT));
			graphics.pushState();
			paintChildren(graphics);
			graphics.popState();
			graphics.restoreState();
		} else {
			if (optimizeClip)
				paintChildren(graphics);
			else {
				graphics.clipRect(getClientArea(PRIVATE_RECT));
				graphics.pushState();
				paintChildren(graphics);
				graphics.popState();
				graphics.restoreState();
			}
		}
	}



	/**
	 * Translates this Figure's bounds, without firing a move.
	 * @param dx The amount to translate horizontally
	 * @param dy The amount to translate vertically
	 * @see #translate(int, int)
	 * @since 2.0
	 */
	protected void primTranslate(int dx, int dy) {
		bounds.x += dx;
		bounds.y += dy;
		if (useLocalCoordinates()) {
			fireCoordinateSystemChanged();
			return;
		}
		for (Iterator<IFigure> i = children.iterator(); i.hasNext();)
			i.next().translate(dx, dy);
	}

	/**
	 * Removes the given child Figure from this Figure's hierarchy and revalidates this
	 * Figure. The child Figure's {@link #removeNotify()} method is also called.
	 * @param figure The Figure to remove
	 */
	public void remove(IFigure figure) {
		if ((figure.getParent() != this))
			throw new IllegalArgumentException(
			"Figure is not a child"); //$NON-NLS-1$
		if (getFlag(FLAG_REALIZED))
			figure.removeNotify();
		if (layoutManager != null)
			layoutManager.remove(figure);
		// The updates in the UpdateManager *have* to be
		// done asynchronously, else will result in 
		// incorrect dirty region corrections.
		figure.erase();
		figure.setParent(null);
		children.remove(figure);
		revalidate();
	}

	
	/**
	 * Removes all figures in this figure.
	 */
	public void removeAll() {
		LinkedList<IFigure> oldChildren = (LinkedList<IFigure>)children;
		this.children = new LinkedList<IFigure>();
		ListIterator<IFigure> i = oldChildren.listIterator(children.size());
		while (i.hasPrevious()) {
			IFigure next = i.previous();
			next.erase();
			next.setParent(null);
			if (getFlag(FLAG_REALIZED))
				next.removeNotify();
			layoutManager.remove(next);
		}
		revalidate();
	}
	


	/**
	 * Called prior to this figure's removal from its parent
	 */
	public void removeNotify() {
		for (Iterator<IFigure> i = children.iterator(); i.hasNext();)
			i.next().removeNotify();
		if (internalGetEventDispatcher() != null)
			internalGetEventDispatcher().requestRemoveFocus(this);
		setFlag(FLAG_REALIZED, false);
	}

}
