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
package ca.uvic.chisel.javasketch.launching.ui;

import static ca.uvic.chisel.javasketch.FilterSettings.EXCLUSION_FILTERS;
import static ca.uvic.chisel.javasketch.FilterSettings.INCLUSION_FILTERS;
import static ca.uvic.chisel.javasketch.FilterSettings.USE_PREVIOUS_SKETCH;
import static ca.uvic.chisel.javasketch.FilterSettings.USE_PROJECT_CLASSES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.persistence.ui.internal.FiltersComposite;
import ca.uvic.chisel.javasketch.ui.IFilterContext;
import ca.uvic.chisel.javasketch.ui.ISketchImageConstants;
import ca.uvic.chisel.javasketch.utils.LaunchConfigurationUtilities;

/**
 * @author Del Myers
 *
 */
public class FilterTab extends AbstractLaunchConfigurationTab implements IFilterContext{
//	public static final String USE_PROJECT_CLASSES = "javasketch.filters.project";
//
//	public static final String INCLUSION_FILTERS = "javasketch.filters.include";
//
//	public static final String EXCLUSION_FILTERS = "javasketch.filters.exclude";
//	
	public static final String PAUSE_ON_START = "javasketch.launch.pause";
	
	public static final String APPLY_AT_RUNTIME = "javasketch.launch.apply-filters";
//	
//	public static final String USE_PREVIOUS_SKETCH = "javasketch.filters.previous";

	private FiltersComposite filterComposite;

	private Button pauseOnStart;
	
	private Button applyAtRuntime;

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new GridLayout());
		createTraceOptions(page);
		this.filterComposite = new FiltersComposite(page);
		filterComposite.setParentContext(this);
		filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		setControl(page);
	}

	private void createTraceOptions(Composite page) {
		pauseOnStart = new Button(page, SWT.CHECK);
		pauseOnStart.setText("Pause On Start");
		pauseOnStart.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
				getLaunchConfigurationDialog().updateButtons();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		pauseOnStart.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {}
			
			@Override
			public void focusGained(FocusEvent e) {
				getShell().setDefaultButton(pauseOnStart);
			}
		});
		pauseOnStart.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));
		
		applyAtRuntime = new Button(page, SWT.CHECK);
		applyAtRuntime.setText("Apply Filters at Runtime");
		applyAtRuntime.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
				getLaunchConfigurationDialog().updateButtons();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		applyAtRuntime.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {}
			
			@Override
			public void focusGained(FocusEvent e) {
				getShell().setDefaultButton(applyAtRuntime);
			}
		});
		applyAtRuntime.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	@Override
	public String getName() {
		return "Java Trace";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			filterComposite.setLaunchType(configuration.getType().getIdentifier());
			IJavaProject[] projects = LaunchConfigurationUtilities.getReferencedJavaProjects(configuration);
			filterComposite.setJavaContext(projects);
			List<String> filters = configuration.getAttribute(INCLUSION_FILTERS, new ArrayList<String>());
			filterComposite.setInclusionFilters(filters.toArray(new String[filters.size()]));
			filters = configuration.getAttribute(EXCLUSION_FILTERS, new ArrayList<String>());
			filterComposite.setExclusionFilters(filters.toArray(new String[filters.size()]));
			boolean projectsOnly = configuration.getAttribute(USE_PROJECT_CLASSES, true);
			String referenceId = configuration.getAttribute(USE_PREVIOUS_SKETCH, (String)null);
			IProgramSketch referenceSketch = SketchPlugin.getDefault().getSketch(referenceId);
			if (projectsOnly) {
				filterComposite.setFilterShortcut(FiltersComposite.SHORTCUT_PROJECT_CLASSES);
			} else if (referenceSketch != null) {
				filterComposite.setFilterShortcut(FiltersComposite.SHORTCUT_PREVIOUS_SKETCH);
			} else {
				filterComposite.setFilterShortcut(FiltersComposite.SHORTCUT_NONE);
			}
			
			pauseOnStart.setSelection(configuration.getAttribute(PAUSE_ON_START, true));
			applyAtRuntime.setSelection(configuration.getAttribute(APPLY_AT_RUNTIME, true));
		} catch (CoreException e) {
			SketchPlugin.getDefault().log(e);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		String[] filters = filterComposite.getInclusionFilters();
		configuration.setAttribute(INCLUSION_FILTERS, Arrays.asList(filters));
		filters = filterComposite.getExclusionFilters();
		configuration.setAttribute(EXCLUSION_FILTERS, Arrays.asList(filters));
		configuration.setAttribute(USE_PROJECT_CLASSES, filterComposite.isUsingProjectClassesOnly());

		configuration.setAttribute(PAUSE_ON_START, pauseOnStart.getSelection());
		configuration.setAttribute(APPLY_AT_RUNTIME, applyAtRuntime.getSelection());
		//setDirty(false);
		
	}
	
	protected Button getApplyAtRuntimeButton() {
		return applyAtRuntime;
	}
	
	protected Button getPauseOnStartButton() {
		return pauseOnStart;
	}
	
	protected FiltersComposite getFiltersComposite() {
		return filterComposite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(USE_PROJECT_CLASSES, true);
		configuration.setAttribute(INCLUSION_FILTERS, new ArrayList<String>());
		configuration.setAttribute(EXCLUSION_FILTERS, new ArrayList<String>());
		configuration.setAttribute(PAUSE_ON_START, true);
		configuration.setAttribute(APPLY_AT_RUNTIME, false);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.persistence.ui.internal.IFilterContext#filterChanged(ca.uvic.chisel.javasketch.persistence.ui.internal.FiltersComposite)
	 */
	@Override
	public void filterChanged() {
		setDirty(true);
		getLaunchConfigurationDialog().updateButtons();
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.persistence.ui.internal.IFilterContext#contextChanged(org.eclipse.jdt.core.IJavaProject[])
	 */
	@Override
	public void contextChanged(IJavaProject[] newProjects) {
		filterComposite.setJavaContext(newProjects);				
	}
	
	@Override
	public Image getImage() {
		return SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_TRACE);
	}

}
