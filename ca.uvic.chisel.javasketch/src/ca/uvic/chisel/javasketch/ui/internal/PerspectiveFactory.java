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
package ca.uvic.chisel.javasketch.ui.internal;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;

import ca.uvic.chisel.javasketch.ui.internal.presentation.JavaThreadSequenceView;
import ca.uvic.chisel.javasketch.ui.internal.views.TraceNavigator;
import ca.uvic.chisel.javasketch.ui.internal.views.java.TraceOutlineView;

/**
 * @author Del Myers
 *
 */
public class PerspectiveFactory implements IPerspectiveFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		
		IFolderLayout javaFolderArea = layout.createFolder("java", IPageLayout.LEFT, .25f, editorArea);
		javaFolderArea.addView(JavaUI.ID_PACKAGES);
		javaFolderArea.addPlaceholder(JavaUI.ID_TYPE_HIERARCHY);
		javaFolderArea.addPlaceholder(IPageLayout.ID_RES_NAV);
		
		IFolderLayout outlineArea = layout.createFolder("outline", IPageLayout.RIGHT, .75f, editorArea);
		outlineArea.addView(IPageLayout.ID_OUTLINE);
		outlineArea.addView(TraceOutlineView.VIEW_ID);
		
		IFolderLayout launchArea = layout.createFolder("launch", IPageLayout.BOTTOM, .25f, "outline");
		launchArea.addView(IDebugUIConstants.ID_DEBUG_VIEW);
		
		IFolderLayout debugArea = layout.createFolder("debug", IPageLayout.BOTTOM, .38f, "launch");
		debugArea.addView(IDebugUIConstants.ID_VARIABLE_VIEW);
		debugArea.addView(IDebugUIConstants.ID_BREAKPOINT_VIEW);
		debugArea.addPlaceholder(IDebugUIConstants.ID_EXPRESSION_VIEW);
		debugArea.addPlaceholder(IDebugUIConstants.ID_MEMORY_VIEW);
		debugArea.addPlaceholder(IDebugUIConstants.ID_REGISTER_VIEW);
		
		IFolderLayout traceArea = layout.createFolder("trace", IPageLayout.BOTTOM, .5f, "java");
		traceArea.addView(TraceNavigator.VIEW_ID);
		
		IPlaceholderFolderLayout logArea = layout.createPlaceholderFolder("error", IPageLayout.BOTTOM, .25f, "trace");
		logArea.addPlaceholder("org.eclipse.ui.views.log");
		
		IFolderLayout graphicsArea = layout.createFolder("graphics", IPageLayout.BOTTOM, .5f, editorArea);
		graphicsArea.addView(JavaThreadSequenceView.VIEW_ID);
		graphicsArea.addView("org.eclipse.ui.console.ConsoleView");
		
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ELEMENT_CREATION_ACTION_SET);
		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);
		
		

	}

}
