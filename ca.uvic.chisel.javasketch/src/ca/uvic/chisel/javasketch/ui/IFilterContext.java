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
package ca.uvic.chisel.javasketch.ui;

import org.eclipse.jdt.core.IJavaProject;

/**
 * @author Del Myers
 *
 */
public interface IFilterContext {
	/**
	 * Notifies the context that the filters have changed.
	 * @param composite
	 */
	void filterChanged();
	
	/**
	 * Updates the context to set the java projects associated with it.
	 * @param newProjects
	 */
	void contextChanged(IJavaProject[] newProjects);

}
