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
package ca.uvic.chisel.javasketch.launching.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import ca.gc.drdc.oasis.tracing.cjvmtracer.internal.OasisCommand;
import ca.uvic.chisel.javasketch.FilterSettings;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.launching.LocalDataBaseTraceClient;
import ca.uvic.chisel.javasketch.launching.ui.FilterTab;

public class JavaAgentTraceClient extends LocalDataBaseTraceClient {

	/**
	 * 
	 */
	public static final String PROCESS_PROPERTIES_FILE = "process.properties";
	/**
	 * 
	 */
	public static final String ATTACH_TIME_PROPERTY = "time";
	/**
	 * 
	 */
	public static final String HOST_PROPERTY = "host";
	/**
	 * 
	 */
	public static final String PROCESS_PROPERTY = "process";
	public static final String LABEL_PROPERTY = "label";
	public static final String ID_PROPERTY = "id";
	public static final String CONFIGURATION_FILE = "launch.configuration";
	private Socket clientSocket;
	private boolean connected;
	private OutputStream commandOut;
	private InputStream commandIn;
	private ReadThread readThread;
	private long terminationTime;
	
	
	private class ReadThread extends Thread {
		private volatile boolean done;
		ReadThread() {
			done = false;
		}
		@Override
		public void run() {
			while (!done) {
				try {
					OasisCommand cmd = OasisCommand.readExternal(commandIn);
					switch (cmd.getCommand()) {
					case OasisCommand.ACK_COMMAND:
						//check to see if it is a pause or a restart
						switch (cmd.getData()[0]) {
						case OasisCommand.PAUSE_COMMAND:
							handlePause();
							break;
						case OasisCommand.RESUME_COMMAND:
							handleResume();
							break;
						}
						break;
					}
				} catch (IOException e) {	
					done = true;
				}
			}
			terminationTime = System.currentTimeMillis();
			try {
				clientSocket.close();
			} catch (IOException e1) {}
			JavaAgentTraceClient.this.finish();
		}
		protected void finish() {
			done = true;
		}
	}
	
	public JavaAgentTraceClient() {
		readThread = new ReadThread();
		terminationTime = -1;
	}

	
	@Override
	public void doAttach(ILaunch launch, ILaunchConfiguration configuration, IProgressMonitor monitor)
			throws CoreException {
		int portNumber = configuration.getAttribute(JavaTraceConfigurationDelegate.TRACE_PORT, 0);

		try {
			for (int tries = 1; tries <= 5; tries++) {
				//try to get the socket up to 5 times
				try {
					clientSocket = new Socket("localhost", portNumber);
					//success
					tries = 6;
				} catch (IOException e) {
					clientSocket = null;
					if (tries > 5) {
						//re-throw the exception so it gets caught outside
						//this loop
						throw e;
					} else {
						//wait a second for the java process to start
						try {
							Thread.sleep(1000);
						} catch (InterruptedException ie) {
							Thread.interrupted();
						}
					}
				}
			}
			handshake(configuration);
			readThread.start();
		} catch (NullPointerException e) {
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "No port to attach for process"));
		} catch (UnknownHostException e) {
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Could not connect to host process", e));
		} catch (IOException e) {
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Could not connect to host process", e));
		}
		
		
	}

	

	private void handshake(ILaunchConfiguration config) throws IOException, CoreException {
		commandOut = clientSocket.getOutputStream();
		commandIn = new BufferedInputStream(clientSocket.getInputStream());
		OasisCommand.newConnectCommand().writeExternal(commandOut);
		
//		System.out.println("Before reading response");
		//waitForAvailable(commandIn, 1000);
		OasisCommand result = OasisCommand.readExternal(commandIn);
		if (result == null) {
			throw new IOException("Null response from server");
		} else if (result.getCommand() != OasisCommand.ACK_COMMAND) {
			throw new IOException("Expected Acknowledgement from server");
		}
		
//		System.out.println("Response: " + result.getDataString());
//		
		this.connected = true;
		if (config.getAttribute(FilterTab.APPLY_AT_RUNTIME, false)) {
			FilterSettings filters = FilterSettings.newSettings(config);
			for (String filter : filters.getResolvedInclusionFilters()) {
				applyAtRuntime(filter, false);
			}
			for (String filter : filters.getResolvedExclusionFilters()) {
				applyAtRuntime(filter, true);
			}
		}
		//send the begin command
		OasisCommand.newStartCommand(getFileName()).writeExternal(commandOut);
		//get the acknowledgement
		//waitForAvailable(commandIn, 1000);
		result = OasisCommand.readExternal(commandIn);
		if (result == null) {
			throw new IOException("Null response from server");
		} else if (result.getCommand() != OasisCommand.ACK_COMMAND) {
			throw new IOException("Expected Acknowledgement from server");
		}
	}


	/**
	 * @param filter
	 * @throws IOException
	 */
	private void applyAtRuntime(String filter, boolean isExclusion) throws IOException {
		OasisCommand result;
		OasisCommand command = OasisCommand.newFilterCommand(filter, isExclusion);
		if (command != null) {
			command.writeExternal(commandOut);
			result = OasisCommand.readExternal(commandIn);
			if (result == null) {
				throw new IOException("Null response from server");
			} else if (result.getCommand() != OasisCommand.ACK_COMMAND) {
				throw new IOException("Expected Acknowledgement from server");
			}
		}
	}
	
	



	
	@Override
	public boolean canPauseTrace() {
		return (connected);
	}



	

	@Override
	protected void performPauseRequest() {
		try {
		if (canPauseTrace()) {
			OasisCommand.newPauseCommand().writeExternal(commandOut);
		}
		} catch (IOException e) {};
	}

	@Override
	protected boolean performResumeRequest() {
		try {
			if (canPauseTrace()) {
				OasisCommand.newResumeCommand().writeExternal(commandOut);
			}
		} catch (IOException e) {};
		return false;
	}

	@Override
	public void sendEvent(Object event) throws IllegalArgumentException {
		try {
			OasisCommand command = (OasisCommand) event;
			try {
				command.writeExternal(commandOut);
			} catch (IOException e) {}
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(e);
		}

	}

	

	@Override
	public int getExitValue() throws DebugException {
		return 0;
	}

		
	@SuppressWarnings({"rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canTerminate() {
		return (getLaunch() != null && clientSocket != null && !isTerminated());
	}

	@Override
	public boolean isTerminated() {
		return clientSocket.isClosed();
	}

	@Override
	public void terminate() throws DebugException {
		if (canTerminate()) {
			try {
				clientSocket.close();
				readThread.finish();
			} catch (IOException e) {
				throw new DebugException(new Status(IStatus.ERROR, SketchPlugin.PLUGIN_ID, "Error terminating client.", e));
			}
		}
	}
	
	

	
	

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.internal.ITraceClient#getTerminationTime()
	 */
	@Override
	public long getTerminationTime() {
		return terminationTime;
	}

	@Override
	protected void doInitialize(ILaunchConfiguration configuration) throws CoreException {
	}

}
