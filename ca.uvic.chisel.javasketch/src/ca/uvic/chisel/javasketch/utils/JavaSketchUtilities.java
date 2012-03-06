/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.utils;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

import ca.uvic.chisel.javasketch.IProgramSketch;

/**
 * @author Del Myers
 *
 */
public class JavaSketchUtilities {

	
	
	
	/**
	 * Attempts to create a Java search scope for the given configuration, if possible. Otherwise, returns null.
	 * @param cf the configuration to create the scope from.
	 * @return the new search scope, or null if none could be created.
	 */
	public static IJavaSearchScope createJavaSearchScope(IProgramSketch sketch) {
		
		//get the launch type from the filters first
		String launchType = sketch.getFilterSettings().getLaunchType();
		if (LaunchConfigurationUtilities.ECLIPSE_LAUNCH_TYPE.equals(launchType)) {
			return createPluginSearchScope();
		}
		
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList<IJavaProject> javaProjects = new ArrayList<IJavaProject>();
		for (IProject project : projects) {
			try {
				if (project.isOpen() && project.isAccessible() && project.hasNature(JavaCore.NATURE_ID)) {
					javaProjects.add(JavaCore.create(project));
				}
			} catch (CoreException e) {
				//do nothing, just ignore
			}
		}
		if (projects.length > 0) {
			return SearchEngine.createJavaSearchScope(javaProjects.toArray(new IJavaProject[javaProjects.size()]));
		}
		return null;
	}

	/**
	 * Makes the search scope based on all of the plugins in the target.
	 * @return
	 */
	private static IJavaSearchScope createPluginSearchScope() {
		HashSet<IJavaElement> scope = new HashSet<IJavaElement>();
		for (IPluginModelBase base : PluginRegistry.getActiveModels()) {
			IResource resource = base.getUnderlyingResource();
			if (resource != null) {
				IJavaProject jp = JavaCore.create(resource.getProject());
				if (jp != null) {
					scope.add(jp);
				}
			}
		}
		//add the default proxy search project
		IProject proxy = ResourcesPlugin.getWorkspace().getRoot().getProject("External Plug-in Libraries");
		if (proxy != null) {
			IJavaProject jp = JavaCore.create(proxy);
			scope.add(jp);
		}
		return SearchEngine.createJavaSearchScope(scope.toArray(new IJavaElement[scope.size()]));
	}
	
	
}
