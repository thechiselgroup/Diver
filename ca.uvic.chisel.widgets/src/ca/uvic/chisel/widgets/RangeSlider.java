/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.widgets;


import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TypedListener;

/**
 * A simple range slider
 * @author Del Myers
 *
 */
public class RangeSlider extends Composite {

	private Canvas canvas;
	private long min;
	private long max;
	private long rangeMin;
	private long rangeMax;
	private double scale;
	private int visualHigh;
	private int visualLow;
	private LinkedList<RangeAnnotation> items;
	private DisposeListener itemDisposedListener;
	private RangeAnnotation selectedAnnotation;
	
	static final int HANDLE_SIZE = 3;

	/**
	 * @param parent
	 * @param style
	 */
	public RangeSlider(Composite parent, int style) {
		super(parent, style);
		items = new LinkedList<RangeAnnotation>();
		setLayout(new FillLayout());
		this.canvas = new Canvas(this, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				paintCanvas(e);
			}
		});
		canvas.addControlListener(new ControlListener() {		
			public void controlResized(ControlEvent e) {
				resetScale();
			}
			
			public void controlMoved(ControlEvent e) {}
		});
		itemDisposedListener = new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				items.remove(e.widget);
				redraw();
			}
		};
		new RangeSliderHelper(this);
		
	}

	/**
	 * 
	 */
	protected void resetScale() {
		Rectangle bounds = getCanvas().getClientArea();
		int visualRangeSize = bounds.width-HANDLE_SIZE*2;
		if (visualRangeSize < HANDLE_SIZE*2) {
			scale = 0;
		}
		scale = ((double)visualRangeSize)/(getMaximum()-getMinimum());
		setVisualLow(toVisualValue(getSelectedMinimum()));
		setVisualHigh(toVisualValue(getSelectedMaximum()));
	}

	/**
	 * @param e
	 */
	protected void paintCanvas(PaintEvent e) {
		Rectangle bounds = canvas.getClientArea();

		Color black = getDisplay().getSystemColor(SWT.COLOR_BLACK);
		GC gc = e.gc;
		gc.setBackground(getBackground());
		gc.fillRectangle(bounds);
		gc.setAntialias(SWT.OFF);
		//paint the edges the same colour as the parent, so that it
		//is apparent that they don't belong to the range of this slider.
		gc.setBackground(getParent().getBackground());
		gc.fillRectangle(bounds.x, bounds.y, HANDLE_SIZE, bounds.height);
		gc.fillRectangle(bounds.x + bounds.width-HANDLE_SIZE, bounds.y, HANDLE_SIZE, bounds.height);
		
		//the actual area for the range will actually be a few pixels in from the
		//sides so that there will be room for the small arrow handles
		int visualLow = getVisualLow(); 
		int visualHigh = getVisualHigh(); 
		
		
		
		gc.setBackground(getForeground());
		
		paintRanges(gc, bounds);
		gc.setAlpha(100);
		gc.setForeground(getForeground());
		gc.setBackground(getForeground());
		//draw a rectangle for the selected range
		gc.fillRectangle(visualLow+bounds.x, bounds.y, visualHigh-visualLow, bounds.height);
		gc.setForeground(black);
		gc.setBackground(black);
		
		//draw triangle handles;
		gc.setAlpha(255);
		//the minimum handles
		gc.fillPolygon(new int[]{
			visualLow + bounds.x - HANDLE_SIZE, bounds.y,
			visualLow + bounds.x, bounds.y,
			visualLow + bounds.x, bounds.y+HANDLE_SIZE
		});
		
		gc.fillPolygon(new int[]{
			visualLow + bounds.x - HANDLE_SIZE, bounds.y+bounds.height,
			visualLow + bounds.x, bounds.y+bounds.height,
			visualLow + bounds.x, bounds.y+bounds.height-HANDLE_SIZE
		});
		
		//the max handles
		gc.fillPolygon(new int[]{
			visualHigh + bounds.x + HANDLE_SIZE, bounds.y,
			visualHigh + bounds.x, bounds.y,
			visualHigh + bounds.x, bounds.y+HANDLE_SIZE+1
		});
			
		gc.fillPolygon(new int[]{
			visualHigh + bounds.x + HANDLE_SIZE, bounds.y+bounds.height,
			visualHigh + bounds.x, bounds.y+bounds.height,
			visualHigh + bounds.x, bounds.y+bounds.height-HANDLE_SIZE-1
		});
		
		//draw separater lines
		gc.drawLine(visualLow, bounds.y, visualLow, bounds.y+bounds.height);
		gc.drawLine(visualHigh-1, bounds.y, visualHigh-1, bounds.y+bounds.height);
		
	}
	
	
	
	/**
	 * @param gc
	 */
	private void paintRanges(GC gc, Rectangle bounds) {
		//draw a rectangle for each range
		gc.setAlpha(200);
		for (RangeAnnotation a : items) {
			if (a == selectedAnnotation) {
				continue;
			}
			if (a.isDisposed()) continue;
			int visualLow = toVisualValue(a.getOffset()) + bounds.x;
			int visualHigh = toVisualValue(a.getLength()) + visualLow;
			if (visualLow < bounds.x + HANDLE_SIZE) {
				visualLow = bounds.x + HANDLE_SIZE;
			}
			if (visualHigh > bounds.x + bounds.width - HANDLE_SIZE) {
				visualHigh = bounds.x + bounds.width - HANDLE_SIZE;
			}
			Color fg = a.getForeground();
			if (a == selectedAnnotation) {
				fg = gc.getDevice().getSystemColor(SWT.COLOR_WHITE);
			}
			if (fg == null) {
				fg = getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
			}
			Color bg = a.getBackground();
			if (bg == null) {
				bg = getDisplay().getSystemColor(SWT.COLOR_GRAY);
			}
			gc.setForeground(fg);
			gc.setBackground(bg);
			gc.fillRectangle(visualLow, bounds.y, visualHigh-visualLow, bounds.height-1);
			gc.drawRectangle(visualLow, bounds.y, visualHigh-visualLow-1, bounds.height-1);
		}
		//paint the selected annotation
		if (selectedAnnotation != null) {
			RangeAnnotation a = selectedAnnotation;
			if (a.isDisposed()) return;
			int visualLow = toVisualValue(a.getOffset()) + bounds.x;
			int visualHigh = toVisualValue(a.getLength()) + visualLow;
			if (visualLow < bounds.x + HANDLE_SIZE) {
				visualLow = bounds.x + HANDLE_SIZE;
			}
			if (visualHigh > bounds.x + bounds.width - HANDLE_SIZE) {
				visualHigh = bounds.x + bounds.width - HANDLE_SIZE;
			}
			Color fg =  gc.getDevice().getSystemColor(SWT.COLOR_WHITE);
			if (fg == null) {
				fg = getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
			}
			Color bg = a.getBackground();
			if (bg == null) {
				bg = getDisplay().getSystemColor(SWT.COLOR_GRAY);
			}
			gc.setForeground(fg);
			gc.setBackground(bg);
			gc.fillRectangle(visualLow, bounds.y, visualHigh-visualLow, bounds.height-1);
			gc.drawRectangle(visualLow, bounds.y, visualHigh-visualLow-1, bounds.height-1);
		}
	}
	
	/**
	 * Returns the item under the given point. The point is in display coordinates, relative
	 * to this composite.
	 * @param p the point query.
	 * @return an annotation under that point, or null if none.
	 */
	public RangeAnnotation itemAt(Point p) {
		checkWidget();
		RangeAnnotation winner = null;
		//first, look for all the ones that are in the pixel range
		RangeAnnotation[] currentRanges = items.toArray(new RangeAnnotation[items.size()]);
		LinkedList<RangeAnnotation> annotationsToScore = new LinkedList<RangeAnnotation>();
		for (RangeAnnotation a : currentRanges) {
			int low = toVisualValue(a.getOffset());
			int high = toVisualValue(a.getLength()+a.getOffset());
			if (low == high) {
				high++;
			}
			if (low <= p.x && high >= p.x) {
				annotationsToScore.add(a);
			}
		}
		long score = -1;
		long rangeValue = toRangeValue(p.x);
		for (RangeAnnotation a : annotationsToScore) {
			long localScore = 0;
			long leftDiff = Math.abs(rangeValue - a.getOffset());
			long rightDiff = Math.abs((a.getOffset() + a.getLength()) - rangeValue);
			//normalize to 0 because the scaling might be huge
			localScore = rightDiff + leftDiff;
			if (score == -1 || localScore < score) {
				winner = a;
				score = localScore;
			}
		}
		return winner;
	}
	
	public RangeAnnotation[] getRanges() {
		return items.toArray(new RangeAnnotation[items.size()]);
	}

	/**
	 * @return
	 */
	int getVisualHigh() {
		return visualHigh;
	}

	/**
	 * @return
	 */
	int getVisualLow() {
		return visualLow;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#setBackground(org.eclipse.swt.graphics.Color)
	 */
	@Override
	public void setBackground(Color color) {
		super.setBackground(color);
	}
	
	/**
	 * @return the minimum value for the range
	 */
	public long getMinimum() {
		return min;
	}
	
	/**
	 * Sets the minimum value of the range.
	 * @param l the min to set
	 */
	public void setMinimum(long l) {
		checkWidget();
		this.min = l;
		resetScale();
		setVisualHigh(toVisualValue(getMaximum()));
		setVisualLow(toVisualValue(getMinimum()));
		canvas.redraw();
	}
	
	/**
	 * @return the maximum value of the range
	 */
	public long getMaximum() {
		return max;
	}
	
	/**
	 * Sets the maximum value of the range.
	 * @param max the new maximum
	 */
	public void setMaximum(long max) {
		checkWidget();
		this.max = max;
		resetScale();
		setVisualHigh(toVisualValue(getMaximum()));
		setVisualLow(toVisualValue(getMinimum()));
		canvas.redraw();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#redraw(int, int, int, int, boolean)
	 */
	@Override
	public void redraw(int x, int y, int width, int height, boolean all) {
		super.redraw(x, y, width, height, all);
		if (!all) {
			//make sure that the canvas is redrawn as well
			canvas.redraw();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#redraw()
	 */
	@Override
	public void redraw() {
		super.redraw();
		canvas.redraw();
	}
	
	/**
	 * Sets the minimum selected value for the range.
	 * @param rangeMin the new selected value;
	 */
	public void setSelectedMinimum(long rangeMin) {
		checkWidget();
		if (rangeMin < min) {
			rangeMin = min;
		} else if (rangeMin > this.rangeMax) {
			rangeMin = this.rangeMax;
		}
		this.rangeMin = rangeMin;
		setVisualLow(toVisualValue(rangeMin));
		canvas.redraw();
	}
	
	/**
	 * @param visualValue
	 */
	private void setVisualLow(int visualValue) {
		Rectangle bounds = canvas.getClientArea();
		int limit = bounds.x + HANDLE_SIZE;
		if (visualValue < limit) {
			visualValue = limit;
		}
		visualLow = visualValue;
	}

	/**
	 * Sets the current maximum selected value for the range.
	 * @param rangeMax the new selected value.
	 */
	public void setSelectedMaximum(long rangeMax) {
		checkWidget();
		if (rangeMax > max) {
			rangeMax = max;
		} else if (rangeMax < this.rangeMin) {
			rangeMax = this.rangeMin;
		}
		this.rangeMax = rangeMax;
		setVisualHigh(toVisualValue(rangeMax));
		canvas.redraw();
	}
	
	/**
	 * @param rangeMax2
	 * @return
	 */
	private int toVisualValue(long rangeValue) {
		if (scale == 0.0) {
			return 0;
		}
		long highOffset = rangeValue - getMinimum();
		return (int)(Math.round(scale*highOffset) + canvas.getClientArea().x+HANDLE_SIZE);
	}

	private void setVisualHigh(int visualValue) {
		Rectangle bounds = canvas.getClientArea();
		int limit = bounds.x + bounds.width - HANDLE_SIZE;
		if (visualValue > limit) {
			visualValue = limit;
		}
		this.visualHigh = visualValue;
	}
	
	/**
	 * @return the selected maximum value for the range
	 */
	public long getSelectedMaximum() {
		return rangeMax;
	}
	
	/**
	 * @return the selected minumum value for the range
	 */
	public long getSelectedMinimum() {
		return rangeMin;
	}
	
	Canvas getCanvas() {
		return canvas;
	}

	/**
	 * Converts the given 
	 * @param x
	 * @return
	 */
	public long toRangeValue(int x) {
//		if (x <= getVisualLow()) {
//			x -= HANDLE_SIZE;
//		} else if (x >= getVisualHigh()) {
//			x -= HANDLE_SIZE*2;
//		}
		Rectangle bounds = getCanvas().getClientArea();
		if (scale == 0.0) {
			return 0;
		}
		return Math.round((x-bounds.x+HANDLE_SIZE)/scale) + getMinimum();
	}
	
	/**
	 * Adds the given listener to listen for when the minimum and maximum values change, or
	 * when an annotation is selected in the receiver. Clients can tell the difference by 
	 * querying the width and height values of the selection event. If they are negative, then
	 * the selected annotation has changed. Otherwise, the selected range has changed.
	 * On the selection change event, the bounds will contain the rectangle bounds that
	 * the minimum and maximum values are visualized in. In order to get the actual values,
	 * clients should cast the event's widget to a RangeSlider and use
	 * {@link #getSelectedMaximum()} and {@link #getSelectedMinimum()}.
	 * @param listener
	 */
	public void addSelectionListener(SelectionListener listener) {
		checkWidget();
		if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
		TypedListener tl = new TypedListener(listener);
		addListener (SWT.Selection,tl);
		addListener (SWT.DefaultSelection,tl);
	}

	/**
	 * @param rangeValue
	 */
	void internalSetSelectedMinimum(long min) {
		setSelectedMinimum(min);
		fireSelectionChanged();
	}
	
	void internalSetVisualMinimum(int min) {
		setVisualLow(min);
		rangeMin = toRangeValue(getVisualLow());
		fireSelectionChanged();
		canvas.redraw();
	}

	/**
	 * @param rangeValue
	 */
	public void internalSetSelectedMaximum(long max) {
		setSelectedMaximum(max);
		fireSelectionChanged();
	}
	
	void internalSetVisualMaximum(int max) {
		setVisualHigh(max);
		rangeMax = toRangeValue(getVisualHigh());
		fireSelectionChanged();
		canvas.redraw();
	}

	/**
	 * 
	 */
	private void fireSelectionChanged() {
		Event event = new Event();
		event.button = 1;
		Rectangle bounds = getCanvas().getBounds();
		int visualLow = getVisualLow(); 
		int visualHigh = getVisualHigh();
		event.x = visualLow+bounds.x;
		event.y = bounds.y;
		event.width = visualHigh-visualLow;
		event.height = bounds.height;
		event.item = null;
		notifyListeners(SWT.Selection, event);
	}
	
	private void fireItemSelectionChanged(int button) {
		Event event = new Event();
		event.button = button;
		event.x = 0;
		event.y =0;
		event.width = -1;
		event.height = -1;
		event.item = selectedAnnotation;
		event.data = (selectedAnnotation !=null) ?
				selectedAnnotation.getData() :
					null;
		notifyListeners(SWT.Selection, event);
	}

	/**
	 * Creates a new annotation for this item
	 * @param rangeAnnotation
	 */
	void createItem(RangeAnnotation rangeAnnotation) {
		items.add(rangeAnnotation);
		rangeAnnotation.addDisposeListener(itemDisposedListener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Composite#layout(boolean, boolean)
	 */
	@Override
	public void layout(boolean changed, boolean all) {
		super.layout(changed, all);
		resetScale();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Composite#layout(org.eclipse.swt.widgets.Control[])
	 */
	@Override
	public void layout(Control[] changed) {
		super.layout(changed);
		resetScale();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#setMenu(org.eclipse.swt.widgets.Menu)
	 */
	@Override
	public void setMenu(Menu menu) {
		canvas.setMenu(menu);
	}

	/**
	 * Updates the selection for the given mouse event.
	 * @param e
	 */
	void updateSelection(MouseEvent e) {
		Item item = itemAt(new Point(e.x, e.y));
		if (item != selectedAnnotation) {
			if (item instanceof RangeAnnotation) {
				selectedAnnotation = (RangeAnnotation) item;
			} else {
				selectedAnnotation = null;
			}
			redraw();
			fireItemSelectionChanged(e.button);
		}
	}

	/**
	 * @return the index of the selected range item, or -1 if none selected.
	 */
	public int getSelectionIndex() {
		checkWidget();
		return getIndex(selectedAnnotation);
	}

	/**
	 * Returns the index at which the given annotation exists in the range.
	 * -1 if the item is disposed, or doesn't exist in this slider. 
	 * @param item
	 * @return the index at which the given annotation exists in the range.
	 * -1 if the item is disposed, or doesn't exist in this slider. 
	 */
	public int getIndex(RangeAnnotation item) {
		checkWidget();
		if (item == null || item.isDisposed()) {
			return -1;
		}
		return (items.indexOf(item));
	}
	
	/**
	 * Returns the annotation at the given index. Null is returned if the index
	 * is out of range.
	 * @param index the index of the annotation to return.
	 * @return the annotation at the given index. Null is returned if the index
	 * is out of range.
	 */
	public RangeAnnotation getItem(int index) {
		if (index < 0 || index >= items.size()) {
			return null;
		}
		return items.get(index);
	}

	/**
	 * Sets the selected annotation to the annotation at the given index. 
	 * Returns true if the annotation could be selected, or false otherwise.
	 * @param index the index of the annotation to select.
	 * @return true if the annotation could be selected, or false otherwise.
	 */
	public boolean setSelectionIndex(int index) {
		checkWidget();
		if (index < 0 || index >= items.size()) {
			return false;
		}
		RangeAnnotation newSelection = items.get(index);
		if (newSelection == selectedAnnotation) return true;
		selectedAnnotation = newSelection;
		redraw();
		fireItemSelectionChanged(1);
		return true;
	}
		
}
