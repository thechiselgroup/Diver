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
package ca.gc.drdc.oasis.tracing.cjvmtracer.internal;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

import ca.uvic.chisel.javasketch.SketchPlugin;

/**
 * Interface for commands supported by the Oasis trace server.
 * @author Del Myers
 *
 */
public class OasisCommand {
	/**
	 * Version of this object.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The "command acknowledged" command. Sent to clients after a command has been
	 * received. The data of the command will always be the value of the command last
	 * received. In case of an error (for example, the server is in the wrong state to
	 * receive a command), an acknowledgement will not be sent; an error will be sent
	 * instead.
	 */
	public static final char ACK_COMMAND = 'a';
	final short ACK_COMMAND_LENGTH = 1;

	/**
	 * An error command. Sent by the server to the client to indicate that an error has
	 * occurred. The data will be an ASCII character array with a length equal to
	 * the size of data_len will always be 8, and the data will be an error code as
	 * defined in oasis_errors.h.
	 */
	public static final char ERROR_COMMAND = 'e';

	/**
	 * The "connect command" sent by clients to indicate that they are ready for a connection.
	 * It is expected to be immediately followed by a FILE_COMMAND or a START_COMMAND.
	 */
	public static final char CONNECT_COMMAND = 'c';
	final short CONNECT_COMMAND_LENGTH = 0;

	/**
	 * The command used to set a new file for logging. Sent by clients to the server.
	 * It may come after a CONNECT_COMMAND
	 * in order to set the file, but not begin tracing immediately. The data is expected
	 * to be an ASCII character array of length data_len, containing the path to the
	 * destination file. The character array is not expected to be null terminating, and
	 * should not contain any control characters (such as new lines or line feeds).
	 */
	public static final char FILE_COMMAND = 'f';

	/**
	 * The command used to start logging. Sent by clients to the server.
	 * If data_len is zero, than a previous FILE_COMMAND
	 * must have been sent to set the log file. Otherwise, the semantics for the data
	 * are the same as FILE_COMMAND.
	 */
	public static final char START_COMMAND = 's';

	/**
	 * The command to pause logging. Sent by clients to the server. data_len is expected
	 * to be zero. Can only be called after a START_COMMAND, and has no meaning if already
	 * paused. If either of these cases occurs, then clients will receive an error.
	 */
	public static final char PAUSE_COMMAND = 'p';

	/**
	 * The command to resume previously paused logging. Sent by clients to the server.
	 * data_len is expected to be zero. Can only be called while the server has paused
	 * logging, otherwise an error will be issued.
	 */
	public static final char RESUME_COMMAND = 'r';
	
	
	/**
	 * The command to apply a filter to the server. When using server-side
	 * filters, only method calls that match the filter will be saved to disk,
	 * reducing the size of the traced files and the amount of time that it
	 * takes to analyze them, but also reducing robustness.
	 */
	public static final char FILTER_COMMAND	= 'x';
	/**
	 * The command code
	 */
	private char command;

		
	/**
	 * The data
	 */
	private byte[] data;
	
	/**
	 * A string representation of the data.
	 */
	private transient String dataString;
	
	
		
	private OasisCommand(char command, byte[] data) {
		this.command = command;
		this.data = data;
	}
	
