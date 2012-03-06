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

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.FreeformViewport;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutAnimator;
import org.eclipse.draw2d.ScalableFigure;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.ScrollPane;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.zest.custom.sequence.visuals.LayerConstants;

public class ScrollingFigure extends Figure {
	IFigure pane;
	private ScalableFreeformLayeredPane layers;
	IFigure connections;
	IFigure feedback;
	Viewport viewport;
	ScrollPane scrollpane;
	public ScrollingFigure() {
		scrollpane = new ScrollPane();
		layers = new ScalableFreeformLayeredPane();
		pane = new FreeformLayer();
		connections = new ConnectionLayer();
		feedback = new FreeformLayer();
		feedback.setEnabled(false);
		connections.addLayoutListener(LayoutAnimator.getDefault());
		layers.add(pane, LayerConstants.PRIMARY_LAYER);
		layers.add(connections, LayerConstants.CONNECTION_LAYER);
		layers.add(feedback, LayerConstants.FEEDBACK_LAYER);
		pane.setLayoutManager(new FreeformLayout());
		pane.addLayoutListener(LayoutAnimator.getDefault());
		setLayoutManager(new StackLayout());
		add(scrollpane);
		viewport = new FreeformViewport();
		scrollpane.setViewport(viewport);
		scrollpane.setContents(layers);
		//layers.setLayoutManager(new FreeformLayout());
//		setBackgroundColor(ColorConstants.listBackground);
//		setOpaque(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#getPreferredSize(int, int)
	 */
	@Override
	public Dimension getPreferredSize(int wHint, int hHint) {
		Dimension size = getContentPane().getPreferredSize(wHint, hHint).getCopy();
		return size;
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#setBounds(org.eclipse.draw2d.geometry.Rectangle)
	 */
	@Override
	public void setBounds(Rectangle rect) {
		super.setBounds(rect);
		//getViewport().setSize(rect.getSize());
	}
	
	public IFigure getContentPane() {
		return pane;
	}
	
	public IFigure getConnectionLayer() {
		return connections;
	}
	
	public IFigure getFeedbackLayer() {
		return feedback;
	}
	
	public Viewport getViewport() {
		return viewport;
	}
	
	
	public ScalableFigure getLayers() {
		return layers;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.Figure#getClientArea(org.eclipse.draw2d.geometry.Rectangle)
	 */
	@Override
	public Rectangle getClientArea(Rectangle rect) {
		Rectangle result = super.getClientArea(rect);//getContentPane().getClientArea(rect);
		//translateToAbsolute(result);
		return result;
	}
	
	public ScrollPane getScrollPane() {
		return scrollpane;
	}
	

}