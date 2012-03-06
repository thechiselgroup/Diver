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
package org.eclipse.zest.custom.sequence.figures;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.geometry.Transposer;
import org.eclipse.zest.custom.sequence.figures.SashFigure.SashBarFigure;

/**
 * Layout for sash forms.
 * @author Del Myers
 */

public class SashLayout extends AbstractLayout {
	public static final int SASH_BAR_SIZE = 5;
	
	private boolean isHorizontal;
	
	private int[] sashSizes;

	private int padding;
	
	private Dimension lastSize;
	
	public SashLayout(boolean horizontal) {
		this.isHorizontal = horizontal;
		sashSizes = new int[0];
		lastSize = new Dimension();
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.AbstractLayout#getMinimumSize(org.eclipse.draw2d.IFigure, int, int)
	 */
	@Override
	public Dimension getMinimumSize(IFigure container, int wHint, int hHint) {
		Dimension preferred = getPreferredSize(container, wHint, hHint);
		Dimension result = new Dimension(Math.min(preferred.width, wHint), Math.min(preferred.height, hHint));
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.AbstractLayout#calculatePreferredSize(org.eclipse.draw2d.IFigure, int, int)
	 */
	@Override
	protected Dimension calculatePreferredSize(IFigure container, int wHint,
			int hHint) {
		int width = 0;
		int height = 0;
		Transposer t = new Transposer();
		t.setEnabled(isHorizontal);
		List<?> children = container.getChildren();
		for (Object o : children) {
			Dimension size = ((IFigure)o).getPreferredSize(wHint, hHint);
			t.t(size);
			height += size.height;
			if (size.width > width) {
				width = size.width;
			}
		}
		height += (SASH_BAR_SIZE+padding+padding) * (children.size()-1);
		Dimension d = new Dimension(
			Math.max(wHint, width),
			Math.max(hHint, height)
		);
		return t.t(d);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.LayoutManager#layout(org.eclipse.draw2d.IFigure)
	 */
	public void layout(IFigure container) {
		resetSashBars(container);
		List<?> children = container.getChildren();
		Transposer t = new Transposer();
		t.setEnabled(isHorizontal);
		Rectangle bounds = t.t(container.getClientArea());
		int x = bounds.x;
		int y = bounds.y;
		int pad = padding+padding;
		Dimension sashSize = new Dimension(bounds.width, SASH_BAR_SIZE);
		for (int i = 0; i < children.size(); i++) {
			IFigure child = (IFigure) children.get(i);
			if (child instanceof SashBarFigure) {
				Rectangle barBounds = new Rectangle(new Point(x,y), sashSize);
				y+= pad + SASH_BAR_SIZE;
				child.setBounds(t.t(barBounds));
			} else {
				int size = sashSizes[i/2];
				Rectangle newBounds = new Rectangle(x, y, bounds.width, size);
				y += size;
				if (!(child instanceof SashFigure)) {
					newBounds.shrink(padding, padding);
					y+= pad;
				}
				child.setBounds(t.t(newBounds));
			}
		}
	}
	
	public void resetSashBars(IFigure sashContainer) {
		List<?> children = sashContainer.getChildren();
		if (children.size()/2 + 1!= sashSizes.length || !lastSize.equals(sashContainer.getSize())) {
			lastSize = sashContainer.getSize().getCopy();
			int[] newSizes = new int[children.size()/2 + 1];
			int i = 0;
			for (i = 0; i < sashSizes.length; i++) {
				if (i >= newSizes.length) break;
				newSizes[i] = sashSizes[i];
			}
			for (; i < newSizes.length; i++) {
				newSizes[i] = -1;
			}
			sashSizes = newSizes;
			
			//reset the constraints to fill up the container's area.
			Dimension containerSize = sashContainer.getSize();
			int majorSize = (isHorizontal) ? containerSize.width : containerSize.height;
			int maximumArea = majorSize - (children.size()-1)*SASH_BAR_SIZE;
			if (maximumArea < 0) {
				maximumArea = 0;
			}
			int areaLeft = maximumArea;
			List<Integer> uninitialized = new LinkedList<Integer>();
			for (i = 0; i < sashSizes.length; i++) {
				if (sashSizes[i] >= 0) {
					if (sashSizes[i] > areaLeft) {
						sashSizes[i] = areaLeft;
					}
					areaLeft -= sashSizes[i];
				} else {
					uninitialized.add(i);
				}
			}
			if (uninitialized.size() > 0) {
				int distributed = areaLeft/uninitialized.size();
				int pad = areaLeft % uninitialized.size();
				for (int u : uninitialized) {
					sashSizes[u] = distributed;
				}
				sashSizes[uninitialized.get(0)] += pad;
			} else if (areaLeft > 0) {
				//add it to the end
				sashSizes[sashSizes.length-1] += areaLeft;
			}
			
		}
	}
	
	public void moveSashBar(IFigure sashContainer, int index, int difference) {
		resetSashBars(sashContainer);
		if (index >= sashSizes.length-1) return;
		if (index < 0) return;
		int successor = sashSizes[index+1] - difference;
		int newSize = sashSizes[index] + difference;
		if (newSize < 0) {
			successor = sashSizes[index+1] + sashSizes[index];
			newSize = 0;
		} else if (successor < 0) {
			newSize = sashSizes[index] + sashSizes[index+1];
			successor = 0;
		}
		sashSizes[index] = newSize;
		sashSizes[index+1] = successor;
		sashContainer.revalidate();
	}
	
	public int getSashBarCount(IFigure sashContainer) {
		resetSashBars(sashContainer);
		return sashSizes.length -1;
	}
	
	public int getSashBarSize(IFigure sashContainer, int index) {
		if (index < 0 || index > getSashBarCount(sashContainer)) {
			return -1;
		}
		return sashSizes[index];
	}
	


	/**
	 * Sets padding around the sashes.
	 * @param padding the amount of space surrounding the sashes.
	 */
	public void setPadding(int padding) {
		this.padding = padding;
	}
	
	/**
	 * @return the padding
	 */
	public int getPadding() {
		return padding;
	}

}