	public static OasisCommand newFileCommand(String fileName) {
		try {
			return new OasisCommand(FILE_COMMAND, fileName.getBytes("US-ASCII"));
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	public static OasisCommand newFileCommand(File file) {
		return newFileCommand(file.getAbsolutePath());
	}
	
	public static OasisCommand newPauseCommand() {
		return new OasisCommand(PAUSE_COMMAND, new byte[0]);
	}
	
	public static OasisCommand newResumeCommand() {
		return new OasisCommand(RESUME_COMMAND, new byte[0]);
	}
	
	public static OasisCommand newConnectCommand() {
		return new OasisCommand(CONNECT_COMMAND, new byte[0]);
	}
	
	public static OasisCommand newStartCommand() {
		return new OasisCommand(START_COMMAND, new byte[0]);
	}
	
	public static OasisCommand newStartCommand(String fileName) {
		try {
			fileName = fileName + '\0';
			return new OasisCommand(START_COMMAND, fileName.getBytes("US-ASCII"));
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	public static OasisCommand newStartCommand(File file) {
		return newStartCommand(file.getAbsolutePath());
	}
	
	public static OasisCommand newFilterCommand(String filterString, boolean isExclusion) {
		byte[] stringBytes;
		try {
			stringBytes = filterString.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			SketchPlugin.getDefault().log(new InvocationTargetException(e, "Unable to generate filter for java application"));
			return null;
		}
		byte[] data = new byte[stringBytes.length + 1];
		System.arraycopy(stringBytes, 0, data, 1, stringBytes.length);
		data[0] = (isExclusion) ? (byte)1 : (byte)0;
		return new OasisCommand(FILTER_COMMAND, data);
	}
	/**
	 * Returns the command id.
	 * @return the command id.
	 */
	public char getCommand() {
		return command;
	}
	
	
	/**
	 * Returns the data for this command.
	 * @return the data for this command.
	 */
	public byte[] getData() {
		return data;
	}
	
	/**
	 * Returns the as a string representation.
	 * @return the data as a string representation, or null if none could be decoded.
	 */
	public String getDataString() {
		if (dataString != null) {
			return dataString;
		}
		try {
			dataString = new String(getData(), "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			dataString = null;
		}
		return dataString;
	}
	
	
	
	/**
	 * Returns a byte representation of this command, suitable for sending over a wire.
	 * @return the byte representation of this command.
	 */
	private byte[] getBytes() throws IOException {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream(data.length+3);
		DataOutputStream out = new DataOutputStream(byteOut);
		out.write(command);
		out.writeShort(data.length);
		if (data.length > 0) {
			out.write(data);
		}
		out.close();
		byte[] bytes = byteOut.toByteArray();
		if (bytes.length != data.length+3) {
			throw new IOException("Byte length mismatch got " + bytes.length + " expected " + (data.length+3));
		}
		return bytes;
		
	}
	
	/**
	 * Custom code for writing the commands to an output stream.
	 * @param out
	 * @throws IOException
	 */
	public void writeExternal(OutputStream out) throws IOException {
		out.write(getBytes());
	}
	
	public static void writeExternal(OasisCommand command, OutputStream out) throws IOException {
		command.writeExternal(out);
	}
	
	/**
	 * Custom code for reading a command from an output stream.
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static OasisCommand readExternal(InputStream inStream) throws IOException {
		/**
		 * First, read the command.
		 */
		DataInputStream in = new DataInputStream(inStream);
		int input = in.read();
		checkCommand(input);
		char command = (char)input;
		//get the next two bytes
		short s = in.readShort();
		//figure out the length
		int dataLength = (s & 0xFFFF);
		if (dataLength < 0 || dataLength > 0xFFFF) {
			//out of range
			throw new IOException("Data size out of range");
		}
		byte[] data = new byte[dataLength];
		//read the data
		if (dataLength > 0) {
			int read = in.read(data, 0, dataLength);
			if (read != dataLength) {
				throw new IOException("Data length mismatch. Got " + read + " expected " + dataLength);
			}
		}
		return new OasisCommand(command, data);
	}
	
	private static void checkCommand(int command) throws IOException {
		if (command < 0) {
			throw new IOException("Error reading command: end of data reached");
		}
		switch (command) {
		case ACK_COMMAND:
		case CONNECT_COMMAND:
		case ERROR_COMMAND:
		case FILE_COMMAND:
		case PAUSE_COMMAND:
		case RESUME_COMMAND:
		case START_COMMAND:
			return; //valid command.
		}
		throw new IOException("Invalid command: '" + ((char)command) + "'");
	}


	
	
}
