/*******************************************************************************
 * Copyright 2005-2006, CHISEL Group, University of Victoria, Victoria, BC, Canada.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Chisel Group, University of Victoria
 *******************************************************************************/
package org.eclipse.zest.custom.sequence.widgets.internal;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.zest.custom.sequence.widgets.UMLChart;
import org.eclipse.zest.custom.sequence.widgets.UMLSequenceChart;

/**
 * @author Del Myers
 *
 */
public class SequenceDiagramImageAdapter implements IUMLImageAdapter {

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.internal.IUMLImageAdapter#getImage(org.eclipse.zest.custom.sequence.widgets.UMLChart)
	 */
	public Image getImage(UMLChart chart) {
		if (!(chart instanceof UMLSequenceChart)) {
			return null;
		}
		
		UMLSequenceChart sd = (UMLSequenceChart) chart;
		FigureCanvas llC = (FigureCanvas) sd.getLifelineControl();
		FigureCanvas sC = (FigureCanvas) sd.getSequenceControl();
		
		Rectangle bounds = new Rectangle(0,0, sC.getContents().getClientArea().width,
			llC.getContents().getClientArea().height + sC.getContents().getClientArea().height);
		Image image = new Image(chart.getDisplay(), bounds);
		GC gc = new GC(image);
		Graphics g = new SWTGraphics(gc);
		llC.getContents().paint(g);
		g.translate(0, llC.getContents().getClientArea().height);
		sC.getContents().paint(g);
		gc.dispose();
		return image;
	}

}
