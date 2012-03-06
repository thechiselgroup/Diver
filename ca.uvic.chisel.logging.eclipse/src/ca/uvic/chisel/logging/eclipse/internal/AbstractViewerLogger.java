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


import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchPart;

import ca.uvic.chisel.logging.eclipse.IPartLogger;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * @author Del Myers
 *
 */
public abstract class AbstractViewerLogger extends AbstractPartLogger {

	Viewer loggedViewer;
	IPartLogger viewerLogger;
	/**
	 * 
	 */
	public AbstractViewerLogger() {
		super();
	}

	/**
	 * Returns the viewer that is being logged within the part.
	 * @return the viewer being logged.
	 */
	public Viewer getLoggedViewer() {
		return loggedViewer;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.internal.AbstractPartLogger#unhook()
	 */
	@Override
	protected void unhook() {
		if (getLoggedViewer() == null) return;
		super.unhook();
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.internal.AbstractPartLogger#doHookToPart(org.eclipse.ui.IWorkbenchPart, org.eclipse.core.runtime.IConfigurationElement)
	 */
	@Override
	protected void doHookToPart(IWorkbenchPart part,
			IConfigurationElement elementForViewer) {
		IConfigurationElement viewerElement = (IConfigurationElement) elementForViewer.getParent();
		IConfigurationElement partElement = (IConfigurationElement) viewerElement.getParent();
		boolean searchHierarchy = Boolean.parseBoolean(partElement.getAttribute("searchHierarchy"));
		
		String viewerClassName = viewerElement.getAttribute("viewerClass");
		String viewerMethodName = viewerElement.getAttribute("viewerGetMethod");
		viewerLogger = createLogger();
		//use reflection to search for the part
		Class<?> partClass = part.getClass();
		if (viewerMethodName != null) {
			Method viewerMethod = findMethod(part, viewerMethodName);
			if (viewerMethod != null) {
				Object viewerObject = null;
				try {
					viewerObject = viewerMethod.invoke(part);
				} catch (Exception e) {}
				if (viewerObject instanceof Viewer) {
					loggedViewer = (Viewer) viewerObject;
				}
			}
		}
		if (loggedViewer == null) {
			Field viewerField = findField(searchHierarchy, viewerClassName,
				partClass);
			if (viewerField != null) {
				viewerField.setAccessible(true);
				Object v = null;
				try {
					v = viewerField.get(part);
				} catch (Exception e) {
					WorkbenchLoggingPlugin.getDefault().log(e);
					return;
				}
				if (v instanceof Viewer) {
					loggedViewer = (Viewer) v;
				}
			}
		}
		
	}

}