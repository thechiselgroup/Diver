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


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.FreeformViewport;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.zest.custom.sequence.events.SequenceEvent;
import org.eclipse.zest.custom.sequence.events.SequenceListener;
import org.eclipse.zest.custom.sequence.events.internal.ListenerList;
import org.eclipse.zest.custom.sequence.figures.internal.SuspendableDeferredUpdateManager;
import org.eclipse.zest.custom.sequence.figures.internal.SuspendableLightweightSystem;
import org.eclipse.zest.custom.sequence.internal.AbstractSimpleProgressRunnable;
import org.eclipse.zest.custom.sequence.internal.IUIProgressService;
import org.eclipse.zest.custom.sequence.internal.SimpleProgressMonitor;
import org.eclipse.zest.custom.sequence.internal.UIJobProcessor;
import org.eclipse.zest.custom.sequence.tools.IWidgetTool;
import org.eclipse.zest.custom.sequence.tools.ToolEventDispatcher;
import org.eclipse.zest.custom.sequence.visuals.MessageBasedSequenceVisuals;
import org.eclipse.zest.custom.sequence.widgets.internal.CustomSashForm;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;
import org.eclipse.zest.custom.sequence.widgets.internal.SimpleProgressComposite;
import org.eclipse.zest.custom.sequence.widgets.internal.ThrownErrorDialog;

/**
 * Special uml chart for sequence diagrams.
 * @author Del Myers
 */

public class UMLSequenceChart extends UMLChart {
	private class SequenceChartProgressService implements IUIProgressService, ControlListener {
		/**
		 * Composite used to display progress services.
		 */
		private SimpleProgressComposite progressComposite;
		private Button cancelButton;
		private SimpleProgressMonitor monitor;
		private Composite parent;
		private Control oldTop;
		/**
		 * Used to gauge when to open up the actual progress composite. It will open
		 * after 3 seconds.
		 */
		private final int DELAY = 3000;
		//private AbstractSimpleProgressRunnable runnable;
		Timer openTimer = null;
		public void runInUIThread(AbstractSimpleProgressRunnable runnable,
				final boolean enableCancelButton) throws InvocationTargetException {
			if (this.monitor != null) {
				//if this happens, there has been an error in the scheduling.
				throw new InvocationTargetException(new IllegalStateException("Cannot run concurrent ui jobs"));
			}
			synchronized (this) {
				if (openTimer == null) {
					openTimer = new Timer();
					openTimer.schedule(new TimerTask(){
						public void run() {
							getDisplay().asyncExec(new Runnable(){
								public void run() {
									open(enableCancelButton);
								}
							}
						);
						}
					}, DELAY);
				}
			}
			this.monitor = new SimpleProgressMonitor(this, enableCancelButton) {
				@Override
				public void cancel() {
					if (enableCancelButton) {
						super.cancel();
						close();
					}
				}
			};
						
			try {
				if (isRedrawing()) {
					((SuspendableDeferredUpdateManager)sequenceCanvas.getLightweightSystem().getUpdateManager()).suspend();
				}
				runnable.runInUIThread(monitor);
				
			} finally {
				if (isRedrawing()) {
					((SuspendableDeferredUpdateManager)sequenceCanvas.getLightweightSystem().getUpdateManager()).resume();
				}
				close();
				this.monitor = null;
			}
			
		}
		public void setSubTask(String taskName) {
			if (progressComposite != null && !progressComposite.isDisposed()) {
				progressComposite.setSubTask(taskName);
			}
		}
		public void setTask(String taskName, int totalWork) {
			if (progressComposite != null && !progressComposite.isDisposed()) {
				progressComposite.setTask(taskName, totalWork);
			}
		}
		public void setTaskName(String taskName) {
			if (progressComposite != null && !progressComposite.isDisposed()) {
				progressComposite.setSubTask(taskName);
			}
		}
		public void setWorked(int work) {
//			if (progressComposite == null || progressComposite.isDisposed()) {
//				long currentTime = System.currentTimeMillis();
//				if (currentTime-startTime >= DELAY) {
//					open(enableCancelButton);
//				}
//			}
			if (progressComposite != null && !progressComposite.isDisposed()) {
				progressComposite.setWorked(work);
				if (work == WORKED_DONE) {
					close();
				}
			}
			
		}
		
		/**
		 * Creates the composite. It is expected that the parent will be layed out
		 * using a grid layout.
		 * @param parent
		 */
		protected SequenceChartProgressService(Composite parent) {
			this.parent = parent;
			if (!(parent.getLayout() instanceof StackLayout)) {
				throw new IllegalArgumentException("Expected a stack layout");
			}
			this.oldTop = ((StackLayout)parent.getLayout()).topControl;
			progressComposite = new SimpleProgressComposite(parent);
			cancelButton = new Button(progressComposite, SWT.PUSH);
			cancelButton.setText("Stop");
			GridData gd = new GridData(GridData.END, GridData.FILL, false, false);
			cancelButton.setLayoutData(gd);
			cancelButton.addSelectionListener(new SelectionListener(){
				public void widgetDefaultSelected(SelectionEvent e) {
					if (monitor != null) {
						monitor.cancel();
					}
				}
				public void widgetSelected(SelectionEvent e) {
					if (monitor != null) {
						monitor.cancel();	
					}
				}
			});

			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			int width = progressComposite.getParent().getSize().x;
			gd.minimumWidth = width;
			gd.widthHint = width;
			progressComposite.setLayoutData(gd);
		}
		private synchronized void open(boolean showCancelButton) {
			StackLayout parentLayout = (StackLayout) parent.getLayout();
			this.oldTop = parentLayout.topControl;
			if (monitor == null) {
				return;
			}
			progressComposite.setVisible(true);
			cancelButton.setVisible(showCancelButton);
			parentLayout.topControl = progressComposite;
			parent.layout();
		}	
		private synchronized void close() {
			if (openTimer != null) {
				openTimer.cancel();
				openTimer = null;
			}
			this.monitor = null;
			if (progressComposite != null && !progressComposite.isDisposed()) {
				StackLayout parentLayout = (StackLayout) parent.getLayout();
				if (oldTop != null) {
					parentLayout.topControl = oldTop;
				}
				parent.layout();
			}
		}

