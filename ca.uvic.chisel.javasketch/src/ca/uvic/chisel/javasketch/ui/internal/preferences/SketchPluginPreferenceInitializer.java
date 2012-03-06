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
package ca.uvic.chisel.javasketch.ui.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.internal.SketchUI;

/**
 * Initializes the preferences for this plug-in
 * @author Del Myers
 *
 */
public class SketchPluginPreferenceInitializer extends
		AbstractPreferenceInitializer {

	/**
	 * 
	 */
	public SketchPluginPreferenceInitializer() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
		store.setDefault(ISketchPluginPreferences.COMPACT_LOOPS_PREFERENCE, true);
		store.setDefault(ISketchPluginPreferences.DISPLAY_GROUPS_PREFERENCE, true);
		store.setDefault(SketchUI.PREFERENCE_FILTER_PACKAGE_EXPLORER, true);
		//in previous versions, the package explorer filter was off by default
		//we want to turn it on.
		store.setDefault("preference.packageExplore.update", false);
		if (!store.getBoolean("preference.packageExplore.update")) {
			store.setValue("preference.packageExplore.update", true);
			store.setValue(SketchUI.PREFERENCE_FILTER_PACKAGE_EXPLORER, true);
		}
		
	}

}
