/*******************************************************************************
 * Copyright (c) 2010 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.logging.eclipse.internal;

import java.util.Arrays;
import java.util.LinkedList;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.internal.WorkbenchPartReference;

import ca.uvic.chisel.logging.eclipse.IPartWidgetAdapter;

/**
 * Searches the workbench reference for the first part that matches a target
 * control.
 * @author Del Myers
 *
 */
public class DefaultPartWidgetAdapter implements IPartWidgetAdapter {

	private Class<?> target;

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.IPartWidgetAdapter#findControl(org.eclipse.ui.IWorkbenchPartReference)
	 */
	@SuppressWarnings("restriction")
	public Control findControl(IWorkbenchPartReference part) {
		//we cheat a little by using internal classes.
		if (part instanceof WorkbenchPartReference) {
			WorkbenchPartReference pr = (WorkbenchPartReference) part;
			if (!pr.isDisposed()) {
				Control root = pr.getPane().getControl();
				LinkedList<Control> controls = new LinkedList<Control>();
				controls.add(root);
				while (!controls.isEmpty()) {
					Control c = controls.removeFirst();
					if (target.isAssignableFrom(c.getClass())) {
						return c;
					} else if (c instanceof Composite) {
						Composite composite = (Composite) c;
						controls.addAll(Arrays.asList(composite.getChildren()));
					}
				}
			}
		}
		return null;
	}
	
	public void setTargetWidget(Class<?> target) {
		this.target = target;
	}

}
