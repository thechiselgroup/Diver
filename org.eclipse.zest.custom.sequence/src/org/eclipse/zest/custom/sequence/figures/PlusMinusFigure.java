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

import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Toggle;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;


/**
 * A plus/minus toggle figure.
 * @author Chris Callendar
 */
public class PlusMinusFigure extends Toggle {
	
	public PlusMinusFigure() {
		this(16);
	}
	
	/**
	 * Initializes this figure with the given listener (for 
	 * being notified when the figure is toggled).
	 * @param plusSize The plus size
	 */
	public PlusMinusFigure(int plusSize) {
		setPreferredSize(plusSize, plusSize);
		setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_ARROW));
	}
	
	/**
	 * Adds an action listener to be notified when the PlusMinusFigure is toggled.
	 * @see org.eclipse.draw2d.Clickable#addActionListener(org.eclipse.draw2d.ActionListener)
	 */
	public void addActionListener(ActionListener listener) {
		if (listener != null) {
			super.addActionListener(listener);
		}
	}

	/**
	 * @see org.eclipse.draw2d.Figure#paintFigure(org.eclipse.draw2d.Graphics)
	 */
	protected void paintFigure(Graphics g) {
		g.setClip(getClientArea());
		super.paintFigure(g);
		
		Rectangle r = Rectangle.SINGLETON;
		r.setBounds(getBounds());
		g.fillRectangle(r);
		
		Insets insets = getInsets();
		if (!insets.isEmpty()) {
			r.resize(0 - (insets.left + insets.right), 0 - (insets.top + insets.bottom));
			r.translate(insets.left, insets.top);
		}
		// define a square for the border
		if (r.width <= r.height) {
			r.y = r.y + (r.height - r.width) / 2;
			r.height = r.width;
		} else {
			r.x = r.x + (r.width - r.height) / 2;
			r.width = r.height;
		}
		g.setBackgroundColor(ColorConstants.white);
		Color fg = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
		Color bg = Display.getDefault().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT);
		if (!isSelected()) {
			Color t = fg;
			fg = bg;
			bg = t;
		}
		g.setForegroundColor(fg);
		g.setBackgroundColor(bg);
		g.fillGradient(r, true);
		
//		g.setForegroundColor(Display.getDefault().getSystemColor(SWT.COLOR_CYAN));
//		g.drawRectangle(new Rectangle(r.x+1, r.y+1, r.width-2, r.height-2));
		g.setForegroundColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
		r = new Rectangle(r.x, r.y, r.width-1, r.height-1);
		g.drawRectangle(r);
		
		int xMid = r.x + r.width / 2;
		int yMid = r.y + r.height / 2;
//		if (r.width >= 10 && r.height >= 10) {
//			//draw the cyan outline... 
//			g.setForegroundColor(Display.getDefault().getSystemColor(SWT.COLOR_CYAN));
//			g.drawLine(r.x + 2, yMid+1, r.right() - 1, yMid+1);
//			if (isSelected()) {
//				g.drawLine(xMid+1, r.y + 3, xMid+1, r.bottom() - 2);
//			}
//			g.setForegroundColor(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
//			g.drawLine(r.x + 2, yMid, r.right() - 2, yMid);
//			if (isSelected()) {
//				g.drawLine(xMid, r.y + 2, xMid, r.bottom() - 2);
//			}
//		} else {
			//just draw the plus or minus in the middle.
			int left = r.x+1;
			int right = r.right()-1;
			int top = r.y+1;
			int bottom = r.bottom()-1;
			
			if (r.width > 7 && r.height > 7) {
				left+=1;
				right-=1;
				top+=1;
				bottom-=1;
			}
			g.drawLine(left, yMid, right, yMid);
			if (isSelected()) {
				g.drawLine(xMid, top, xMid, bottom);
			}
//		}
			
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Clickable#paintBorder(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void paintBorder(Graphics graphics) {
		//don't paint a border.
	}
	
}
