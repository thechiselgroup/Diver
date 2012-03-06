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
/**
 * 
 */
package ca.uvic.chisel.logging.eclipse.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * @author Del Myers
 *
 */
public class CommandLogger implements IExecutionListener {
	private HashMap<String, Set<Pattern>> commandFilters;
	
	public CommandLogger() {
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListener#notHandled(java.lang.String, org.eclipse.core.commands.NotHandledException)
	 */
	public void notHandled(String commandId, NotHandledException exception) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteFailure(java.lang.String, org.eclipse.core.commands.ExecutionException)
	 */
	public void postExecuteFailure(String commandId,
			ExecutionException exception) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListener#postExecuteSuccess(java.lang.String, java.lang.Object)
	 */
	public void postExecuteSuccess(String commandId, Object returnValue) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IExecutionListener#preExecute(java.lang.String, org.eclipse.core.commands.ExecutionEvent)
	 */
	public synchronized void preExecute(String commandId, ExecutionEvent event) {
		load();
		long time = System.currentTimeMillis();
		for (String categoryID : commandFilters.keySet()) {
			Set<Pattern> filters = commandFilters.get(categoryID);
			ILoggingCategory category = WorkbenchLoggingPlugin.getDefault().getCategoryManager().getCategory(categoryID);
			if (category instanceof LoggingCategory) {
				Log log = ((LoggingCategory)category).getLog();
				for (Pattern filter : filters) {
					Matcher matcher = filter.matcher(commandId);
					if (matcher.matches()) {
						log.logLine(time + "\tcommandEvent\t" + "id="+commandId + "\texecuted");
					}
				}
			}
		}

	}
	
	private synchronized void load() {
		if (commandFilters != null) return;
		commandFilters = new HashMap<String, Set<Pattern>>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint("ca.uvic.chisel.logging.eclipse.loggers");
		IConfigurationElement[] elements = point.getConfigurationElements();
		for (IConfigurationElement loggerElement : elements) {
			String categoryID = loggerElement.getAttribute("categoryID");
			if (categoryID != null) {
				for (IConfigurationElement workbenchElement : loggerElement.getChildren("workbench")) {
					for (IConfigurationElement logElement : workbenchElement.getChildren("command")) {
						//get the filters for the command
						String filter = logElement.getAttribute("commandFilter");
						if (filter != null) {
							//create a pattern for the filter
							filter = filter.replace(".", "\\.");
							filter = filter.replace("*", ".*");
							Pattern pattern = Pattern.compile(filter);
							Set<Pattern> filters = commandFilters.get(categoryID);
							if (filters == null) {
								filters = new HashSet<Pattern>();
								commandFilters.put(categoryID, filters);
							}
							filters.add(pattern);
						}
					}
				}
			}
		}
	}
	

}
