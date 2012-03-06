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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import ca.uvic.chisel.logging.eclipse.ICategoryManager;
import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * Used for finding the logs associated with different workbench parts or
 * perspectives.
 * @author Del Myers
 *
 */
public class WorkbenchLogger {
	private class FilterPattern {
		private String filter;
		private Pattern pattern;

		public FilterPattern(String filter) {
			this.filter = filter;
			this.pattern = Pattern.compile(filter);
		}
		
		/**
		 * @return the pattern
		 */
		public Pattern getPattern() {
			return pattern;
		}
		
		/**
		 * @return the filter
		 */
		public String getFilter() {
			return filter;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj == null || !this.getClass().equals(obj.getClass())) {
				return false;
			}
			return this.filter.equals(((FilterPattern)obj).getFilter());
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return filter.hashCode();
		}
		
	}

	private static final WorkbenchLogger DEFAULT = new WorkbenchLogger();
	
	private HashMap<FilterPattern, HashSet<LoggingCategory>> logMap;
	
	private synchronized void load() {
		if (logMap != null) {
			return;
		}
		logMap = new HashMap<FilterPattern, HashSet<LoggingCategory>>();
		IConfigurationElement[] elements =
			Platform.getExtensionRegistry().getConfigurationElementsFor("ca.uvic.chisel.logging.eclipse.loggers");
		ICategoryManager categoryManager = WorkbenchLoggingPlugin.getDefault().getCategoryManager();
		
		for (IConfigurationElement element : elements) {
			String categoryID = element.getAttribute("categoryID");
			if (categoryID != null) {
				ILoggingCategory ic = categoryManager.getCategory(categoryID);
				if (!(ic instanceof LoggingCategory)) {
					continue;
				}
				LoggingCategory lc = (LoggingCategory) ic;
				//now search for the workbench loggers
				IConfigurationElement[] workbenchElements = 
					element.getChildren("workbench");
				for (IConfigurationElement we : workbenchElements) {
					IConfigurationElement[] lifecycleElements = 
						we.getChildren("lifecycle");
					for (IConfigurationElement lce : lifecycleElements) {
						String filter = lce.getAttribute("filter");
						if (filter != null) {
							//create a pattern for the filter
							filter = filter.replace(".", "\\.");
							filter = filter.replace("*", ".*");
							FilterPattern pattern = new FilterPattern(filter);
							
							HashSet<LoggingCategory> categories = logMap.get(pattern);
							if (categories == null) {
								categories = new HashSet<LoggingCategory>();
								logMap.put(pattern, categories);
							}
							categories.add(lc);
						}
						
					}
				}
			}
		}
			
	}
	
	public static Set<LoggingCategory> getMatchingCategories(String filterString) {
		DEFAULT.load();
		Set<LoggingCategory> matched = new HashSet<LoggingCategory>();
		for (FilterPattern pattern : DEFAULT.logMap.keySet()) {
			if (pattern.getPattern().matcher(filterString).matches()) {
				matched.addAll(DEFAULT.logMap.get(pattern));
			}
		}
		return matched;
	}

}
