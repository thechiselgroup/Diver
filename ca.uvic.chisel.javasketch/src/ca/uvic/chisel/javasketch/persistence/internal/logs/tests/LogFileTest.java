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
package ca.uvic.chisel.javasketch.persistence.internal.logs.tests;

import java.io.File;
import java.io.IOException;

import ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLog;
import ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent;

/**
 * @author Del Myers
 *
 */
public class LogFileTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			TraceLog log = new TraceLog(new File("c:\\test.dat"));
			TraceLogEvent event = null;
			while ((event = log.nextEvent()) != null) {
				System.out.println(event.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
