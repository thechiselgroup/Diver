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


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import ca.uvic.chisel.logging.eclipse.DefaultSWTInterpreter;
import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.IPartLogger;
import ca.uvic.chisel.logging.eclipse.IPartWidgetAdapter;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * @author Del Myers
 *
 */
public class ViewerWidgetLogger extends AbstractPartLogger {
	private static final Map<String, Integer> EVENT_MAP;
	private static final Map<Integer, String> EXTENSION_MAP;
	static {
		EVENT_MAP = new HashMap<String, Integer>();
		EVENT_MAP.put("selected", SWT.Selection);
		EVENT_MAP.put("mouseUp", SWT.MouseUp);
		EVENT_MAP.put("mouseDown", SWT.MouseDown);
		EVENT_MAP.put("mouseWheel", SWT.MouseWheel);
		EVENT_MAP.put("keyDown", SWT.KeyDown);
		EXTENSION_MAP = new HashMap<Integer, String>();
		EXTENSION_MAP.put(SWT.Selection, "selected");
		EXTENSION_MAP.put( SWT.MouseUp, "mouseUp");
		EXTENSION_MAP.put(SWT.MouseDown, "mouseDown");
		EXTENSION_MAP.put( SWT.MouseWheel, "mouseWheel");
		EXTENSION_MAP.put(SWT.KeyDown, "keyDown");
	}
	
	private class WidgetLogger implements IPartLogger, Listener {

		private Log log;

		/* (non-Javadoc)
		 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
		 */
		public void handleEvent(Event event) {
			if (control == null || control.isDisposed()) return;
			if (events.contains(event.type)) {
				Widget widget = event.widget;
				if (searchHierarchy) {
					while (widget != null && widget != control) {
						if (widget instanceof Control) {
							widget = ((Control)widget).getParent();
						} else if (widget instanceof ScrollBar) {
							widget = ((ScrollBar)widget).getParent();
						}else {
							break;
						}
					}
				}
				if (widget == control) {
					String eventString =
						System.currentTimeMillis() +
						"\twidgetEvent\tpart=" + getPartID() +
						"\t" + EXTENSION_MAP.get(event.type) +
						"\tdata=" + new DefaultSWTInterpreter().toString(event);
					Log log = getLog();
					if (log != null) {
						if (targetWidget.isAssignableFrom(widget.getClass())) {
							log.logLine(eventString);
						} else if (event.item != null && targetWidget.isAssignableFrom(event.item.getClass())) {
							log.logLine(eventString);
						}
					}
				}
			}
		}

		private Log getLog() {
			if (log != null) return log;
			ILoggingCategory category = 
				WorkbenchLoggingPlugin.getDefault().getCategoryManager().getCategory(getCategoryID());
			if (category instanceof LoggingCategory) {
				this.log = ((LoggingCategory)category).getLog();
			}
			return log;
		}
		
	}

	private Control control;
	private boolean searchHierarchy;
	private Class<?> targetWidget;
	private Set<Integer> events;
	private IPartWidgetAdapter widgetAdapter;
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.internal.AbstractViewerLogger#createLogger()
	 */
	@Override
	protected IPartLogger createLogger() {
		return new WidgetLogger();
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.internal.AbstractViewerLogger#hookListeners()
	 */
	@Override
	protected void hookListeners() {
		
		if (searchHierarchy) {
			for (Integer event : events) {
				PlatformUI.getWorkbench().getDisplay().addFilter(event, (Listener)getLogger());
			}
		} else {
			if (control != null && !control.isDisposed()) {
				for (Integer event : events) {
					control.addListener(event, (Listener)getLogger());
				}
			}
		}

	}

	/**
	 * @return
	 */
	private Control getControl() {
		return widgetAdapter.findControl(getPartReference());
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.internal.AbstractViewerLogger#unhookListeners()
	 */
	@Override
	protected void unhookListeners() {
		if (searchHierarchy) {
			for (Integer event : events) {
				PlatformUI.getWorkbench().getDisplay().removeFilter(event, (Listener)getLogger());
			}
		} else {
			if (control != null && !control.isDisposed()) {
				for (Integer event : events) {
					control.removeListener(event, (Listener)getLogger());
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.internal.AbstractPartLogger#doHookToPart(org.eclipse.ui.IWorkbenchPart, org.eclipse.core.runtime.IConfigurationElement)
	 */
	@Override
	protected void doHookToPart(IWorkbenchPart part,
			IConfigurationElement element) {
		events = new TreeSet<Integer>();
		String targetWidgetName = element.getAttribute("targetWidget");
		if (targetWidgetName == null) {
			targetWidgetName = "org.eclipse.swt.widgets.Widget";
		}
		String contributer = element.getContributor().getName();
		Bundle contributingBundle = Platform.getBundle(contributer);
		try {
			targetWidget = contributingBundle.loadClass(targetWidgetName);
		} catch (Exception e) {
			WorkbenchLoggingPlugin.getDefault().log(e);
			return;
		}
		widgetAdapter = null;
		try {
			widgetAdapter = (IPartWidgetAdapter) element.createExecutableExtension("adapter");
		} catch (CoreException e) {
		} catch (NullPointerException e) {}
		if (widgetAdapter == null) {
			widgetAdapter = new DefaultPartWidgetAdapter();
			((DefaultPartWidgetAdapter)widgetAdapter).setTargetWidget(targetWidget);
		}
		this.control = getControl();
		this.searchHierarchy = Boolean.parseBoolean(element.getAttribute("searchChildren"));
		
		
		for (IConfigurationElement eventElement : element.getChildren()) {
			Integer swtEvent = EVENT_MAP.get(eventElement.getName());
			if (swtEvent != null) {
				events.add(swtEvent);
			}
		}
		
		
	}

}
