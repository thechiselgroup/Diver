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

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import ca.uvic.chisel.logging.eclipse.ICategoryManager;
import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * Manages the logging categories for this logger.
 * @author Del Myers
 *
 */
public class CategoryManager implements ICategoryManager {
	
	private HashMap<String, ILoggingCategory> categories;
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.internal.ICategoryManager#getCategories()
	 */
	public ILoggingCategory[] getCategories() {
		loadCategories();
		Collection<ILoggingCategory> c = categories.values();
		return c.toArray(new ILoggingCategory[c.size()]);
		
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.internal.ICategoryManager#getCategory(java.lang.String)
	 */
	public ILoggingCategory getCategory(String id) {
		loadCategories();
		return categories.get(id);
	}

	/**
	 * 
	 */
	private synchronized void loadCategories() {
		if (categories == null) {
			categories = new HashMap<String, ILoggingCategory>();
			IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("ca.uvic.chisel.logging.eclipse.category");
			for (IConfigurationElement element : point.getConfigurationElements()) {
				if ("category".equals(element.getName())) {
					try {
						LoggingCategory category = new LoggingCategory(element);
						categories.put(category.getCategoryID(), category);
					} catch (Exception e) {
						WorkbenchLoggingPlugin.getDefault().log(e);
					}
				}
			}
		}
	}

}
