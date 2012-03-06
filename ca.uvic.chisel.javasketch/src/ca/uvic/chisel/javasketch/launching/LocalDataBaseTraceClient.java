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
package ca.uvic.chisel.javasketch.launching;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IStreamsProxy;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.launching.ui.FilterTab;

/**
 * A trace client that is set up to store data in a local database.
 * @author Del Myers
 *
 */
public abstract class LocalDataBaseTraceClient implements ITraceClient {
	
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
	private String hostLabel;
	private ILaunchConfiguration configuration;
	private String id;
	private long attachTime;
	private String fileName;
	private ILaunch launch;
	private IPath storagePath;
	private HashMap<String, String> atts;
	private boolean paused;
	private String label;
	
	public LocalDataBaseTraceClient() {
		atts = new HashMap<String, String>();
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.ITraceClient#attach(org.eclipse.debug.core.ILaunch, org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public final void attach(ILaunch launch, ILaunchConfiguration configuration,
			IProgressMonitor monitor) throws CoreException {
		this.launch = launch;
		this.configuration = configuration;
		this.attachTime = System.currentTimeMillis();
		this.label = configuration.getName();
		try {
			createProperties();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, SketchPlugin.PLUGIN_ID, "Unable to attach client ",e));
		}
		doAttach(launch, configuration, monitor);
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {
				new DebugEvent(this, DebugEvent.CREATE)
			});
		launch.addProcess(this);
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IProcess#getLabel()
	 */
	@Override
	public String getLabel() {
		return "Tracing " + this.label;
	}
	
	
	protected abstract void doAttach(ILaunch launch, ILaunchConfiguration configuration,
			IProgressMonitor monitor) throws CoreException;

	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.ITraceClient#getAttachTime()
	 */
	@Override
	public long getAttachTime() {
		return attachTime;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.ITraceClient#getID()
	 */
	@Override
	public String getID() {
		if (this.id == null) {
			this.id = getLaunchConfiguration().getName() + "." + System.currentTimeMillis();
		}
		return id;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.ITraceClient#getLaunchConfiguration()
	 */
	@Override
	public ILaunchConfiguration getLaunchConfiguration() {
		return configuration;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.ITraceClient#initialize(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public final void initialize(ILaunchConfiguration configuration)
			throws CoreException {
		//this method will set up all the files that we need.
		this.configuration = configuration;
		this.paused = configuration.getAttribute(FilterTab.PAUSE_ON_START, true);
		getFileName();
		doInitialize(configuration);
	}

	
	protected abstract void doInitialize(ILaunchConfiguration configuration) throws CoreException;

	
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.ITraceClient#sendEvent(java.lang.Object)
	 */
	@Override
	public void sendEvent(Object event) throws IllegalArgumentException {
		
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.ITraceClient#pauseTrace()
	 */
	@Override
	public final void pauseTrace() {
		performPauseRequest();
	}
	
	protected abstract void performPauseRequest();

	protected void handlePause() {
		paused = true;
		DebugEvent event =new DebugEvent(this, DebugEvent.MODEL_SPECIFIC, TRACE_PAUSED);
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {event});
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.launching.ITraceClient#resumeTrace()
	 */
	@Override
	public final boolean resumeTrace() {
		return performResumeRequest();
	}
	
	protected abstract boolean performResumeRequest();
	
	protected void handleResume() {
		paused = false;
		DebugEvent event =new DebugEvent(this, DebugEvent.MODEL_SPECIFIC, TRACE_RESUMED);
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {event});
	}
	
	@Override
	public boolean isPaused() {
		return paused;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IProcess#getAttribute(java.lang.String)
	 */
	@Override
	public String getAttribute(String key) {
		return atts.get(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IProcess#setAttribute(java.lang.String, java.lang.String)
	 */
	@Override
	public void setAttribute(String key, String value) {
		atts.put(key, value);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IProcess#getLaunch()
	 */
	@Override
	public ILaunch getLaunch() {
		return launch;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IProcess#getStreamsProxy()
	 */
	@Override
	public IStreamsProxy getStreamsProxy() {
		return null;
	}

	

		
	
	/**
	 * Gets the filename used to save the information that the server will
	 * write to.
	 * @return the filename to write to.
	 * @throws CoreException 
	 */
	protected String getFileName() throws CoreException {
		if (fileName != null) {
			return fileName;
		}
		IPath filePath = getStoragePath();
		File projectFolder = new File(filePath.toOSString());
		if (!projectFolder.exists()) {
			if (!projectFolder.mkdirs()) {
				throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Unable to create location for trace files."));
			} 
		}

		String result = filePath.toOSString();
		//convert escape characters
		result = result.replaceAll("\\\\", "\\\\\\\\");
		this.fileName = result;
		return result;
	}
	
	/**
	 * Returns the path in which data will be stored for this trace.
	 * @return the path in which data will be stored for this trace.
	 */
	public IPath getStoragePath() throws CoreException {
		if (this.storagePath != null) {
			return storagePath;
		}
		IPath pluginState = SketchPlugin.getDefault().getStateLocation();
		String configurationName = configuration.getName();
		IPath configurationPath = pluginState.append(configurationName);
		IPath filePath = configurationPath.append(getID());
		File file = new File(filePath.toOSString());
		if (!file.exists()) {
			file.mkdirs();
		}
		storagePath = filePath;
		return storagePath;
	}

	/**
	 * Stores information about this launch for later reference in a standard Java properties
	 * file.
	 * @param projectFolder
	 * @throws IOException 
	 * @throws CoreException 
	 */
	private void createProperties() throws IOException, CoreException {
		IPath storagePath = getStoragePath();
		File projectFolder = new File(storagePath.toOSString());
		Properties props = new Properties();
		props.put(PROCESS_PROPERTY, getLabel());
		props.put(HOST_PROPERTY, getHostLabel());
		props.put(LABEL_PROPERTY, getLabel());
		props.put(ATTACH_TIME_PROPERTY, ""+getAttachTime());
		props.put(ID_PROPERTY, getID());
		File propertiesFile = new File(projectFolder, PROCESS_PROPERTIES_FILE);
		props.store(new FileOutputStream(propertiesFile), "");
		
		//write out the launch configuration so that it can be loaded later
		File lcFile = new File(projectFolder, CONFIGURATION_FILE);
		String lcMemento =  configuration.getMemento();
		FileWriter writer = new FileWriter(lcFile);
		writer.write(lcMemento);
		writer.close();
	}
	
	protected String getHostLabel() {
		return configuration.getName();
	}
	
	/**
	 * Cleans up the process so that it will display as terminated.
	 */
	protected void finish() {
		DebugPlugin manager= DebugPlugin.getDefault();
		if (manager != null) {
			manager.fireDebugEventSet(new DebugEvent[]{new DebugEvent(this, DebugEvent.TERMINATE)});
		}
		paused = false;
	}

}
