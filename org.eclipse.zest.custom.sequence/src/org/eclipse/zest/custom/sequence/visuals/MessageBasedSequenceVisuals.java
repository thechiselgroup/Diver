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

import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.LayoutAnimator;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.UpdateListener;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.zest.custom.sequence.figures.RectangleZoomManager;
import org.eclipse.zest.custom.sequence.figures.internal.AntialiasingScalableFreeformLayeredPane;
import org.eclipse.zest.custom.sequence.figures.internal.AntialiasingScalableLayeredPane;
import org.eclipse.zest.custom.sequence.figures.internal.QuickClearFreeformLayer2;
import org.eclipse.zest.custom.sequence.internal.AbstractSimpleProgressRunnable;
import org.eclipse.zest.custom.sequence.internal.SimpleProgressMonitor;
import org.eclipse.zest.custom.sequence.internal.UIJobProcessor;
import org.eclipse.zest.custom.sequence.tools.IWidgetFinder;
import org.eclipse.zest.custom.sequence.widgets.Activation;
import org.eclipse.zest.custom.sequence.widgets.Lifeline;
import org.eclipse.zest.custom.sequence.widgets.Message;
import org.eclipse.zest.custom.sequence.widgets.MessageGroup;
import org.eclipse.zest.custom.sequence.widgets.PropertyChangeListener;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;
import org.eclipse.zest.custom.sequence.widgets.UMLSequenceChart;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * A new approach to drawing the sequence chart. These visuals try and listen
 * only for events that cause an item in the sequence chart to be dirty. These
 * will include such things as when items are created, destroyed, or
 * reconnected. These visuals no longer use a clone pain or a package pane.
 * Instead, it will expose functionality to make for easier linking between
 * viewers. The package pane will have to be reimplemented as a new viewer.
 * 
 * @author Del Myers
 * 
 */
public class MessageBasedSequenceVisuals implements PropertyChangeListener, DisposeListener, IWidgetFinder {

	/**
	 * The data key used to access the visual part for an item.
	 */
	public static final String VISUAL_KEY = "visuals";
	
	/**
	 * A marker for activations to indicate that they should not be visited again during refresh.
	 */
	private static final String VISIT_MARKER = "rfshVisit";

	public static final int OBJECT_HEIGHT = 60;

	/**
	 * The sequence chart that these visuals work for.
	 */
	private UMLSequenceChart chart;

	private HashMap<IFigure, UMLItem> figureItemMap;

	//private FreeformLayer objectContents;

	private AntialiasingScalableFreeformLayeredPane primaryPane;
	
	private UMLItem[] currentSelection;
	
	private ArrayList<WidgetVisualPart> visuals;
	
	private final Object REFRESH_FAMILY = new Object();
	private final Object LAYOUT_FAMILY;

	private AntialiasingScalableLayeredPane objectLayers;

	private SelectionDecorationLayer selectionDecorator;

	private AntialiasingScalableLayeredPane objectGroupLayers;

