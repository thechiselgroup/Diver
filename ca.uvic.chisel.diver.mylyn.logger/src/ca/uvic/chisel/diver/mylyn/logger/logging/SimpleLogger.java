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
package ca.uvic.chisel.diver.mylyn.logger.logging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.context.core.IInteractionContextManager;

import ca.uvic.chisel.diver.mylyn.logger.MylynLogger;

/**
 * @author Del
 *
 */
public class SimpleLogger {
	
	PrintStream output;
	
	public SimpleLogger() throws FileNotFoundException {
		IPath state = MylynLogger.getDefault().getStateLocation();
		File file = new File(state.toFile(), "simpleLog.log");
		FileOutputStream fStream = new FileOutputStream(file, true);
		BufferedOutputStream bStream = new BufferedOutputStream(fStream);
		output = new PrintStream(bStream, true);
	}
	
	public synchronized void logLine(String line) {
		if (output != null) {
			String eventString = "time=" + System.currentTimeMillis();
			IInteractionContextManager manager = ContextCore.getContextManager();
			IInteractionContext context = manager.getActiveContext();
			eventString += "\tcontext=" + ((context != null) ? context.getHandleIdentifier() : "null");
			output.println(eventString + "\t" + line);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected synchronized void finalize() throws Throwable {
		if (output != null) {
			try {
				output.close();
			} catch (Exception e) {
				
			}
		}
		super.finalize();
	}

}
