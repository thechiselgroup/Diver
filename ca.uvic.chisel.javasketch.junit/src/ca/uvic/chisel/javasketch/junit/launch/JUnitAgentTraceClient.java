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
package ca.uvic.chisel.javasketch.junit.launch;

import ca.uvic.chisel.javasketch.launching.internal.JavaAgentTraceClient;

/**
 * A trace client for JUnit traces. You can't pause or resume JUnit traces.
 * @author Del
 *
 */
public class JUnitAgentTraceClient extends JavaAgentTraceClient {
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.internal.JavaAgentTraceClient#canPauseTrace()
	 */
	@Override
	public boolean canPauseTrace() {
		return false;
	}

}
