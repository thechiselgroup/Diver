/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.ui.internal.views;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.ui.internal.SketchUI;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.ToggleSketchActivationHandler;

/**
 * A subclass of the Common Navigator, which creates a tree viewer with columns.
 * 
 * @author Del Myers
 * 
 */
public class TraceNavigator extends ViewPart implements
		ITabbedPropertySheetPageContributor {

	
	public static final String VIEW_ID = "ca.uvic.chisel.javasketch.views.traces";
	protected static final int VISIBLE_TRACE_COLUMN = 2;
	protected static final int ACTIVE_TRACE_COLUMN = 1;
	protected static final int LABEL_COLUMN = 0;
	
	private EmptyThreadFilter threadFilter = new EmptyThreadFilter();

	private class TreeMouseListener extends MouseAdapter {

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.swt.events.MouseAdapter#mouseDoubleClick(org.eclipse.
		 * swt.events.MouseEvent)
		 */
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			if (e.button == 1) {
				ViewerCell cell = viewer.getCell(new Point(e.x, e.y));
				if (cell != null) {
					handleDoubleClick(cell, e);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.swt.events.MouseAdapter#mouseUp(org.eclipse.swt.events
		 * .MouseEvent)
		 */
		@Override
		public void mouseUp(MouseEvent e) {
			if (e.button == 1) {
				IHandlerService service = (IHandlerService) SketchPlugin
					.getDefault().getWorkbench().getService(
						IHandlerService.class);
				if (service == null)
					return;
				ViewerCell cell = viewer.getCell(new Point(e.x, e.y));
				if (cell == null)
					return;
				if (cell.getElement() instanceof IProgramSketch) {
					if (cell != null) {
						try {
							Event event = new Event();
							event.button = e.button;
							event.x = e.x;
							event.y = e.y;
							event.display = e.display;
							event.data = e.data;
							event.widget = e.widget;
							if (cell.getColumnIndex() == ACTIVE_TRACE_COLUMN) {
								if (cell.getElement() instanceof IProgramSketch) {
									service
										.executeCommand(
											ToggleSketchActivationHandler.COMMAND_ID,
											event);
								}
							} else if (cell.getColumnIndex() == VISIBLE_TRACE_COLUMN) {
								service.executeCommand(
									ToggleSketchFilteredHandler.COMMAND_ID,
									event);
							}
						} catch (CommandException ex) {
							SketchPlugin.getDefault().log(ex);
						}
					}
				}
			}
		}
	}


	private TreeViewer viewer;
	// TraceViewerActions actions = new TraceViewerActions();
	private MouseListener treeMouseListener = new TreeMouseListener();


	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION);
		viewer.setUseHashlookup(true);
		// TreeViewerColumn plusColumn = new TreeViewerColumn(viewer, SWT.NONE,
		// 0);
		// plusColumn.getColumn().setWidth(60);
		final TreeViewerColumn labelColumn = new TreeViewerColumn(viewer,
			SWT.NONE, LABEL_COLUMN);
		labelColumn.getColumn().setWidth(100);
		TreeViewerColumn activeTraceColumn = new TreeViewerColumn(viewer,
			SWT.NONE, ACTIVE_TRACE_COLUMN);
		activeTraceColumn.getColumn().setWidth(16);
		TreeViewerColumn visibleTraceColumn = new TreeViewerColumn(viewer,
			SWT.NONE, VISIBLE_TRACE_COLUMN);
		visibleTraceColumn.getColumn().setWidth(16);
		viewer.getTree().addMouseListener(treeMouseListener);

		viewer.getTree().addControlListener(new ControlAdapter() {
			/*
			 * (non-Javadoc)
			 * @see
			 * org.eclipse.swt.events.ControlAdapter#controlResized(org.eclipse
			 * .swt.events.ControlEvent)
			 */
			@Override
			public void controlResized(ControlEvent e) {
				Composite composite = (Composite) e.widget;
				Rectangle b = composite.getBounds();
				ScrollBar scrollbar = composite.getVerticalBar();
				int width = b.width - 32;
				if (scrollbar != null && scrollbar.getVisible()) {
					width -= 20;
				}
				labelColumn.getColumn().setWidth(Math.max(0, width));
			}
		});
		viewer.setContentProvider(new TraceNavigatorContentProvider());
		viewer.setLabelProvider(new TraceNavigatorLabelProvider());
		viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object o = selection.getFirstElement();
				if (o instanceof IThread) {
					SketchPlugin.getDefault().getDOI().setThreadSelection((IThread) o);
				} else {
					SketchPlugin.getDefault().getDOI().setThreadSelection(null);
				}
				SketchUI.INSTANCE.refreshJavaUI();
			}
		});
		createContextMenu(viewer.getControl());
		getSite().setSelectionProvider(viewer);
		IMenuManager manager = getViewSite().getActionBars().getMenuManager();
		Action toggleFilterAction = new Action("Filter Empty Threads", SWT.CHECK) {
			/* (non-Javadoc)
			 * @see org.eclipse.jface.action.Action#run()
			 */
			@Override
			public void run() {
				IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
				boolean checked = store.getBoolean("tracenavigator.filter.emptythread");
				if (checked) {
					//uncheck
					viewer.removeFilter(threadFilter);
				} else {
					viewer.addFilter(threadFilter);
				}
				store.setValue("tracenavigator.filter.emptythread", !checked);
				viewer.refresh();
			}
		};
		IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
		boolean checked = true; 
		if (!store.contains("tracenavigator.filter.emptythread")) {
			store.setValue("tracenavigator.filter.emptythread", true);
		} else {
			checked = store.getBoolean("tracenavigator.filter.emptythread");
		}
		toggleFilterAction.setChecked(checked);
		if (checked) {
			viewer.addFilter(threadFilter);
		}
		manager.add(toggleFilterAction);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
	}

	/**
	 * @param cell
	 * @param data
	 */
	public void handleDoubleClick(ViewerCell cell, MouseEvent event) {
		IHandlerService hs = (IHandlerService) SketchPlugin.getDefault()
			.getWorkbench().getService(IHandlerService.class);
		if (cell != null) {
			if (cell.getElement() instanceof IThread) {
				if (hs != null) {
					try {
						Event e = new Event();
						e.button = event.button;
						e.x = event.x;
						e.y = event.y;
						e.type = SWT.MouseDoubleClick;
						e.data = event.data;
						hs.executeCommand(OpenIThreadCommandHandler.COMMAND_ID,
							e);

					} catch (CommandException ex) {
						SketchPlugin.getDefault().log(ex);
					}
				}
			} else {
				viewer.setExpandedState(cell.getElement(), !viewer
					.getExpandedState(cell.getElement()));
			}
		}
	}

	/**
	 * @param control
	 */
	private void createContextMenu(Control control) {
		MenuManager manager = new MenuManager("#PopUp");
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		control.setMenu(manager.createContextMenu(control));
		getViewSite().registerContextMenu(manager, viewer);
	}

	protected void fillContextMenu(IMenuManager manager) {
		// actions will be added using commands
		manager.add(new Separator(ICommonMenuConstants.GROUP_OPEN));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
	}

	/**
	 * @return
	 */
	public TreeViewer getTreeViewer() {
		return viewer;
	}

	

	
	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor
	 * #getContributorId()
	 */
	@Override
	public String getContributorId() {
		return "ca.uvic.chisel.javasketch.modelProperties";
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		if (IPropertySheetPage.class.equals(adapter)) {
			return new TabbedPropertySheetPage(this);
		}
		return super.getAdapter(adapter);
	}

}
