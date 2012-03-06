/*******************************************************************************
 * Copyright 2005-2008, CHISEL Group, University of Victoria, Victoria, BC, Canada.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Chisel Group, University of Victoria
 *******************************************************************************/
package ca.uvic.chisel.diver.sequencediagrams.sc.java.editors;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.zest.custom.sequence.widgets.UMLSequenceChart;
import org.eclipse.zest.custom.uml.viewers.BreadCrumbViewer;
import org.eclipse.zest.custom.uml.viewers.ISequenceViewerListener;
import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerEvent;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerGroupEvent;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerRootEvent;

import ca.uvic.chisel.diver.sequencediagrams.sc.java.StaticSequenceEditorPlugin;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.actions.CollapseAllAction;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.actions.ExpandAllAction;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.actions.FocusInAction;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.actions.FocusUpAction;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.IJavaActivation;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaActivation;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaMessage;


/**
 * An editor that uses an {@link  UMLSequenceViewer} to view a representation of the Java AST's supplied
 * by the Eclipse JDT. Look especially at the {@link JavaSequenceContentProvider} class, and the {@link #createPartControl(Composite)}
 * method for examples on how to get started with the UML Sequence Viewer.
 * 
 * XXX UML Sequence Viewer example
 * 
 * @author Del Myers
 *
 */
public class ASTSequenceEditor extends EditorPart {
			
	private UMLSequenceViewer viewer;
	private Object fOutlinePage;
	private FocusInAction focusIn;
	private FocusUpAction focusUp;
	private CollapseAllAction collapseAll;
	private ExpandAllAction expandAll;
	private Action cloneAction;
	private Composite control;
	private BreadCrumbViewer breadcrumb;
	private BreadCrumbSelectionListener breadcrumbListener = new BreadCrumbSelectionListener();
	private SequenceViewerListener sequenceListener = new SequenceViewerListener();
	/**
	 * Listener for adjusting the breadcrumb for when the root changes.
	 * @author Del Myers
	 */
	
	private final class SequenceViewerListener implements
			ISequenceViewerListener {
		public void elementCollapsed(SequenceViewerEvent event) {}

		public void elementExpanded(SequenceViewerEvent event) {}

		public void groupCollapsed(SequenceViewerGroupEvent event) {}

		public void groupExpanded(SequenceViewerGroupEvent event) {}

		public void rootChanged(SequenceViewerRootEvent event) {
			if (breadcrumb != null) {
				Object a = event.getSequenceViewer().getRootActivation();
				if (breadcrumb != null && a != breadcrumb.getInput()) {
					breadcrumb.setInput(a);
				}
			}
		}
	}

	/**
	 * Listener for adjusting the root when the breadcrumb is selected.
	 * @author Del Myers
	 */
	