		public void controlMoved(ControlEvent e) {
			if (progressComposite != null && !progressComposite.isDisposed()) {
				UMLSequenceChart chart = UMLSequenceChart.this;
				Point location = chart.getLocation();
				location = chart.toControl(chart.getParent().toDisplay(location));
				Point size = progressComposite.getSize();
				location.y = location.y + chart.getSize().y - size.y;
				progressComposite.setLocation(location);
			}
		}

		public void controlResized(ControlEvent e) {
			if (progressComposite != null && !progressComposite.isDisposed()) {
				UMLSequenceChart chart = UMLSequenceChart.this;
				int height = chart.getSize().y/4;
				int preferredHeight = progressComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
				if (preferredHeight != SWT.DEFAULT && preferredHeight < height) {
					height = preferredHeight;
				}
				progressComposite.setSize(chart.getSize().x, height);
				Point location = chart.getLocation();
				location = chart.toControl(chart.getParent().toDisplay(location));
				location.y = location.y + chart.getSize().y - height;
				progressComposite.setLocation(location);
			}
		}
		/* (non-Javadoc)
		 * @see org.eclipse.zest.custom.sequence.internal.IUIProgressService#handleException(java.lang.Throwable)
		 */
		public void handleException(Throwable t) {
			if (!isDisposed() && !getShell().isDisposed()) {
				new ThrownErrorDialog(getShell()).open(t);
			} else {
				t.printStackTrace();
			}
		}
		
	}
	
	private class SequenceNavigator implements KeyListener, SelectionListener {
		/**
		 * Needed to decide where to go next in the case of up/down
		 * on messages.
		 */
		Lifeline lastLifeline;
		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
		 */
		public void keyPressed(KeyEvent e) {
			if (e.stateMask != 0) return;
			int code = e.keyCode;
			
			UMLItem[] selected = getSelection();
			UMLItem selectedItem = null;
			if (selected.length > 0) {
				selectedItem = selected[0];
			}
			if (selectedItem == null) {
				return;
			}
			if (lastLifeline != null) {
				lastLifeline.setHighlight(false);
			}
			switch (code) {
			case SWT.ARROW_LEFT:
				if (selectedItem instanceof Lifeline) {
					Lifeline line = (Lifeline) selectedItem;
					Lifeline[] visibleLines = getVisibleLifelines();
					int i = 0;
					for (; i < visibleLines.length; i++) {
						if (visibleLines[i] == line) {
							break;
						}
					}
					if (i-1 >= 0) {
						internalSetSelection(new UMLItem[]{visibleLines[i-1]});
						navigate(visibleLines[i-1]);
					}
				} else if (selectedItem instanceof Activation) {
					Activation a = (Activation) selectedItem;
					if (a.isExpanded()) {
						a.setExpanded(false);
					}
				} else if (selectedItem instanceof Message) {
					Message m = (Message) selectedItem;
					Activation source = m.getSource();
					Activation target = m.getTarget();
					if (source != null && target != null) {
						Rectangle sourceBounds = getItemBounds(source);
						Rectangle targetBounds = getItemBounds(target);
						if (sourceBounds!=null && targetBounds != null) {
							if (sourceBounds.x >= targetBounds.x) {
								//move to the target.
								if (lastLifeline != null) {
									lastLifeline.setHighlight(false);
								}
								lastLifeline = target.getVisibleLifeline();
								show(target);
							} else {
								//move to the source.
								if (lastLifeline != null) {
									lastLifeline.setHighlight(false);
								}
								lastLifeline = source.getVisibleLifeline();
								show(source);
							}
						}
					}
				}
				break;
			case SWT.ARROW_RIGHT:
				if (selectedItem instanceof Lifeline) {
					Lifeline line = (Lifeline) selectedItem;
					Lifeline[] visibleLines = getVisibleLifelines();
					int i = 0;
					for (; i < visibleLines.length; i++) {
						if (visibleLines[i] == line) {
							break;
						}
					}
					if (i+1 < visibleLines.length) {
						internalSetSelection(new UMLItem[]{visibleLines[i+1]});
						navigate(visibleLines[i+1]);
					}
				} else if (selectedItem instanceof Activation) {
					Activation a = (Activation) selectedItem;
					if (!a.isExpanded()) {
						a.setExpanded(true);
					}
				} else if (selectedItem instanceof Message) {
					Message m = (Message) selectedItem;
					Activation source = m.getSource();
					Activation target = m.getTarget();
					if (source != null && target != null) {
						Rectangle sourceBounds = getItemBounds(source);
						Rectangle targetBounds = getItemBounds(target);
						if (sourceBounds!=null && targetBounds != null) {
							if (sourceBounds.x < targetBounds.x) {
								//move to the target.
								if (lastLifeline != null) {
									lastLifeline.setHighlight(false);
								}
								lastLifeline = target.getVisibleLifeline();
								//internalSetSelection(new UMLItem[]{target});
								show(target);
							} else {
								//move to the source.
								if (lastLifeline != null) {
									lastLifeline.setHighlight(false);
								}
								lastLifeline = source.getVisibleLifeline();
								show(source);
							}
						}
					}
				}
				break;
			case SWT.ARROW_UP:
				if (selectedItem instanceof Lifeline) {
					Lifeline line = (Lifeline) selectedItem;
					if (lastLifeline != null) {
						lastLifeline.setHighlight(false);
					}
					lastLifeline = line;
				} else if ((selectedItem instanceof Activation) || (selectedItem instanceof Message)) {
					Lifeline l;
					if (selectedItem instanceof Activation) {
						l = ((Activation)selectedItem).getVisibleLifeline();
					} else {
						if (lastLifeline != null) {
							l = lastLifeline;
						} else {
							l = ((Message)selectedItem).getSource().getVisibleLifeline();
						}
					}
					List<UMLItem> ordered = getVisibleOrderedItems(l);
					int index = ordered.indexOf(selectedItem);
					if (index <= 0) {
						internalSetSelection(new UMLItem[]{l});
					} else {
						index--;
						if (index < ordered.size()) {
							UMLItem newItem = ordered.get(index);
							internalSetSelection(new UMLItem[]{newItem});
							navigate(ordered.get(index));
						}
					}
					if (lastLifeline != null) {
						lastLifeline.setHighlight(false);
					}
					lastLifeline = l;
				} 
				break;
			case SWT.ARROW_DOWN:
				if (selectedItem instanceof Lifeline) {
					Lifeline line = (Lifeline) selectedItem;
					Activation[] as = line.getOrderedActivations();
					if (as.length > 0) {
						UMLItem reveal = as[0];
						if (as[0].getSourceCall() != null && as[0].getSourceCall().isVisible()) {
							reveal = as[0].getSourceCall();
						}
						internalSetSelection(new UMLItem[]{reveal});
						navigate(reveal);
					}
					if (lastLifeline != null) {
						lastLifeline.setHighlight(false);
					}
					lastLifeline = line;
				} else if ((selectedItem instanceof Activation) || (selectedItem instanceof Message)) {
					Lifeline l;
					if (selectedItem instanceof Activation) {
						l = ((Activation)selectedItem).getVisibleLifeline();
					} else {
						if (lastLifeline != null) {
							l = lastLifeline;
						} else {
							l = ((Message)selectedItem).getSource().getVisibleLifeline();
						}
					}
					List<UMLItem> ordered = getVisibleOrderedItems(l);
					int index = ordered.indexOf(selectedItem);

					index++;
					if (index < ordered.size()) {
						internalSetSelection(new UMLItem[]{ordered.get(index)});
						navigate(ordered.get(index));
					}
					if (lastLifeline != null) {
						lastLifeline.setHighlight(false);
					}
					lastLifeline = l;
				}
				break;
			}
			if (lastLifeline != null) {
				lastLifeline.setHighlight(true);
			}
			
		}

