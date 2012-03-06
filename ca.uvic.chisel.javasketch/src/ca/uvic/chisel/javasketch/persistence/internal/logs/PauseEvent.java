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
package ca.uvic.chisel.javasketch.persistence.internal.logs;

/**
 * Simple place-holder for casting purposes.
 * @author Del Myers
 *
 */
public class PauseEvent extends PauseResumeEvent {

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PauseEvent [stackTrace=" + stackTrace + ", time=" + time
				+ ", type=" + type + "]";
	}

}
