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
package ca.uvic.chisel.widgets.test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ca.uvic.chisel.widgets.RangeSlider;
import ca.uvic.chisel.widgets.TimeField;

/**
 * A simple test application to try out the range slider
 * @author Del Myers
 *
 */
public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Display display = new Display ();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(3, false));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 400;
		gd.heightHint = 50;
		final TimeField minTime = new TimeField(shell, SWT.NONE);
		minTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		RangeSlider slider = new RangeSlider(shell, SWT.NONE);
		slider.setForeground(display.getSystemColor(SWT.COLOR_CYAN));
		slider.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		slider.setLayoutData(gd);
		slider.setMinimum(100);
		slider.setMaximum(200000);
		slider.setSelectedMinimum(100);
		slider.setSelectedMaximum(200000);
		final TimeField maxTime = new TimeField(shell, SWT.NONE);
		maxTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		minTime.setTime(slider.getMinimum());
		maxTime.setTime(slider.getMaximum());
		slider.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void widgetSelected(SelectionEvent e) {
				if (minTime.getTime() != e.x) {
					minTime.setTime(e.x);
				}
				if (maxTime.getTime() != e.width) {
					maxTime.setTime(e.width);
				}
			}
			
		});
		shell.pack();
		shell.open ();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();

	}

}
