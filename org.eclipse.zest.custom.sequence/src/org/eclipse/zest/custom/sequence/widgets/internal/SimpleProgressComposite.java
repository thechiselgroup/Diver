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
package org.eclipse.zest.custom.sequence.widgets.internal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.zest.custom.sequence.internal.IUIProgressService;

public class SimpleProgressComposite extends Composite {
	private Label messageLabel;
	private ProgressBar determinedProgress;
	private ProgressBar undeterminedProgress;
	private FontMetrics fontMetrics;
	private String subTask;
	private String taskName;
	private Composite progressContainer;
	private StackLayout progressLayout;
	//private int workSoFar;
	


	public SimpleProgressComposite(Composite p) {
		super(p, SWT.BORDER);
		setLayout(new GridLayout(2, false));
		setFont(p.getFont());
		GC gc = new GC(this);
		this.fontMetrics = gc.getFontMetrics();
		gc.dispose();
		
		
		// Only set for backwards compatibility
		// progress indicator
		
		GridData gd = new GridData(SWT.FILL, GridData.CENTER, true, true);
		//gd.heightHint = convertVerticalDLUsToPixels(BAR_DLUS);
		progressContainer = new Composite(this, SWT.NONE);
		gd.horizontalSpan=2;
		gd.heightHint=15;
		gd.minimumHeight=10;
		gd.widthHint=40;
		progressContainer.setLayoutData(gd);
		this.progressLayout = new StackLayout();
		progressContainer.setLayout(progressLayout);
		
		this.determinedProgress = new ProgressBar(progressContainer, SWT.HORIZONTAL);
		this.undeterminedProgress = new ProgressBar(progressContainer, SWT.HORIZONTAL | SWT.INDETERMINATE);
		progressLayout.topControl=undeterminedProgress;
		this.undeterminedProgress.setMaximum(0);
		this.messageLabel = new Label(this, SWT.NONE);
		messageLabel.setText("Working...");
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		//gd.heightHint = convertVerticalDLUsToPixels(LABEL_DLUS);
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		messageLabel.setLayoutData(gd);
	}
	
	
	protected int convertVerticalDLUsToPixels(int dlus) {
		// test for failure to initialize for backward compatibility
		if (fontMetrics == null) {
			return 0;
		}
		return Dialog.convertVerticalDLUsToPixels(fontMetrics, dlus);
	}

	
	public void setSubTask(String taskName) {
		this.subTask = taskName;
		updateLabels();
	}

	
	public void setTask(String taskName, int totalWork) {
		setTaskName(taskName);
		if (totalWork == IUIProgressService.UNKNOWN_WORK) {
			determinedProgress.setMaximum(0);
			progressLayout.topControl=undeterminedProgress;
		} else {
			determinedProgress.setMaximum(totalWork);
			progressLayout.topControl=determinedProgress;
		}
		determinedProgress.setSelection(0);
		layout(true);
	}

	
	public void setTaskName(String taskName) {
		this.taskName = taskName;
		updateLabels();
	}

	
	/**
	 * 
	 */
	private void updateLabels() {
		String label = (taskName != null) ? taskName : "";
		label += (subTask != null) ? "..." + subTask : "";
		messageLabel.setText(label);
	}

	public void setWorked(int work) {
		if (isDisposed() || Display.getCurrent() == null) return;
		if (work >= 0) {
			if (progressLayout.topControl == determinedProgress) {
				if (work > determinedProgress.getMaximum()) {
					work = determinedProgress.getMaximum();
				}
				determinedProgress.setSelection(work);
			}
		} else if (work == IUIProgressService.WORKED_DONE) {
			if (!(progressLayout.topControl == determinedProgress)) {
				progressLayout.topControl = determinedProgress;
				layout(true);
			}
			determinedProgress.setSelection(determinedProgress.getMaximum());
		} else if (work == IUIProgressService.UNKNOWN_WORK) {
			determinedProgress.setSelection(0);
			progressLayout.topControl = undeterminedProgress;
			layout(true);
		}
	}

	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (isDisposed()) return;
		for (Control child : getChildren()) {
			child.setVisible(visible);
		}
	}

}
