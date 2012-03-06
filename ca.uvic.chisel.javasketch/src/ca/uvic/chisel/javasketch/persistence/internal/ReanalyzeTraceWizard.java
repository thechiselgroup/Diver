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

import org.eclipse.jface.wizard.Wizard;

import ca.uvic.chisel.javasketch.IProgramSketch;

/**
 * @author Del Myers
 *
 */
public class ReanalyzeTraceWizard extends Wizard {

	private IProgramSketch sketch;
	private ReanalyzeWizardPage page;

	/**
	 * @param sketch
	 */
	public ReanalyzeTraceWizard(IProgramSketch sketch) {
		this.sketch = sketch;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		this.page = new ReanalyzeWizardPage(sketch);
		addPage(page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		sketch.getFilterSettings().setInclusionFilters(page.getInclusionFilters());
		sketch.getFilterSettings().setExclusionFilters(page.getExlusionFilters());
		sketch.getFilterSettings().setUsingProjectClassesOnly(page.isUsingProjectClassesOnly());
//		sketch.getFilterSettings().setReferencedFilterSketch(page.getReferencedSketch());
		if (sketch != null) {
			PersistTraceJob job = new PersistTraceJob(sketch);
			job.schedule();
		}
		return true;
	}

}
