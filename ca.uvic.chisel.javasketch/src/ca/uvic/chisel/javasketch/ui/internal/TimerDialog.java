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
package ca.uvic.chisel.javasketch.ui.internal;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * A simple class that shows a timer that updates every second or so.
 * @author Del Myers
 *
 */
public class TimerDialog extends Window {

	private Object lock;
	private Canvas text;
	private long startTime;
	private long endTime;
	private Composite page;
	private Color black;
	private Color white;
	private TimeUpdateRunnable updater;
	private boolean open;
//	private Timer timer;
//	private TimerTask task;
//	/**
//	 * @param parentShell
//	 */
	protected TimerDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.MODELESS | SWT.ON_TOP | SWT.DIALOG_TRIM);
		lock = new Object();
//		timer = new Timer();
//		task = new TimerTask() {
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				
//			}
//		};
	}
	
	private class TimeUpdateRunnable implements Runnable {
		boolean counting;
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			synchronized (lock) {
				if (text == null || text.isDisposed()) {
					return;
				}
				text.redraw();
				if (counting) {
					text.getDisplay().asyncExec(this);
				}
			}
		}

	}
	
	/**
	 * Sets the text on the shell trim.
	 * @param text
	 */
	public void setShellText(String text) {
		getParentShell().setText(text);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		page = new Composite(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing =0;
		layout.horizontalSpacing = 0;
		page.setLayout(layout);
		text = new Canvas(page, SWT.READ_ONLY | SWT.DOUBLE_BUFFERED);
		black = getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK);
		white = getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE);
		text.setBackground(black);
		text.setForeground(white);
		GridData gd = new GridData();
		gd.heightHint=30;
		gd.widthHint=150;
		text.setLayoutData(gd);
		FontData fd = new FontData("sans-serif", 15, SWT.NORMAL);
		final Font f = new Font(text.getDisplay(), fd);
		text.setFont(f);
		text.addDisposeListener(new DisposeListener(){
			@Override
			public void widgetDisposed(DisposeEvent e) {
				f.dispose();
			}
		});
		text.addPaintListener(new PaintListener() {
			
			@Override
			public void paintControl(PaintEvent e) {
				long currentTime = endTime;
				if (currentTime < 0) {
					currentTime = System.currentTimeMillis();
				}
				long diffTime = currentTime - startTime;
				long secondTime = 1000;
				long minuteTime = secondTime * 60;
				long hourTime = minuteTime * 60;
				long dayTime = hourTime * 24;
				long days = diffTime/dayTime;
				long remainder = diffTime % dayTime;
				long hours = remainder / hourTime;
				remainder = remainder % hourTime;
				long minutes = remainder / minuteTime;
				remainder = remainder % minuteTime;
				long seconds = remainder / secondTime;
				remainder = remainder % secondTime;
				GC gc = e.gc;
				gc.setBackground(black);
				gc.setForeground(black);
				gc.fillRectangle(0,0,text.getBounds().width, text.getBounds().height);
				gc.setForeground(white);
				gc.drawText(days + ":" + hours + ":" + minutes + ":" + seconds + "." + remainder,0,0);
			}
		});
		
		return page;
	}
	
	/**
	 * Opens the window and resets the timer, if requested.
	 */
	public int open(boolean reset) {
		if (reset) {
			reset();
		}
		return open();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#open()
	 */
	@Override
	public int open() {
		open = true;
		return super.open();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#close()
	 */
	@Override
	public boolean close() {
		open = false;
		return super.close();
	}

	/**
	 * Resets the start time for this dialog to the current time.
	 */
	public void reset() {
		synchronized (lock) {
			setStartTime(System.currentTimeMillis());
		}
	}
	
	/**
	 * Sets the start time of this dialog to the given time.
	 * @param timeInMillis
	 */
	public void setStartTime(long timeInMillis) {
		synchronized (lock) {
			if (updater != null) {
				//cancel pending redraws.
				updater.counting = false;
			}
			updater = new TimeUpdateRunnable();
			updater.counting = true;
			startTime = timeInMillis;
			//so that the redrawer knows that it is counting.
			endTime = -1;
			if (getShell() != null) {
				getShell().getDisplay().asyncExec(updater);
			}
			
		}
	}
	
	/**
	 * Sets the final end time of the dialog to the given end,
	 * @param timeInMillis
	 */
	public void setEndTime(long timeInMillis) {
		synchronized (lock) {
			if (updater != null) {
				updater.counting = false;
				updater = null;
			}
			updater = new TimeUpdateRunnable();
			updater.counting = false;
			endTime = timeInMillis;
			if (getShell() != null) {
				getShell().getDisplay().asyncExec(updater);
			}
		}
	}


	/**
	 * Returns true if this window is open and visible.
	 * @return
	 */
	public boolean isOpen() {
		return open;
	}
	
		
	
}
