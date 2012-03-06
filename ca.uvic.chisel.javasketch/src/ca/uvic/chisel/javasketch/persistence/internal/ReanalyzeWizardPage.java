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
package ca.uvic.chisel.javasketch.persistence.internal;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.persistence.ui.internal.FiltersComposite;
import ca.uvic.chisel.javasketch.ui.IFilterContext;

/**
 * @author Del Myers
 *
 */
public class ReanalyzeWizardPage extends WizardPage implements IFilterContext {

	private IProgramSketch sketch;

	/**
	 * @param pageName
	 */
	protected ReanalyzeWizardPage(IProgramSketch sketch) {
		super("Reanalyze Trace");
		this.sketch = sketch;
		setImageDescriptor(SketchPlugin.imageDescriptorFromPlugin("images/wizban/analyze.png"));
		setTitle("Reanalyze Trace");
		setDescription("Reanalyze a trace using different filter settings");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	private FiltersComposite filterControl;
	private String[] inclusion;
	private String[] exclusion;
	private boolean isUsingProjectClassesOnly;
//	private IProgramSketch referencedSketch;

	@Override
	public void createControl(Composite parent) {
		parent.setLayout(new FillLayout());
		this.filterControl = new FiltersComposite(parent);
		updateFromSketch();
		filterControl.setParentContext(this);
		filterControl.setJavaContext(sketch.getFilterSettings().getJavaProjects());
		filterControl.setLaunchType(sketch.getFilterSettings().getLaunchType());
		setControl(filterControl);
	}

	/**
	 * 
	 */
	private void updateFromSketch() {
		this.inclusion = sketch.getFilterSettings().getInclusionFilters();
		this.exclusion = sketch.getFilterSettings().getExclusionFilters();
		this.isUsingProjectClassesOnly = sketch.getFilterSettings().isUsingProjectClassesOnly();
//		this.referencedSketch = sketch.getFilterSettings().getReferencedFilterSketch();
		filterControl.setInclusionFilters(inclusion);
		filterControl.setExclusionFilters(exclusion);
//		if (referencedSketch != null) {
//			filterControl.setReferencedFilterSketch(referencedSketch);
//		}
		if (isUsingProjectClassesOnly) {
			filterControl.setFilterShortcut(FiltersComposite.SHORTCUT_PROJECT_CLASSES);
//		} else if (referencedSketch != null) {
//			filterControl.setFilterShortcut(FiltersComposite.SHORTCUT_PREVIOUS_SKETCH);
		} else {
			filterControl.setFilterShortcut(FiltersComposite.SHORTCUT_NONE);
		}
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.persistence.ui.internal.IFilterContext#filterChanged(ca.uvic.chisel.javasketch.persistence.ui.internal.FiltersComposite)
	 */
	@Override
	public void filterChanged() {
		this.inclusion = filterControl.getInclusionFilters();
		this.exclusion = filterControl.getExclusionFilters();
		this.isUsingProjectClassesOnly = filterControl.isUsingProjectClassesOnly();
//		this.referencedSketch = composite.getReferencedFilterSketch();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	@Override
	public boolean isPageComplete() {
		return true;
	}

	/**
	 * @return
	 */
	public String[] getInclusionFilters() {
		return inclusion;
	}
	
	/**
	 * @return the isUsingProjectClassesOnly
	 */
	public boolean isUsingProjectClassesOnly() {
		return isUsingProjectClassesOnly;
	}

	/**
	 * @return
	 */
	public String[] getExlusionFilters() {
		return exclusion;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.ui.IFilterContext#contextChanged(org.eclipse.jdt.core.IJavaProject[])
	 */
	@Override
	public void contextChanged(IJavaProject[] newProjects) {
		filterControl.setJavaContext(newProjects);
	}
	
	/**
	 * @return the referencedSketch
	 */
//	public IProgramSketch getReferencedSketch() {
//		return referencedSketch;
//	}
}
