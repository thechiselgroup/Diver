/*******************************************************************************
 * Copyright (c) 2010 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.logging.eclipse.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;

import ca.uvic.chisel.logging.eclipse.DefaultInterpreter;
import ca.uvic.chisel.logging.eclipse.DefaultSWTInterpreter;
import ca.uvic.chisel.logging.eclipse.ILogObjectInterpreter;
import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * @author Del Myers
 *
 */
public class LoggingCategory implements ILoggingCategory {
	
	private static final ILogObjectInterpreter defaultInterpreter = new DefaultInterpreter();
	
	private static final ILogObjectInterpreter swtInterpreter = new DefaultSWTInterpreter();
	
	private class InterpreterProxy {
		boolean interpretSubTypes;
		Class<?> target;
		ILogObjectInterpreter interpreter;
		public InterpreterProxy(boolean interpretSubTypes, Class<?> target, ILogObjectInterpreter interpreter) {
			this.interpretSubTypes = interpretSubTypes;
			this.target = target;
			this.interpreter = interpreter;
		}
	}
	
	private class ProxySorter implements Comparator<InterpreterProxy> {
		
		private Class<?> target;
		public ProxySorter(Class<?> target) {
			this.target = target;
		}

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(InterpreterProxy o1, InterpreterProxy o2) {
			if (o1.target.equals(o2.target)) {
				return 0;
			}
			//calculate the distance for each proxy from the target.
			return findDistance(o1) - findDistance(o2);
		}

		/**
		 * @param proxy
		 */
		private int findDistance(InterpreterProxy proxy) {
			int distance = 0;
			if (proxy.target.equals(target)) {
				//distance is equal to 0: they are the same class
				distance = 0;
			} else {
				distance = 1;
				Class<?> parent = target;

				//walk up the call chain until the interface is found.
				boolean found = false;
				while (parent != null && !found) {
					if (proxy.target.equals(parent)) {
						found = true;
						break;
					}
					Class<?>[] interfaces = parent.getInterfaces();
					for (int i = 0; i < interfaces.length; i++) {
						if (interfaces[i].equals(proxy.target)) {
							distance += i;
							found = true;
							break;
						}
					}
					if (!found) {
						distance *= 10;
						parent = parent.getSuperclass();
					}
				}
				
			}
			return distance;
		}
		
		
	}
	
	private List<InterpreterProxy> interpreters;

	private String categoryID;
	
	private String categoryName;

	private String disclaimer;

	private URL url;

	private boolean isHTML;

	private Log log;

	private XMLMemento memento;

	private String provider;
	
	public LoggingCategory(IConfigurationElement element) {
		this.log = new Log(this);
		this.categoryID = element.getAttribute("id");
		this.categoryName = element.getAttribute("name");
		this.provider = element.getAttribute("provider");
		this.disclaimer = element.getChildren("disclaimer")[0].getValue();
		this.isHTML = Boolean.parseBoolean(element.getAttribute("isHTML"));
		try {
			this.url = new URL(element.getAttribute("url"));
		} catch (Exception e) {
			url = null;
		}
		if (categoryID == null || disclaimer == null) {
			throw new RuntimeException("Malformed Element");
		}
		WorkbenchLoggingPlugin.getDefault().getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
			
			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				saveState();
				return true;
			}
			
