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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import ca.uvic.chisel.logging.eclipse.ILogObjectInterpreter;
import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * @author Del Myers
 *
 */
public class SelectionLogger implements ISelectionListener {
	private class InternalSelectionLogger {
		String categoryID;
		private Pattern regex;
		public InternalSelectionLogger(String categoryID, String filter) {
			this.categoryID = categoryID;
			if (filter != null) {
				String regExString = filter.replace(".", "\\.");
				regExString = regExString.replace("*", ".*");
				this.regex = Pattern.compile(regExString);
			}
		}
		
		public void log(IWorkbenchPart part, ISelection selection) {
			String partID = part.getSite().getId();
			if (regex != null) {
				if (!regex.matcher(partID).matches()) {
					return;
				}
			}
			ILoggingCategory category = WorkbenchLoggingPlugin.getDefault().getCategoryManager().getCategory(categoryID);
			Log log = ((LoggingCategory)category).getLog();
			String output = System.currentTimeMillis() + "\tselectionEvent\t" +
				"part=" + partID + "\tselection";
			if (selection instanceof IStructuredSelection) {
				output += "\tdata=";
				for (Iterator<?> i = ((IStructuredSelection)selection).iterator(); i.hasNext();) {
					Object o = i.next();
					if (o != null) {
						ILogObjectInterpreter interpreter = category.getInterpreter(o.getClass());
						String s = interpreter.toString(o);
						s=s.replaceAll("(\\s+)|=", " ");
						output += s + ",";
					}
				}
			}
			log.logLine(output);
			
		}
		
	}

	private LinkedList<InternalSelectionLogger> loggers;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		load();
		for (InternalSelectionLogger logger : loggers) {
			logger.log(part, selection);
		}
	}

	/**
	 * 
	 */
	private void load() {
		if (loggers != null) {
			return;
		}
		loggers = new LinkedList<InternalSelectionLogger>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint("ca.uvic.chisel.logging.eclipse.loggers");
		IConfigurationElement[] elements = point.getConfigurationElements();
		for (IConfigurationElement loggerElement : elements) {
			if (!"logger".equals(loggerElement.getName())) continue;
			String categoryID = loggerElement.getAttribute("categoryID");
			for (IConfigurationElement workbenchElement: loggerElement.getChildren("workbench")){
				for (IConfigurationElement selectionElement : workbenchElement.getChildren("selection")) {
					String filter = selectionElement.getAttribute("partFilter");
					loggers.add(new InternalSelectionLogger(categoryID, filter));
				}
			}
		}		
	}

}
