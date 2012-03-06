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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.TreeMap;

/**
 * @author Del Myers
 *
 */
public class TraceLogEvent {
	
	
	public static final int VERSION = 0;

	public static final int NONE = 0;
	public static final int METHOD_ENTERED = 1;
	public static final int METHOD_EXITED = 2;
	public static final int PAUSED = 3;
	public static final int RESUMED = 4;
	public static final int THREAD_INIT = 5;
	public static final int HEADER = 6;
	
	
	
	
	public short type;
	private TreeMap<String, String> decorations;
	
	/**
	 * 
	 */
	public TraceLogEvent() {
		decorations = new TreeMap<String, String>();
		type = NONE;
	}
	
	public void decorate(String key, String value) {
		decorations.put(key, value);
	}
	
	public String getDecoration(String key) {
		return decorations.get(key);
	}
	
	protected void load(RandomAccessFile file) throws IOException {
		type = file.readShort();
	}
	
	protected String readShortString(RandomAccessFile file) throws IOException {
		//read the length
		int length = file.readUnsignedShort();
//		String utfString = file.readUTF();
//		return utfString;
		byte[] buffer = new byte[length];
		file.read(buffer);
		Charset charset = Charset.forName("UTF-8");
		CharsetDecoder decoder = charset.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPLACE);
		decoder.replaceWith("]");
		CharBuffer chars = decoder.decode(ByteBuffer.wrap(buffer));
		return chars.toString();
	}
}
