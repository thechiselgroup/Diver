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
package ca.uvic.chisel.logging.eclipse;

import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * A class for finding a control within a workbench part to start listening to.
 * @author Del Myers
 *
 */
public interface IPartWidgetAdapter {
	
	/**
	 * Searches the workbench part for a control.
	 * @param part the part to search.
	 * @return the control that was found.
	 */
	public Control findControl(IWorkbenchPartReference part);

}
