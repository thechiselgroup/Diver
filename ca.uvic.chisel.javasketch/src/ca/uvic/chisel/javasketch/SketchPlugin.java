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
package ca.uvic.chisel.javasketch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceEvent;
import ca.uvic.chisel.javasketch.data.model.ITraceEventListener;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.internal.DBProgramSketch;
import ca.uvic.chisel.javasketch.internal.SketchEvents;
import ca.uvic.chisel.javasketch.internal.interest.DegreeOfInterest;
import ca.uvic.chisel.javasketch.launching.ITraceClient;
import ca.uvic.chisel.javasketch.launching.internal.JavaAgentTraceClient;
import ca.uvic.chisel.javasketch.ui.ISketchColorConstants;
import ca.uvic.chisel.javasketch.ui.ISketchImageConstants;
import ca.uvic.chisel.javasketch.ui.internal.SketchUI;


/**
 * The activator class controls the plug-in life cycle
 */
public class SketchPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ca.uvic.chisel.javasketch";

	// The shared instance
	private static SketchPlugin plugin;
	
	// Listener for debug events.
	private DebugListener debugListener;
	
	private SketchEvents events;
	
	private DegreeOfInterest doi;


	private ColorRegistry colorRegistry;
	
	private Map<String, IProgramSketch> cachedSketches;

	private ITraceEventListener staticJavaModelListener;


	private class StaticJavaModelListener implements ITraceEventListener {

		private long eventTime = 0;
		/* (non-Javadoc)
		 * @see ca.uvic.chisel.javasketch.data.model.ITraceEventListener#handleEvents(ca.uvic.chisel.javasketch.data.model.ITraceEvent[])
		 */
		@Override
		public void handleEvents(ITraceEvent[] events) {
			
			for (ITraceEvent event : events) {
				switch (event.getType()) {
				case MethodEventType:
				case TypeEventType:
					long currentTime = System.currentTimeMillis();
					if (currentTime - eventTime >= 3000) {
						SketchUI.INSTANCE.refreshJavaUI();
						eventTime = currentTime;
					}
				}
			}
		}
		
	}
	
	private class DebugListener implements IDebugEventSetListener {

		@Override
		public void handleDebugEvents(DebugEvent[] events) {
			SketchPlugin.this.handleDebugEvents(events);
		}
		
	}
	
	/**
	 * The constructor
	 */
	public SketchPlugin() {
		staticJavaModelListener = new StaticJavaModelListener();
		doi = new DegreeOfInterest();
		cachedSketches = new HashMap<String, IProgramSketch>();
		this.debugListener = new DebugListener();
	}

	
	protected void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event: events) {
			if (event.getSource() instanceof ITraceClient) {
				SketchUI.INSTANCE.refreshCommands();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		DebugPlugin.getDefault().addDebugEventListener(debugListener);
		events = new SketchEvents();
		getPreferenceStore().addPropertyChangeListener(SketchUI.INSTANCE);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		DebugPlugin.getDefault().removeDebugEventListener(debugListener);
		synchronized(cachedSketches) {
			for (IProgramSketch sketch : cachedSketches.values()) {
				if (sketch.isConnected()) {
					sketch.getPortal().close();
				}
			}
		}
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static SketchPlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Returns the associated trace client for the given process, or NULL if one doesn't 
	 * exist.
	 * @param process
	 * @return
	 */
	public ITraceClient getAssociatedClient(IProcess process) {
		if (process != null) {
			if (process instanceof ITraceClient) {
				return (ITraceClient) process;
			}
			for (IProcess p : process.getLaunch().getProcesses()) {
				if (p instanceof ITraceClient) {
					return (ITraceClient) p;
				}
			}
		}
		return null;
	}

	/**
	 * Returns all of the traces stored for this workspace. The returned traces
	 * are not cached, but there should be no transient information in the
	 * returned sketches that cannot be computed.
	 * @return all of the traces stored for this workspace.
	 */
	public IProgramSketch[] getStoredSketches() {
		//all of the traces are stored in the plugin state location.
		synchronized (cachedSketches) {
			File stateLocation = getStateLocation().toFile();
			File[] launchDirectories = stateLocation.listFiles();
			for (File launchDirectory : launchDirectories) {
				if (launchDirectory.isDirectory()) {
					//process all of the directories inside this one
					for (File traceDirectory : launchDirectory.listFiles()) {
						if (traceDirectory.isDirectory()) {
							//check to see if it has the correct data stored
							File propertiesFile = new File(traceDirectory, JavaAgentTraceClient.PROCESS_PROPERTIES_FILE);
							if (propertiesFile.exists()) {	
								try {
									Properties props = new Properties();
									FileInputStream fis = new FileInputStream(propertiesFile);
									props.load(fis);
									fis.close();
									String id = props.getProperty(JavaAgentTraceClient.ID_PROPERTY);
									if (!cachedSketches.containsKey(id)) {
										FileReader reader = new FileReader(new File(traceDirectory, "launch.configuration"));
										char[] buf = new char[1024];
										int read = -1;
										StringWriter writer = new StringWriter();
										while ((read = reader.read(buf)) != -1) {
											writer.write(buf, 0, read);
										}
										reader.close();
										writer.close();
										ILaunchConfiguration cf = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(writer.toString());
										if (cf != null) {
											cachedSketches.put(id, new DBProgramSketch(traceDirectory));
										}
										
									}
								} catch (IOException e) {
									Status s = new Status(Status.WARNING, PLUGIN_ID, "An error occurred reading sketch at location " + traceDirectory +". It will be deleted.", e);
									getLog().log(s);
									delete(traceDirectory);
								} catch (CoreException e) {
									Status s = new Status(Status.WARNING, PLUGIN_ID, "An error occurred reading sketch at location " + traceDirectory +". It will be deleted.", e);
									getLog().log(s);
									delete(traceDirectory);
								}
							} else {
								Status s = new Status(Status.WARNING, PLUGIN_ID, "An error occurred reading sketch at location " + traceDirectory +". It will be deleted.");
								getLog().log(s);
								delete(traceDirectory);
							}
						}
					}
				}
			}
			Collection<IProgramSketch> sketches = cachedSketches.values();
			return sketches.toArray(new IProgramSketch[sketches.size()]);
		}
		
	}
	
	private void delete(File file) {
		IPath storageLocation = getStateLocation();
		IPath filePath = new Path(file.getAbsolutePath());
		if (!storageLocation.isPrefixOf(filePath)) return;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			for (File child : children) {
				delete(child);
			}
		}
		file.delete();
	}
	
		
	public void addSketchEventListener(ISketchEventListener listener) {
		events.addListener(listener);
	}
	
	/**
	 * @param traceNavigatorContentProvider
	 */
	public void removeSketchEventListener(ISketchEventListener listener) {
		events.removeListener(listener);			
	}
	
	
	/**
	 * Returns the stored sketches for the given launch configuration.
	 * @param configuration the configuration to check.
	 * @return
	 */
	public IProgramSketch[] getStoredSketches(String launchConfigurationName) {
		ArrayList<IProgramSketch> traces = new ArrayList<IProgramSketch>();
		for (IProgramSketch sketch : getStoredSketches()) {
			String sketchName = sketch.getTracedLaunchConfiguration().getName();
			if (sketchName.equals(launchConfigurationName)) {
				traces.add(sketch);
			}
		}
		return traces.toArray(new IProgramSketch[traces.size()]);
	}
	
	
	public void log(Exception e) {
		if (e instanceof CoreException) {
			getLog().log((((CoreException) e).getStatus()));
		} else {
			getLog().log(createStatus(e));
		}
	}
	
	public IStatus createStatus(Exception e) {
		String message = e.getMessage();
		if (message == null) {
			message = "";
		}
		return new Status(IStatus.ERROR, PLUGIN_ID, message, e);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(ISketchImageConstants.ICON_TRACE_EDITOR, imageDescriptorFromPlugin(PLUGIN_ID, "images/obj16/trace_editor.png"));
		reg.put(ISketchImageConstants.ICON_TRACE_ACTIVE, imageDescriptorFromPlugin(PLUGIN_ID, "images/etool16/trace-active.gif"));
		reg.put(ISketchImageConstants.ICON_TRACE_INACTIVE, imageDescriptorFromPlugin(PLUGIN_ID, "images/etool16/trace-inactive.gif"));
		
		reg.put(ISketchImageConstants.ICON_PROCESS_TRACE, imageDescriptorFromPlugin(PLUGIN_ID, "images/obj16/trace_proc.png"));
		reg.put(ISketchImageConstants.ICON_THREAD_TRACE, imageDescriptorFromPlugin(PLUGIN_ID, "images/obj16/trace_thread.png"));
		reg.put(ISketchImageConstants.ICON_CALENDAR, imageDescriptorFromPlugin(PLUGIN_ID, "images/obj16/calendar.png"));
		reg.put(ISketchImageConstants.ICON_ACTIVATION, imageDescriptorFromPlugin(PLUGIN_ID, "images/obj16/activation.png"));
		reg.put(ISketchImageConstants.ICON_ANNOTATION, imageDescriptorFromPlugin(PLUGIN_ID, "images/obj16/annotation.png"));
		reg.put(ISketchImageConstants.ICON_ANNOTATIONS, imageDescriptorFromPlugin(PLUGIN_ID, "images/obj16/annotations.png"));
		reg.put(ISketchImageConstants.ICON_TRACE, imageDescriptorFromPlugin(PLUGIN_ID, "images/trace_view.png"));
		reg.put(ISketchImageConstants.OVERLAY_ANALYSE, imageDescriptorFromPlugin(PLUGIN_ID, "images/dec8/ovr_gear.png"));
		reg.put(ISketchImageConstants.OVERLAY_PLAY, imageDescriptorFromPlugin(PLUGIN_ID, "images/dec8/ovr_play.png"));
		reg.put(ISketchImageConstants.OVERLAY_STOP, imageDescriptorFromPlugin(PLUGIN_ID, "images/dec8/ovr_stop.png"));
		reg.put(ISketchImageConstants.ICON_ELEMENT_FILTERED, imageDescriptorFromPlugin("images/etool16/closedeye.png"));
		reg.put(ISketchImageConstants.ICON_ELEMENT_VISIBLE, imageDescriptorFromPlugin("images/etool16/openeye.png"));
		reg.put(ISketchImageConstants.ICON_ELEMENT_VISIBLE+"1-3", imageDescriptorFromPlugin("images/etool16/1-3eye.png"));
		reg.put(ISketchImageConstants.ICON_ELEMENT_VISIBLE+"2-3", imageDescriptorFromPlugin("images/etool16/2-3eye.png"));
		reg.put(ISketchImageConstants.ICON_LOGO, imageDescriptorFromPlugin("images/logo16.png"));
		
		super.initializeImageRegistry(reg);
	}
	
	public ColorRegistry getColorRegistry() {
		if (this.colorRegistry == null) {
			colorRegistry = new ColorRegistry(PlatformUI.getWorkbench().getDisplay());
			colorRegistry.put(ISketchColorConstants.RED_KEY, new RGB(200, 0,0));
			colorRegistry.put(ISketchColorConstants.GREEN_KEY, new RGB(0, 150,0));
			colorRegistry.put(ISketchColorConstants.BLUE_KEY, new RGB(0, 0,200));
			colorRegistry.put(ISketchColorConstants.LIGHT_RED_KEY, new RGB(255,225,225));
			colorRegistry.put(ISketchColorConstants.LIGHT_GREEN_KEY, new RGB(225,255,225));
			colorRegistry.put(ISketchColorConstants.LIGHT_BLUE_KEY, new RGB(225,225,255));
			colorRegistry.put(ISketchColorConstants.ERROR_BG_KEY, new RGB(255,250,250));
			colorRegistry.put(ISketchColorConstants.CONDITION_BG_KEY, new RGB(250,255,250));
			colorRegistry.put(ISketchColorConstants.LOOP_BG_KEY, new RGB(250,250,255));
			colorRegistry.put(ISketchColorConstants.AMBER_KEY, new RGB(255, 160, 32));
			colorRegistry.put(ISketchColorConstants.LIGHT_AMBER_KEY, new RGB(255, 225, 180));
			colorRegistry.put(ISketchColorConstants.LIGHT_PURPLE_KEY, new RGB(255, 225, 255));
			colorRegistry.put(ISketchColorConstants.PURPLE_KEY, new RGB(255, 180, 255));
			colorRegistry.put(ISketchColorConstants.GRAY_KEY, new RGB(150, 150, 150));
			colorRegistry.put(ISketchColorConstants.BLACK_KEY, new RGB(0, 0, 0));
			
		}
		return colorRegistry;
	}

	/**
	 * Deletes a sketch from the system.
	 * @param sketch the sketch
	 */
	public void deleteSketch(IProgramSketch sketch) {
		deleteSketch(sketch, true);
	}
	
	public void deleteSketch(final IProgramSketch sketch, boolean fork) {
		final Runnable work = new Runnable() {
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				if (sketch instanceof DBProgramSketch) {
					DBProgramSketch s = (DBProgramSketch) sketch;
					if (s != null) {
						if (s.equals(getActiveSketch())) {
							setActiveSketch(null);
						}
						getDOI().setSketchHidden(sketch, false, new NullProgressMonitor());
						boolean deleted = s.delete();
						if (deleted) {
							synchronized (cachedSketches) {
								Iterator<IProgramSketch> sketches = cachedSketches.values().iterator();
								while (sketches.hasNext()) {
									IProgramSketch next = sketches.next();
									if (next.equals(sketch)) {
										sketches.remove();
									}
								}
							}
							events.fireEvent(new SketchEvent(sketch, SketchEvent.SketchEventType.SketchDeleted));

						}
						if (sketch.equals(doi.getActiveSketch())) {
							setActiveSketch(null);
						}
					}
				}
			}
		};
		if (fork) {
			WorkspaceJob job = new WorkspaceJob("Deleting " + sketch.getLabel()) {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor)
				throws CoreException {
					work.run();
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		} else {
			work.run();
		}
	}

	/**
	 * @param traceElement
	 * @return
	 */
	public IProgramSketch getSketch(ITraceModel traceElement) {
		if (traceElement==null) return null;
		ITrace trace = traceElement.getTrace();
		String launchID = trace.getLaunchID();
		IProgramSketch[] sketches = getStoredSketches();
		for (IProgramSketch sketch : sketches) {
			String sketchLaunch = sketch.getID();
			if (sketchLaunch.equals(launchID)) {
				return sketch;
			}
		}
		return null;
	}

	/**
	 * Sets the active sketch to the given sketch. May be null.
	 * @param sketch the new active sketch. May be null.
	 */
	public synchronized void setActiveSketch(final IProgramSketch sketch) {
		IProgramSketch activeSketch = doi.getActiveSketch();
		if (doi.getActiveSketch() != null) {
			if (activeSketch.getTraceData() != null) {
				activeSketch.getTraceData().removeListener(staticJavaModelListener);
			}
		}
		try {
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					doi.setActiveSketch(sketch, monitor);
					if (sketch != null) {
						sketch.getTraceData().addListener(staticJavaModelListener);
					}
					SketchUI.INSTANCE.refreshJavaUI();
					
				}
			};
			if (Display.getCurrent() != null) {
				getWorkbench().getProgressService().busyCursorWhile(runnable);
			} else {
				runnable.run(new NullProgressMonitor());
			}
			
		} catch (InterruptedException e) {
		} catch (Exception e) {
			log(e);
			doi.setActiveSketch(null, new NullProgressMonitor());
			SketchUI.INSTANCE.refreshJavaUI();
		}
	}

	/**
	 * Returns the active sketch in the framework. May be null.
	 * @return
	 */
	public synchronized IProgramSketch getActiveSketch() {
		return doi.getActiveSketch();
	}
	
	public static ImageDescriptor imageDescriptorFromPlugin(String location) {
		return imageDescriptorFromPlugin(PLUGIN_ID, location);
	}


	/**
	 * @param lastTrace
	 * @return
	 */
	public IProgramSketch getSketch(String identifier) {
		if (identifier == null) return null;
		for (IProgramSketch sketch : getStoredSketches()) {
			if (sketch.getID().equals(identifier)) {
				return sketch;
			}
		}
		return null;
	}


	/**
	 * @return
	 */
	public IDegreeOfInterest getDOI() {
		return doi;
	}

}
