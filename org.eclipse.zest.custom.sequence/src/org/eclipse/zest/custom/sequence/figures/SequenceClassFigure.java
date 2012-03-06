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

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;

/**
 * A figure that represents a sequence class. Sequence classes have
 * labels for the class name and a stereo type. The figure drawn
 * depends on the type of class. This type can either be the
 * standard CLASS, in which case a box with an underlined
 * class name is drawn, or it can be ACTOR, in which case a 
 * stick figure is drawn.
 * 
 * Children cannot be added to the sequence class, nor can
 * a layout be set on it. Trying to do so will not cause an
 * error, but it will have no effect.
 * 
 * @author Del Myers
 *
 */
public class SequenceClassFigure extends Figure {
	public static final int CLASS = 0;
	public static final int COLLECTION = 1;
	public static final int ACTOR = 2;
	public static final int BOUNDARY = 3;
	public static final int CONTROL = 4;
	public static final int ENTITY = 5;
	public static final int DATA_STORE = 6;
	public static final int PACKAGE = 7;
	
	private final int MINIMUM_SIZE = 16;
	private int type;
	private Shape classFigure;
	private Label classLabel;
	private Label stereoTypeLabel;
	private class UnderlineLabel extends Label {
		private String subStringText;

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Label#paintFigure(org.eclipse.draw2d.Graphics)
		 */
		protected void paintFigure(Graphics graphics) {
			super.paintFigure(graphics);
			Dimension size = getSubStringTextSize();
			Point location = getTextLocation().getCopy();
			Border b = getBorder();
			if (b != null) {
				location.x -= b.getInsets(this).left;
				location.y -= b.getInsets(this).top;
			}
			Rectangle r = getClientArea();
			graphics.translate(r.x, r.y);
			Rectangle c =r.getCopy();
			translateToRelative(c);
			//graphics.setClip(c);
			graphics.setLineWidth(2);
			graphics.drawLine(location.x, location.y+size.height, location.x+size.width, location.y+size.height);
			graphics.translate(-r.x, -r.y);
			
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Figure#getBounds()
		 */
		public Rectangle getBounds() {
			Rectangle b = super.getBounds().getCopy();
			b.height+=6;
			return b;
		}
		
		public String getSubStringText() {
//			if (subStringText != null)
//				return subStringText;
			String text = getText();
			subStringText = getText();
			int dot = text.lastIndexOf('.');
			String className = "";
			if (dot >=0 && dot < text.length()) {
				className = text.substring(dot+1);
			}
			int widthShrink = getPreferredSize().width - getSize().width;
			if (widthShrink <= 0)
				return subStringText;
			
			Dimension effectiveSize = getTextSize().getExpanded(-widthShrink, 0);
			Font currentFont = getFont();
			int dotsWidth = FigureUtilities.getTextWidth("....", currentFont);
			
			if (effectiveSize.width < dotsWidth)
				effectiveSize.width = dotsWidth;
			
			int subStringLength = getLargestSubstringConfinedTo(text,
														  currentFont,
														  effectiveSize.width - dotsWidth);
			if (text.length() > subStringLength) {
				if (!"".equals(className)) {
					if (className.length() >= subStringLength) {
						subStringLength = getLargestSubstringConfinedTo(className,
								  currentFont,
								  effectiveSize.width - dotsWidth);
						subStringText = new String(className.substring(0, subStringLength) + "...");
					} else {
						subStringText = className;
					}
				} else {
					subStringText = new String(text.substring(0, subStringLength) + "...");
				}
				
			} else {
				subStringText = text;
			}
			return subStringText;
		}
		
		int getLargestSubstringConfinedTo(String s, Font f, int availableWidth) {
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
	
	}
	public SequenceClassFigure(int type) {
		super();
		this.type = type;
		classFigure = createClassFigure();
		classLabel = new UnderlineLabel();
		classLabel.setBorder(new SpacerBorder(3,3,3,3));
		stereoTypeLabel = new Label();
		this.type = type;
	}
	
	protected Dimension calculatePreferredSize(int wHint, int hHint) {
		updateInternalFigures();
		Rectangle classTextSize = classLabel.getTextBounds();
		Rectangle stereoTextSize = stereoTypeLabel.getTextBounds();
		//don't show the stereotype if it is empty
		if (stereoTypeLabel.getText() == null || (stereoTypeLabel.getText().trim().length()==0)) {
			stereoTextSize.height = 0;
		}
		classTextSize.expand(classLabel.getBorder().getInsets(classLabel));
		int labelHeight = classTextSize.height + stereoTextSize.height+5;
		
		int height = labelHeight;
		switch (getType()) {
		case ACTOR:
		case BOUNDARY:
		case CONTROL:
		case ENTITY:
		case DATA_STORE:
			height += MINIMUM_SIZE;
		}
		int width = Math.max(classTextSize.width, stereoTextSize.width);
		if (hHint > height) {
			height = hHint;
		}
		if (wHint > width) {
			width = wHint;
		}
		
		return new Dimension(width, height);
	}
	
	/**
	 * 
	 */
	private void updateInternalFigures() {
		classFigure.setBackgroundColor(getBackgroundColor());
		classFigure.setForegroundColor(getForegroundColor());
		classFigure.setFont(getFont());
		
		classLabel.setBackgroundColor(getBackgroundColor());
		classLabel.setForegroundColor(getForegroundColor());
		classLabel.setFont(getFont());
		
		stereoTypeLabel.setBackgroundColor(getBackgroundColor());
		stereoTypeLabel.setForegroundColor(getForegroundColor());
		stereoTypeLabel.setFont(getFont());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#getPreferredSize(int, int)
	 */
	@Override
	public Dimension getPreferredSize(int wHint, int hHint) {
		Dimension localSize = calculatePreferredSize(wHint, hHint);
		updateInternalFigures();
		if (getLayoutManager() != null) {
			Dimension d = getLayoutManager().getPreferredSize(this, wHint, hHint);
			if (d != null) {
				localSize.height += d.height;
				if (d.width > localSize.width) {
					localSize.width = localSize.width;
				}
			}
		}
		return localSize;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#getMaximumSize()
	 */
	@Override
	public Dimension getMaximumSize() {
		return super.getMaximumSize();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#getMinimumSize(int, int)
	 */
	@Override
	public Dimension getMinimumSize(int hint, int hint2) {
		Dimension d = super.getMinimumSize(hint, hint2);
		//put the minimum size on the top of this area
		d.height+=MINIMUM_SIZE;
		if (d.width < MINIMUM_SIZE) {
			d.width = MINIMUM_SIZE;
		}
		return d;
	}
	
	private void layoutLocalFigures() {
		updateInternalFigures();
		Dimension classTextSize = classLabel.getPreferredSize();
		Dimension stereoTextSize = stereoTypeLabel.getPreferredSize();
		Rectangle bounds = getBounds().getCopy();
		Rectangle clientArea = getClientArea();
		bounds.y += clientArea.height;
		bounds.height -= clientArea.height;
		//don't show the stereotype if it is empty
		if (stereoTypeLabel.getText() == null || (stereoTypeLabel.getText().trim().length()==0)) {
			stereoTextSize.height = 0;
		}
		int labelHeight = classTextSize.height + stereoTextSize.height+5;
		
		switch(getType()) {
		case ACTOR:
		case BOUNDARY:
		case CONTROL:
		case ENTITY:
		case DATA_STORE:
			//place the label at the bottom, and 
			//give the rest of the room to the class figure.
			if (bounds.height - MINIMUM_SIZE < labelHeight) {
				labelHeight = 0;
				classTextSize.height = 0;
			}
			int actorHeight = bounds.height - labelHeight;
			//keep the aspect ratio, and center the actor.
			classFigure.setBounds(new Rectangle(bounds.x + bounds.width/2 - actorHeight/2, bounds.y, actorHeight, actorHeight));
			classLabel.setBounds(new Rectangle(bounds.x, bounds.y+actorHeight, bounds.width, classTextSize.height));
			stereoTypeLabel.setBounds(new Rectangle(bounds.x, bounds.y+actorHeight+classTextSize.height+5, bounds.width, stereoTextSize.height));
			break;
		default:
			//center the labels inside the box.
			classFigure.setBounds(getBounds().getCopy());
			bounds = classFigure.getClientArea();
			classLabel.setBounds(new Rectangle(bounds.x, bounds.y + bounds.height/2 - labelHeight/2, bounds.width, classTextSize.height));
			stereoTypeLabel.setBounds(new Rectangle(bounds.x, classLabel.getBounds().y + classTextSize.height + 5, bounds.width, stereoTextSize.height));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#paintClientArea(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void paintFigure(Graphics graphics) {
		super.paintFigure(graphics);
		layoutLocalFigures();
		classFigure.paint(graphics);
		classLabel.paint(graphics);
		stereoTypeLabel.paint(graphics);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#paintChildren(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void paintChildren(Graphics graphics) {
		if (getClientArea().isEmpty()) {
			return;
		}
		super.paintChildren(graphics);
	}
	
	private Shape createClassFigure() {
		switch (getType()) {
		case COLLECTION:
			return new CollectionFigure();
		case BOUNDARY:
			return new CircleFigure();
		case CONTROL:
			return new ControlFigure();
		case ENTITY:
			return new EntityFigure();
		case ACTOR:
			return new ActorFigure();
		case PACKAGE:
			return new PackageFigure();
		case DATA_STORE:
			return new CylinderFigure();
		case CLASS:
		default:
			return new RectangleFigure();
				
		}
	}
	
	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}
	
	public void setClassName(String text) {
		classLabel.setText(text);
	}
	public void setStereoType(String text) {
		if (text != null && !"".equals(text))
			stereoTypeLabel.setText("«"+text+"»");
		else 
			stereoTypeLabel.setText("");
	}
	
	/**
	 *Figures cannont be added to this figure. Trying to do so
	 *will have no effect.
	 */
	public final void add(IFigure figure, Object constraint, int index) {
		super.add(figure, constraint, index);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#getClientArea(org.eclipse.draw2d.geometry.Rectangle)
	 */
	@Override
	public Rectangle getClientArea(Rectangle rect) {
		Rectangle bounds = getBounds().getCopy();
		Dimension clientSize = new Dimension();
		Rectangle result = new Rectangle();
		clientSize.width = bounds.width;
		if (getLayoutManager() != null) {
			Dimension preferred = getLayoutManager().getPreferredSize(this, bounds.width, -1);
			clientSize.width = Math.max(bounds.width, preferred.width);
			clientSize.height = preferred.height;
			if (bounds.height - clientSize.height < MINIMUM_SIZE) {
				Dimension minimum = getLayoutManager().getMinimumSize(this, bounds.width, -1);
				if (bounds.height - minimum.height < MINIMUM_SIZE) {
					clientSize.height = 0;
				}
			}
		}
		if (!isCoordinateSystem()) {
			result.setLocation(bounds.getLocation());
		}
		result.setSize(clientSize);
		return result;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#useLocalCoordinates()
	 */
	protected boolean useLocalCoordinates() {
		return false;
	}
	
	public void setLineWidth(int width) {
		classFigure.setLineWidth(width);
		repaint();
	}
	
	public Label getLabel() {
		return classLabel;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#setBounds(org.eclipse.draw2d.geometry.Rectangle)
	 */
	@Override
	public void setBounds(Rectangle rect) {
		// TODO Auto-generated method stub
		super.setBounds(rect);
	}
}
