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
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.zest.custom.sequence.widgets.internal.ThrownErrorDialog;

/**
 * A progress monitor dialog that will update progress in a runnable. We have to use custom
 * interfaces in order to remove dependencies from the Eclipse runtime.
 * @author Del Myers
 *
 */
public class DelayedProgressMonitorDialog extends IconAndMessageDialog implements IUIProgressService {
	/**
	 * Constants for label and monitor size
	 */
	private static int LABEL_DLUS = 21;

	private static int BAR_DLUS = 9;
	private ProgressIndicator progressIndicator;
	private Label subTaskLabel;

	private Button cancel;

	private Cursor arrowCursor;

	private boolean enableCancelButton;

	private int work;

	private double worked;

	volatile boolean isOpenned;

	private SimpleProgressMonitor monitor;
	

	public DelayedProgressMonitorDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public void setSubTask(String messageString) {
		if (getContents() == null) return;
		// must not set null text in a label
		String message = messageString == null ? "" : messageString; //$NON-NLS-1$
		if (messageLabel == null || messageLabel.isDisposed()) {
			return;
		}
		if (subTaskLabel.isVisible()) {
			subTaskLabel.setToolTipText(message);
			subTaskLabel.setText(shortenText(message, subTaskLabel));
		}
	}

	public void setMainTask(String task) {
		if (getContents() == null) return;
		setMessage(task, true);		
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setMessage("", false);
		createMessageArea(parent);
		// Only set for backwards compatibility
		// progress indicator
		progressIndicator = new ProgressIndicator(parent);
		GridData gd = new GridData();
		gd.heightHint = convertVerticalDLUsToPixels(BAR_DLUS);
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		progressIndicator.setLayoutData(gd);
		// label showing current task
		subTaskLabel = new Label(parent, SWT.LEFT | SWT.WRAP);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = convertVerticalDLUsToPixels(LABEL_DLUS);
		gd.horizontalSpan = 2;
		subTaskLabel.setLayoutData(gd);
		subTaskLabel.setFont(parent.getFont());
		return parent;
	}
	
	/**
	 * Set the message in the message label.
	 * 
	 * @param messageString
	 *            The string for the new message.
	 * @param force
	 *            If force is true then always set the message text.
	 */
	private void setMessage(String messageString, boolean force) {
		// must not set null text in a label
		message = messageString == null ? "" : messageString; //$NON-NLS-1$
		if (messageLabel == null || messageLabel.isDisposed()) {
			return;
		}
		if (force || messageLabel.isVisible()) {
			messageLabel.setToolTipText(message);
			messageLabel.setText(shortenText(message, messageLabel));
		}
	}
	
	void setWork(int work) {
		if (getShell().isDisposed()) return;
		if (work == IUIProgressService.UNKNOWN_WORK) {
			progressIndicator.beginAnimatedTask();
		} else {
			progressIndicator.beginTask(work);
		}
		this.work = work;
	}
	
	public void setWorked(int totalWork) {
		if (getContents() == null) return;
		if (getShell() == null || getShell().isDisposed()) {
			return;
		}
		if (totalWork == WORKED_DONE) {
			if (isOpenned) {
				close();
			}
			return;
		}
		if (totalWork > work) {
			progressIndicator.sendRemainingWork();
		} else {
			double diff = totalWork-this.worked;
			progressIndicator.worked(diff);
		}
	}
	
	/*
	 * (non-Javadoc) Method declared on Dialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// cancel button
		createCancelButton(parent);
	}

	/**
	 * Creates the cancel button.
	 * 
	 * @param parent
	 *            the parent composite
	 * @since 3.0
	 */
	protected void createCancelButton(Composite parent) {
		cancel = createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, true);
		if (arrowCursor == null) {
			arrowCursor = new Cursor(cancel.getDisplay(), SWT.CURSOR_ARROW);
		}
		cancel.setCursor(arrowCursor);
		cancel.setEnabled(enableCancelButton);
//		if (enableCancelButton) {
//			cancel.addSelectionListener(new SelectionListener(){
//				@Override
//				public void widgetDefaultSelected(SelectionEvent e) {
//					cancel();
//				}
//
//				@Override
//				public void widgetSelected(SelectionEvent e) {
//					cancel();
//				}});
//		}
	}
	

	@Override
	protected Image getImage() {
		return getInfoImage();
	}

	
	public void runInUIThread(final AbstractSimpleProgressRunnable runnable, boolean enableCancelButton) throws InvocationTargetException {
		setBlockOnOpen(false);
		if (Display.getCurrent() == null) {
			throw new SWTException(SWT.ERROR_THREAD_INVALID_ACCESS);
		}
		final Display finalDisplay = Display.getCurrent();
		this.enableCancelButton = enableCancelButton;
		create();
		this.monitor = new SimpleProgressMonitor(this, enableCancelButton);
		new Timer("Delayed Progress Service").schedule(new TimerTask(){
			public void run() {
				finalDisplay.asyncExec(new Runnable(){
					public void run() {
						if (!(monitor.isCancelled() || monitor.isDone())) {
							open();
						}
					}
				});
			}
		}, 1500);
		progressIndicator.beginAnimatedTask();
		runnable.runInUIThread(monitor);
		if (isOpenned) {
			close();
		}
	}
	
	@Override
	protected void cancelPressed() {
		if (monitor != null && !monitor.isCancelled()) {
			monitor.cancel();
		}
		super.cancelPressed();
	}
	
	@Override
	public synchronized int open() {
		this.isOpenned = true;
		return super.open();
	}
	
	@Override
	public synchronized boolean close() {
		this.isOpenned = false;
		return super.close();
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Working...");
	}

	public void setTask(String taskName, int totalWork) {
		if (getContents() == null) return;
		setMainTask(taskName);
		setWork(totalWork);
	}

	public void setTaskName(String taskName) {
		if (getContents() == null) return;
		setMainTask(taskName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.internal.IUIProgressService#handleException(java.lang.Throwable)
	 */
	public void handleException(Throwable t) {
		new ThrownErrorDialog(getShell()).open(t);		
	}
	

}
