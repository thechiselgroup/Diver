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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.FreeformViewport;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.draw2d.RangeModel;
import org.eclipse.draw2d.UpdateListener;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.zest.custom.sequence.visuals.MessageBasedSequenceVisuals;

/**
 * Creates a control that clones a UMLSequenceChart
 * @author Del Myers
 *
 */
class SequenceClone {
	/**
	 * The canvas that will house the control.
	 */
	private UMLSequenceChart chart;
	private FigureCanvas localSequenceCanvas;
	private FigureCanvas localObjectCanvas;
	private Composite page;
	private PropertyChangeListener lifeLineScroller;
	private PropertyChangeListener verticalScroller;
	private PropertyChangeListener sourceSyncScroller;
	private PropertyChangeListener targetSyncScroller;
	
	private class CloneFigure extends Figure {
		
		private IFigure figureToClone;

		public CloneFigure(IFigure figureToClone) {
			this.figureToClone = figureToClone;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.Figure#paint(org.eclipse.draw2d.Graphics)
		 */
		@Override
		public void paint(Graphics graphics) {
			figureToClone.paint(graphics);
		}
	}
	
	private class UpdateHook implements FigureListener, DisposeListener, UpdateListener {
		
		private IFigure cloneFigure;
		private FigureCanvas cloneCanvas;
		private FigureCanvas clonedCanvas;

