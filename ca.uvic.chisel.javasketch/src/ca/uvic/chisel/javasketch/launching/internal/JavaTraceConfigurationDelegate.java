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

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.launching.ITraceClient;
import ca.uvic.chisel.javasketch.launching.ui.FilterTab;

/**
 * @author Del Myers
 *
 */
@SuppressWarnings("restriction")
public class JavaTraceConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	public static String TRACE_PORT = "javasketch.trace.port";


	/**
	 * 
	 */
	public JavaTraceConfigurationDelegate() {
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void launch(final ILaunchConfiguration configuration, final String mode,
			final ILaunch launch, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Launching Java Application Trace...", 1000);
		IProgressMonitor initializeMonitor = new SubProgressMonitor(monitor, 100);
		final IProgressMonitor launchJavaMonitor = new SubProgressMonitor(monitor, 400);
		IProgressMonitor connectProbesMonitor = new SubProgressMonitor(monitor, 500);
		ITraceClient client = prepareClient(configuration, mode, launch, initializeMonitor);
		client.initialize(configuration);
		if (monitor.isCanceled()) return;
		launchJava(configuration, mode, launch, launchJavaMonitor);
		if (monitor.isCanceled()) return;
		connectClient(configuration, mode, launch, connectProbesMonitor, client);
		monitor.done();
	}
	
	private ITraceClient prepareClient(ILaunchConfiguration configuration,
			String mode, ILaunch launch, IProgressMonitor monitor) {
		monitor.beginTask("Preparing client", 1);
		ITraceClient client = new JavaAgentTraceClient();
		monitor.worked(1);
		monitor.done();
		return client;
	}

	
	

	/**
	 * @param configuration
	 * @param mode
	 * @param launch
	 * @param probeCode 
	 * @param connectProbesMonitor
	 * @throws CoreException 
	 */
	private void connectClient(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor, ITraceClient client) throws CoreException {
		RuntimeProcess rp = null;
		for (IProcess ip : launch.getProcesses()) {
			if (ip instanceof RuntimeProcess) {
				rp = (RuntimeProcess) ip;
			}
		}
		if (rp == null) {
			monitor.done();
			return;
		}
		try {
			//set the port number
			//rp.setAttribute(TRACE_PORT, getPort(configuration) + "");
			client.attach(launch, configuration, monitor);
		} catch (CoreException e) {
			if (launch.canTerminate()) {
				launch.terminate();
			}
			throw e;
		} 
	}

	

	@SuppressWarnings("unchecked")
	protected void launchJava(ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		monitor.beginTask(MessageFormat.format("{0}...", new Object[]{configuration.getName()}), 3); //$NON-NLS-1$
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}
		try {
			monitor.subTask(LaunchingMessages.JavaLocalApplicationLaunchConfigurationDelegate_Verifying_launch_attributes____1); 
							
			String mainTypeName = verifyMainTypeName(configuration);
			IVMRunner runner = getVMRunner(configuration, mode);
	
			File workingDir = verifyWorkingDirectory(configuration);
			String workingDirName = null;
			if (workingDir != null) {
				workingDirName = workingDir.getAbsolutePath();
			}
			
			// Environment variables
			String[] envp= getEnvironment(configuration);
			
			// Program & VM arguments
			String pgmArgs = getProgramArguments(configuration);
			String vmArgs = getVMArguments(configuration);
			ExecutionArguments execArgs = new ExecutionArguments(vmArgs, pgmArgs);
			
			// VM-specific attributes
			Map vmAttributesMap = getVMSpecificAttributesMap(configuration);
			
			// Classpath
			String[] classpath = getClasspath(configuration);
			
			// Create VM config
			VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
			runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
			runConfig.setEnvironment(envp);
			runConfig.setVMArguments(execArgs.getVMArgumentsArray());
			runConfig.setWorkingDirectory(workingDirName);
			runConfig.setVMSpecificAttributesMap(vmAttributesMap);
	
			// Bootpath
			runConfig.setBootClassPath(getBootpath(configuration));
			
			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}		
			
			// stop in main
			prepareStopInMain(configuration);
			
			// done the verification phase
			monitor.worked(1);
			
			monitor.subTask(LaunchingMessages.JavaLocalApplicationLaunchConfigurationDelegate_Creating_source_locator____2); 
			// set the default source locator if required
			setDefaultSourceLocator(launch, configuration);
			monitor.worked(1);		
			
			// Launch the configuration - 1 unit of work
			runner.run(runConfig, launch, monitor);
			
			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}	
		}
		finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getVMArguments(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {
		String args = super.getVMArguments(configuration);
		String pause = 
			configuration.getAttribute(FilterTab.PAUSE_ON_START, true) ?
					",pause=on" : "";
		try {
			IPath agent = JavaAgentUtil.getJavaAgent(configuration);
			String agentPath = "\"" + agent.toOSString() + "\"";
			
			args = "-agentpath:" + agentPath + "=port=" + getPort(configuration) + pause + " " + args;
		} catch (IOException e) {
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Could not open port for program tracing", e));
		}
		return args;
	}
	
	
	private int getPort(ILaunchConfiguration configuration) throws IOException, CoreException {
		ILaunchConfigurationWorkingCopy copy = configuration.getWorkingCopy();
		int port = copy.getAttribute(TRACE_PORT, 0);
		if (port != 0) {
			try {
				//make sure that the port is still available.
				ServerSocket server=new ServerSocket(port);
				server.close();
			} catch (IOException e) {
				port = 0;
			}
		}
		if (port == 0) {
			//make sure that the port is still available.
			ServerSocket server=new ServerSocket(port);
			port = server.getLocalPort();
			copy.setAttribute(TRACE_PORT, port);
			copy.doSave();
			server.close();
		}
		return port;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getBootpath(org.eclipse.debug.core.ILaunchConfiguration)
	 */
//	@Override
//	public String[] getClasspath(ILaunchConfiguration configuration)
//			throws CoreException {
//		String[] pathArray = super.getClasspath(configuration);
//		if (pathArray == null) {
//			pathArray = new String[0];
//		}
//		ArrayList<String> additionalPaths = new ArrayList<String>(Arrays.asList(pathArray));
//		String javaAgent = getJavaAgent(configuration);
		//locate the others in the jar location
//		IPath agentPath = new Path(javaAgent);
//		IPath agentLocation = agentPath.removeLastSegments(1);
//		String probeLocation = getProbeClass(configuration);
//		boolean foundJar = false;
//		for (String s : pathArray) {
//			if (s.endsWith(bootPath.lastSegment())) {
//				foundJar = true;
//				break;
//			}
//		}
//		if (!foundJar) {
//			additionalPaths.add(bootPath.toOSString());
//		}
//		foundJar =false;
//		for (String s : pathArray) {
//			if (s.endsWith(asmPath.lastSegment())) {
//				foundJar = true;
//				break;
//			}
//		}
//		if (!foundJar) {
//			additionalPaths.add(asmPath.toOSString());
//		}
		
//		additionalPaths.add(probeLocation);
//		return additionalPaths.toArray(new String[additionalPaths.size()]);
//	}
	
	
	
//	@Override
//	public String[] getBootpath(ILaunchConfiguration configuration)
//			throws CoreException {
//		ILaunchConfigurationWorkingCopy copy = configuration.getWorkingCopy();
//		IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedRuntimeClasspath(configuration);
//		HashSet<IRuntimeClasspathEntry> newEntries = new HashSet<IRuntimeClasspathEntry>();
//		//make sure to put on the entry for the probe class
//		IPath probePath = new Path(getProbeClass(configuration));
//		IRuntimeClasspathEntry runtimeProbeEntry = JavaRuntime.newArchiveRuntimeClasspathEntry(probePath);
//		runtimeProbeEntry.setClasspathProperty(IRuntimeClasspathEntry.BOOTSTRAP_CLASSES);
//		ArrayList<String> mementos = new ArrayList<String>(newEntries.size());
//		newEntries.add(runtimeProbeEntry);
//		newEntries.addAll(Arrays.asList(entries));
//		System.out.println();
//		for (IRuntimeClasspathEntry bootStrapEntry : newEntries) {
//			mementos.add(bootStrapEntry.getMemento());
//		}
//		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
//		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, mementos);
//		//configuration = copy.doSave();
//		return super.getBootpath(copy);
//	}
}
