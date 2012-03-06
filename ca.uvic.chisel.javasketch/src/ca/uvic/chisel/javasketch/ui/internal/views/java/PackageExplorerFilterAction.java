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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.internal.SketchUI;
@Deprecated
public class PackageExplorerFilterAction implements IViewActionDelegate, IActionDelegate2 {
	@Override
	public void init(IViewPart view) {
	}

	@Override
	public void run(IAction action) {
		IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
		boolean enabled = store.getBoolean(SketchUI.PREFERENCE_FILTER_PACKAGE_EXPLORER);
		store.setValue(SketchUI.PREFERENCE_FILTER_PACKAGE_EXPLORER, !enabled);
		
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate2#dispose()
	 */
	@Override
	public void dispose() {}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate2#init(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void init(IAction action) {
		boolean enabled = SketchPlugin.getDefault().getPreferenceStore().getBoolean(SketchUI.PREFERENCE_FILTER_PACKAGE_EXPLORER);
		action.setChecked(enabled);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate2#runWithEvent(org.eclipse.jface.action.IAction, org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

}
