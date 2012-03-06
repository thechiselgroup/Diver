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
package ca.uvic.chisel.javasketch.junit.launch.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import ca.uvic.chisel.javasketch.junit.JUnitSketchPlugin;
import ca.uvic.chisel.javasketch.launching.ui.FilterTab;

/**
 * @author Del
 *
 */
public class JUnitFilterTab extends FilterTab {
	
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.ui.FilterTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		// TODO Auto-generated method stub
		super.initializeFrom(configuration);
		try {
			if (configuration.getAttribute(PAUSE_ON_START, false)) {
				JUnitSketchPlugin.getDefault().log(new Status(IStatus.WARNING, JUnitSketchPlugin.PLUGIN_ID, "Cannot pause a JUnit trace. This launch attribute will be ignored."));
			} else if (configuration.getAttribute(APPLY_AT_RUNTIME, false)) {
				JUnitSketchPlugin.getDefault().log(new Status(IStatus.WARNING, JUnitSketchPlugin.PLUGIN_ID, "Cannot apply filters to a JUnit trace. This launch attribute will be ignored."));
			}
			getApplyAtRuntimeButton().setSelection(false);
			getPauseOnStartButton().setSelection(false);
			getApplyAtRuntimeButton().setEnabled(false);
			getPauseOnStartButton().setEnabled(false);
		} catch (CoreException ex) {
			JUnitSketchPlugin.getDefault().log(ex);
		}

	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.ui.FilterTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		//the configuration is never allowed to have "pause on start" or
		//apply filters at runtime available
		configuration.setAttribute(PAUSE_ON_START, false);
		configuration.setAttribute(APPLY_AT_RUNTIME, false);
	}

}
