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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.operations.TimeTriggeredProgressMonitorDialog;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.zest.custom.sequence.widgets.UMLSequenceChart;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;

/**
 * @author Del Myers
 *
 */
@SuppressWarnings("restriction")
public class NavigateToCodeAction extends Action {

	private Object focusElement;
	private UMLSequenceChart chart;
	
	/**
	 * 
	 */
	public NavigateToCodeAction(UMLSequenceChart chart) {
		this.chart = chart;
	}

	public void run() {
		CommandExecuter.execute(FakeCommandHandler.NAVIGATE_TO_CODE_ID, null);
		if (focusElement instanceof ITraceModel) {
			TimeTriggeredProgressMonitorDialog dialog = 
				new TimeTriggeredProgressMonitorDialog(chart.getDisplay().getActiveShell(), 
					1000);
			try {
				dialog.run(true, true, new IRunnableWithProgress(){
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
						IJavaElement element = null;
						try {
							element = JavaSearchUtils.findElement((ITraceModel)focusElement, monitor);
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
						if (element != null) {
							final IJavaElement finalElement = element;
							PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable(){

								@Override
								public void run() {
									try {
										IEditorPart editor = JavaUI.openInEditor(finalElement);
										if (focusElement instanceof IOriginMessage) {
											if (editor instanceof ITextEditor) {
												ITextEditor te = (ITextEditor) editor;
												int line = ((IOriginMessage)focusElement).codeLine()-1;
												if (line >= 0) {
													IDocument doc = te.getDocumentProvider().getDocument(editor.getEditorInput());
													if (doc != null) {
														int offset = doc.getLineOffset(line);
														int length = doc.getLineLength(line);
														te.selectAndReveal(offset, length);
													}
												}
											}
										}
									} catch (PartInitException e) {
										SketchPlugin.getDefault().log(e);
									} catch (JavaModelException e) {
										SketchPlugin.getDefault().log(e);
									} catch (BadLocationException e) {
										SketchPlugin.getDefault().log(e);
									}
								}
							});

						}
					}
				});
			} catch (InvocationTargetException e) {
				SketchPlugin.getDefault().log(e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			
		}
	}
	
	
	
	public void setFocusElement(Object element) {
		this.focusElement = element;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return (focusElement instanceof ITraceModel);
	}
}