	public boolean refreshing;
	
		
	private class RefreshWithProgressRunnable extends AbstractSimpleProgressRunnable {
		/**
		 * Used to make sure that the when we are processing messages to activations that have already been
		 * visited, that they are reachable on the current call stack. Otherwise, the message is invalid.
		 */
		private HashSet<Activation> callStack;
		//private TreeSet<UMLItem> visited;
		/* (non-Javadoc)
		 * @see org.eclipse.zest.custom.sequence.internal.AbstractSimpleProgressRunnable#doRunInUIThread(org.eclipse.zest.custom.sequence.internal.SimpleProgressMonitor)
		 */
		@Override
		protected void doRunInUIThread(SimpleProgressMonitor monitor)
				throws InvocationTargetException {
			//There are several things that have to be done:
			//1: keep selection
			//2: for each dirty item, reconnect it
			//3: on a concurrent modification exception, quit and let the next runnable run the change.
			//we must synchronize this whole process on the dirty items
			chart.getSequenceControl().setCursor(chart.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
			callStack = new HashSet<Activation>();
			MessageBasedSequenceVisuals.this.refreshing = true;
			//long time = System.currentTimeMillis();
			monitor.beginTask("Refreshing Chart", 5001);
			//update selection
			ArrayList<UMLItem> newSelection = new ArrayList<UMLItem>();
			for (UMLItem item : currentSelection) {
				if (!item.isDisposed()) {
					newSelection.add(item);
				}
			}
			monitor.worked(1);
			readAndDispatch();
			try {
				updateChart(monitor);
				selectionDecorator.refresh();
			} catch (ConcurrentModificationException e) {
				callStack.clear();
				monitor.done();
				//do nothing, let the next run complete the update.
			}
			MessageBasedSequenceVisuals.this.refreshing = false;
			//System.err.println(System.currentTimeMillis() - time);
			chart.getSequenceControl().setCursor(null);
			
		}

		/**
		 * @param monitor
		 */
		private void updateChart(SimpleProgressMonitor monitor) {
			//mark all the current visuals as "trashed" so that we can remove them later.
			ArrayList<WidgetVisualPart> newVisuals = new ArrayList<WidgetVisualPart>();
			HashSet<WidgetVisualPart> trash = new HashSet<WidgetVisualPart>();
			try {
				trash.addAll(visuals);
				Activation root = chart.getRootActivation();

				visitActivation(root, newVisuals, monitor);
				//activate all the new parts
				LinkedList<ConnectionVisualPart> connections = new LinkedList<ConnectionVisualPart>();
				//disconnect the panes from their parents so that an update only has to be done once.
				disconnectPanes();
				for (WidgetVisualPart v : newVisuals) {
					if (v instanceof ConnectionVisualPart) {
						//add to a list of connections so that we can re-connect after the endpoints are
						//activated.
						connections.add((ConnectionVisualPart) v);
					} else {
						if (!v.isActive()) {
							v.activate();
						}
						v.installFigures();
						v.refreshVisuals();
					}
					trash.remove(v);
					unvisit(v.getWidget());
				}
				while (connections.size() > 0) {
					ConnectionVisualPart v = connections.removeFirst();
					UMLItem widget = v.getWidget();
					if (widget instanceof Message) {
						Message m = (Message) widget;
						NodeVisualPart oldSourceVisual = v.getSource();
						NodeVisualPart sourceVisual = (NodeVisualPart) m.getSource().getData(VISUAL_KEY);
						NodeVisualPart oldTargetVisual = v.getTarget();
						NodeVisualPart targetVisual = (NodeVisualPart) m.getTarget().getData(VISUAL_KEY);
						if ((oldSourceVisual == null) || (sourceVisual.getWidget() != oldSourceVisual.getWidget())) {
							if (v.isActive()) {
								v.deactivate();		
							}
							if (oldSourceVisual != null) {
								oldSourceVisual.removeSourceConnection(v);
							}
							sourceVisual.addSourceConnection(v);
						}
						if ((oldTargetVisual == null) || (targetVisual.getWidget() != oldTargetVisual.getWidget())) {
							if (v.isActive()) {
								v.deactivate();		
							}
							if (oldTargetVisual != null) {
								oldTargetVisual.removeSourceConnection(v);
							}
							if (targetVisual != null) {
								targetVisual.addTargetConnection(v);
							}
						}
						if (!v.isActive()) {
							v.activate();
						}
						v.installFigures();
						v.refreshVisuals();
					}
					
				}
				//delete the trash
				for (WidgetVisualPart v : visuals) {
					if (!v.getWidget().isDisposed()) {
						unvisit(v.getWidget());
						if (trash.contains(v)) {
							v.deactivate();
						}
					} else {
						v.deactivate();
					}
				}
			} finally {
				for (WidgetVisualPart v : newVisuals) {
					unvisit(v.getWidget());
				}
				reconnectPanes();
			}
			visuals = newVisuals;
			
		}

		/**
		 * Visits the given activation, updating its corresponding visuals.
		 * @param root
		 * @param newVisuals
		 */
		private void visitActivation(Activation activation,
				ArrayList<WidgetVisualPart> newVisuals, SimpleProgressMonitor monitor) {
			//we aren't dealing with a simple tree anymore, so we have mark each activation as
			//visited, in order to avoid cycles.
			if (activation.isDisposed()) {
				return;
			}
			if (visited(activation)) {
				return;
			}
			setVisited(activation);
			addToCallStack(activation);
			//check the lifeline to make sure that there is a visual part for it.
			Lifeline activationLifeline = activation.getVisibleLifeline();
			visitLifeline(activationLifeline, newVisuals);
			
			//add a visual for this activation.
			ActivationVisual aVisual = (ActivationVisual) activation.getData(VISUAL_KEY);
			if (aVisual == null) {
				aVisual = new ActivationVisual(activation, VISUAL_KEY);
			}
			newVisuals.add(aVisual);
			Message[] messages = activation.getMessages();
			MessageGroup[] groups = activation.getMessageGroups();
			int groupIndex = 0;
			for (int i = 0; i < messages.length; i++) {
				Message m = messages[i];
				boolean groupClosed = false;
				int end = i;
				while (groupIndex < groups.length && groups[groupIndex].getOffset() <= end) {
					MessageGroup group = groups[groupIndex];
					if (!groupClosed && activation.isExpanded() && group.getLength() >= 0) {
						MessageGroupVisual gv = (MessageGroupVisual) group.getData(VISUAL_KEY);
						if (gv == null) {
							gv = new MessageGroupVisual(group, VISUAL_KEY);
						}
						newVisuals.add(gv);
					}
					if (!activation.isExpanded() || (!group.isExpanded())) {
						if (group.getLength() > 0) {
							groupClosed = true;
							//set the next index forward, so that everything else gets hidden.
							i = group.getOffset() + group.getLength() - 1;
							end = i;
						}
					}
					groupIndex++;
				}
				if (groupClosed) {
					//don't process any more messages inside the group.
					continue;
				}
				monitor.setSubTask(m.getText());
				Activation target = m.getTarget();
				if (target != null && target != activation && !m.isHidden()) {
					if (!visited(target)) {
						if (target.getSourceCall() != m) {
							//the target isn't reachable, just continue.
							MessageVisual mVisual = (MessageVisual) m.getData(VISUAL_KEY);
							if (mVisual == null) {
								mVisual = new MessageVisual(m, VISUAL_KEY);
							}
							newVisuals.add(mVisual);
							continue;
						}
					}
					if (activation.isExpanded()) {
						//always show all messages if the activation is expanded.
						MessageVisual mVisual = (MessageVisual) m.getData(VISUAL_KEY);
						if (mVisual == null) {
							mVisual = new MessageVisual(m, VISUAL_KEY);
						}
						newVisuals.add(mVisual);
						visitActivation(target, newVisuals, monitor);
					} else if (visited(target)) {
						if (!isOnCallStack(target)) {
							//this is a malformed sequence diagram. Just ignore the message.
							//@tag sequencediagram.revisit: Maybe we should throw an exception?
							continue;
						}
						//this message is a return of some sort, so we have to display it.
						//@tag sequencediagram.revisit : maybe just don't show the return if the activation is collapsed
						//we certainly shouldn't show the message groups.
						MessageVisual mVisual = (MessageVisual) m.getData(VISUAL_KEY);
						if (mVisual == null) {
							mVisual = new MessageVisual(m, VISUAL_KEY);
						}
						newVisuals.add(mVisual);
					}
				}
			}
			removeFromCallStack(activation);
			if (monitor.isCancelled()) {
				throw new ConcurrentModificationException();
			}
			readAndDispatch();
		}
		
		/**
		 * Visits the given lifeline to create all of the visuals for it.
		 * @param activationLifeline
		 * @param newVisuals
		 */
		private void visitLifeline(Lifeline activationLifeline,
				ArrayList<WidgetVisualPart> newVisuals) {
			if (activationLifeline == null || visited(activationLifeline)) {
				return;
			}
			setVisited(activationLifeline);
			WidgetVisualPart o = (WidgetVisualPart) activationLifeline.getData(VISUAL_KEY);
			LifelineVisual llVisual = null;
			if (!(o instanceof LifelineVisual)) {
				if (o != null) {
					o.deactivate();
				}
				llVisual = new LifelineVisual(activationLifeline, VISUAL_KEY);
				llVisual.activate();
			} else {
				llVisual = (LifelineVisual) o;
			}
			newVisuals.add(llVisual);
			
			//walk up the hierarchy, adding all the parents. 
			Lifeline parent = activationLifeline.getParent();
			while (parent != null) {
				if (visited(parent)) {
					break;
				}
				o = (WidgetVisualPart) parent.getData(VISUAL_KEY);
				if (!(o instanceof LifelineGroupVisual)) {
					if (o != null) {
						o.deactivate();
					}
					o = new LifelineGroupVisual(parent, VISUAL_KEY);
					o.activate();
				}
				newVisuals.add(o);
				setVisited(parent);
				parent = parent.getParent();
			}
		}

		private void addToCallStack(Activation a) {
			callStack.add(a);
		}
		
		private boolean isOnCallStack(Activation a) {
			return callStack.contains(a);
		}
		
		private void removeFromCallStack(Activation a) {
			callStack.remove(a);
		}
		
		private void setVisited(Widget a) {
			a.setData(VISIT_MARKER, Boolean.TRUE);
		}
		
		private boolean visited(Widget a) {
			return a.getData(VISIT_MARKER) != null;
		}
		
		private void unvisit(Widget a) {
			a.setData(VISIT_MARKER, null);
		}
		
	}

