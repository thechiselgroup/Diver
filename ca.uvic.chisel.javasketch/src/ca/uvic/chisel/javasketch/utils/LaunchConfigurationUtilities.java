/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

/**
 * A set of utility methods for extracting information from launch configurations.
 * @author Del Myers
 *
 */
public class LaunchConfigurationUtilities {
	
	/**
	 * Indicates that a launch configuration is set up for tracing eclipse applications.
	 */
	public static final String ECLIPSE_LAUNCH_TYPE = "ca.uvic.chisel.javasketch.eclipseTraceConfiguration";
	/**
	 * Indicates that a launch configuration is set up for tracing java applications.
	 */
	public static final String JAVA_LAUNCH_TYPE = "ca.uvic.chisel.javasketch.javaTraceConfiguration";
	
	/**
	 * Used just in case the launch configuration can contain multiple projects
	 */
	public static final String ATTR_PROJECT_NAMES = "ca.uvic.chisel.javasketch.trace.projects";
	
	/**
	 * Returns the launch configuration in the workbench that has the given name.
	 * @param name the name of the configuration.
	 * @return the configuration with the given name, or null if none can be found.
	 */
	public static ILaunchConfiguration getLaunchConfiguration(String name) {
		try {
			for (ILaunchConfiguration cf : DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations()) {
				if (cf.getName().equals(name)) {
					return cf;
				}
			}
		} catch (CoreException e) {}
		return null;
	}
	
	/**
	 * Returns the launch configurations associated with the given project, if any exist.
	 * @param project the project to query.
	 * @return the launch configurations associated with the given project, if any exist. An empty list if none.
	 */
	public static ILaunchConfiguration[] getLaunchConfigurations(IProject project) {
		ArrayList<ILaunchConfiguration> result = new ArrayList<ILaunchConfiguration>();
		ILaunchConfiguration[] cfs;
		try {
			cfs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();
		} catch (CoreException e) {
			return new ILaunchConfiguration[0];
		}
		for (ILaunchConfiguration cf : cfs) {
			if (project.equals(getProjectForConfiguration(cf))) {
				result.add(cf);
			}
		}
		return null;
		
	}

