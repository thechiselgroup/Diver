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
package ca.uvic.chisel.javasketch.ui.internal.presentation.commands;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.internal.preferences.ISketchPluginPreferences;

/**
 * @author Del Myers
 *
 */
public class ToggleDisplayGroupHandler extends AbstractHandler implements IElementUpdater{

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		//just set the preference
		IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
		boolean displayGroups = store.getBoolean(ISketchPluginPreferences.DISPLAY_GROUPS_PREFERENCE);
		store.setValue(ISketchPluginPreferences.DISPLAY_GROUPS_PREFERENCE, !displayGroups);
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.commands.IElementUpdater#updateElement(org.eclipse.ui.menus.UIElement, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
		element.setChecked(store.getBoolean(ISketchPluginPreferences.DISPLAY_GROUPS_PREFERENCE));	
	}

}