	public MessageBasedSequenceVisuals(UMLSequenceChart chart) {
		this.chart = chart;
		chart.addPropertyChangeListener(this);
		chart.addDisposeListener(this);
		figureItemMap = new HashMap<IFigure, UMLItem>();
		currentSelection = new UMLItem[0];
		visuals = new ArrayList<WidgetVisualPart>();
		this.LAYOUT_FAMILY = SequenceLayoutRunnable.getFamily(chart);
	}

	/**
	 * 
	 */
	protected void reconnectPanes() {
		FigureCanvas groupCanvas = (FigureCanvas)chart.getLifelineGroupControl();
		FigureCanvas sequenceCanvas = (FigureCanvas)chart.getSequenceControl();
		FigureCanvas objectCanvas = (FigureCanvas)chart.getLifelineControl();
		groupCanvas.getViewport().setContents(objectGroupLayers);
		sequenceCanvas.getViewport().setContents(primaryPane);
		objectCanvas.getViewport().setContents(objectLayers);
	}

	/**
	 * 
	 */
	protected void disconnectPanes() {
		
		quickClear(primaryPane);
		quickClear(objectLayers);
		quickClear(objectGroupLayers);
		FigureCanvas groupCanvas = (FigureCanvas)chart.getLifelineGroupControl();
		FigureCanvas sequenceCanvas = (FigureCanvas)chart.getSequenceControl();
		FigureCanvas objectCanvas = (FigureCanvas)chart.getLifelineControl();
		
		groupCanvas.getViewport().setContents(null);
		sequenceCanvas.getViewport().setContents(null);
		objectCanvas.getViewport().setContents(null);
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.zest.custom.sequence.widgets.PropertyChangeListener#propertyChanged(java.lang.Object,
	 *      java.lang.String, java.lang.Object, java.lang.Object)
	 */
	public void propertyChanged(Object source, String property,
			Object oldValue, Object newValue) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
	 */
	public void widgetDisposed(DisposeEvent e) {
		primaryPane.removeAll();
		objectLayers.removeAll();
		visuals.clear();
		currentSelection = null;
		figureItemMap.clear();
	}
	

