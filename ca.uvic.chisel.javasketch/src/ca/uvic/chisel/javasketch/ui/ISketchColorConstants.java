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
package ca.uvic.chisel.javasketch.ui;

import org.eclipse.swt.graphics.Color;

import ca.uvic.chisel.javasketch.SketchPlugin;

/**
 * Color constants used for this plugin.
 * @author Del Myers
 *
 */
public interface ISketchColorConstants {

	String RED_KEY = "red";
	String GREEN_KEY = "green";
	String BLUE_KEY = "blue";
	String LIGHT_RED_KEY = "lightred";
	String LIGHT_GREEN_KEY = "lightgreen";
	String LIGHT_BLUE_KEY = "lightblue";
	String AMBER_KEY = "amber";
	String LIGHT_AMBER_KEY = "lightamber";
	String PURPLE_KEY = "purple";
	String LIGHT_PURPLE_KEY = "lightpurple";
	String GRAY_KEY = "gray";
	String BLACK_KEY = "black";
	
	Color RED = SketchPlugin.getDefault().getColorRegistry().get(RED_KEY);
	Color GREEN = SketchPlugin.getDefault().getColorRegistry().get(GREEN_KEY);
	Color BLUE = SketchPlugin.getDefault().getColorRegistry().get(BLUE_KEY);
	Color LIGHT_RED = SketchPlugin.getDefault().getColorRegistry().get(LIGHT_RED_KEY);
	Color LIGHT_GREEN = SketchPlugin.getDefault().getColorRegistry().get(LIGHT_GREEN_KEY);
	Color LIGHT_BLUE = SketchPlugin.getDefault().getColorRegistry().get(LIGHT_BLUE_KEY);
	Color AMBER = SketchPlugin.getDefault().getColorRegistry().get(AMBER_KEY);
	Color LIGHT_AMBER = SketchPlugin.getDefault().getColorRegistry().get(LIGHT_AMBER_KEY);
	Color LIGHT_PURPLE = SketchPlugin.getDefault().getColorRegistry().get(PURPLE_KEY);
	Color PURPLE = SketchPlugin.getDefault().getColorRegistry().get(LIGHT_PURPLE_KEY);
	Color BLACK = SketchPlugin.getDefault().getColorRegistry().get(BLACK_KEY);
	Color GRAY = SketchPlugin.getDefault().getColorRegistry().get(GRAY_KEY);
	
	Color ERROR_FG = RED;
	String ERROR_BG_KEY = "error_bg";
	Color ERROR_BG =  SketchPlugin.getDefault().getColorRegistry().get(ERROR_BG_KEY);
	String LOOP_BG_KEY = "loop_bg";
	Color LOOP_BG =  SketchPlugin.getDefault().getColorRegistry().get(LOOP_BG_KEY);
	Color LOOP_FG = BLUE;
	Color CONDITION_FG = GREEN;
	String CONDITION_BG_KEY = "condition_bg";
	Color CONDITION_BG = SketchPlugin.getDefault().getColorRegistry().get(CONDITION_BG_KEY);
	
	Color PRIVATE_FG = RED;
	Color PRIVATE_BG = LIGHT_RED;
	
	Color PUBLIC_FG = GREEN;
	Color PUBLIC_BG = LIGHT_GREEN;
	
	Color FRIEND_FG = BLUE;
	Color FRIEND_BG = LIGHT_BLUE;
	
	Color PROTECTED_FG = AMBER;
	Color PROTECTED_BG = LIGHT_AMBER;
	
	Color PACKAGE = AMBER;
		
	

}
