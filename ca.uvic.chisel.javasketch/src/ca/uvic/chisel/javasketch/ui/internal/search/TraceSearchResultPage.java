/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.ui.internal.search;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;
import org.eclipse.search.ui.ISearchResultPage;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.SearchResultEvent;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.Page;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.CommandAction;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.RevealActivationHandler;

/**
 * Page for displaying the results of a trace search.
 * 
 * @author Del Myers
 * 
 */
public class TraceSearchResultPage extends Page implements ISearchResultPage {
	private String fid;
	private ISearchResultViewPart fpart;
	private TreeViewer fViewer;
	private ISearchResultListener resultsListener;
	private Composite page;

	/**
	 * @author Del Myers
	 *
	 */
	private final class SelectionForward implements
			ISelectionProvider, ISelectionChangedListener {
		ListenerList listeners = new ListenerList();
		@Override
		public void setSelection(ISelection selection) {
			fViewer.setSelection(selection);
		}

		@Override
		public void removeSelectionChangedListener(
				ISelectionChangedListener listener) {
			listeners.remove(listener);
			
		}

		@Override
		public ISelection getSelection() {
			return adaptSelection(fViewer.getSelection());
		}

		@Override
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			listeners.add(listener);
			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			ISelection selection = adaptSelection(event.getSelection());
			SelectionChangedEvent adapted = new SelectionChangedEvent(this, selection);
			for (Object o : listeners.getListeners()) {
				((ISelectionChangedListener)o).selectionChanged(adapted);
			}
		}

		/**
		 * @param selection
		 */
		private ISelection adaptSelection(ISelection selection) {
			List<Object> selected = new LinkedList<Object>();
			
			if (selection instanceof IStructuredSelection) {
				for (Iterator<?> i = ((IStructuredSelection)selection).iterator(); i.hasNext();) {
					Object o = i.next();
					if (o instanceof Match) {
						ITraceModelProxy proxy = (ITraceModelProxy) ((Match)o).getElement();
						if (proxy.getElement() != null) {
							selected.add(proxy.getElement());
						}
					} else {
						selected.add(o);
					}
				}
			}
			return new StructuredSelection(selected);
		}
	}

	private class TraceSearchResultListener implements ISearchResultListener {

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.search.ui.ISearchResultListener#searchResultChanged(org
		 * .eclipse.search.ui.SearchResultEvent)
		 */
		@Override
		public void searchResultChanged(SearchResultEvent e) {
			SketchPlugin.getDefault().getWorkbench().getDisplay().asyncExec(
				new Runnable() {

					@Override
					public void run() {
						updatePage();
					}

				});
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResultPage#getID()
	 */
	@Override
	public String getID() {
		return fid;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResultPage#setID(java.lang.String)
	 */
	@Override
	public void setID(String id) {
		fid = id;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.part.Page#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		page = new Composite(parent, SWT.NULL);
		page.setLayout(new GridLayout());
		fViewer = new TreeViewer(page, SWT.NONE);
		fViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableLayout tl = new TableLayout();
		fViewer.getTree().setLayout(tl);
		// create columns for the viewer
		TreeViewerColumn c = new TreeViewerColumn(fViewer, SWT.NONE);
		c.getColumn().setText("Search Result");
		c.setLabelProvider(new TraceSearchResultLabelProvider());
		GC gc = new GC(fViewer.getTree());
		tl.addColumnData(new ColumnPixelData(Dialog
			.convertWidthInCharsToPixels(gc.getFontMetrics(), 60)));
		gc.dispose();

		c = new TreeViewerColumn(fViewer, SWT.NONE);
		c.getColumn().setText("Kind");
		c.setLabelProvider(new TraceSearchResultLabelProvider());
		tl.addColumnData(new ColumnPixelData(50));

		c = new TreeViewerColumn(fViewer, SWT.NONE);
		c.getColumn().setText("Trace");
		c.setLabelProvider(new TraceSearchResultLabelProvider());
		
		tl.addColumnData(new ColumnPixelData(50));
		resultsListener = new TraceSearchResultListener();

		fViewer.getTree().setHeaderVisible(true);

		fViewer.setContentProvider(new TraceSearchResultContentProvider());
		//create a context menu for the viewer so that results can be 
		//linked to
		MenuManager manager = new MenuManager("TraceSearchResultsPage", "#TraceSearchResults");
		Menu menu = manager.createContextMenu(fViewer.getTree());
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
				IStructuredSelection ss = (IStructuredSelection) getSite().getSelectionProvider().getSelection();
				if (!ss.isEmpty()) {
					Object o = ss.getFirstElement();
					if (o instanceof IActivation || o instanceof IMessage) {
						IAction action = new CommandAction(RevealActivationHandler.COMMAND_ID, null);
						action.setText("Reveal");
						manager.add(action);
					}
				}
			}
		});
		getSite().registerContextMenu("#TraceSearchResults", manager, fViewer);
		fViewer.getTree().setMenu(menu);
		SelectionForward forward = new SelectionForward();
		fViewer.addSelectionChangedListener(forward);
		getSite().setSelectionProvider(forward);

	}

	/**
	 * @param manager
	 */
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.Page#getControl()
	 */
	@Override
	public Control getControl() {
		return page;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.Page#setFocus()
	 */
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResultPage#getLabel()
	 */
	@Override
	public String getLabel() {
		return "";
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResultPage#getUIState()
	 */
	@Override
	public Object getUIState() {
		return fViewer.getSelection();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.search.ui.ISearchResultPage#restoreState(org.eclipse.ui.IMemento
	 * )
	 */
	@Override
	public void restoreState(IMemento memento) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.search.ui.ISearchResultPage#saveState(org.eclipse.ui.IMemento
	 * )
	 */
	@Override
	public void saveState(IMemento memento) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.search.ui.ISearchResultPage#setInput(org.eclipse.search.ui
	 * .ISearchResult, java.lang.Object)
	 */
	@Override
	public void setInput(ISearchResult search, Object uiState) {
		if (!(search instanceof TraceSearchQueryResults)) {
			fViewer.setInput(null);
		} else {
			if (fViewer.getInput() != null) {
				((TraceSearchQueryResults) fViewer.getInput())
					.removeListener(resultsListener);
			}
			search.addListener(resultsListener);
			fViewer.setInput(search);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.search.ui.ISearchResultPage#setViewPart(org.eclipse.search
	 * .ui.ISearchResultViewPart)
	 */
	@Override
	public void setViewPart(ISearchResultViewPart part) {
		fpart = part;
	}

	/**
	 * Sets the layout to either a tree layout or a flat layout. If the layout
	 * is flat, then sorting is available.
	 * 
	 * @param tree
	 *            true if the layout should be as a tree.
	 */
	public void setLayout(boolean tree) {

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.Page#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (fViewer.getInput() != null) {
			((ISearchResult) fViewer.getInput())
				.removeListener(resultsListener);
		}
		fViewer = null;
	}

	/**
	 * 
	 */
	protected void updatePage() {
		if (fViewer != null) {
			fViewer.refresh();
			fpart.updateLabel();

		}
	}

}