	private final class BreadCrumbSelectionListener implements
			ISelectionChangedListener {
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelection() instanceof IStructuredSelection) {
				Object o = ((IStructuredSelection)event.getSelection()).getFirstElement();
				if (viewer != null && o instanceof IJavaActivation) {
					viewer.setRootActivation(o);
				}
			}
		}
	}

	private class NavigateToCodeListener extends MouseAdapter {
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			Object element = viewer.elementAt(e.x, e.y);
			int startPosition = -1;
			int length = 1;
			if (element instanceof JavaMessage) {
				JavaMessage message = (JavaMessage) element;
				startPosition = message.getAST().getStartPosition();
				message.getAST().getLength();
				element = message.getSource();
			}
			if (element instanceof IAdaptable) {
				IJavaElement je = (IJavaElement) ((IAdaptable)element).getAdapter(IJavaElement.class);
				if (je != null) {
					try {
						IEditorPart editor = JavaUI.openInEditor(je);
						if (startPosition >= 0) {
							if (editor instanceof ITextEditor) {
								((ITextEditor)editor).selectAndReveal(startPosition, 0);
							}
						}
					} catch (PartInitException e1) {
					} catch (JavaModelException e1) {
					}
				}
			}
		}
	}

	public ASTSequenceEditor() {
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setInput(input);
		setSite(site);
		firePropertyChange(IWorkbenchPartConstants.PROP_INPUT);
		setPartName(input.getName());

	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		control = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight=0;
		layout.marginWidth=0;
		layout.verticalSpacing=3;
		control.setLayout(layout);
		
		breadcrumb = new BreadCrumbViewer(control, SWT.BORDER);
		breadcrumb.setContentProvider(new JavaSequenceBreadCrumbContentProvider());
		breadcrumb.setLabelProvider(new JavaSequenceLabelProvider());
		breadcrumb.addSelectionChangedListener(new BreadCrumbSelectionListener());
		breadcrumb.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		viewer = new UMLSequenceViewer(control, SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);
		viewer.setContentProvider(new JavaSequenceContentProvider());
		viewer.setLabelProvider(new JavaSequenceLabelProvider());
		viewer.setMessageGrouper(new JavaMessageGrouper());
		viewer.setInput(getEditorInput());
		viewer.getChart().addMouseListener(new NavigateToCodeListener());
		viewer.getChart().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.addSequenceListener(new SequenceViewerListener());
		viewer.getChart().setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		MenuManager manager = new MenuManager("ASTSequenceEditor", "#ASTSequenceEditorContext");
		manager.setRemoveAllWhenShown(true);
		Menu contextMenu = manager.createContextMenu(viewer.getChart());
		manager.addMenuListener(new IMenuListener(){
			public void menuAboutToShow(IMenuManager manager) {
				Point location = Display.getCurrent().getCursorLocation();
				location = viewer.getChart().toControl(location);
				Object element = viewer.elementAt(location.x, location.y);
				if (element instanceof JavaActivation) {
					String activationName = ((ILabelProvider)viewer.getLabelProvider()).getText(element);
					focusIn.setFocusElement(element);
					focusIn.setText("Focus On " + activationName);
					manager.add(focusIn);
					expandAll.setText("Expand All Activations Under " + activationName);
					expandAll.setFocusElement(element);
					manager.add(expandAll);
					collapseAll.setText("Collapse All Activations Under " + activationName);
					collapseAll.setFocusElement(element);
					manager.add(collapseAll);
				}
				Object root = viewer.getRootActivation();
				if (root instanceof JavaActivation) {
					JavaActivation jRoot = (JavaActivation) root;
					if (jRoot.getCallingMessage() != null && jRoot.getCallingMessage().getSource() != null) {
						manager.add(focusUp);
					}
				}
				manager.add(cloneAction);
			}
		});
		viewer.getChart().setMenu(contextMenu);
		viewer.getChart().setCloneVisible(false);
		breadcrumb.setInput(viewer.getRootActivation());
		makeActions();
	}

	
	private void makeActions() {
		focusIn = new FocusInAction(viewer);
		ImageDescriptor descriptor = StaticSequenceEditorPlugin.getImageDescriptor("icons/in.gif");
		focusIn.setImageDescriptor(descriptor);
		
		focusUp = new FocusUpAction(viewer);
		focusUp.setText("Focus On Caller");
		descriptor = StaticSequenceEditorPlugin.getImageDescriptor("icons/up.gif");
		focusUp.setImageDescriptor(descriptor);
		
		cloneAction = new Action("Toggle Clone Pane", IAction.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				UMLSequenceChart chart = getViewer().getChart();
				if (!chart.isCloneVisible()) {
					setText("Hide Clone Pane");
				} else {
					setText("Show Clone Pane");
				}
				chart.setCloneVisible(!chart.isCloneVisible());
				setChecked(chart.isCloneVisible());
			}
		};
		getViewer().getChart().setCloneVisible(false);
		cloneAction.setChecked(!getViewer().getChart().isCloneVisible());
		if (getViewer().getChart().isCloneVisible()) {
			cloneAction.setText("Hide Clone Pane");
		} else {
			cloneAction.setText("Show Clone Pane");
		}
		
		collapseAll = new CollapseAllAction(viewer);
		descriptor = StaticSequenceEditorPlugin.getImageDescriptor("icons/collapseAll.gif");
		collapseAll.setImageDescriptor(descriptor);
		expandAll = new ExpandAllAction(viewer);
		descriptor = StaticSequenceEditorPlugin.getImageDescriptor("icons/expandAll.gif");
		expandAll.setImageDescriptor(descriptor);
	}
	
	@Override
	public void setFocus() {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
			if (fOutlinePage == null) {
				fOutlinePage = new ThumbnailOutlinePage(viewer.getChart());
			}
			return fOutlinePage;
		}
		return super.getAdapter(adapter);
	}
	
	@Override
	public void dispose() {
		getViewer().removeSequenceListener(sequenceListener);
		breadcrumb.removeSelectionChangedListener(breadcrumbListener);
		super.dispose();
	}

	/**
	 * @return
	 */
	public UMLSequenceViewer getViewer() {
		return viewer;
	}


}
