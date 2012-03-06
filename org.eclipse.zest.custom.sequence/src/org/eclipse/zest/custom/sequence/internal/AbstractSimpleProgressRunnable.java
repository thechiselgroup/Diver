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
package org.eclipse.zest.custom.sequence.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Display;

/**
 * An interface to run long-running processes in a dialog with. The runnable is designed to be run in the
 * UI thread only. In order to prevent UI blocking in long-running processes (especially when run within
 * a DelayedProgressMonitorDialog, or by using the UIJobProcessor), extenders should make frequent calls
 * to the readAndDispatch() method to give the UI an opportunity to update.
 * 
 * @author Del Myers
 *
 */
public abstract class AbstractSimpleProgressRunnable {
	
	private boolean cancelled;
	private SimpleProgressMonitor monitor;

	public final void runInUIThread(SimpleProgressMonitor monitor) throws InvocationTargetException {
		if (this.cancelled == true) {
			//the monitor might ignore this.
			monitor.cancel();
		}
		this.monitor = monitor;
		doRunInUIThread(monitor);
		this.monitor = null;
	}
	
	
	/**
	 * Performs the operation for this runnable. Will be run in the UI thread.
	 * @param monitor
	 * @throws InvocationTargetException
	 */	
	protected abstract void doRunInUIThread(SimpleProgressMonitor monitor) throws InvocationTargetException;
	/**
	 * Updates the UI from within the dialog. Should be called periodically within he runInUIThread method
	 * in order to update the ui. Clients may override, but must call this implementation first.
	 */
	protected void readAndDispatch() {
		Display disp = Display.getCurrent();
		while (disp != null && !disp.isDisposed() && disp.readAndDispatch());
	}
	/**
	 * Returns the family of runnables that this runnable belongs to. Allows the UIJobProcessor to cancel
	 * jobs that are of the same family. May return null. Clients should override to set the family. Returns
	 * null by default.
	 * @return
	 */
	public Object getFamily() {
		return null;
	}
	
	/**
	 * Cancels the current runnable if it is running.
	 */
	public void cancel() {
		this.cancelled = true;
		if (this.monitor != null) {
			monitor.cancel();
		}
	}
}