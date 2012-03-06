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

import java.util.Iterator;

import org.eclipse.draw2d.ActionEvent;
import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;


/**
 * A figure that draws a box around a group.
 * @author Del Myers
 */

public class ActivationGroupFigure extends Shape {
	
	/**
	 * 
	 */
	private static final String ELLIPSIS = "...";
	private Rectangle clickBounds;
	private String text;
	private String subStringText;
	private boolean down;
	private int labelOffset;
	
	private class MouseActionListener implements MouseListener {
		public void mouseDoubleClicked(MouseEvent me) {
			ActivationGroupFigure.this.mouseDoubleClicked(me);
		}
		public void mousePressed(MouseEvent me) {
			ActivationGroupFigure.this.mousePressed(me);
		}
		public void mouseReleased(MouseEvent me) {
			ActivationGroupFigure.this.mouseReleased(me);
		}
	}
	
	/**
	 * 
	 */
	public ActivationGroupFigure() {
		addMouseListener(new MouseActionListener());
		clickBounds = new Rectangle();
		down = true;
	}
	
	public void addActionListener(ActionListener listener) {
		addListener(ActionListener.class, listener);
	}
	
	public void removeActionListener(ActionListener listener) {
		removeListener(ActionListener.class, listener);
	}

	public void setText(String text) {
		this.text = text;
		recalculateTextExtents();
	}
	
	/**
	 * @param me
	 */
	protected void mouseReleased(MouseEvent me) {
	}

	/**
	 * @param me
	 */
	protected void mousePressed(MouseEvent me) {
		if (me.button == 1) {
			//check the region.
			if (clickBounds != null && clickBounds.contains(me.x, me.y)) {
				down = !down;
				resetClickBounds();
				repaint();
				fireActionPerformed();
			} else {
				Rectangle clientArea = getClientArea();
				Rectangle activeArea = new Rectangle(
					clientArea.getLocation(),
					getLabelSize()
				);
				if (!activeArea.contains(me.getLocation())) {
					me.consume();
				}
			}
		}
	}
	
	public boolean isExpanded() {
		return down;
	}

	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void fireActionPerformed() {
		Iterator listeners = getListeners(ActionListener.class);
		ActionEvent event = new ActionEvent(this);
		while (listeners.hasNext()) {
			((ActionListener)listeners.next()).actionPerformed(event);
		}
		
	}

