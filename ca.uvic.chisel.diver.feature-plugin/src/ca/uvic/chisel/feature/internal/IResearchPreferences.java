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
package ca.uvic.chisel.feature.internal;

/**
 * Preferences for the research plugin.
 * @author Del Myers
 *
 */
public interface IResearchPreferences {

	/**
	 * Preference for the last date queried. Stored as a long, according
	 * to the current system time.
	 */
	String LAST_QUERY_DATE = "query"; //$NON-NLS$
	/**
	 * Preference to indicate whether or not the user has participated in
	 * the study. Stored as a boolean.
	 */
	String HAS_PARTICIPATED = "participated"; //$NON-NLS$
	String REMIND_PARTICIPATE = "will.participate"; //$NON-NLS$

}
