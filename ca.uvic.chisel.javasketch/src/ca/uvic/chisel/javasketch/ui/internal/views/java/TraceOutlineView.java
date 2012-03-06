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
package ca.uvic.chisel.javasketch.ui.internal.views.java;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import ca.uvic.chisel.javasketch.ui.internal.presentation.JavaThreadSequenceView;

/**
 * @author Del Myers
 *
 */
public class TraceOutlineView extends ViewPart {

	/**
	 * @author Del Myers
	 *
	 */
	private final class TraceViewListener implements IPartListener {
		private JavaThreadSequenceView sketchView;
		private Control outlinePage;

		@Override
		public void partOpened(IWorkbenchPart part) {
			if (part instanceof JavaThreadSequenceView) {
				if (part != this.sketchView) {
					if (this.outlinePage != null && !this.outlinePage.isDisposed()) {
						this.outlinePage.dispose();
					}
					this.sketchView = (JavaThreadSequenceView)part;
					IContentOutlinePage contentOutlinePage = (IContentOutlinePage) sketchView.getAdapter(IContentOutlinePage.class);
					if (contentOutlinePage != null) {
						contentOutlinePage.createControl(page);
						this.outlinePage = contentOutlinePage.getControl();
					}
				}
			}
		}

		@Override
		public void partDeactivated(IWorkbenchPart part) {}

		@Override
		public void partClosed(IWorkbenchPart part) {
			if (part instanceof JavaThreadSequenceView) {
				if (this.outlinePage != null && !outlinePage.isDisposed()) {
					outlinePage.dispose();
					sketchView = null;
				}
			}
		}

		@Override
		public void partBroughtToTop(IWorkbenchPart part) {}

		@Override
		public void partActivated(IWorkbenchPart part) {
			if (part instanceof JavaThreadSequenceView) {
				part.getSite().getPage().bringToTop(TraceOutlineView.this);
				partOpened(part);
			}
		}
	}

	public static final String VIEW_ID = "ca.uvic.chisel.javasketch.traceoutline";
	private IPartListener partListener;
	private Composite page;
	
	/**
	 * 
	 */
	public TraceOutlineView() {
		this.partListener = new TraceViewListener();
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		if (partListener == null) {
			partListener = new TraceViewListener();
		}
		this.page = new Composite(parent, SWT.NONE);
		page.setLayout(new FillLayout());
		IViewPart view =PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(JavaThreadSequenceView.VIEW_ID);
		if (view != null) {
			partListener.partOpened(view);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListener);
		} catch (NullPointerException e) {
			//part of the workbench is already gone. Ignore it.
		}
		partListener = null;
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite)
	 */
	@Override
	public void init(IViewSite site) throws PartInitException {
		
		super.init(site);
	}

}