			public void postShutdown(IWorkbench workbench) {
			}
		});
	}

	protected synchronized void saveState() {
		if (memento != null) {
			IPath stateLocation = WorkbenchLoggingPlugin.getDefault().getStateLocation();
			File stateDirectory = stateLocation.toFile();
			File mementoFile = new File(stateDirectory, getCategoryID() + ".state");
			try {
				FileWriter writer = new FileWriter(mementoFile);
				memento.save(writer);
				writer.close();
			} catch (IOException e) {
				WorkbenchLoggingPlugin.getDefault().log(e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.ILoggingCategory#getDisclaimer()
	 */
	public String getDisclaimer() {
		return disclaimer;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.ILoggingCategory#getID()
	 */
	public String getCategoryID() {
		return categoryID;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.ILoggingCategory#getInterpreter(java.lang.Class)
	 */
	public ILogObjectInterpreter getInterpreter(Class<?> clazz) {
		if (interpreters == null) {
			loadInterpreters();
		}
		if (clazz == null) return defaultInterpreter;
		TreeSet<InterpreterProxy> interpreterSet = new TreeSet<InterpreterProxy>(new ProxySorter(clazz));
		for (InterpreterProxy proxy : interpreters) {
			if (!proxy.interpretSubTypes) {
				if (clazz.equals(proxy.target)) {
					interpreterSet.add(proxy);
				}
			} else {
				if (proxy.target.isAssignableFrom(clazz)) {
					interpreterSet.add(proxy);
				}
			}
		}
		if (interpreterSet.size() == 0) {
			if (Event.class.isAssignableFrom(clazz)) {
				return swtInterpreter;
			} else {
				return defaultInterpreter;
			}
		} else {
			return interpreterSet.first().interpreter;
		}
	}

	/**
	 * 
	 */
	private void loadInterpreters() {
		interpreters = new LinkedList<InterpreterProxy>();
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("ca.uvic.chisel.logging.eclipse.interpreter");
		IConfigurationElement[] elements = point.getConfigurationElements();
		for (IConfigurationElement element:elements) {
			if ("interpreter".equals(element.getName())) {
				String categoryID = element.getAttribute("categoryID");
				if (getCategoryID().equals(categoryID)) {
					try {
						ILogObjectInterpreter interpreter = (ILogObjectInterpreter) element.createExecutableExtension("class");
						String target = element.getAttribute("target");
						Bundle bundle = Platform.getBundle(element.getContributor().getName());
						Class<?> targetClass = bundle.loadClass(target);
						boolean interpretSubTypes = Boolean.parseBoolean(element.getAttribute("implements"));
						interpreters.add(new InterpreterProxy(interpretSubTypes, targetClass, interpreter));
					} catch (Exception e) {
						WorkbenchLoggingPlugin.getDefault().log(e);
					}
				}
			}
		}

		
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.ILoggingCategory#getURL()
	 */
	public URL getURL() {
		return url;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.logging.eclipse.ILoggingCategory#isHTML()
	 */
	public boolean isHTML() {
		return isHTML;
	}
	
	public Log getLog() {
		return log;
	}

	public String getName() {
		return categoryName;
	}

	public synchronized IMemento getMemento() {
		if (memento != null) return memento;
		IPath stateLocation = WorkbenchLoggingPlugin.getDefault().getStateLocation();
		File stateDirectory = stateLocation.toFile();
		File mementoFile = new File(stateDirectory, getCategoryID() + ".state");
		if (mementoFile.exists()) {
			try {
				Reader reader = new FileReader(mementoFile);
				memento = XMLMemento.createReadRoot(reader);
				reader.close();
			} catch (FileNotFoundException e) {
			} catch (WorkbenchException e) {
			} catch (IOException e) {
				WorkbenchLoggingPlugin.getDefault().log(e);
			} 
		}
		if (memento == null){
			//the memento couldn't be read, create a new memento
			memento = XMLMemento.createWriteRoot("loggingCategory");
			memento.putBoolean("enabled", true);
			saveState();
		}
		return memento;
	}

	public File getLogLocation() {
		File stateLocation = WorkbenchLoggingPlugin.getDefault().getStateLocation().toFile();
		File categoryLocation =  new File(stateLocation, getCategoryID());
		if (!categoryLocation.isDirectory()) {
			if (!categoryLocation.isDirectory()) {
				categoryLocation.delete();
			}
			categoryLocation.mkdirs();
		}
		return categoryLocation;
	}

	public boolean isEnabled() {
		IMemento memento = getMemento();
		if (memento == null) {
			return false;
		}
		return memento.getBoolean("enabled");
	}

	public void setEnabled(boolean enabled) {
		IMemento memento = getMemento();
		if (memento != null){
			memento.putBoolean("enabled", enabled);
			saveState();
		}
	}

	public String getProvider() {
		return provider;
	}

	public File[] getFilesToUpload() {
		IMemento memento = log.getCategory().getMemento();
		File logLocation = log.getCategory().getLogLocation();
		if (logLocation.isDirectory()) {
			String lastUpload = memento.getString("lastUpload");
			long uploadTime = 0;
			try {
				if (lastUpload != null) {
					String timeString = lastUpload.substring(0, lastUpload.length()-".zip".length());
					uploadTime = Long.parseLong(timeString);
				}
			} catch (NumberFormatException e) {
			}
			final long fTime = uploadTime;
			File[] children = logLocation.listFiles(new FileFilter(){
				public boolean accept(File pathname) {
					String fileName = pathname.getName();
					if (fileName.endsWith(".zip")) {
						long date;
						try {
							String fileDate = fileName.substring(0, fileName.length()-".zip".length());
							String[] parts = fileDate.split("\\-");
							date = Long.parseLong(parts[0]);
							return date > fTime;
						} catch (Exception e) {
						}
					}
					return false;
				}});
			return children;
		}
		return new File[0];
	}
	
	/**
	 * Returns true if there is any data to upload to the server.
	 * @return true if there is any data to upload to the server.
	 */
	public boolean isStale() {
		File[] backupFiles = getFilesToUpload();
		return (log.getLogFile().length() > 32) || ( backupFiles != null && backupFiles.length > 0);
	}

}