	/**
	 * Rebuilds the panels used in these visuals.
	 */
	public void createFigures() {
		objectGroupLayers = new AntialiasingScalableLayeredPane(){
			/* (non-Javadoc)
			 * @see org.eclipse.draw2d.Figure#setBounds(org.eclipse.draw2d.geometry.Rectangle)
			 */
			@Override
			public void setBounds(Rectangle rect) {
				// TODO Auto-generated method stub
				super.setBounds(rect);
			}
		};
		objectGroupLayers.setFont(chart.getFont());
		objectGroupLayers.setAntialiasing(SWT.ON);
		objectGroupLayers.addFigureListener(new FigureListener(){

			public void figureMoved(IFigure source) {
				source.getBounds();
			}
			
		});
		objectGroupLayers.setLayoutManager(new StackLayout(){
			/* (non-Javadoc)
			 * @see org.eclipse.draw2d.StackLayout#calculatePreferredSize(org.eclipse.draw2d.IFigure, int, int)
			 */
			@Override
			protected Dimension calculatePreferredSize(IFigure figure,
					int wHint, int hHint) {
				org.eclipse.swt.graphics.Point s =
					getChart().getLifelineGroupControl().getSize();
				Dimension preferred = super.calculatePreferredSize(figure, wHint, hHint);
				return new Dimension(Math.max(s.x, preferred.width), Math.max(s.y, preferred.height));
			}
			/* (non-Javadoc)
			 * @see org.eclipse.draw2d.StackLayout#calculateMinimumSize(org.eclipse.draw2d.IFigure, int, int)
			 */
			@Override
			protected Dimension calculateMinimumSize(IFigure figure, int hint,
					int hint2) {
				org.eclipse.swt.graphics.Point s =
					getChart().getLifelineGroupControl().getSize();
				return new Dimension(s.x, s.y);
			}
		});
		Layer objectGroupContents = new FreeformLayer();
		objectGroupContents.setFont(chart.getFont());
		objectGroupContents.setLayoutManager(new ContainmentTreeLayout());
		objectGroupLayers.add(objectGroupContents, LayerConstants.OBJECT_GROUP_LAYER);
		Layer objectGroupConnections = new ConnectionLayer();
		objectGroupConnections.setEnabled(false);
		objectGroupLayers.add(objectGroupConnections, LayerConstants.OBJECT_GROUP_CONNECTION_LAYER);
		
		final Layer objectContents = new Layer() {
			/* (non-Javadoc)
			 * @see org.eclipse.draw2d.Layer#containsPoint(int, int)
			 */
			@Override
			public boolean containsPoint(int x, int y) {
				//don't want this layer to be transparent
				return getBounds().contains(x, y);
			}
		};
		objectContents.setFont(chart.getFont());
		objectContents.setLayoutManager(new XYLayout(){
			@Override
			protected Dimension calculatePreferredSize(IFigure figure,
					int wHint, int hHint) {
				Dimension size = super.calculatePreferredSize(figure, wHint, hHint);
				if (primaryPane != null) {
					size.width = primaryPane.getSize().width;
					FigureCanvas sequenceCanvas = (FigureCanvas)chart.getSequenceControl();
					if (sequenceCanvas.getVerticalBar() != null && sequenceCanvas.getVerticalBar().isVisible()) {
						size.width += sequenceCanvas.getVerticalBar().getSize().x;
					}
				}
				//return super.calculatePreferredSize(figure, hint, hint2);
				return size;
			}
		});
		objectLayers = new AntialiasingScalableLayeredPane();
		objectLayers.setFont(chart.getFont());
		objectLayers.setLayoutManager(new StackLayout(){
			/* (non-Javadoc)
			 * @see org.eclipse.draw2d.StackLayout#calculatePreferredSize(org.eclipse.draw2d.IFigure, int, int)
			 */
			@Override
			protected Dimension calculatePreferredSize(IFigure figure,
					int hint, int hint2) {
				//use the actual size of the children, rather than their 
				//preferred size. This is a little bit of a hack.
				List<?> children = figure.getChildren();
				Dimension size = new Dimension();
				for (Iterator<?> i = children.iterator(); i.hasNext();) {
					IFigure child = (IFigure)i.next();
					size.union(child.getSize());
				}
				if (primaryPane != null) {
					size.width = primaryPane.getSize().width;
					FigureCanvas sequenceCanvas = (FigureCanvas)chart.getSequenceControl();
					if (sequenceCanvas.getVerticalBar() != null && sequenceCanvas.getVerticalBar().isVisible()) {
						size.width += sequenceCanvas.getVerticalBar().getSize().x;
					}
				}
				//return super.calculatePreferredSize(figure, hint, hint2);
				return size;
			}
		});
		objectLayers.setAntialiasing(SWT.ON);
		objectLayers.add(objectContents, LayerConstants.OBJECT_LAYER);
		//objectContents.addLayoutListener(LayoutAnimator.getDefault());
		//objectContents.setLayoutManager(new FreeformLayout());
		
		this.primaryPane = new AntialiasingScalableFreeformLayeredPane();
		primaryPane.setFont(chart.getFont());
		primaryPane.setAntialiasing(SWT.ON);
		//primaryPane.setLayoutManager(new StackLayout());
		createPrimaryLayers();
		primaryPane.addFigureListener(new FigureListener(){
			public void figureMoved(IFigure source) {
				//make sure that the object layers have the same width
				FigureCanvas sequenceCanvas = (FigureCanvas)chart.getSequenceControl();
				Dimension d = new Dimension(source.getSize().width, OBJECT_HEIGHT);
				if (sequenceCanvas.getVerticalBar() != null && sequenceCanvas.getVerticalBar().isVisible()) {
					d.width += sequenceCanvas.getVerticalBar().getSize().x;
				}
				objectContents.setSize(d);
			}
		});
		
		selectionDecorator = new SelectionDecorationLayer(this);
		primaryPane.add(selectionDecorator.getLayer(), selectionDecorator.getLayerKey());
		FigureCanvas groupCanvas = (FigureCanvas)chart.getLifelineGroupControl();
		groupCanvas.getViewport().setContents(objectGroupLayers);
		FigureCanvas sequenceCanvas = (FigureCanvas)chart.getSequenceControl();
		FigureCanvas objectCanvas = (FigureCanvas)chart.getLifelineControl();
		sequenceCanvas.getViewport().setContents(primaryPane);
		objectCanvas.getViewport().setContents(objectLayers);
		
		sequenceCanvas.getViewport().getHorizontalRangeModel().addPropertyChangeListener(
				new java.beans.PropertyChangeListener(){
					public void propertyChange(PropertyChangeEvent evt) {
						FigureCanvas sequenceCanvas = (FigureCanvas)chart.getSequenceControl();
						FigureCanvas objectCanvas = (FigureCanvas)chart.getLifelineControl();
						int newValue = sequenceCanvas.getViewport().getHorizontalRangeModel().getValue();
						objectCanvas.getViewport().getHorizontalRangeModel().setValue(newValue);
						objectCanvas.getViewport().getContents().repaint();
					}
				}
			);
		sequenceCanvas.getLightweightSystem().getUpdateManager().addUpdateListener(new UpdateListener(){
			@SuppressWarnings("unchecked")
			public void notifyPainting(Rectangle damage, Map dirtyRegions) {
			}
			public void notifyValidating() {
				FigureCanvas objectCanvas = (FigureCanvas)chart.getLifelineControl();
				objectCanvas.getLightweightSystem().getRootFigure().invalidateTree();
				objectCanvas.getLightweightSystem().getRootFigure().revalidate();
			}
		});
		RectangleZoomManager zoomManager = new RectangleZoomManager(primaryPane, sequenceCanvas.getViewport());
		chart.setData("ZoomManager", zoomManager);
		return;
	}
	
	
	private void createPrimaryLayers() {
		IFigure l = newLayer();
		l.setLayoutManager(new FreeformLayout());
		l.addLayoutListener(LayoutAnimator.getDefault());
		primaryPane.add(l, LayerConstants.BACKGROUND_LAYER);
		l = newLayer();
		l.setLayoutManager(new FreeformLayout());
		l.addLayoutListener(LayoutAnimator.getDefault());
		primaryPane.add(l, LayerConstants.LIFELINE_LAYER);
		l = newLayer();
		l.setLayoutManager(new FreeformLayout());
		l.addLayoutListener(LayoutAnimator.getDefault());
		primaryPane.add(l, LayerConstants.PRIMARY_LAYER);
		l = newLayer();
		l.setLayoutManager(new FreeformLayout());
		l.addLayoutListener(LayoutAnimator.getDefault());
		primaryPane.add(l, LayerConstants.CONNECTION_LAYER);
		l = newLayer();
		l.setLayoutManager(new FreeformLayout());
		l.addLayoutListener(LayoutAnimator.getDefault());
		primaryPane.add(l, LayerConstants.FEEDBACK_LAYER);
		l = newLayer();
		l.setLayoutManager(new FreeformLayout());
		l.addLayoutListener(LayoutAnimator.getDefault());
		primaryPane.add(l, LayerConstants.ACTIVE_FEEDBACK_LAYER);
	}
	
