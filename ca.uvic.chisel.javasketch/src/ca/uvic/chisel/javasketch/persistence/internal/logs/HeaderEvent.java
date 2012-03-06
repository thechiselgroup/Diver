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
package ca.uvic.chisel.javasketch.persistence.internal.logs;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Del Myers
 *
 */
public class HeaderEvent extends TraceLogEvent {
	
	public String identifierString;
	public int versionNumber;
	
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent#load(java.io.RandomAccessFile)
	 */
	@Override
	protected void load(RandomAccessFile file) throws IOException {
		super.load(file);
		versionNumber = file.readUnsignedShort();
		if (versionNumber != VERSION) {
			throw new IOException("Unsupported Version: " + versionNumber);
		}
		identifierString = readShortString(file);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return identifierString;
	}

}