		protected UpdateHook(FigureCanvas clonee, FigureCanvas clone, IFigure cloneFigure) {
			this.cloneFigure = cloneFigure;
			this.cloneCanvas = clone;
			this.clonedCanvas = clonee;
			clonedCanvas.getViewport().getContents().addFigureListener(this);
			clonedCanvas.getLightweightSystem().getUpdateManager().addUpdateListener(this);
			
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.FigureListener#figureMoved(org.eclipse.draw2d.IFigure)
		 */
		public void figureMoved(IFigure source) {
			if (cloneFigure.getParent() != null) {
				LayoutManager manager = cloneFigure.getParent().getLayoutManager();
				if (manager instanceof XYLayout) {
					Rectangle bounds = source.getBounds();
					manager.setConstraint(cloneFigure, new Rectangle(0,0, bounds.width, bounds.height));
					cloneFigure.invalidate();
				}
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
		 */
		public void widgetDisposed(DisposeEvent e) {
			if (!clonedCanvas.isDisposed()) {
				clonedCanvas.removeDisposeListener(this);
			}
			if (!cloneCanvas.isDisposed()) {
				cloneCanvas.removeDisposeListener(this);
			}
			clonedCanvas.getLightweightSystem().getUpdateManager().removeUpdateListener(this);
			clonedCanvas.getViewport().getContents().removeFigureListener(this);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.UpdateListener#notifyPainting(org.eclipse.draw2d.geometry.Rectangle, java.util.Map)
		 */
		@SuppressWarnings("unchecked")
		public void notifyPainting(Rectangle damage, Map dirtyRegions) {
			cloneCanvas.getLightweightSystem().getUpdateManager().addDirtyRegion(cloneFigure, cloneFigure.getBounds().getCopy());
			cloneCanvas.getLightweightSystem().getUpdateManager().performUpdate(cloneFigure.getBounds().getCopy());
		}

		/* (non-Javadoc)
		 * @see org.eclipse.draw2d.UpdateListener#notifyValidating()
		 */
		public void notifyValidating() {
			cloneCanvas.getLightweightSystem().getRootFigure().invalidateTree();
			cloneCanvas.getLightweightSystem().getRootFigure().revalidate();
		}
		
	}
	
	private class EventForwarder implements Listener {
		
		private FigureCanvas target;
		private FigureCanvas source;

		protected EventForwarder(FigureCanvas source, FigureCanvas target) {
			this.target = target;
			this.source = source;
			source.addListener(SWT.MouseMove, this);
			source.addListener(SWT.MouseUp, this);
			source.addListener(SWT.MouseDown, this);
			source.addListener(SWT.MouseHover, this);
			source.addListener(SWT.MouseWheel, this);
			source.addListener(SWT.MouseDoubleClick, this);
			source.addListener(SWT.KeyUp, this);
			source.addListener(SWT.KeyDown, this);
			target.addListener(SWT.Dispose, this);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
		 */
		public void handleEvent(Event event) {
			Event e = new Event();
			e.keyCode = event.keyCode;
			e.stateMask = event.stateMask;
			e.text = event.text;
			e.type = event.type;
			e.time = event.time;
			e.widget = chart;
			e.button = event.button;
			e.character = event.character;
			e.count = event.count;
			e.data = event.data;
			e.detail = event.detail;
			e.display = event.display;
			e.doit = event.doit;
			e.end = event.end;
			e.gc = event.gc;
			e.height = event.height;
			e.index = event.index;
			
			switch (event.type) {
			case SWT.Dispose:
				if (!source.isDisposed()) {
					source.removeListener(SWT.MouseMove, this);
					source.removeListener(SWT.MouseUp, this);
					source.removeListener(SWT.MouseDown, this);
					source.removeListener(SWT.MouseHover, this);
					source.removeListener(SWT.MouseWheel, this);
					source.removeListener(SWT.MouseDoubleClick, this);
					source.removeListener(SWT.KeyUp, this);
					source.removeListener(SWT.KeyDown, this);
				}
				break;
			case SWT.MouseMove:
			case SWT.MouseDown:
			case SWT.MouseUp:
			case SWT.MouseWheel:
			case SWT.MouseDoubleClick:
				Point p = translateToTarget(event.x, event.y);
				e.x = p.x;
				e.y = p.y;
			case SWT.KeyDown:
			case SWT.KeyUp:
				target.notifyListeners(e.type, e);
			}
			
		}

		/**
		 * Translates the given point in the local canvas to the same point in the
		 * target canvas.
		 * @param x
		 * @param y
		 * @return
		 */
		private Point translateToTarget(int x, int y) {
			org.eclipse.draw2d.geometry.Point p = new
				org.eclipse.draw2d.geometry.Point(x, y);
			//int horiz = source.getViewport().getHorizontalRangeModel().getValue();
			//int vert = source.getViewport().getHorizontalRangeModel().getValue();
			source.getViewport().getContents().translateToRelative(p);
			target.getViewport().getContents().translateToAbsolute(p);
			//target.getViewport().translateFromParent(p);
			return new Point(p.x, p.y);
		}
		
	}
	/**
	 * Creates a clone of the given sequence chart inside the given parent control.
	 * @param parent the parent to create the clone within.
	 * @param chartToClone the chart to clone
	 */
	public SequenceClone(Composite parent, UMLSequenceChart chartToClone) {
		this.chart = chartToClone;
		page = new Composite(parent, SWT.NONE);
		page.setBackground(chartToClone.getBackground());
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing=0;
		layout.verticalSpacing=0;
		layout.numColumns=1;
		layout.marginHeight=0;
		layout.marginWidth=0;
		page.setLayout(layout);
		FigureCanvas chartSequenceCanvas = (FigureCanvas) chart.getSequenceControl();
		FigureCanvas chartObjectCanvas = (FigureCanvas) chart.getLifelineControl();
		
				
		
		
		//add a canvas to clone the object area.
		localObjectCanvas = new FigureCanvas(page);
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		localObjectCanvas.setLayoutData(gd);
		gd.minimumHeight = MessageBasedSequenceVisuals.OBJECT_HEIGHT;
		gd.heightHint = MessageBasedSequenceVisuals.OBJECT_HEIGHT;
		localObjectCanvas.setLayoutData(gd);
		localObjectCanvas.setViewport(new FreeformViewport());
		FreeformLayer objectContents = new FreeformLayer();
		objectContents.setLayoutManager(new FreeformLayout());
		localObjectCanvas.getViewport().setContents(objectContents);
		CloneFigure objectClone = new CloneFigure(chartObjectCanvas.getViewport().getContents());
		objectContents.add(objectClone);
		localObjectCanvas.setScrollBarVisibility(FigureCanvas.NEVER);
		//add hooks
		localObjectCanvas.getViewport().setContentsTracksHeight(true);
		localObjectCanvas.getViewport().setContentsTracksHeight(true);
		UpdateHook objectHook = new UpdateHook(chartObjectCanvas, localObjectCanvas, objectClone);
		objectHook.figureMoved(chartObjectCanvas.getViewport().getContents());
		new EventForwarder(localObjectCanvas, chartObjectCanvas);
		
		localSequenceCanvas = new FigureCanvas(page);
		localSequenceCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		localSequenceCanvas.setViewport(new FreeformViewport());
		FreeformLayer localContents = new FreeformLayer();
		localSequenceCanvas.getViewport().setContents(localContents);
		localSequenceCanvas.setHorizontalScrollBarVisibility(FigureCanvas.AUTOMATIC);
		localSequenceCanvas.setVerticalScrollBarVisibility(FigureCanvas.NEVER);
		localContents.setLayoutManager(new FreeformLayout());
		
		IFigure sequenceContents = chartSequenceCanvas.getViewport().getContents();
		CloneFigure cloneFigure = new CloneFigure(sequenceContents);
		localContents.add(cloneFigure);
		
		//add hooks
		localSequenceCanvas.getViewport().setContentsTracksHeight(true);
		localSequenceCanvas.getViewport().setContentsTracksWidth(true);
		UpdateHook sequenceUpdateHook = new UpdateHook(chartSequenceCanvas, localSequenceCanvas, cloneFigure);
		sequenceUpdateHook.figureMoved(chartSequenceCanvas.getViewport().getContents());
		new EventForwarder(localSequenceCanvas, chartSequenceCanvas);
		
		hookScrolling();
		page.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				unhookScrolling();
			}
		});
	}
	/**
	 * 
	 */
	protected void unhookScrolling() {
		RangeModel mySeqHorizontalModel = localSequenceCanvas.getViewport().getHorizontalRangeModel();
		RangeModel theirVerticalModel = ((FigureCanvas) chart
				.getSequenceControl()).getViewport().getVerticalRangeModel();
		RangeModel theirHorizontalModel = ((FigureCanvas) chart
				.getSequenceControl()).getViewport().getHorizontalRangeModel();
		mySeqHorizontalModel.removePropertyChangeListener(lifeLineScroller);
		mySeqHorizontalModel.removePropertyChangeListener(targetSyncScroller);
		theirVerticalModel.removePropertyChangeListener(verticalScroller);
		theirHorizontalModel.removePropertyChangeListener(sourceSyncScroller);
	}
	/**
	 * 
	 */
	private void hookScrolling() {
		final RangeModel mySeqHorizontalModel = localSequenceCanvas.getViewport().getHorizontalRangeModel();
		final RangeModel mySeqVerticalModel = localSequenceCanvas.getViewport().getVerticalRangeModel();
		final RangeModel myObjHorizontalModel = localObjectCanvas.getViewport().getHorizontalRangeModel();
		//first, hook the whole cloned figure to make sure that the object canvas and
		//sequence canvas are in sync.
		lifeLineScroller = new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				if (myObjHorizontalModel.getValue() != mySeqHorizontalModel.getValue()) {
					myObjHorizontalModel.setValue(mySeqHorizontalModel.getValue());
				}
			}
		};
		mySeqHorizontalModel.addPropertyChangeListener(lifeLineScroller);
		final RangeModel theirVerticalModel = ((FigureCanvas) chart
				.getSequenceControl()).getViewport().getVerticalRangeModel();
		final RangeModel theirHorizontalModel = ((FigureCanvas) chart
				.getSequenceControl()).getViewport().getHorizontalRangeModel();
		
