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
package ca.uvic.chisel.javasketch;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;

import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.utils.LaunchConfigurationUtilities;

/**
 * Settings for filters for a program sketch.
 * @author Del Myers
 *
 */
public class FilterSettings {
	private static final String JAVA_SKETCH_FILTER = "javasketch.filter";

	public static final String USE_PROJECT_CLASSES = "javasketch.filters.project";
	
	public static final String USE_PREVIOUS_SKETCH = "javasketch.filters.previous";

	public static final String INCLUSION_FILTERS = "javasketch.filters.include";

	public static final String EXCLUSION_FILTERS = "javasketch.filters.exclude";

	private static final String REFERENCED_PROJECTS = "javasketch.filters.projects";

	private static final String LAUNCH_TYPE = "javasketch.filters.launchtype";
	
	IDialogSettings settings;

	
	private String[] inclusionFilters;
	
	private IJavaProject[] referenceProjects = null;
	
	private ListenerList listeners;
	
	/**
	 * Creates 
	 * @param file
	 * @throws IOException 
	 */
	private FilterSettings() {
		this.settings = new DialogSettings(JAVA_SKETCH_FILTER);
		referenceProjects = new IJavaProject[0];
		listeners = new ListenerList();
		initializeDefaultSettings();
	}
	
	@SuppressWarnings("unchecked")
	public static FilterSettings newSettings(ILaunchConfiguration configuration) throws CoreException {
		FilterSettings settings = new FilterSettings();
		List<String> filters = configuration.getAttribute(INCLUSION_FILTERS, new ArrayList<String>());
		settings.setInclusionFilters(filters.toArray(new String[filters.size()]));
		filters = configuration.getAttribute(EXCLUSION_FILTERS, new ArrayList<String>());
		settings.setExclusionFilters(filters.toArray(new String[filters.size()]));
		settings.setUsingProjectClassesOnly(configuration.getAttribute(USE_PROJECT_CLASSES, true));
		String sketchID = configuration.getAttribute(USE_PREVIOUS_SKETCH, (String)null);
		IProgramSketch sketch = SketchPlugin.getDefault().getSketch(sketchID);
		settings.setReferencedFilterSketch(sketch);
		settings.setReferencedProjects(LaunchConfigurationUtilities.getReferencedJavaProjects(configuration));
		settings.referenceProjects = LaunchConfigurationUtilities.getReferencedJavaProjects(configuration);
		settings.settings.put(LAUNCH_TYPE, configuration.getType().getIdentifier());
		return settings;
	}
	
	private void setReferencedProjects(IJavaProject[] projects) {
		String[] projectNames = new String[projects.length];
		for (int i = 0; i < projectNames.length; i++) {
			projectNames[i] = projects[i].getProject().getName();
		}
		settings.put(REFERENCED_PROJECTS, projectNames);
		referenceProjects = projects;
	}
	
	/**
	 * Loads the settings from the given reader.
	 * @param reader
	 * @param configuration
	 * @return
	 * @throws IOException
	 */
	public static FilterSettings load(Reader reader, ILaunchConfiguration configuration) throws IOException {
		FilterSettings settings = new FilterSettings();
		settings.settings.load(reader);
		String[] projectNames = settings.settings.getArray(REFERENCED_PROJECTS);
		if (projectNames != null) {
			ArrayList<IJavaProject> javaProjects = new ArrayList<IJavaProject>();
			for (String projectName : projectNames) {
				if (projectName != null) {
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					if (project != null && project.exists()) {
						IJavaProject javaProject = JavaCore.create(project);
						javaProjects.add(javaProject);
					}
				}
			}
			settings.referenceProjects = javaProjects.toArray(new IJavaProject[javaProjects.size()]);
		} else {
			IJavaProject[] referenceProjects = LaunchConfigurationUtilities.getReferencedJavaProjects(configuration);
			settings.setReferencedProjects(referenceProjects);
		}
		String launchType = settings.getLaunchType();
		if (launchType == null) {
			try {
				settings.settings.put(LAUNCH_TYPE, configuration.getType().getIdentifier());
			} catch (CoreException e) {
				settings.settings.put(LAUNCH_TYPE, LaunchConfigurationUtilities.JAVA_LAUNCH_TYPE);
			}
		}
		return settings;
	}
	
		
	/**
	 * 
	 */
	private IDialogSettings initializeDefaultSettings() {
		settings.put(USE_PROJECT_CLASSES, true);
		settings.put(INCLUSION_FILTERS, new String[0]);
		settings.put(EXCLUSION_FILTERS, new String[0]);
		settings.put(USE_PREVIOUS_SKETCH, (String)null);
		return settings;
	}
	
