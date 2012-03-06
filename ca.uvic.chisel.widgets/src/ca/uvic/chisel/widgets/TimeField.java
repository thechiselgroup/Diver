/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;

/**
 * A simple widget that presents millisecond time values in hh:mm:ss._ms format.
 * Can't display more than 99 hours.
 * 
 * @author Del Myers
 * 
 */
public class TimeField extends Composite {
	private static final long MAX_TIME = 999 + 1000 * (59 + 60 * (59 + 60 * 24));

	private class FieldModifyListener extends MouseAdapter implements
			KeyListener, FocusListener {

		private int focusTime;

		/*
		 * (non-Javadoc)
		 * @see
		 * java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseDown(MouseEvent e) {
			if (e.time == focusTime) {
				((Text) e.widget).selectAll();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.
		 * events.ModifyEvent)
		 */
		public void keyReleased(KeyEvent e) {
			if (e.keyCode == '\r') {
				doUpdate();
			}
		}

		private void doUpdate() {
			if (updating) {
				return;
			}

			// set the new time
			long hour = 0;
			long minute = 0;
			long second = 0;
			long ms = 0;
			try {
				ms += Long.parseLong(msText.getText());
			} catch (NumberFormatException ex) {

			}
			try {
				second += Long.parseLong(secondText.getText());
			} catch (NumberFormatException ex) {

			}

			try {
				minute += Long.parseLong(minuteText.getText());
			} catch (NumberFormatException ex) {

			}
			try {
				hour += Long.parseLong(hourText.getText());
			} catch (NumberFormatException ex) {

			}
			long time = ms + 1000 * (second + 60 * (minute + 60 * (hour)));

			updateTime(time);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events
		 * .KeyEvent)
		 */
		public void keyPressed(KeyEvent e) {
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.
		 * events.FocusEvent)
		 */
		public void focusGained(FocusEvent e) {
			focusTime = e.time;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events
		 * .FocusEvent)
		 */
		public void focusLost(FocusEvent e) {
			doUpdate();
		}

	}

	private Text hourText;
	private Text minuteText;
	private Text secondText;
	private Text msText;
	private long time;
	private FieldModifyListener modifyListener;
	private boolean updating;
	private static final long SECOND_TIME = 1000;
	private static final long MINUTE_TIME = SECOND_TIME * 60;
	private static final long HOUR_TIME = MINUTE_TIME * 60;

	/**
	 * @param parent
	 * @param style
	 */
	public TimeField(Composite parent, int style) {
		super(parent, style);
		this.modifyListener = new FieldModifyListener();
		GridLayout layout = new GridLayout(7, false);
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		Composite page = this;
		page.setLayout(layout);
		hourText = createText(page, style);
		Label l = new Label(page, SWT.NONE);
		l.setText(":");
		l.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		minuteText = createText(page, style);
		l = new Label(page, SWT.NONE);
		l.setText(":");
		l.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		secondText = createText(page, style);
		l = new Label(page, SWT.NONE);
		l.setText(".");
		l.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		msText = createText(page, style);
		setTime(0);
	}

	/**
	 * @param page
	 * @param style
	 * @return
	 */
	private Text createText(Composite page, int style) {
		// get rid of multiple lines if they are present in the style.
		style = (style | SWT.MULTI | SWT.SINGLE) ^ SWT.MULTI;
		Text t = new Text(page, style);
		t.setLayoutData(new GridData(SWT.END, SWT.END, false, false));
		t.addKeyListener(modifyListener);
		t.addFocusListener(modifyListener);
		t.addMouseListener(modifyListener);
		return t;
	}

	private void updateTime(long time) {
		if (time == getTime()) {
			return;
		}
		setTime(time);
		fireChange();
	}

	/**
	 * 
	 */
	private void fireChange() {
		Event e = new Event();
		e.data = time;
		e.text = "" + time;
		notifyListeners(SWT.Modify, e);
	}

	/**
	 * Sets the time to the given millisecond time.
	 * 
	 * @param time
	 */
	public void setTime(long time) {
		checkWidget();
		if (time > MAX_TIME) {
			time = MAX_TIME;
		}
		this.time = time;
		updateFields();
	}

	/**
	 * 
	 */
	private void updateFields() {
		updating = true;
		long hours = time / HOUR_TIME;
		long remainder = time % HOUR_TIME;
		long minutes = remainder / MINUTE_TIME;
		remainder = remainder % MINUTE_TIME;
		long seconds = remainder / SECOND_TIME;
		remainder = remainder % SECOND_TIME;
		String text = "" + hours;
		text = padWithZeros(text, 2);
		hourText.setText(text);
		text = "" + minutes;
		text = padWithZeros(text, 2);
		minuteText.setText(text);
		text = "" + seconds;
		text = padWithZeros(text, 2);
		secondText.setText(text);
		text = "" + remainder;
		text = padWithZeros(text, 3);
		msText.setText(text);
		updating = false;
	}

	/**
	 * Returns the "hours" field of the time.
	 * 
	 * @param time
	 * @return the "hours" field of the time.
	 */
	public static String getHours(long time) {
		return padWithZeros((time / HOUR_TIME) + "", 2);
	}

	/**
	 * Returns the "minutes" field of the time.
	 * 
	 * @param time
	 * @return the "minutes" field of the time.
	 */
	public static String getMinutes(long time) {
		return padWithZeros(((time % HOUR_TIME) / MINUTE_TIME) + "", 2);
	}

	/**
	 * Returns the "seconds" field of the time.
	 * 
	 * @param time
	 * @return the "seconds" field of the time.
	 */
	public static String getSeconds(long time) {
		return padWithZeros((((time % HOUR_TIME) % MINUTE_TIME) / SECOND_TIME)
				+ "", 2);
	}

	/**
	 * Returns the "milliseconds" field of the time.
	 * 
	 * @param time
	 * @return the "milliseconds" field of the time.
	 */
	public static String getMillis(long time) {
		return padWithZeros(
			((((time % HOUR_TIME) % MINUTE_TIME) % SECOND_TIME)) + "", 3);
	}

	/**
	 * @return a string representation of the given time.
	 */
	public static String toString(long time) {
		return getHours(time) + ":" + getMinutes(time) + ":" + getSeconds(time)
				+ "." + getMillis(time);
	}

	/**
	 * @param text
	 * @param i
	 */
	private static String padWithZeros(String text, int i) {
		while (text.length() < i) {
			text = 0 + text;
		}
		return text;

	}

	/**
	 * @return
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Adds a listener for when the value of the time has changed. This sort of
	 * breaks the purpose of a modify listener, but I don't really have any idea
	 * what other kind of listener to put in.
	 * 
	 * @param typedTimeListener
	 */
	public void addModifyListener(ModifyListener listener) {
		checkWidget();
		if (listener == null)
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		TypedListener tl = new TypedListener(listener);
		addListener(SWT.Modify, tl);
	}
}
