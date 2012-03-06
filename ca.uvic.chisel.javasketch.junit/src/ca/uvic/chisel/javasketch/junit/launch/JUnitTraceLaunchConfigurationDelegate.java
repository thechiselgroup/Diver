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

import java.io.IOException;
import java.net.ServerSocket;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.launching.ITraceClient;
import ca.uvic.chisel.javasketch.launching.internal.JavaAgentTraceClient;
import ca.uvic.chisel.javasketch.launching.internal.JavaAgentUtil;

import static ca.uvic.chisel.javasketch.launching.internal.JavaTraceConfigurationDelegate.TRACE_PORT;

/**
 * @author Del
 *
 */
public class JUnitTraceLaunchConfigurationDelegate extends
		JUnitLaunchConfigurationDelegate {
	
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
		super.launch(configuration, mode, launch, launchJavaMonitor);
		if (monitor.isCanceled()) return;
		connectClient(configuration, mode, launch, connectProbesMonitor, client);
		monitor.done();
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

	
	private ITraceClient prepareClient(ILaunchConfiguration configuration,
			String mode, ILaunch launch, IProgressMonitor monitor) {
		monitor.beginTask("Preparing client", 1);
		ITraceClient client = new JUnitAgentTraceClient();
		monitor.worked(1);
		monitor.done();
		return client;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getVMArguments(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {
		String args = super.getVMArguments(configuration);
		String pause = ",pause=on";
			
		try {
			IPath agent = JavaAgentUtil.getJavaAgent(configuration);
			String agentPath = "\"" + agent.toOSString() + "\"";
			
			args = "-agentpath:" + agentPath + "=port=" + getPort(configuration) + pause + ",junit=true" + " " + args;
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

}