	private IFigure newLayer() {
		return new QuickClearFreeformLayer2();
		//return new FreeformLayer();
	}
	
	/**
	 * Quickly clears all of the children on the given parent.
	 * @param parent
	 */
	protected void quickClear(IFigure parent) {
		for (Object child : parent.getChildren()) {
			((Figure)child).removeAll();
		}
	}

	@SuppressWarnings("unchecked")
	protected void register(WidgetVisualPart part) {
		LinkedList stack = new LinkedList();
		stack.addAll(part.getFigures());
		while (stack.size() > 0) {
			IFigure fig = (IFigure) stack.removeFirst();
			figureItemMap.put(fig, part.getWidget());
			stack.addAll(fig.getChildren());
		}
	}

	@SuppressWarnings("unchecked")
	protected void deregister(WidgetVisualPart part) {
		LinkedList stack = new LinkedList();		
		stack.addAll(part.getFigures());
		while (stack.size() > 0) {
			IFigure fig = (IFigure) stack.removeFirst();
			figureItemMap.remove(fig);
			stack.addAll(fig.getChildren());
		}
	}
	
	/**
	 * An item is visible only if it has an active visual part.
	 * @param item
	 * @return
	 */
	public boolean isVisible(UMLItem item) {
		WidgetVisualPart part = (WidgetVisualPart) item.getData(VISUAL_KEY);
		return (part != null && part.isActive());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.tools.IWidgetFinder#getWidget(org.eclipse.draw2d.IFigure)
	 */
	public Widget getWidget(IFigure figure) {
		return figureItemMap.get(figure);
	}
	
	public void setSelection(UMLItem[] selection) {
		HashSet<UMLItem> newSelectionSet = new HashSet<UMLItem>(Arrays.asList(selection));
		if (currentSelection != null) {
			for (UMLItem unselected : currentSelection) {
				if (!newSelectionSet.contains(unselected)) {
					//unpin it.
					pin(unselected, false);
				}
			}
		}
		if (selection != null) {
			for (UMLItem selected : selection) {
				pin(selected, true);
			}
		}
		currentSelection = selection;
	}

	/**
	 * Sets the "pinned" state of the given item to the passed state. In the case
	 * of activations, all of the sub messages, and sub-activations are also set. 
	 * @param selected
	 * @param pinned
	 */
	private void pin(UMLItem selected, boolean pinned) {
		if (pinned == isPinned(selected)) {
			return;
		}
		selected.setData("pin", ((pinned) ? true : null));
//		if (selected instanceof Activation) {
//			for (Message m : ((Activation)selected).getMessages()) {
//				if (m.getSource() == selected) {
//					pin(m, pinned);
//				}
//			}
//		} if (selected instanceof Message) {
//			Message m = (Message) selected;
//			if (m instanceof Call) {
//				//pin the activations on the "creator" messages.
//				pin(m.getTarget(), pinned);
//			}
//		}
	}
	
	private boolean isPinned(UMLItem item) {
		return Boolean.TRUE.equals(item.getData("pin"));
	}
	
		
	/**
	 * Returns the display location of the given item in the visuals, either in the 
	 * package pane or in the main pain.
	 * @param item
	 * @return
	 */
	public Rectangle getLocation(UMLItem item) {
		if (item == null || item.isDisposed()) {
			return null;
		}
		WidgetVisualPart visual = (WidgetVisualPart) item.getData(VISUAL_KEY);
		if (visual == null) return null;
		Rectangle p = visual.getFigure().getBounds().getCopy();
//		visual.getFigure().translateFromParent(p);
//		visual.getFigure().translateToAbsolute(p);
//		((FigureCanvas)chart.getSequenceControl()).getViewport();
		return p;
	}

	/**
	 * @return
	 */
	public Rectangle getSequencePanelArea() {
		return primaryPane.getClientArea();
	}

	/**
	 * @param progressService
	 */
	public void scheduleLayout(UIJobProcessor processor) {
		processor.cancelJobsInFamily(LAYOUT_FAMILY);
		processor.runInUIThread(new SequenceLayoutRunnable(chart), chart.getDisplay(), true);
	}

	/**
	 * @param progressService
	 */
	public void scheduleRefresh(UIJobProcessor processor) {
		processor.cancelJobsInFamily(REFRESH_FAMILY);
		processor.cancelJobsInFamily(LAYOUT_FAMILY);
		processor.runInUIThread(new RefreshWithProgressRunnable(), chart.getDisplay(), false);
		processor.runInUIThread(new SequenceLayoutRunnable(chart), chart.getDisplay(), true);
	}
	
	
	IFigure getLayer(Object key) {
		IFigure layer = ((LayeredPane)primaryPane).getLayer(key);
		if (layer == null) {
			layer = objectLayers.getLayer(key);
		} if (layer == null) {
			layer = objectGroupLayers.getLayer(key);
		}
		return layer;
	}

	/**
	 * returns the chart that these visuals are working on.
	 */
	public UMLSequenceChart getChart() {
		return chart;		
	}

	/**
	 * Returns the primary figure for the given widget.
	 * @param the item to get the figure for.
	 */
	public IFigure getFigure(UMLItem item) {
		if (item == null || item.isDisposed()) {
			return null;
		}
		WidgetVisualPart part = (WidgetVisualPart) item.getData(VISUAL_KEY);
		if (part != null) {
			return part.getFigure();
		}
		return null;
	}
	
	/**
	 * Returns the lifelines that are currently visible, in the order that they appear.
	 * @return
	 */
	public Lifeline[] getVisibleLifelines() {
		TreeSet<Lifeline> lines = new TreeSet<Lifeline>(new Comparator<Lifeline>(){

			public int compare(Lifeline o1, Lifeline o2) {
				Rectangle layout1 = (Rectangle) o1.getData(IWidgetProperties.LAYOUT);
				Rectangle layout2 = (Rectangle) o2.getData(IWidgetProperties.LAYOUT);
				if (layout1 == null && layout2 == null) {
					return 0;
				} else if (layout1 == null) {
					return -1;
				} else if (layout2 == null) {
					return 1;
				}
				return layout1.x - layout2.x;
			}
			
		});
		IFigure layer = getLayer(LayerConstants.OBJECT_LAYER);
		for (Object o : layer.getChildren()) {
			IFigure child = (IFigure) o;
			Widget w = getWidget(child);
			if (w instanceof Lifeline) {
				lines.add((Lifeline) w);
			}
		}
		return lines.toArray(new Lifeline[lines.size()]);
	}

	/**
	 * @param parent
	 */
	public WidgetVisualPart getVisualPart(UMLItem item) {
		return (WidgetVisualPart) item.getData(VISUAL_KEY);
	}

	/**
	 * Checks where the given item is displayed, and returns its bounds relative to the
	 * chart's composite.
	 * @param item
	 * @return
	 */
	public Rectangle getRelativeLocation(UMLItem item) {
		FigureCanvas canvas = (FigureCanvas) getContainingComposite(item);
		if (canvas == null) {
			return null;
		}
		IFigure figure = getFigure(item);
		if (figure.getParent() == null) {
			return null;
		}
		Rectangle relative = figure.getBounds().getCopy();
		figure.getParent().translateToAbsolute(relative);
		Point p = canvas.toDisplay(relative.x, relative.y);
		p = getChart().toControl(p);
		relative.setLocation(p.x, p.y);
		return relative;
	}
	
	/**
	 * Returns the low level composite that the given item is contained within.
	 * @param item
	 * @return
	 */
	public Control getContainingComposite(UMLItem item) {
		if (!isVisible(item)) {
			return null;
		}
		if (item instanceof Lifeline) {
			WidgetVisualPart part = getVisualPart(item);
			if (part instanceof LifelineGroupVisual) {
				return getChart().getLifelineGroupControl();
			}
			return getChart().getLifelineControl();
		}
		return getChart().getSequenceControl();
	}

}