		/**
		 * Scrolls x position of the window to show the given activation.
		 * @param target
		 */
		private void show(Activation item) {
			Rectangle itemBounds = getItemBounds(item);
			if (itemBounds != null) {
				Rectangle clientBounds = getBounds();
				setScrollX((itemBounds.x + itemBounds.width/2) - (clientBounds.width/2));
			}
		}

		
		private List<UMLItem> getVisibleOrderedItems(Lifeline l) {
			TreeSet<UMLItem> items = new TreeSet<UMLItem>(new Comparator<UMLItem>(){
				public int compare(UMLItem o1, UMLItem o2) {
					if (o1 == o2) return 0;
					Rectangle r1 = getItemBounds(o1);
					Rectangle r2 = getItemBounds(o2);
					if (r1 == null) {
						if (r2 == null) {
							return 0;
						}
						return -1;
					}
					if (r2 == null) {
						return 1;
					}
					int diff = r1.y-r2.y;
					if (diff == 0) {
						if (o1 instanceof Activation) {
							diff=1;
						} else {
							diff = -1;
						}
					}
					return diff;
				}});
			List<Activation> activations = Arrays.asList(l.getOrderedActivations());
			items.add(l);
			items.addAll(activations);
			for (Activation a : activations) {
				items.addAll(a.getVisibleOrderedMessages());
			}
			List<UMLItem> list = new ArrayList<UMLItem>(items);
			return list;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetSelected(SelectionEvent e) {
			UMLItem[] selection = getSelection();
			if (selection.length > 0) {
				if (lastLifeline != null) {
					lastLifeline.setHighlight(false);
				}
				UMLItem newItem = selection[0];
				if (newItem instanceof Lifeline) {
					lastLifeline = (Lifeline) newItem;
				} else if (newItem instanceof Message) {
					lastLifeline = ((Message)newItem).getSource().getVisibleLifeline();
				} else if (newItem instanceof Activation) {
					lastLifeline = ((Activation)newItem).getVisibleLifeline();
				} else {
					lastLifeline = null;
				}
				if (lastLifeline != null) {
					lastLifeline.setHighlight(true);
				}
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
		 */
		public void keyReleased(KeyEvent e) {}
		
	}
	
	private class SashPainter implements Listener {
		boolean armed = false;
		private CustomSashForm form;
		
		public SashPainter(CustomSashForm form) {
			this.form = form;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
		 */
		public void handleEvent(Event event) {
			
			switch (event.type) {
			case SWT.MouseEnter:
			case SWT.MouseDown:
				armed = true;
				((Control)event.widget).redraw();
				break;
			case SWT.MouseUp:
				if (armed) {
					if (event.widget instanceof Sash) {
						Control c = form.findControl((Sash) event.widget);
						if (c != null) {
							int weight = form.getWeight(c);
							if (weight != 0) {
								form.collapseControl(c);
							} else {
								form.extendControl(c);
							}
						}
					}
				}
				armed = false;
				break;
			case SWT.DragDetect:
			case SWT.MouseExit:
				((Control)event.widget).redraw();
				armed = false;
				break;
			case SWT.Move:
			case SWT.Resize:
				((Control)event.widget).redraw();
				break;
			case SWT.Paint:
				paint(event);
				break;
			}
		}
		/**
		 * @param gc
		 */
		private void paint(Event e) {
			GC gc = e.gc;
			
			if (!(e.widget instanceof Sash)){
				return;
			}
			Sash c = (Sash) e.widget;
			Rectangle bounds = c.getBounds();
			int ar = 1;
			if (bounds.height != 0) {
				ar = bounds.width/bounds.height;
			}
			
			if (armed) {
				gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_GRAY));
			} else {
				gc.setBackground(getBackground());
			}
			gc.fillRectangle(e.x, e.y, e.width, e.height);
			gc.setForeground(c.getDisplay().getSystemColor(SWT.COLOR_BLACK));
			
			if (ar > 0) {
				int centerX = e.width/2;
				int top = 0;//getBounds().y;
				int bottom = top+e.height-1;
				if (((CustomSashForm)contents).getWeights()[0] > 0) {
					//draw the arrows going up
					gc.setLineWidth(2);
					gc.drawLine(centerX-8, bottom, centerX-4,top);
					gc.drawLine(centerX-4, top, centerX,bottom);
					gc.drawLine(centerX, bottom, centerX+4,top);
					gc.drawLine(centerX+4, top, centerX+8,bottom);
				} else {
					//draw the arrows going down
					gc.setLineWidth(2);
					gc.drawLine(centerX-8, top, centerX-4,bottom);
					gc.drawLine(centerX-4, bottom, centerX,top);
					gc.drawLine(centerX, top, centerX+4,bottom);
					gc.drawLine(centerX+4, bottom, centerX+8,top);
				}
			} else {
				int centerY = e.height/2;
				int left = 0;//getBounds().y;
				int right = left+e.width-1;
				if (((CustomSashForm)contents).getWeights()[0] > 0) {
					//draw the arrows going up
					gc.setLineWidth(2);
					gc.drawLine(left, centerY-8, right, centerY-4);
					gc.drawLine(right, centerY-4, left, centerY);
					gc.drawLine(left, centerY, right, centerY+4);
					gc.drawLine(right, centerY+4, left, centerY+8);
				} else {
					//draw the arrows going down
					gc.setLineWidth(2);
					gc.setLineWidth(2);
					gc.drawLine(right, centerY-8, left, centerY-4);
					gc.drawLine(left, centerY-4, right, centerY);
					gc.drawLine(right, centerY, left, centerY+4);
					gc.drawLine(left, centerY+4, right, centerY+8);
				}
			}
		}	
	};
	
	/**
	 * Property indicating that the root of this chart has changed.
	 */
	public static final String ROOT_PROP = "root";
	
	/**
	 * Property indicating that the timing on this chart has changed. The old value
	 * of the property change indication will always be <code>Boolean.FALSE</code>
	 * the new value will always be <code>Boolean.TRUE</code>
	 */
	public static final String TIMING_PROP = "time";
	
	private Activation rootActivation;
	private ListenerList sequenceListeners;

	private PropertyChangeListener expandListener;
	
	private MessageBasedSequenceVisuals widgetVisuals;

	private int deferRedrawCount;

	private UIJobProcessor jobProcessor;
	private SequenceChartProgressService progressService;

	/**
	 * The control that is used to draw the sequence diagram
	 */
	private FigureCanvas sequenceCanvas;

	/**
	 * The control that is used to draw the objects.
	 */
	private FigureCanvas objectCanvas;

	/**
	 * The control that contains both the sequence canvas and the object canvas.
	 */
	private CustomSashForm contents;

	private ToolEventDispatcher[] dispatchers;

	private SequenceChartEventFilter eventFilter;

	private FigureCanvas groupingCanvas;

	private CustomSashForm sequenceSash;

	private SequenceClone clone;

	private Composite sequenceContainer;

	
	private class ExpandPropogator implements PropertyChangeListener {

		/* (non-Javadoc)
		 * @see org.eclipse.mylar.zest.custom.sequence.widgets.PropertyChangeListener#propertyChanged(java.lang.Object, java.lang.String, java.lang.Object, java.lang.Object)
		 */
		public void propertyChanged(Object source, String property, Object oldValue, Object newValue) {
			if (IWidgetProperties.EXPANDED.equals(property)) {
				fireExpandChanged((IExpandableItem)source, (Boolean)newValue);
			}
		}
		
	}
	
	/**
	 * Creates a new UMLSequenceChart on the given parent with the given style. Note that UMLSequenceCharts,
	 * as with all charts, are complex composites. A second Composite is created for the actual viewing of
	 * the chart. Therefore, it is best to not set a layout on the chart itself. Instead, if you would like
	 * to have windowed children on the chart, it is best to place them on the control recieved by 
	 * getControl().
	 * 
	 * @param parent
	 * @param style
	 */
	public UMLSequenceChart(Composite parent, int style) {
		super(parent, style);
		setBackground(parent.getBackground());
		//hide scrollbars
		if (getHorizontalBar() != null) {
			super.getHorizontalBar().setVisible(false);
		}
		if (getVerticalBar() != null) {
			super.getVerticalBar().setVisible(false);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.UMLChart#createContents(org.eclipse.swt.widgets.Composite, int)
	 */
	@Override
	protected Composite createContents(Composite parent, int style) {
		rootActivation = null;
		this.deferRedrawCount = 0;
		this.sequenceListeners = new ListenerList();
		this.expandListener = new ExpandPropogator();
		
		createFigureContents(parent, style);
		return contents;
	}


	/**
	 * @param parent
	 * @param style
	 * TODO: All of this stuff should be in the MessageBasedSequenceVisuals,
	 * just to support better encapsulation.
	 */
	public void createFigureContents(Composite parent, int style) {
		contents = new CustomSashForm(parent, SWT.FLAT);
		((CustomSashForm)contents).SASH_WIDTH = 5;
		contents.setBackground(parent.getBackground());
		this.widgetVisuals = new MessageBasedSequenceVisuals(this);
		this.dispatchers = new ToolEventDispatcher[] {
				new ToolEventDispatcher(widgetVisuals, this),
				new ToolEventDispatcher(widgetVisuals, this),
				new ToolEventDispatcher(widgetVisuals, this)
			};
		//create the container pane
		LightweightSystem system = new SuspendableLightweightSystem();
		system.setEventDispatcher(dispatchers[0]);
		groupingCanvas = new FigureCanvas(contents, SWT.NONE, system){
			/* (non-Javadoc)
			 * @see org.eclipse.draw2d.FigureCanvas#computeSize(int, int, boolean)
			 */
			@Override
			public Point computeSize(int hint, int hint2, boolean changed) {
				Point size = super.computeSize(hint, hint2, changed);
				if (!isVisible()) {
					size.y = 0;
				}
				return size;
			}
		};
		groupingCanvas.setBackground(parent.getBackground());
		//groupingCanvas.setViewport(new FreeformViewport());
		groupingCanvas.setScrollBarVisibility(FigureCanvas.NEVER);
		
		SashPainter painter = new SashPainter(contents);
		contents.addSashListener(SWT.Move, groupingCanvas, painter);
		contents.addSashListener(SWT.MouseDown, groupingCanvas, painter);
		contents.addSashListener(SWT.MouseUp, groupingCanvas, painter);
		contents.addSashListener(SWT.MouseEnter, groupingCanvas, painter);
		contents.addSashListener(SWT.MouseExit, groupingCanvas, painter);
		contents.addSashListener(SWT.DragDetect, groupingCanvas, painter);
		contents.addSashListener(SWT.Resize, groupingCanvas, painter);
		contents.addSashListener(SWT.Paint, groupingCanvas, painter);
		contents.setOrientation(SWT.VERTICAL);
		
		
		sequenceSash = new CustomSashForm(contents, SWT.NONE);
		sequenceSash.SASH_WIDTH = 5;
		Composite cloneContainer = new Composite(sequenceSash, (style | SWT.H_SCROLL | SWT.V_SCROLL)^( SWT.H_SCROLL | SWT.V_SCROLL));
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.makeColumnsEqualWidth = true;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginBottom = 0;
		layout.marginHeight = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		layout.marginTop = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		cloneContainer.setLayout(layout);
		
		sequenceContainer = new Composite(sequenceSash, (style | SWT.H_SCROLL | SWT.V_SCROLL)^( SWT.H_SCROLL | SWT.V_SCROLL));
		
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.makeColumnsEqualWidth = true;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginBottom = 0;
		layout.marginHeight = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		layout.marginTop = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		sequenceContainer.setLayout(layout);

		Composite objectAndProgressContainer = new Composite(sequenceContainer, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.minimumHeight = MessageBasedSequenceVisuals.OBJECT_HEIGHT;
		gd.heightHint = gd.minimumHeight;
		objectAndProgressContainer.setLayoutData(gd);
		objectAndProgressContainer.setLayout(new StackLayout());
		
		setData(MessageBasedSequenceVisuals.VISUAL_KEY, widgetVisuals);
		
		setActiveTool(new SelectionTool());
		system = new SuspendableLightweightSystem();
		system.setEventDispatcher(dispatchers[1]);
		this.objectCanvas = new FigureCanvas(objectAndProgressContainer, system);
		//objectCanvas.setViewport(new FreeformViewport());
		objectCanvas.setLayout(new FillLayout());
		objectCanvas.setScrollBarVisibility(FigureCanvas.NEVER);
		system = new SuspendableLightweightSystem();
		system.setUpdateManager(new SuspendableDeferredUpdateManager());
		system.setEventDispatcher(dispatchers[2]);
		
		this.sequenceCanvas = new FigureCanvas(sequenceContainer, SWT.H_SCROLL | SWT.V_SCROLL, system);
		sequenceCanvas.setViewport(new FreeformViewport());
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		sequenceCanvas.setLayoutData(layoutData);
		
		if ((style & SWT.H_SCROLL) != 0) {
			sequenceCanvas.setHorizontalScrollBarVisibility(FigureCanvas.AUTOMATIC);
		} else {
			sequenceCanvas.setHorizontalScrollBarVisibility(FigureCanvas.NEVER);
		}
		if ((style & SWT.V_SCROLL) != 0) {
			sequenceCanvas.setVerticalScrollBarVisibility(FigureCanvas.AUTOMATIC);
		} else {
			sequenceCanvas.setVerticalScrollBarVisibility(FigureCanvas.NEVER);
		}
		sequenceCanvas.getViewport().setContentsTracksHeight(true);
		sequenceCanvas.getViewport().setContentsTracksWidth(true);
		objectCanvas.getViewport().setContentsTracksHeight(true);
		objectCanvas.getViewport().setContentsTracksWidth(true);
		groupingCanvas.getViewport().setContentsTracksHeight(true);
		groupingCanvas.getViewport().setContentsTracksWidth(true);
		widgetVisuals.createFigures();
		SequenceNavigator navigator = new SequenceNavigator();
		addKeyListener(navigator);
		addSelectionListener(navigator);
		this.progressService = new SequenceChartProgressService(objectAndProgressContainer);
		setData("progress", progressService);
		this.jobProcessor = new UIJobProcessor(progressService);
		setData("jobs", jobProcessor);
		((StackLayout)objectAndProgressContainer.getLayout()).topControl = objectCanvas;
		this.eventFilter = new SequenceChartEventFilter(this);
		eventFilter.hookToDisplay();
		
		//add the clone
		clone = new SequenceClone(cloneContainer, this);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		clone.getControl().setLayoutData(data);
		clone.getControl().setBackground(getBackground());
		painter = new SashPainter(sequenceSash);
		sequenceSash.addSashListener(SWT.Move, cloneContainer, painter);
		sequenceSash.addSashListener(SWT.MouseDown, cloneContainer, painter);
		sequenceSash.addSashListener(SWT.MouseUp, cloneContainer, painter);
		sequenceSash.addSashListener(SWT.MouseEnter, cloneContainer, painter);
		sequenceSash.addSashListener(SWT.MouseExit, cloneContainer, painter);
		sequenceSash.addSashListener(SWT.DragDetect, cloneContainer, painter);
		sequenceSash.addSashListener(SWT.Resize, cloneContainer, painter);
		sequenceSash.addSashListener(SWT.Paint, cloneContainer, painter);
		sequenceSash.setOrientation(SWT.HORIZONTAL);
		contents.setWeights(new int[] {0, 1000});
		sequenceSash.setWeights(new int[] {0, 1000});
	}
	
	/**
	 * Returns the control that contains all of the headers of the lifelines.
	 * @return the control that contains all of the headers of the lifelines.
	 */
	public Control getLifelineControl() {
		return objectCanvas;
	}
	
	/**
	 * Returns the control that draws the sequence diagram itself.
	 * @return the control that draws the sequence diagram itself.
	 */
	public Control getSequenceControl() {
		return sequenceCanvas;
	}
	
	/**
	 * Returns the control that draws the "clone' of the sequence diagram.
	 * @return the control that draws the "clone' of the sequence diagram.
	 */
	public Control getCloneControl() {
		return clone.getControl();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Scrollable#getHorizontalBar()
	 */
	@Override
	public ScrollBar getHorizontalBar() {
		checkWidget();
		//cheat a little. This chart will never have a horizontal scroll bar, but the sequence canvas
		//might.
		if (sequenceCanvas != null && sequenceCanvas.getHorizontalScrollBarVisibility() != FigureCanvas.NEVER) {
			return sequenceCanvas.getHorizontalBar();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Scrollable#getVerticalBar()
	 */
	@Override
	public ScrollBar getVerticalBar() {
		checkWidget();
		//cheat a little. This chart will never have a horizontal scroll bar, but the sequence canvas
		//might.
		if (sequenceCanvas != null && sequenceCanvas.getVerticalScrollBarVisibility() != FigureCanvas.NEVER) {
			return sequenceCanvas.getVerticalBar();
		}
		return null;
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.widgets.UMLChart#createItem(org.eclipse.mylar.zest.custom.sequence.widgets.UMLItem)
	 */
	@Override
	void createItem(UMLItem item) {
		super.createItem(item);
		if (item instanceof IExpandableItem) {
			item.addPropertyChangeListener(expandListener);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.widgets.UMLChart#deleteItem(org.eclipse.mylar.zest.custom.sequence.widgets.UMLItem)
	 */
	@Override
	void deleteItem(UMLItem item) {
		if (item instanceof IExpandableItem) {
			item.removePropertyChangeListener(expandListener);
		}
		super.deleteItem(item);
	}
	
	
	
	
	/**
	 * Sets the activation that will be the visual "root" of all activations in this chart.
	 * Only this activation and its sub activations will be visible in the chart. A chart cannot
	 * be seen until the root activation is set.
	 * @param activation
	 */
	public void setRootActivation(Activation activation) {
		if (!activation.getSequenceChart().equals(this)) SWT.error(SWT.ERROR_INVALID_PARENT);
		checkWidget();
		Activation old = this.rootActivation;
		this.rootActivation = activation;
		markDirty();
		firePropertyChange(ROOT_PROP, old, activation);
		fireRootChanged();
	}
	

	/**
	 * Performs a refresh regardless of whether or not the viewer is redrawing
	 * @param force
	 */
	public void refresh(boolean force) {
		if (isDisposed() || (!force && !isRedrawing())) return;
		if (isDirty() && getRootActivation() != null) {
			widgetVisuals.scheduleRefresh(jobProcessor);
			this.dirty = false;
		}
	}
	
		
	/**
	 * If <code>redraw</code> is set to false, all operations that cause a refresh in the viewer will be deferred until
	 * <code>redraw</code> is reset to true. Every call to setRedraw(false) must be followed by a subsequent setRedraw(true),
	 * otherwise redraws will not occur later. Setting <code>redraw</code> to true causes an immediate refresh.
	 */
	public synchronized void setRedraw(boolean redraw) {
		if (!redraw) {
			this.deferRedrawCount++;
		} else {
			if (deferRedrawCount > 0) {
				deferRedrawCount--;
			}
		}
		if (isRedrawing()) {
			((SuspendableDeferredUpdateManager)sequenceCanvas.getLightweightSystem().getUpdateManager()).resume();
			layout();
		} else {
			((SuspendableDeferredUpdateManager)sequenceCanvas.getLightweightSystem().getUpdateManager()).suspend();
		}
	}
	
	private synchronized boolean isRedrawing() {
		return deferRedrawCount == 0;
	}
	

	/**
	 * @return the rootActivation
	 */
	public Activation getRootActivation() {
		return rootActivation;
	}


	private SelectionTool defaultTool;

	private boolean disposing;


	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.widgets.UMLChart#performLayout()
	 */
	@Override
	protected void performLayout() {
		performLayout(false);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.UMLChart#markDirty()
	 */
	@Override
	protected void markDirty() {
		super.markDirty();
		refresh();
	}
	/**
	 * Performs the layout, forcing it to occur even if not redrawing.
	 * @param force
	 */
	protected void performLayout(boolean force) {
		if (isDisposed() || (!force && !isRedrawing())) return;
		widgetVisuals.scheduleLayout(jobProcessor);
	}
	
	@Override
	public void layout(boolean changed, boolean all) {
		if (isRedrawing()) {
			super.layout(changed, all);
		}
	}
	
	@Override
	public void layout(boolean changed) {
		if (isRedrawing()) {
			super.layout(changed);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.widgets.UMLChart#clearChart()
	 */
	@Override
	public void clearChart() {
		this.rootActivation = null;
		super.clearChart();
	}
	
	/**
	 * Adds the given SequenceListener to the list of listeners. Does nothing if an identical listener is
	 * already registered.
	 * @param listener the listener to register.
	 */
	public void addSequenceListener(SequenceListener listener) {
		sequenceListeners.add(listener);
	}
	
	/**
	 * Removes the given SequenceListener from the list of listeners, if it exists in the list.
	 * @param listener the listener to remove.
	 */
	public void removeSequenceListener(SequenceListener listener) {
		sequenceListeners.remove(listener);
	}
	
	
	protected void fireExpandChanged(IExpandableItem item, Boolean newValue) {
		Event e = new Event();
		e.item = (Widget) item;
		e.widget = this;
		e.doit=true;
		e.time = (int)(System.currentTimeMillis() & 0x0000FFFFL);
		SequenceEvent se = new SequenceEvent(e);
		for (Object o : sequenceListeners.getListeners()) {
			SequenceListener l = (SequenceListener) o;
			if (newValue) {
				l.itemExpanded(se);
			} else {
				l.itemCollapsed(se);
			}
		}
	}
	
	protected void fireRootChanged() {
		Event e = new Event();
		e.item = getRootActivation();
		e.widget = this;
		e.doit=true;
		e.time = (int)(System.currentTimeMillis() & 0x0000FFFFL);
		e.data = e.item.getData();
		SequenceEvent se = new SequenceEvent(e);
		for (Object o : sequenceListeners.getListeners()) {
			SequenceListener l = (SequenceListener) o;
			l.rootChanged(se);
		}
	}
	

	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.widgets.UMLChart#setSelection(org.eclipse.mylar.zest.custom.sequence.widgets.UMLItem[])
	 */
	@Override
	public void setSelection(UMLItem[] items) {
		super.setSelection(items);
		widgetVisuals.setSelection(items);
	}

             
	/**
	 * @param tool
	 */
	public void setActiveTool(IWidgetTool tool) {
		for (ToolEventDispatcher dispatcher : dispatchers) {
			dispatcher.setTool(tool);
			setCursor(dispatcher.getTool().getDefaultCursor());
		}
	}
	
	public IWidgetTool getActiveTool() {
		return dispatchers[2].getTool();
	}
	
	public IWidgetTool getDefaultTool() {
		if (defaultTool == null) {
			defaultTool = new SelectionTool();
		}
		return defaultTool;
	}
	
	/**
	 * Returns an item at the given point relative to the reciever, if available.
	 * @param p
	 * @return
	 */
	public Widget getItemAt(Point p) {
		return getItemAt(p.x, p.y);
	}

	/**
	 * Locates the item in the chart and scrolls to it so that it can be seen.
	 * If the item is hidden, then the chart is expanded appropriately so that
	 * the item can be seen.
	 * 
	 * @param w
	 */
	public void navigate(UMLItem item) {
		if (item instanceof MessageGroup) {
			reveal2((MessageGroup)item);
		} else if (item instanceof Activation) {
			reveal2((Activation)item);
		}
		Rectangle itemBounds = getRelativeBounds(item);
		FigureCanvas containingControl = (FigureCanvas) widgetVisuals.getContainingComposite(item);
		if (itemBounds == null || containingControl == null) {
			return;
		}
		if (containingControl == getLifelineGroupControl()) {
			setLifelineGroupsVisible(true);
			return;
		}
		if (item instanceof Message) {
			//make sure that the source is visible
			Rectangle bounds2 = getRelativeBounds(((Message)item).getSource());
			itemBounds.x = bounds2.x;
			itemBounds.width = bounds2.width;
		}
		Point displayPoint = toDisplay(itemBounds.x, itemBounds.y);
		itemBounds.x = displayPoint.x;
		itemBounds.y = displayPoint.y;
		if (itemBounds != null) {
			Rectangle sequenceBounds = containingControl.getClientArea();
			displayPoint = containingControl.getParent().toDisplay(containingControl.getBounds().x, containingControl.getBounds().y);
			sequenceBounds.x = displayPoint.x;
			sequenceBounds.y = displayPoint.y;
			if (getVerticalBar().isVisible()) {
				sequenceBounds.width -= getVerticalBar().getSize().x;
			}
			if (getHorizontalBar().isVisible()) {
				sequenceBounds.height -= getHorizontalBar().getSize().y;
			}
			//scroll y
			if (!(containingControl == getLifelineControl())) {
				if (itemBounds.y < sequenceBounds.y) {
					int diffy = itemBounds.y - sequenceBounds.y;
					scrollToY(diffy-10);
				} else if (itemBounds.y > (sequenceBounds.y + sequenceBounds.height)) {
					int diffy = (itemBounds.y + 10) - (sequenceBounds.y + sequenceBounds.height);
					scrollToY(diffy);
				}
			}
			//scroll x
			if (itemBounds.x < sequenceBounds.x) {
				int diffx = itemBounds.x - sequenceBounds.x;
				scrollToX(diffx-10);
			} else if ((itemBounds.x+itemBounds.width) > (sequenceBounds.x + sequenceBounds.width)) {
				int diffx = (itemBounds.x + itemBounds.width) - (sequenceBounds.x + sequenceBounds.width);
				scrollToX(diffx);
			}
		}
	}
	
	/**
	 * Locates the item in the chart and scrolls to it so that it can be seen. Centers the item in the view.
	 * If the item is hidden, then the chart is expanded appropriately so that
	 * the item can be seen.
	 * 
	 * @param w
	 */
	public void reveal(final UMLItem item) {
		if (item instanceof MessageGroup) {
			reveal2((MessageGroup)item);
		} else if (item instanceof Activation) {
			reveal2((Activation)item);
		}
		//run in a job in order to allow any layout to finish.
		jobProcessor.runInUIThread(new AbstractSimpleProgressRunnable() {
			
			@Override
			protected void doRunInUIThread(SimpleProgressMonitor monitor)
					throws InvocationTargetException {
				monitor.beginTask("Revealing " + item.getText(), 1);
				Rectangle itemBounds = getRelativeBounds(item);
				FigureCanvas containingControl = (FigureCanvas) widgetVisuals.getContainingComposite(item);
				if (itemBounds == null || containingControl == null) {
					return;
				}
				if (containingControl == getLifelineGroupControl()) {
					setLifelineGroupsVisible(true);
					return;
				}
				if (item instanceof Message) {
					//make sure that the source is visible
					Rectangle bounds2 = getRelativeBounds(((Message)item).getSource());
					itemBounds.x = bounds2.x;
					itemBounds.width = bounds2.width;
				}
				Point displayPoint = toDisplay(itemBounds.x, itemBounds.y);
				itemBounds.x = displayPoint.x;
				itemBounds.y = displayPoint.y;
				if (itemBounds != null) {
					Rectangle sequenceBounds = containingControl.getClientArea();
					displayPoint = containingControl.getParent().toDisplay(containingControl.getBounds().x, containingControl.getBounds().y);
					sequenceBounds.x = displayPoint.x;
					sequenceBounds.y = displayPoint.y;
					if (getVerticalBar().isVisible()) {
						sequenceBounds.width -= getVerticalBar().getSize().x;
					}
					if (getHorizontalBar().isVisible()) {
						sequenceBounds.height -= getHorizontalBar().getSize().y;
					}
					//scroll y
					if (!(containingControl == getLifelineControl())) {
						if (itemBounds.y < sequenceBounds.y) {
							int diffy = itemBounds.y - sequenceBounds.y - sequenceBounds.height/2;
							scrollToY(diffy-10);
						} else if (itemBounds.y > (sequenceBounds.y + sequenceBounds.height)) {
							int diffy = (itemBounds.y + 10) - (sequenceBounds.y + sequenceBounds.height) + sequenceBounds.height/2;
							scrollToY(diffy);
						}
					}
					//scroll x
					if (itemBounds.x < sequenceBounds.x) {
						int diffx = itemBounds.x - sequenceBounds.x - sequenceBounds.width/2;
						scrollToX(diffx-10);
					} else if ((itemBounds.x+itemBounds.width) > (sequenceBounds.x + sequenceBounds.width)) {
						int diffx = (itemBounds.x + itemBounds.width) - (sequenceBounds.x + sequenceBounds.width) + sequenceBounds.width/2;
						scrollToX(diffx);
					}
				}
				monitor.done();
			}
		}, getDisplay(), false);
		
	}
	
	
	
	private void scrollToX(int amount) {
		int currentValue = sequenceCanvas.getViewport().getHorizontalRangeModel().getValue();
		int currentY = sequenceCanvas.getViewport().getVerticalRangeModel().getValue();
		sequenceCanvas.scrollSmoothTo( currentValue+amount, currentY);
	}
	
	private void scrollToY(int amount) {
		int currentValue = sequenceCanvas.getViewport().getVerticalRangeModel().getValue();
		int currentX = sequenceCanvas.getViewport().getHorizontalRangeModel().getValue();
		sequenceCanvas.scrollSmoothTo(currentX, currentValue+amount);
	}
	
	private void setScrollX(int location) {
		int currentValue = sequenceCanvas.getViewport().getVerticalRangeModel().getValue();
		sequenceCanvas.scrollSmoothTo(location, currentValue);
	}
	

	private void reveal2(MessageGroup group) {
		if (!group.isVisible()) {
			Activation a = group.getActivation();
			if (a== null || a.isDisposed()) {
				return;
			}
			//walk up, expanding the activations.
			setRedraw(false);
			while (a != null && !a.isDisposed() && !a.isVisible()) {
				if (!a.isExpanded()) {
					a.setExpanded(true);
				}
				if (!(a.getSourceCall() == null || a.getSourceCall().isDisposed())) {
					a = a.getSourceCall().getSource();
				} else {
					a = null;
				}
			}
			if (a != null && !a.isDisposed() && !a.isExpanded()) {
				a.setExpanded(true);
			}
			setRedraw(true);
			refresh(true);
		}
	}
	
	private void reveal2(Activation activation) {
		if (!activation.isVisible()) {
			setRedraw(false);
			Message call = activation.getSourceCall();
			while (call != null && !call.isDisposed()) {
				Activation source = call.getSource();
				if (source == null || source.isDisposed()) {
					return;
				}
				if (!source.isExpanded()) {
					source.setExpanded(true);
				}
				//if (!source.isVisible()) {
					call = source.getSourceCall();
				//}
			}
			setRedraw(true);
			refresh(true);
		}
	}
	

	/**
	 * Returns the bounds in this chart of the given item, relative to origin of the
	 * scrollable area on the chart, if it is currently displayed. Null otherwise. The
	 * x and y location of the bounds will therefore always be positive. To get
	 * the bounds of the item relative to the chart area, use #getRelativeBounds(UMLItem);
	 * @param item the item to get the bounds of.
	 * @return the chart-relative bounds of the given item.
	 */
	public Rectangle getItemBounds(UMLItem item) {
		org.eclipse.draw2d.geometry.Rectangle b = widgetVisuals.getLocation(item);
		if (b != null) {
			return new Rectangle(b.x, b.y, b.width, b.height);
		}
		return null;
	}
	
	/**
	 * Returns the bounds of the given item relative to the origin of the chart, not
	 * the scrollable area.
	 * @param item
	 * @return
	 */
	public Rectangle getRelativeBounds(UMLItem item) {
		org.eclipse.draw2d.geometry.Rectangle b = widgetVisuals.getRelativeLocation(item);
		if (b != null) {
			return new Rectangle(b.x, b.y, b.width, b.height);
		}
		return null;
	}
	
	/**
	 * Returns the rectangular region that displays the main sequence diagram
	 * (the region in the right-hand sash if both the clone pane and
	 * the sequence pane are visible).
	 * @return the rectangular region that displays the main sequence diagram.
	 */
	public Rectangle getSequenceArea() {
		org.eclipse.draw2d.geometry.Rectangle region =
			widgetVisuals.getSequencePanelArea();
		if (region != null) {
			return new Rectangle(region.x, region.y, region.width, region.height);
		}
		return new Rectangle(0,0,0,0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.UMLChart#setBackground(org.eclipse.swt.graphics.Color)
	 */
	@Override
	public void setBackground(Color color) {
		super.setBackground(color);
		contents.setBackground(color);
		groupingCanvas.setBackground(color);
		objectCanvas.setBackground(color);
		sequenceCanvas.setBackground(color);
		clone.setBackground(color);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.UMLChart#isVisible(org.eclipse.zest.custom.sequence.widgets.UMLItem)
	 */
	@Override
	boolean isVisible(UMLItem item) {
		return widgetVisuals.isVisible(item);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.UMLChart#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
	 */
	@Override
	protected void widgetDisposed(DisposeEvent e) {
		this.disposing = true;
		jobProcessor.quit();
		super.widgetDisposed(e);
		eventFilter.unhookFromDisplay();
		sequenceListeners.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#isDisposed()
	 */
	@Override
	public boolean isDisposed() {
		return super.isDisposed() || disposing;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.widgets.UMLChart#refresh()
	 */
	@Override
	public void refresh() {
		refresh(false);
	}
	
	/**
	 * Returns the item at the given x and y point relative to the chart coordinates,
	 * or null if none could be found. 
	 * @param x
	 * @param y
	 * @return
	 */
	public Widget getItemAt(int x, int y) {
		//translate the point to the chart contents.
		Point p = toDisplay(x, y);
		Point p2 = contents.toControl(p);
		if (p2.y <= MessageBasedSequenceVisuals.OBJECT_HEIGHT) {
			//use the object canvas
			Point localPoint = objectCanvas.toControl(p);
			IFigure f = objectCanvas.getLightweightSystem().getRootFigure().findFigureAt(localPoint.x, localPoint.y);
			if (f != null) {
				return widgetVisuals.getWidget(f);
			}
		} else {
			Point localPoint = sequenceCanvas.toControl(p);
			IFigure f = sequenceCanvas.getLightweightSystem().getRootFigure().findFigureAt(localPoint.x, localPoint.y);
			if (f != null) {
				return widgetVisuals.getWidget(f);
			}
		}
		return null;
	}
	
	
	/**
	 * Returns the lifelines that are currently visible in the chart, in the order that
	 * they appear.
	 * @return the visible lifelines in the chart.
	 */
	public Lifeline[] getVisibleLifelines() {
		return widgetVisuals.getVisibleLifelines();
	}
	
	/**
	 * True iff the lifeline group panel is visible.
	 * @return true iff the lifeline panel is visible.
	 */
	public boolean isLifelineGroupsVisible() {
		return contents.getMaximizedControl() == null;
	}	
	
	/**
	 * Sets whether or not the lifeline group area can be viewed. When visible is false, then
	 * the lifeline group area will never be drawn. If it is true, then the lifeline group area will be
	 * expandable using a draggable sash.
	 * @param visible the visible state that the lifeline group area should have.
	 */
	public void setLifelineGroupsVisible(boolean visible) {
		if (visible == isLifelineGroupsVisible()) {
			return;
		}
		if (visible) {
			contents.setMaximizedControl(null);
		} else {
			contents.setMaximizedControl(sequenceSash);
		}
	}
	
	/**
	 * Sets whether or not the clone area can be viewed. When visible is false, then
	 * the clone area will never be drawn. If it is true, then the clone area will be
	 * expandable using a draggable sash.
	 * @param visible the visible state that the clone area should have.
	 */
	public void setCloneVisible(boolean visible) {
		if (visible == isCloneVisible()) {
			return;
		}
		if (visible) {
			sequenceSash.setMaximizedControl(null);
		} else {
			sequenceSash.setMaximizedControl(sequenceContainer);
		}
	}


	/**
	 * True iff the clone panel is visible.
	 * @return true iff the clone panel is visible.
	 */
	public boolean isCloneVisible() {
		return sequenceSash.getMaximizedControl() == null;
	}


	/**
	 * The control that draws the lifeline groups.
	 * @return
	 */
	public Control getLifelineGroupControl() {
		return groupingCanvas;
	}
}
