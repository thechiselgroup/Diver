/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.ui.internal.presentation;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PartInitException;

import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.CommandAction;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.CommandExecuter;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.FakeCommandHandler;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.FocusInHandler;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.RevealActivationHandler;
import ca.uvic.chisel.widgets.RangeAnnotation;

/**
 * A class that creates a hook for opening annotations in a timeline.
 * 
 * @author Del Myers
 * 
 */
public class TimeLineAnnotationHook extends MouseAdapter implements
		IMenuListener {

	private class OpenJavaElementAction extends Action {
		private IJavaElement je;

		public OpenJavaElementAction() {
			setJavaElement(null);
		}

		@Override
		public void run() {
			if (je != null) {
				try {
					CommandExecuter
						.execute(FakeCommandHandler.NAVIGATE_TO_CODE_ID, null);
					JavaUI.openInEditor(je);
				} catch (PartInitException e1) {
				} catch (JavaModelException e1) {
				}
			}
		}

		void setJavaElement(IJavaElement element) {
			this.je = element;
			setEnabled(element != null);
			if (isEnabled()) {
				setText("Open " + element.getElementName());
			} else {
				setText("Open Java Element");
			}
		}

	}

//	private class RevealAction extends Action {
//		private Object activation;
//
//		public RevealAction() {
//		}
//
//		void setActivation(Object activation) {
//			this.activation = activation;
//			setText("Reveal "
//					+ ((ILabelProvider) editor.getSequenceChartViewer()
//						.getLabelProvider()).getText(activation));
//		}
//
//		/*
//		 * (non-Javadoc)
//		 * @see org.eclipse.jface.action.Action#run()
//		 */
//		@Override
//		public void run() {
//			//editor.reveal((IActivation)activation);
//		}
//		/* (non-Javadoc)
//		 * @see org.eclipse.jface.action.Action#runWithEvent(org.eclipse.swt.widgets.Event)
//		 */
//		@Override
//		public void runWithEvent(Event event) {
//			IHandlerService hs = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
//			ICommandService cs = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
//			Command command = cs.getCommand(RevealActivationHandler.COMMAND_ID);
//			IActivation a = (IActivation) activation;
//			if (command != null && activation != null) {
//				Map<String, String> parameters = new HashMap<String, String>();
//				parameters.put(RevealActivationHandler.TRACE_PARAMETER, a.getTrace().getIdentifier());
//				parameters.put(RevealActivationHandler.THREAD_PARAMETER, a.getThread().getIdentifier());
//				ParameterizedCommand pc = ParameterizedCommand.generateCommand(command, parameters);
//				try {
//					hs.executeCommand(pc, event);
//				} catch (CommandException e) {
//					SketchPlugin.getDefault().log(e);
//				} 
//			}
//			
//			super.runWithEvent(event);
//		}
//	}

	private IJavaSketchPresenter editor;
//	private IContributionItem focusInAction;
	private OpenJavaElementAction openJEAction;

	/**
	 * Adds a menu and mouse listeners to the time range to open up markers on
	 * it.
	 * 
	 * @param javaSketchEditor
	 */
	public TimeLineAnnotationHook(IJavaSketchPresenter javaSketchEditor) {
		this.editor = javaSketchEditor;
		createActions();
		hookContextMenu();
		editor.getTimeRange().addMouseListener(this);
	}

	/**
	 * 
	 */
	private void createActions() {
//		this.focusInAction = createContributionItem(FocusInHandler.COMMAND_ID, null);
		this.openJEAction = new OpenJavaElementAction();
	}

//	private CommandContributionItem createContributionItem(String commandId, Map params) {
//		CommandContributionItemParameter parameters = new CommandContributionItemParameter(
//			SketchPlugin.getDefault().getWorkbench(), null, commandId, SWT.PUSH);
//		parameters.parameters = params;
//		return new CommandContributionItem(parameters);
//	}

	private void hookContextMenu() {
		MenuManager manager = new MenuManager();
		manager.addMenuListener(this);
		manager.setRemoveAllWhenShown(true);
		Menu menu = manager.createContextMenu(editor.getTimeRange());
		editor.getTimeRange().setMenu(menu);
	}

	@Override
	public void menuAboutToShow(IMenuManager manager) {
		Point p = editor.getTimeRange().getDisplay().getCursorLocation();
		p = editor.getTimeRange().toControl(p);
		RangeAnnotation r = editor.getTimeRange().itemAt(p);
		if (r != null) {
			IActivation a = getActivation(r.getData());
			IJavaElement je = getJavaElement(r.getData());
			if (a != null) {
				Map<String, String> params = new HashMap<String, String>();
				params.put(RevealActivationHandler.THREAD_PARAMETER, a.getThread().getIdentifier());
				params.put(RevealActivationHandler.TRACE_PARAMETER, a.getTrace().getIdentifier());
				Action revealAction = new CommandAction(RevealActivationHandler.COMMAND_ID, params);
				revealAction.setText("Reveal " + a.getMethod().getName());
				manager.add(revealAction);
				Action focusInAction = new CommandAction(FocusInHandler.COMMAND_ID, null);
				focusInAction.setText("Focus On " + a.getMethod().getName());
				manager.add(focusInAction);
				openJEAction.setJavaElement(je);
			}
		} else {
			openJEAction.setJavaElement(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.swt.events.MouseAdapter#mouseUp(org.eclipse.swt.events.MouseEvent
	 * )
	 */
	@Override
	public void mouseUp(MouseEvent e) {
		// if (e.button == 1) {
		// RangeAnnotation annotation =editor.getTimeRange().itemAt(new
		// Point(e.x, e.y));
		// if (annotation != null) {
		// //check the key combination
		// if (((e.stateMask & SWT.CTRL) & SWT.SHIFT) != 0) {
		// //open the root
		// IActivation a = getActivation(annotation.getData());
		// if (a != null) {
		// focusInAction.setFocusElement(a);
		// focusInAction.run();
		// }
		// } else if ((e.stateMask & SWT.CTRL) != 0) {
		// IJavaElement je = getJavaElement(annotation.getData());
		// if (je != null) {
		// openJEAction.setJavaElement(je);
		// openJEAction.run();
		// }
		// } else if ((e.stateMask & SWT.SHIFT) != 0) {
		// IActivation a = getActivation(annotation.getData());
		// if (a != null) {
		// revealAction.setFocusElement(a);
		// revealAction.run();
		// }
		// }
		// }
		// }
	}

	private IActivation getActivation(Object data) {
		if (data instanceof IAdaptable) {
			return (IActivation) ((IAdaptable) data)
				.getAdapter(IActivation.class);
		}
		return null;
	}

	private IJavaElement getJavaElement(Object data) {
		if (data instanceof IAdaptable) {
			return (IJavaElement) ((IAdaptable) data)
				.getAdapter(IJavaElement.class);
		}
		return null;
	}

}
