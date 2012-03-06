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
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.IPartLogger;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * @author Del Myers
 *
 */
public abstract class AbstractPartLogger {

	private IWorkbenchPart part;
	private IConfigurationElement configurationElement;
	private String categoryID;
	private IPartLogger logger;
	private Log log;

	/**
	 * 
	 */
	public AbstractPartLogger() {
		super();
	}

	/**
	* Returns the part ID that is being logged by this viewer logger.
	* @return the part ID.
	*/
	public String getPartID() {
		return part.getSite().getId();
	}

	/**
	 * 
	 */
	protected void unhook() {
		unhookListeners();
	}

	protected abstract void unhookListeners();

	protected abstract void hookListeners();

	protected abstract IPartLogger createLogger();

	/**
	 * @param part2
	 * @param elementForViewer
	 */
	protected final void hookToPart(IWorkbenchPart part, String categoryID, IConfigurationElement configurationElement) {
		this.part = part;
		this.configurationElement = configurationElement;
		this.categoryID = categoryID;
		doHookToPart(part, configurationElement);
		hookListeners();
	}
	
	protected abstract void doHookToPart(IWorkbenchPart part, IConfigurationElement elementForViewer);

	/**
	 * @return the configurationElement
	 */
	protected IConfigurationElement getConfigurationElement() {
		return configurationElement;
	}

	/**
	 * @param searchHierarchy
	 * @param fieldClass
	 * @param partClass
	 * @return
	 */
	protected Field findField(boolean searchHierarchy, String fieldClass, Class<?> partClass) {
		Field viewerField = null;
		while (viewerField == null && partClass != null) {
			for (Field field : partClass.getDeclaredFields()) {
				Class<?> viewerClassType = null;
				try {
					ClassLoader loader = field.getType().getClassLoader();
					if (loader == null) {
						loader = ClassLoader.getSystemClassLoader();
					}
					viewerClassType = loader.loadClass(fieldClass);
				} catch (ClassNotFoundException e) {}
				if (viewerClassType != null && viewerClassType.isAssignableFrom(field.getType())) {
					viewerField = field;
					break;
				}
			}
			if (!searchHierarchy) {
				break;
			} else if (viewerField == null) {
				partClass = partClass.getSuperclass();
			}
		}
		return viewerField;
	}

	/**
	 * @param loggedViewer2
	 * @param addMethodName
	 * @return
	 */
	protected Method findMethod(Object loggedViewer, String methodName, Class<?>...parameterTypes) {
		if (loggedViewer == null || methodName == null) {
			return null;
		}
		Method[] methods = loggedViewer.getClass().getMethods();
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				Class<?>[] methodParameterTypes = method.getParameterTypes();
				if (methodParameterTypes.length == parameterTypes.length) {
					boolean match = true;
					for (int i = 0; i < methodParameterTypes.length; i++) {
						if (!methodParameterTypes[i].isAssignableFrom(parameterTypes[i])) {
							match = false;
							break;
						}
					}
					if (match) {
						return method;
					}
				}
			}
		}
		return null;
	}

	/**
	 * @return the part
	 */
	public IWorkbenchPart getPart() {
		return part;
	}
	
	public IWorkbenchPartReference getPartReference() {
		return part.getSite().getPage().getReference(part);
	}

	/**
	 * @return the viewerLogger
	 */
	public IPartLogger getLogger() {
		if (this.logger == null) {
			this.logger = createLogger();
		}
		return logger;
	}

	public String getCategoryID() {
		return categoryID;
		
	}
	
	protected Log getLog() {
		if (log != null) return log;
		ILoggingCategory category = 
			WorkbenchLoggingPlugin.getDefault().getCategoryManager().getCategory(getCategoryID());
		if (category instanceof LoggingCategory) {
			this.log = ((LoggingCategory)category).getLog();
		}
		return log;
	}

}