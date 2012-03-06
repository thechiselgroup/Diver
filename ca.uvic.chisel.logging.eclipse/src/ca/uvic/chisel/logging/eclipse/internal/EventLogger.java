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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ca.uvic.chisel.logging.eclipse.IEventLogger;
import ca.uvic.chisel.logging.eclipse.ILogObjectInterpreter;
import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.IPartLogger;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * @author Del Myers
 *
 */
public class EventLogger implements IEventLogger {
	
	private Map<IPartLogger, AbstractPartLogger> loggerMap;
	
	
	public EventLogger() {
		loggerMap = Collections.synchronizedMap(new HashMap<IPartLogger, AbstractPartLogger>());
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.IEventLogger#logPartEvent(org.eclipse.ui.IWorkbenchPart, ca.uvic.chisel.logging.eclipse.IViewerLogger, java.lang.String, java.lang.Object)
	 */
	public void logPartEvent(IPartLogger logger,
			String event, Object eventObject) {
		if (!WorkbenchLoggingPlugin.isEnabled()) return;
		try {
			AbstractPartLogger viewerLogger = loggerMap.get(logger);
			ILoggingCategory category = WorkbenchLoggingPlugin.getDefault().getCategoryManager().getCategory(viewerLogger.getCategoryID());
			if (category == null) return;
			String output = System.currentTimeMillis() + "\tpartEvent\t" + "part="+viewerLogger.getPartID()+"\t"+event;
			if (eventObject != null) {
				ILogObjectInterpreter interpreter = category.getInterpreter(eventObject.getClass());
				String s = interpreter.toString(eventObject);
				s=s.replaceAll("(\\s+)|=", " ");
				output += "\tdata=" + s;
			}
			((LoggingCategory)category).getLog().logLine(output);
			
		} catch (Exception e) {
			WorkbenchLoggingPlugin.getDefault().log(e);
		}

	}
	
	public void registerLogger(IPartLogger logger, AbstractPartLogger viewerLogger) {
		if (!WorkbenchLoggingPlugin.isEnabled()) return;
		loggerMap.put(logger, viewerLogger);
	}
	
	public void clearLogger(IPartLogger logger) {
		if (!WorkbenchLoggingPlugin.isEnabled()) return;
		loggerMap.remove(logger);
	}

}
