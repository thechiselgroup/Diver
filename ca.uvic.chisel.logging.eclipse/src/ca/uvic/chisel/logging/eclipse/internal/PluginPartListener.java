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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * @author Del Myers
 *
 */
class PluginPartListener implements IPartListener {
	Map<String, List<AbstractPartLogger>> loggerMap;
	Map<String, List<IConfigurationElement>> partElementMap;
	
	public PluginPartListener() {
		loggerMap = new HashMap<String, List<AbstractPartLogger>>();
	}
	
	/**
	 * Creates a new viewer listener for the part and hooks it up
	 * for listening to events.
	 */
	public void partOpened(IWorkbenchPart part) {
		String id = part.getSite().getId();
		String mapID = getMapID(part);
		//create new logger from plugin registry
		List<IConfigurationElement> partElements = getElementsForPart(id);
		List<AbstractPartLogger> loggers = loggerMap.get(mapID);
		if (loggers == null) {
			loggers = new LinkedList<AbstractPartLogger>();
			loggerMap.put(mapID, loggers);
		}
		EventLogger eventLogger = (EventLogger) WorkbenchLoggingPlugin.getDefault().getEventLogger();
		for (IConfigurationElement partElement: partElements) {
			String categoryID = ((IConfigurationElement)partElement.getParent()).getAttribute("categoryID");
			for (IConfigurationElement elementForPart : partElement.getChildren("viewer")) {
				for (IConfigurationElement elementForViewer : elementForPart.getChildren("viewerListener")) {
					try {
						ViewerLogger logger = new ViewerLogger();
						logger.hookToPart(part, categoryID,  elementForViewer);
						loggers.add(logger);
						eventLogger.registerLogger(logger.getLogger(), logger);
					} catch (Exception e) {
						WorkbenchLoggingPlugin.getDefault().log(e);
					}
				} 
			} for (IConfigurationElement elementForPart : partElement.getChildren("partWidgetListener")) {
				try {
					ViewerWidgetLogger logger = new ViewerWidgetLogger();
					logger.hookToPart(part, categoryID, elementForPart);
					loggers.add(logger);
					eventLogger.registerLogger(logger.getLogger(), logger);
				} catch (Exception e) {
					WorkbenchLoggingPlugin.getDefault().log(e);
				}
			}
				
		}
		//log the opening of the part, if necessary
		if (part == null) return;
		Set<LoggingCategory> categories = 
			WorkbenchLogger.getMatchingCategories(part.getSite().getId());
		for (LoggingCategory c : categories) {
			c.getLog().logLine(System.currentTimeMillis() + "\tworkbenchEvent\tpart=" +
				part.getSite().getId() + "\topened");
		}
		
	}

	/**
	 * @param id
	 * @return
	 */
	private List<IConfigurationElement> getElementsForPart(String id) {
		if (partElementMap == null) {
			try {
				partElementMap = new HashMap<String, List<IConfigurationElement>>();
				IExtensionRegistry registry = Platform.getExtensionRegistry();
				IExtensionPoint point = registry.getExtensionPoint("ca.uvic.chisel.logging.eclipse.loggers");
				IConfigurationElement[] elements = point.getConfigurationElements();
				for (IConfigurationElement loggerElement : elements) {
					if (!"logger".equals(loggerElement.getName())) continue;
					for (IConfigurationElement element: loggerElement.getChildren("part")){
						String elementPartID = element.getAttribute("partID");
						List<IConfigurationElement> elementList = partElementMap.get(elementPartID);
						if (elementList == null) {
							elementList = new LinkedList<IConfigurationElement>();
							partElementMap.put(elementPartID, elementList);
						}
						elementList.add(element);
					}
				}
			} catch (Exception e) {
				WorkbenchLoggingPlugin.getDefault().log(e);
			}
		}
		
		List<IConfigurationElement> elementList = partElementMap.get(id);
		if (elementList != null) {
			return elementList;
		}
		return Collections.emptyList();
	}

	/**
	 * @param part
	 * @param mapID
	 */
	private String getMapID(IWorkbenchPart part) {
		String mapID = part.getSite().getId();
		if (part instanceof IViewPart) {
			String secondaryID = ((IViewPart)part).getViewSite().getSecondaryId();
			if (secondaryID != null) {
				mapID += "$" + secondaryID;
			}
		}
		return mapID;
	}

	public void partDeactivated(IWorkbenchPart part) {
		//log the opening of the part, if necessary
		if (part == null) return;
		Set<LoggingCategory> categories = 
			WorkbenchLogger.getMatchingCategories(part.getSite().getId());
		for (LoggingCategory c : categories) {
			c.getLog().logLine(System.currentTimeMillis() + "\tworkbenchEvent\tpart=" +
				part.getSite().getId() + "\tdeactivated");
		}
	}

	public void partClosed(IWorkbenchPart part) {
		List<AbstractPartLogger> loggers = loggerMap.get(getMapID(part));
		EventLogger eventLogger = (EventLogger) WorkbenchLoggingPlugin.getDefault().getEventLogger();
		if (loggers != null) {
			for (AbstractPartLogger logger : loggers) {
				logger.unhook();
				eventLogger.clearLogger(logger.getLogger());
			}
			loggerMap.remove(getMapID(part));
		}
		//log the opening of the part, if necessary
		if (part == null) return;
		Set<LoggingCategory> categories = 
			WorkbenchLogger.getMatchingCategories(part.getSite().getId());
		for (LoggingCategory c : categories) {
			c.getLog().logLine(System.currentTimeMillis() + "\tworkbenchEvent\tpart=" +
				part.getSite().getId() + "\tclosed");
		}
	}

	public void partBroughtToTop(IWorkbenchPart part) {
		//log the opening of the part, if necessary
		if (part == null) return;
		Set<LoggingCategory> categories = 
			WorkbenchLogger.getMatchingCategories(part.getSite().getId());
		for (LoggingCategory c : categories) {
			c.getLog().logLine(System.currentTimeMillis() + "\tworkbenchEvent\tpart=" +
				part.getSite().getId() + "\ttop");
		}
	}

	
	public void partActivated(IWorkbenchPart part) {
		//log the opening of the part, if necessary
		if (part == null) return;
		Set<LoggingCategory> categories = 
			WorkbenchLogger.getMatchingCategories(part.getSite().getId());
		for (LoggingCategory c : categories) {
			c.getLog().logLine(System.currentTimeMillis() + "\tworkbenchEvent\tpart=" +
				part.getSite().getId() + "\tactivated");
		}
	}
}