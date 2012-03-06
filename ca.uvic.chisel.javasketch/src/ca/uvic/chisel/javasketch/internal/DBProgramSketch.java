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
package ca.uvic.chisel.javasketch.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;

import ca.uvic.chisel.hsqldb.server.DBPlugin;
import ca.uvic.chisel.hsqldb.server.DefaultDataPortal;
import ca.uvic.chisel.hsqldb.server.IDataPortal;
import ca.uvic.chisel.javasketch.FilterSettings;
import ca.uvic.chisel.javasketch.IFilterChangedListener;
import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.imple.internal.TraceImpl;
import ca.uvic.chisel.javasketch.launching.ITraceClient;
import ca.uvic.chisel.javasketch.launching.internal.JavaAgentTraceClient;

/**
 * Represents a trace from a java launch.
 * @author Del Myers
 *
 */
public class DBProgramSketch implements IProgramSketch, ISchedulingRule {
	
	private Properties props;
	private Date date;
	private IPath path;
	//private IProject project;
	private IDataPortal dataPortal;
	private ITrace trace;
	private FilterSettings filterSettings;
	private boolean deleted;
	private ILaunchConfiguration configuration;
	private File filtersFile;
	
	
	/**
	 * Creates a trace from the path in trace store
	 * @param traceStore the path on the files system at which the data is stored
	 * for this trace.
	 * @throws IOException if there was a problem loading the properties for this file.
	 */
	public DBProgramSketch(File traceStore) throws IOException {
		this.path = new Path(traceStore.getAbsolutePath());
		this.filtersFile = new File(traceStore, ".filters");
		
		//find the properties file.
		File properties = new File(traceStore, JavaAgentTraceClient.PROCESS_PROPERTIES_FILE);
		Properties props = new Properties();
		FileInputStream fis = new FileInputStream(properties);
		try {
			props.load(fis);
		} catch (IOException e) {
			fis.close();
			throw e;
		}
		this.props = props;
		fis.close();
		File lcFile = new File(traceStore, JavaAgentTraceClient.CONFIGURATION_FILE);
		FileReader reader = new FileReader(lcFile);
		try {

			char[] buffer = new char[1024];
			int read = -1;
			StringBuilder sb = new StringBuilder();
			while ((read = reader.read(buffer))>=0) {
				sb.append(buffer, 0, read);
			}
			try {
				this.configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(sb.toString());
			} catch (CoreException e) {
				throw new IOException(e);
			} 
		} finally {
			reader.close();
		}
		
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#getProcessName()
	 */
	@Override
	public String getProcessName() {
		return props.getProperty(JavaAgentTraceClient.HOST_PROPERTY);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#getProcessTime()
	 */
	@Override
	public Date getProcessTime() {
		if (date == null) {
			String timeString = props.getProperty(JavaAgentTraceClient.ATTACH_TIME_PROPERTY);
			long time = System.currentTimeMillis();
			if (timeString == null) {
				timeString = "" + System.currentTimeMillis();
			}
			try {
				time = Long.parseLong(timeString);
			} catch (NumberFormatException e) {}
			date = new Date(time);
			
		}
		return date;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#getTraceData()
	 */
	@Override
	public ITrace getTraceData() {
		if (deleted) {
			return null;
		}
		if (this.trace == null) {
			try {
				if (TraceImpl.exists(getID(), getProcessTime(), getPortal())) {
					trace = TraceImpl.load(getID(), getProcessTime(), getPortal());
				} else {
					trace = TraceImpl.create(getID(), getProcessTime(), getPortal());
				}
			} catch (CoreException e) {
				SketchPlugin.getDefault().log(e);
			} 
		}
		return trace;
	}
	
	public boolean delete() {
		//make sure the view is closed
		this.deleted = true;
		clearData();
		if (trace != null) {
			((TraceImpl)trace).dispose();
			trace = null;
		}
		try {
			if (dataPortal != null) {
				getPortal().close();
			}
		} catch (CoreException e) {
			return false;
		}
		File dataLocation = path.toFile();
		recursiveDelete(dataLocation);
		return (!dataLocation.exists());
	}
	
	/**
	 * @param dataLocation
	 */
	private void recursiveDelete(File dataLocation) {
		if (dataLocation.isDirectory()) {
			for (File child : dataLocation.listFiles()) {
				recursiveDelete(child);
			}
		}
		if (dataLocation.exists()) {
			if (!dataLocation.delete()) {
				System.out.println("could not delete " + dataLocation.toString());
			}
		}
	}
	
	/**
	 * Closes and clears all of the database files.
	 * @return
	 * @throws CoreException 
	 * @throws  
	 */
	public boolean reset() throws CoreException {
		clearData();
		try {
			if (((DefaultDataPortal)getPortal()).clear()) {
				if (trace != null) {
					
					((TraceImpl)trace).dispose();
					trace = null;
					
				}
				return true;
			}
		} catch (SQLException e) {
			throw new CoreException(SketchPlugin.getDefault().createStatus(e));
		}
		return false;
	}
	
	public IDataPortal getPortal() throws CoreException {
		if (dataPortal == null) {
			dataPortal = DBPlugin.getDefault().getDataPortal(path); 
		}
		return dataPortal;
	}
	
	
	/**
	 * 
	 * @return the path at which (temporary) trace files are stored.
	 */
	public URL getTracePath() {
		try {
			return path.toFile().toURI().toURL();
		} catch (MalformedURLException e) {
			SketchPlugin.getDefault().log(e);
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj.getClass().equals(this.getClass()))) {
			return false;
		}
		IProgramSketch that = (IProgramSketch) obj;
		return this.getID().equals(that.getID());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getID().hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getProcessName();
	}

//	/* (non-Javadoc)
//	 * @see ca.uvic.chisel.javasketch.IProgramSketch#getTracedProject()
//	 */
//	@Override
//	public IProject getTracedProject() {
//		return project;
//	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#getTracedLaunchConfiguration()
	 */
	@Override
	public ILaunchConfiguration getTracedLaunchConfiguration() {
		return configuration;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#getRule()
	 */
	@Override
	public ISchedulingRule getRule() {
		return this;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#getTracer()
	 */
	@Override
	public ITraceClient getTracer() {
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		for (ILaunch launch : launches) {
			for (IProcess process : launch.getProcesses()) {
				if (process instanceof ITraceClient) {
					if (((ITraceClient)process).getID().equals(getID())) {
						return ((ITraceClient)process);
					}
				}
			}
		}
		return null;
	}
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#isAnalysing()
	 */
	@Override
	public boolean isAnalysing() {
		Job[] jobs = Job.getJobManager().find(IProgramSketch.class);
		for (Job job : jobs) {
			if (job.getRule().isConflicting(this)) {
				return (job.getState() != Job.NONE);
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#isRunning()
	 */
	@Override
	public boolean isRunning() {
		ITraceClient client = getTracer();
		return (client != null && !client.isTerminated());
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#getID()
	 */
	@Override
	public String getID() {
		String id = props.getProperty("id");
		return id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	@Override
	public boolean contains(ISchedulingRule rule) {
		//only contains this rule
		return (rule != null && rule.equals(this));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
	 */
	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		return (rule != null && rule.equals(this));
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#getLabel()
	 */
	@Override
	public String getLabel() {
		if (deleted) {
			return "Deleted Trace";
		}
		String label = props.getProperty(JavaAgentTraceClient.LABEL_PROPERTY);
		if (label == null) {
			label = getProcessName();
		}
		return label;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#getFilterSettings()
	 */
	@Override
	public FilterSettings getFilterSettings() {
		if (filterSettings == null) {
			if (!filtersFile.exists()) {
				try {
					filterSettings = FilterSettings.newSettings(configuration);
					filterSettings.save(filtersFile);
				} catch (IOException e) {
					SketchPlugin.getDefault().log(e);
				} catch (CoreException e) {
					SketchPlugin.getDefault().log(e);
				}
			} else {
				try {
					FileReader reader = new FileReader(filtersFile);
					filterSettings = FilterSettings.load(reader, configuration);
					reader.close();
				} catch (IOException e) {
					SketchPlugin.getDefault().log(e);
				}
			}
			filterSettings.addFilterChangedListener(new IFilterChangedListener() {
				
				@Override
				public void referenceChanged(IProgramSketch old, FilterSettings settings) {
					save();
				}
				
				@Override
				public void projectClassesChanged(boolean old, FilterSettings settings) {
					save();
				}
				
				@Override
				public void inclusionChanged(String[] old, FilterSettings settings) {
					save();
				}
				
				@Override
				public void exclusionChanged(String[] old, FilterSettings settings) {
					save();
				}

				private void save() {
					try {
						filterSettings.save(filtersFile);
					} catch (IOException e) {}
				}
			});
		}
		return filterSettings;
	}
	
	

	/**
	 * 
	 */
	public void clearData() {
		if (trace != null) {
			trace.invalidate();
		}
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IProgramSketch#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return this.trace != null;
	}
	

}