		verticalScroller = new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				int myv = mySeqVerticalModel.getValue();
				int theirv = theirVerticalModel.getValue();
				if (myv != theirv) {
					mySeqVerticalModel.setValue(theirv);
				}
			}
		};
		theirVerticalModel.addPropertyChangeListener(verticalScroller);
		
		targetSyncScroller = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				int myv = mySeqHorizontalModel.getValue();
				int myex = mySeqHorizontalModel.getExtent();
				
				int theirv = theirHorizontalModel.getValue();
//				int theirex = ((FigureCanvas) chart
//						.getSequenceControl()).getViewport().getSize().width;
				//int theirex = theirHorizontalModel.getExtent();
				int newv = theirv-myex;
				if (theirv < myv+myex && newv < myv) {
					if (newv < 0) {
						newv= 0;
					}
					mySeqHorizontalModel.setValue(newv);
				}
			}
		};
		
		sourceSyncScroller = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				int myv = mySeqHorizontalModel.getValue();
				//int mymax = mySeqHorizontalModel.getMaximum();
				
				int theirv = theirHorizontalModel.getValue();
//				int theirex = ((FigureCanvas) chart
//						.getSequenceControl()).getViewport().getSize().width;
				int theirmax = theirHorizontalModel.getMaximum();
				int myex = mySeqHorizontalModel.getExtent();
				int newv = myv + myex;
				if (theirv < newv) {
					if (myv == 0) {
						newv = theirv;
					} else if (newv > theirmax) {
						newv = theirmax;
					}
					theirHorizontalModel.setValue(newv);
				}
			}
		};
		theirHorizontalModel.addPropertyChangeListener(targetSyncScroller);
		mySeqHorizontalModel.addPropertyChangeListener(sourceSyncScroller);
	}
	/**
	 * 
	 */
	public Control getControl() {
		return page;
	}
	
	public void dispose() {
		page.dispose();
	}
	/**
	 * @return
	 */
	public boolean isDisposed() {
		return page == null || page.isDisposed();
	}
	/**
	 * @param color
	 */
	public void setBackground(Color color) {
		page.setBackground(color);
		localObjectCanvas.setBackground(color);
		localSequenceCanvas.setBackground(color);
	}

}