	/**
	 * @param me
	 */
	protected void mouseDoubleClicked(MouseEvent me) {

	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * @return the subStringText
	 */
	public String getSubStringText() {
		return subStringText;
	}
	
	public Dimension getLabelSize() {
		return FigureUtilities.getStringExtents(subStringText, getFont()).expand(14, 4);
	}
	
	public Dimension getPreferredLabelSize() {
		return FigureUtilities.getStringExtents(getText(), getFont()).expand(14, 4);
	}

	/**
	 * 
	 */
	private void recalculateTextExtents() {
		subStringText = text;
		Font font = getFont();
		if (text != null && font != null) {
			Dimension te = FigureUtilities.getTextExtents(text, font);
			if (te.width >= bounds.width) {
				Dimension ee = FigureUtilities.getTextExtents(ELLIPSIS, font);
				int subStringLength = getLargestSubstringConfinedTo(text,
						  font,
						  getBounds().width - ee.width-12);
				subStringText = new String(text.substring(0, subStringLength) + ELLIPSIS);
			}
		}
	}
	
	private int getLargestSubstringConfinedTo(String s, Font f, int availableWidth) {
		FontMetrics metrics = FigureUtilities.getFontMetrics(f);
		int min, max;
		float avg = metrics.getAverageCharWidth();
		min = 0;
		max = s.length() + 1;

		//The size of the current guess
		int guess = 0,
		    guessSize = 0;
		while ((max - min) > 1) {
			//Pick a new guess size
			//	New guess is the last guess plus the missing width in pixels
			//	divided by the average character size in pixels
			guess = guess + (int)((availableWidth - guessSize) / avg);

			if (guess >= max) guess = max - 1;
			if (guess <= min) guess = min + 1;

			//Measure the current guess
			guessSize = FigureUtilities.getTextExtents(s.substring(0, guess), f).width;

			if (guessSize < availableWidth)
				//We did not use the available width
				min = guess;
			else
				//We exceeded the available width
				max = guess;
		}
		return min;
	}

	@Override
	protected final void fillShape(Graphics graphics) {
		Dimension labelSize = getLabelSize();
		graphics.setLineWidth(getLineWidth());
		Rectangle labelBounds = new Rectangle(new Point(getBounds().x+labelOffset, getBounds().y), labelSize);
		PointList points = new PointList(new int[]{
			labelBounds.x, labelBounds.y,
			labelBounds.x+labelBounds.width+4, labelBounds.y,
			labelBounds.x+labelBounds.width+4, labelBounds.y+labelBounds.height,
			labelBounds.x+labelBounds.width, labelBounds.y+labelBounds.height+2,
			labelBounds.x, labelBounds.y+labelBounds.height+2
		});
		graphics.fillPolygon(points);
		outlineShape(graphics);
		
	}

	@Override
	protected final void outlineShape(Graphics graphics) {
		Rectangle bounds = getBounds();
		graphics.setLineWidth(getLineWidth());
		Color foreground = getForegroundColor();
		if (foreground == null) {
			foreground = ColorConstants.black;
		}
		graphics.setForegroundColor(foreground);
		graphics.drawRectangle(bounds.x, bounds.y, bounds.width-1, bounds.height-1);
		PointList list = new PointList();
//		list.addPoint(0,0);
		list.addPoint(clickBounds.x, clickBounds.y);
		//draw the click region.
		if (down) {
//			list.addPoint(4, 8);
//			list.addPoint(8,0);
			list.addPoint(clickBounds.x + clickBounds.width/2, clickBounds.y+clickBounds.height-1);
			list.addPoint(clickBounds.x + clickBounds.width-1, clickBounds.y);
		} else {
//			list.addPoint(8, 4);
//			list.addPoint(0, 8);
			list.addPoint(clickBounds.x, clickBounds.y+clickBounds.height-1);
			list.addPoint(clickBounds.x + clickBounds.width-1, clickBounds.y+clickBounds.height/2);
		}
//		list.translate(clickBounds.x, clickBounds.y);
		Color background = getBackgroundColor();
		graphics.setBackgroundColor(ColorConstants.black);
		graphics.fillPolygon(list);
		graphics.setBackgroundColor(background);
		//draw the text with ellipses

		graphics.drawText(subStringText, bounds.x+2+clickBounds.width+labelOffset, bounds.y+2);
		Dimension labelSize = getLabelSize();
		
		Rectangle labelBounds = new Rectangle(new Point(getBounds().x+labelOffset, getBounds().y), labelSize);
		PointList points = new PointList(new int[]{
				labelBounds.x, labelBounds.y,
				labelBounds.x+labelBounds.width+4, labelBounds.y,
				labelBounds.x+labelBounds.width+4, labelBounds.y+labelBounds.height,
				labelBounds.x+labelBounds.width, labelBounds.y+labelBounds.height+2,
				labelBounds.x, labelBounds.y+labelBounds.height+2
		});
		graphics.drawPolygon(points);
		
		
	}
	
	@Override
	public void setBounds(Rectangle rect) {
		super.setBounds(rect);
		resetClickBounds();
		recalculateTextExtents();
	}
	
	/**
	 * 
	 */
	private void resetClickBounds() {
		int yPad = 4;
		if (down) {
			yPad = 6;
		}
		clickBounds = new Rectangle(getBounds().x+2+labelOffset, getBounds().y+yPad, 9,9);
	}

	@Override
	public boolean containsPoint(int x, int y) {
		Rectangle labelRectangle = new Rectangle(new Point(getBounds().x+labelOffset, getBounds().y), getLabelSize());
		return labelRectangle.contains(x, y);
	}

	/**
	 * @param b
	 */
	public void setArrowDown(boolean b) {
		this.down = b;		
	}

	/**
	 * @param i
	 */
	public void setLabelOffset(int i) {
		this.labelOffset = i;
		
	}
	
	

}
