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

import java.lang.reflect.Method;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.Viewer;

import ca.uvic.chisel.logging.eclipse.IPartLogger;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * Default logger for viewers.
 * @author Del Myers
 *
 */
public class ViewerLogger extends AbstractViewerLogger {
	
	/**
	 * 
	 */
	protected void unhookListeners() {
		IPartLogger viewerLogger = getLogger();
		if (viewerLogger == null) return;
		String removeString = getConfigurationElement().getAttribute("removeMethod");
		Method removeMethod = findMethod(getLoggedViewer(), removeString, viewerLogger.getClass());
		if (removeMethod != null) {
			try {
				removeMethod.invoke(getLoggedViewer(), viewerLogger);
			} catch (Exception e) {
				WorkbenchLoggingPlugin.getDefault().log(e);
			}
		}
	}
	
	protected IPartLogger createLogger() {
		IConfigurationElement configurationElement = getConfigurationElement();
		try {
			IPartLogger viewerLogger = (IPartLogger) configurationElement.createExecutableExtension("listener");
			return viewerLogger;
		} catch (Exception e) {
			WorkbenchLoggingPlugin.getDefault().log(e);
		}
		return null;
	}

	protected void hookListeners() {
		IConfigurationElement configurationElement = getConfigurationElement();
		Viewer loggedViewer = getLoggedViewer();
		IPartLogger viewerLogger = getLogger();
		String addMethodName = configurationElement.getAttribute("addMethod");
		if (loggedViewer != null) {
			Method addMethod = findMethod(loggedViewer, addMethodName, viewerLogger.getClass());
			if (addMethod != null) {
				try {
					addMethod.invoke(loggedViewer, viewerLogger);
				} catch (Exception e) {
					WorkbenchLoggingPlugin.getDefault().log(e);
					return;
				} 
			}
		}
	}

}
