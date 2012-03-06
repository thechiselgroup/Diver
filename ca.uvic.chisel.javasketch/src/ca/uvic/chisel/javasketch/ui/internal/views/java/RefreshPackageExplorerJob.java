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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.internal.SketchUI;

public class RefreshPackageExplorerJob extends UIJob {

	/**
	 * @param view
	 */
	public RefreshPackageExplorerJob() {
		super("Refreshing Package Explorer");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView("org.eclipse.jdt.ui.PackageExplorer");
		if (view instanceof IPackagesViewPart) {
			boolean checked = SketchPlugin.getDefault().getPreferenceStore().getBoolean(SketchUI.PREFERENCE_FILTER_PACKAGE_EXPLORER);
			IPackagesViewPart pview = (IPackagesViewPart) view;
			IProgramSketch activeSketch = SketchPlugin.getDefault().getActiveSketch();
			boolean enabled = activeSketch != null;
						
			TreeViewer viewer = pview.getTreeViewer();
			ViewerFilter filter = new JavaViewFilter3();
			ViewerFilter oldFilter = null;
			for (ViewerFilter vf : viewer.getFilters()) {
				if (vf instanceof JavaViewFilter3) {
					oldFilter = vf;
					break;
				}
			}
			if (enabled && checked) {
				if (oldFilter == null) {
					viewer.addFilter(filter);
				}
			} else if (oldFilter != null) {
				viewer.removeFilter(oldFilter);
			}
			viewer.refresh();
		}
		return Status.OK_STATUS;
	}

}