	/**
	 * Returns the project associated with the given launch configuration, if it is available. Null otherwise.
	 * @param cf the configuration to check.
	 * @return the project associated with the given launch configuration, if it is available. Null otherwise.
	 */
	public static IProject getProjectForConfiguration(ILaunchConfiguration cf) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		try {
			String projectName = cf.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null);
			if (projectName != null) {
				return root.getProject(projectName);
			}
		} catch (CoreException e) {
		}
		return null;
	}
	
	
	
	/**
	 * Searches the configuration to find plugins that are associated with it. Returns an empty list if none
	 * are found.
	 * @param cf the configuration.
	 * @return plugins that are associated with the configuration. Returns an empty list if none
	 * are found.
	 * @throws CoreException 
	 */
	public static IPluginBase[] getPluginsForConfiguration(ILaunchConfiguration cf) throws CoreException {
		if (!ECLIPSE_LAUNCH_TYPE.equals(cf.getType().getIdentifier())) {
			return new IPlugin[0];
		}
		Boolean useDefault = cf.getAttribute(IPDELauncherConstants.USE_DEFAULT, Boolean.FALSE);
		Boolean useFeatures = cf.getAttribute(IPDELauncherConstants.USEFEATURES, Boolean.FALSE);
		Boolean automaticAdd = cf.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, Boolean.FALSE);
		if (!(useDefault || useFeatures || automaticAdd)) {
			//not a plugin launch, return no plugins.
			return new IPlugin[0];
		}
		HashSet<IPluginBase> plugins = new HashSet<IPluginBase>();
		if (!(useDefault && useFeatures)) {
			if (automaticAdd) {
				plugins = getActivePlugins();
				List<?> deselected = cf.getAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, (List<?>)null);
				if (deselected != null) {
					for (Object o : deselected) {
						if (o instanceof String) {
							IPluginModelBase mb = PluginRegistry.findModel(o.toString().trim());
							if (mb != null) {
								IPluginBase pb = mb.getPluginBase();
								if (pb != null) {
									plugins.remove(pb);
								}
							}
						}
					}
				}
			} else {
				//first, add the workspace plugins
				List<?> selected = cf.getAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_PLUGINS, (List<?>)null);
				if (selected != null) {
					for (Object o : selected) {
						if (o instanceof String) {
							IPluginModelBase mb = PluginRegistry.findModel(o.toString().trim());
							if (mb != null) {
								IPluginBase pb = mb.getPluginBase();
								if (pb != null) {
									plugins.remove(pb);
								}
							}
						}
					}
				}
				//now, add the target plugins
				selected = cf.getAttribute(IPDELauncherConstants.SELECTED_TARGET_PLUGINS, (List<?>)null);
				if (selected != null) {
					for (Object o : selected) {
						if (o instanceof String) {
							IPluginModelBase mb = PluginRegistry.findModel(o.toString().trim());
							if (mb != null) {
								IPluginBase pb = mb.getPluginBase();
								if (pb != null) {
									plugins.remove(pb);
								}
							}
						}
					}
				}
				
			}
		}
		return plugins.toArray(new IPluginBase[plugins.size()]);
	}
	
	private static HashSet<IPluginBase> getActivePlugins() {
		IPluginModelBase[] workspaceModels = PluginRegistry.getWorkspaceModels();
		HashSet<IPluginBase> plugins = new HashSet<IPluginBase>();
		for (IPluginModelBase wm : workspaceModels) {
			IPluginBase pb = wm.getPluginBase();
			if (pb != null) {
				plugins.add(pb);
			}
		}
		return plugins;
	}
	
	public static IProject[] getProjectsForConfiguration(ILaunchConfiguration cf) {
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			List<?> projectNames = cf.getAttribute(ATTR_PROJECT_NAMES, Collections.EMPTY_LIST);
			if (projectNames.size() > 0) {
				ArrayList<IProject> projects = new ArrayList<IProject>();
				for (Object projectName : projectNames) {
					if (projectName instanceof String) {
						IProject project = root.getProject((String)projectName);
						if (project != null && project.exists()) {
							projects.add(project);
						}
					}
				}
				return projects.toArray(new IProject[projects.size()]);
			} else {
				IProject project = getProjectForConfiguration(cf);
				if (project != null) {
					return new IProject[] {project};
				}
				return new IProject[0];
			}
		} catch (IllegalArgumentException e) {
			
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new IProject[0];
	}

	/**
	 * @param tracedLaunchConfiguration
	 * @return
	 */
	public static IJavaProject[] getReferencedJavaProjects(
			ILaunchConfiguration cf) {
		try {
			if (ECLIPSE_LAUNCH_TYPE.equals(cf.getType().getIdentifier())) {
				HashSet<IJavaProject> projects = new HashSet<IJavaProject>();
				try {
					for (IPluginBase pb : getPluginsForConfiguration(cf)) {
						IModel model = pb.getModel();
						IResource resource = model.getUnderlyingResource();
						if (resource != null) {
							IJavaProject jp = JavaCore.create(resource.getProject());
							if (jp != null) {
								projects.add(jp);
							}
						}
					}
				} catch (CoreException e) {}
				return projects.toArray(new IJavaProject[projects.size()]);
			}
		} catch (CoreException e) {
			return new IJavaProject[0];
			                     
		}
		IProject[] projects = getProjectsForConfiguration(cf);
		if (projects.length > 0) {
			HashSet<IJavaProject> javaProjects = new HashSet<IJavaProject>();
			for (int i = 0; i < projects.length; i++) {
				IJavaProject jp = JavaCore.create(projects[i]);
				if (jp != null) {
					javaProjects.add(jp);
				}
			}
			return javaProjects.toArray(new IJavaProject[javaProjects.size()]);
		}
		return new IJavaProject[0];
	}

}