	/**
	 * @param settings2
	 * @param jp2
	 * @return
	 */
	public String[] getInclusionFilters() {
		return settings.getArray(INCLUSION_FILTERS);
	}
	
	public String[] getResolvedInclusionFilters() {
		if (isUsingProjectClassesOnly()) {
			if (inclusionFilters == null) {
				//search the java project for packages that should be included.

				LinkedList<String> result = new LinkedList<String>();

				IJavaProject[] jps = referenceProjects;
				HashSet<IPackageFragmentRoot> roots = new HashSet<IPackageFragmentRoot>();
				if (jps != null) {
					for (IJavaProject jp : jps) {
						try {
							roots.addAll(Arrays.asList(jp.getPackageFragmentRoots()));
						} catch (JavaModelException e) {}
					}
				}
				LinkedList<IJavaElement> elements = new LinkedList<IJavaElement>();
				elements.addAll(roots);
				while (elements.size() > 0) {
					IJavaElement element = elements.removeFirst();
					if (element instanceof IPackageFragmentRoot) {
						IPackageFragmentRoot root = (IPackageFragmentRoot) element;
						try {
							if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
								for (IJavaElement child : 
									((IPackageFragmentRoot) element).getChildren()) { 
									if (child instanceof IPackageFragment) {
										elements.add(child);
									}
								}
							}
						} catch (JavaModelException e) {}
					} else if (element instanceof IPackageFragment) {
						IPackageFragment fragment = (IPackageFragment) element;
						try {
							for (IJavaElement child : fragment.getChildren()) {
								if (child instanceof ICompilationUnit) {
									result.add(fragment.getElementName() + ".*");
									break;
								} else if (child instanceof IPackageFragment) {
									elements.add(child);
								}
							}
						} catch (JavaModelException e) {}
					}
				}

				inclusionFilters = result.toArray(new String[result.size()]);
				return inclusionFilters;
			}
		} else if (getReferencedFilterSketch() != null) {
			IProgramSketch sketch = getReferencedFilterSketch();
			if (sketch != this) {
				return sketch.getFilterSettings().getResolvedInclusionFilters();
			}
		}
		return getInclusionFilters();
	}
	
	/**
	 * @param settings2
	 * @return
	 */
	public boolean isUsingProjectClassesOnly() {
		return settings.getBoolean(USE_PROJECT_CLASSES);
	}
	
	public IProgramSketch getReferencedFilterSketch() {
		if (isUsingProjectClassesOnly()) return null;
		String sketchID = settings.get(USE_PREVIOUS_SKETCH);
		return SketchPlugin.getDefault().getSketch(sketchID);
	}
	
	public void setReferencedFilterSketch(IProgramSketch sketch) {
		IProgramSketch old = getReferencedFilterSketch();
		inclusionFilters = null;
		if (sketch == null) {
			settings.put(USE_PREVIOUS_SKETCH, (String)null);
		} else {
			settings.put(USE_PREVIOUS_SKETCH, sketch.getID());
		}
		for (Object listener : listeners.getListeners()) {
			((IFilterChangedListener)listener).referenceChanged(old, this);
		}
	}
 	
	/**
	 * @param settings2
	 * @return
	 */
	public String[] getExclusionFilters() {
		return settings.getArray(EXCLUSION_FILTERS);
	}
	
	public String[] getResolvedExclusionFilters() {
		if (isUsingProjectClassesOnly()) {
			IJavaProject[] jps = referenceProjects;
			if (jps == null || jps.length == 0) {
				//exclude all
				return new String[] {"*"};
			}
			return new String[0];
		}
		IProgramSketch sketch = getReferencedFilterSketch();
		if (sketch != null) {
			LinkedList<String> filters = new LinkedList<String>();
			for (ITraceClass tc : sketch.getTraceData().getClasses()) {
				for (ITraceClassMethod m : tc.getMethods()) {
					filters.add(tc.getName() + "." + m.getName() + m.getSignature());
				}
			}
			return filters.toArray(new String[filters.size()]);
		}
		return getExclusionFilters();
	}
	
	/**
	 * Returns the index of an invalid character, or -1 when valid.
	 * @param value
	 * @return
	 */
	public static int isValidFilterString(String s) {
		//make sure that the only characters in the string are *'s, dots, and characters.
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '*') {
//				if (i > 0) {
//					if (i < s.length()-1) {
//						return i;
//					} else {
//						if (s.length()==2) {
//							return i;
//						}
//					}
//				}
			} else if (c == '.') {
				if (i == 0) {
					return i;
				} else if (i == s.length()-1) {
					return i;
				} else {
					if (s.charAt(i-1) == '.') {
						return i;
					}
				}
			} else if (i == 0 && !Character.isJavaIdentifierStart(c)) {
				return i;
			} else if (!Character.isJavaIdentifierPart(c)) {
				return i;
			}
		}
		return -1;
	}
	
	public void save(File file) throws IOException {
		settings.save(file.getCanonicalPath());
	}


	/**
	 * @param array
	 */
	public void setInclusionFilters(String[] array) {
		String[] old = inclusionFilters;
		this.inclusionFilters = null;
		for (String s : array) {
			int i = isValidFilterString(s);
			if (i >= 0) {
				throw new IllegalArgumentException("Illegal character" + s.charAt(i) + " in filter ");
			}
		}
		settings.put(INCLUSION_FILTERS, array);
		for (Object listener : listeners.getListeners()) {
			((IFilterChangedListener)listener).inclusionChanged(old, this);
		}
	}
	
	public void setExclusionFilters(String[] array) {
		String[] old = settings.getArray(EXCLUSION_FILTERS);
		this.inclusionFilters = null;
		for (String s : array) {
			int i = isValidFilterString(s);
			if (i >= 0) {
				throw new IllegalArgumentException("Illegal character" + s.charAt(i) + " in filter ");
			}
		}
		settings.put(EXCLUSION_FILTERS, array);
		for (Object listener : listeners.getListeners()) {
			((IFilterChangedListener)listener).exclusionChanged(old, this);
		}
	}


	/**
	 * @param attribute
	 */
	public void setUsingProjectClassesOnly(boolean value) {
		if (isUsingProjectClassesOnly() == value) return;
		this.inclusionFilters = null;
		settings.put(USE_PROJECT_CLASSES, value);
		for (Object listener : listeners.getListeners()) {
			((IFilterChangedListener)listener).projectClassesChanged(!value, this);
		}
	}

	/**
	 * @param iFilterChangedListener
	 */
	public void addFilterChangedListener(
			IFilterChangedListener listener) {
		listeners.add(listener);
	}
	
	public void removeFilterChangedListener(IFilterChangedListener listener) {
		listeners.remove(listener);
	}

	/**
	 * @return
	 */
	public IJavaProject[] getJavaProjects() {
		return referenceProjects;
	}

	/**
	 * @return
	 */
	public String getLaunchType() {
		return settings.get(LAUNCH_TYPE);
	}
}
