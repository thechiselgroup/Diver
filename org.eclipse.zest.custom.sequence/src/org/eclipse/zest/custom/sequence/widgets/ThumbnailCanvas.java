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
package org.eclipse.zest.custom.sequence.widgets;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.zest.custom.sequence.figures.ScrollableThumbnail;
import org.eclipse.zest.custom.sequence.figures.Thumbnail;

/**
 * A canvas that acts as a thumbnail view for a target figure. NOTE: The <code>Thumbnail</code>
 * figure supplied by Draw2D has a memory leak in it somewhere, which causes crashes when zooming on large
 * charts.  
 * @author Del Myers
 * @see Thumbnail
 */

public class ThumbnailCanvas extends Canvas {

	Thumbnail thumbnail;
	private LightweightSystem system;
	private Control targetControl;
	private DisposeListener disposeHandler;
	
	/**
	 * @param parent
	 * @param style
	 */
	public ThumbnailCanvas(Composite parent, int style) {
		super(parent, style);
		system = new LightweightSystem(this);
		disposeHandler = new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (thumbnail != null) {
					thumbnail.deactivate();
					thumbnail = null;
				}
			}	
		};
		addDisposeListener(disposeHandler);
	}
	
	
	/**
	 * Set the contents of the viewer to the given figure.
	 * @param figure the target figure to thumbnail.
	 * @param figureControl the control that the figure is on.
	 */
	protected void setFigure(IFigure figure, Control figureControl) {
		if (this.thumbnail != null) {
			this.thumbnail.deactivate();
			this.thumbnail = null;
		} if (figureControl != this.targetControl) {
			if (this.targetControl != null) {
				this.targetControl.removeDisposeListener(disposeHandler);
			}
			this.targetControl = figureControl;
			if (this.targetControl != null) {
				this.targetControl.addDisposeListener(disposeHandler);
			}
		}
		this.thumbnail = createThumbnail(figure);
		this.setBackground(figureControl.getBackground());
		system.setContents(thumbnail);
	}
	
	
	/**
	 * Creates the thumnail for this viewer, and sets its source accordingly to the given figure.
	 * @param contents
	 * @return
	 */
	protected Thumbnail createThumbnail(IFigure figure) {
		Thumbnail nail = null;
		if (figure instanceof Viewport) {
			Viewport port = (Viewport) figure;
			nail = new ScrollableThumbnail(port);
			nail.setSource(port.getContents());
		} else {
			nail = new Thumbnail(figure);
		}
		return nail;
	}
	
	public Control getTargetControl() {
		return targetControl;
	}
		

}
