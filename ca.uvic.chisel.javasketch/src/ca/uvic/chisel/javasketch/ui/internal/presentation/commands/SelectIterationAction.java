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
package ca.uvic.chisel.javasketch.ui.internal.presentation.commands;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.internal.ast.groups.ASTMessageGroupingTree;
import ca.uvic.chisel.javasketch.ui.internal.presentation.IJavaSketchPresenter;
import ca.uvic.chisel.javasketch.ui.internal.presentation.metadata.PresentationData;

/**
 * A simple action to select the iteration of a loop
 * @author Del Myers
 *
 */
public class SelectIterationAction extends Action {
	
	private class SelectActivationDialog extends Dialog {
		private Text input;
		private Label errorLabel;
		protected int iteration;

		/**
		 * @param parentShell
		 */
		protected SelectActivationDialog(IShellProvider parentShell) {
			super(parentShell);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		protected Control createDialogArea(Composite parent) {
			Composite control = (Composite) super.createDialogArea(parent);
			Composite page = new Composite(control, SWT.NONE);
			page.setLayout(new GridLayout(2, false));
			errorLabel = new Label(page, SWT.NONE);
			errorLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan=2;
			errorLabel.setLayoutData(gd);
			errorLabel.setText("");
			Label l = new Label(page, SWT.NONE);
			l.setText("Select iteration (1 to " + maxIterations + "): ");
			l.setLayoutData(new GridData());
			input = new Text(page, SWT.SINGLE);
			gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.widthHint = 50;
			input.setLayoutData(gd);
			input.addVerifyListener(new VerifyListener() {
				
				@Override
				public void verifyText(VerifyEvent e) {
					if (!checkText()) {
						//e.doit =false;
					}
					
				}

				
			});
			
			
			input.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					if (checkText()) {
						iteration = Integer.parseInt(input.getText());
					}
				}
			});
			
			return control;
		}
		
		private boolean checkText() {
			try {
				int iteration = Integer.parseInt(input.getText().trim());
				if (iteration < 1 || iteration > maxIterations) {
					errorLabel.setText("Value must be between 1 and " +maxIterations);
					return false;
				}
			} catch (NumberFormatException ex) {
				errorLabel.setText("Must enter an integer");
				return false;
			}
			errorLabel.setText("");
			return true;
		}
		
		/**
		 * @return the iteration
		 */
		public int getIteration() {
			return iteration;
		}
	
	}
	
	private IActivation activation;
	private ASTMessageGroupingTree[] iterations;
	private int maxIterations;
	private IJavaSketchPresenter presenter;

	public SelectIterationAction(IActivation activation, ASTMessageGroupingTree[] iterations, IJavaSketchPresenter presenter) {
		this.activation = activation;
		this.iterations = iterations;
		this.maxIterations = iterations.length;
		this.presenter = presenter;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		SelectActivationDialog dialog = new SelectActivationDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		dialog.setBlockOnOpen(true);
		int result = dialog.open();
		if (result == Dialog.OK) {
			int iteration = dialog.getIteration();
			PresentationData pd = PresentationData.connect(SketchPlugin.getDefault().getSketch(activation));
			if (pd != null) {
				pd.swapLoop(activation, iterations[iteration-1], true);
				pd.disconnect();
				presenter.getSequenceChartViewer().refresh(activation);
				presenter.resetExpansionStates(activation);
				
			}
		}
		super.run();
	}

}
