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
package ca.uvic.chisel.javasketch.ui.internal.presentation.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.handlers.IHandlerService;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;
import ca.uvic.chisel.javasketch.internal.DBProgramSketch;
import ca.uvic.chisel.javasketch.internal.ast.groups.ASTMessageGroupingTree;

/**
 * A class that stores presentation information for a java sketch.
 * @author Del Myers
 *
 */
public class PresentationData {
	private static HashMap<IProgramSketch, PresentationData> cache = new HashMap<IProgramSketch, PresentationData>();
	private Properties annotations;
	private IProgramSketch sketch;
	private int connections;
	private boolean isCompressingLoops = true;
	private long timestamp;

		
	
	private PresentationData(IProgramSketch sketch) {
		this.sketch = sketch;
		annotations = new Properties();
	}
	
	public static PresentationData connect(IProgramSketch sketch) {
		if (sketch == null) return null;
		PresentationData data;
		synchronized (cache) {
			data = cache.get(sketch);
			if (data == null) {
				data = new PresentationData(sketch);
				cache.put(sketch, data);
			}

		}
		data.connect();
		return data;
	}
	
	
	private synchronized void connect() {
		connections++;
		if (connections == 1) {
			try {
				load();
			} catch (Exception e) {
				SketchPlugin.getDefault().log(e);
			}
		}
	}
	
	public synchronized void disconnect() {
		connections--;
		if (connections == 0) {
			try {
				save();
			} catch (Exception e) {
				SketchPlugin.getDefault().log(e);
			}
			remove();
		}
	}
	
	
	protected void finalize() throws Throwable {
		//save();
	};
	/**
	 * 
	 */
	private void remove() {
		synchronized (cache) {
			cache.remove(getSketch());
		}
	}
		
	/**
	 * Checks to see whether the given activation is expanded in the presentation.
	 * @param activation the activation to query.
	 */
	public synchronized boolean isExpanded(IActivation activation) {
		ThreadData td = getThreadData(activation.getArrival().getThread());
		return td.isExpanded(activation);
	}
	
	public synchronized ASTMessageGroupingTree getGroups(IActivation activation) {
		ActivationData ad = getActivationData(activation);
		return ad.getGroups();
	}
	
	public synchronized boolean isGroupVisible(IActivation activation, ASTMessageGroupingTree node) {
		if (!isCompressingLoops) {
			return true;
		}
		ActivationData ad = getActivationData(activation);
		return ad.isGroupVisible(node);
	}
	
	public synchronized boolean isGroupEmpty(IActivation activation, ASTMessageGroupingTree node) {
		ActivationData ad = getActivationData(activation);
		return ad.isGroupEmpty(node);
	}
	
	/**
	 * Swaps out the visibility of one iteration of a loop for the one represented by the given node.
	 * Only one iteration of a loop is visible at a time when this method is used.
	 * @param activation
	 * @param node
	 */
	public synchronized void swapLoop(IActivation activation, ASTMessageGroupingTree node, boolean firstNonEmpty) {
		ActivationData ad = getActivationData(activation);
		ad.swapLoop(node, firstNonEmpty);
	}

	/**
	 * @param activation
	 * @return
	 */
	private ActivationData getActivationData(IActivation activation) {
		ThreadData td = getThreadData(activation.getArrival().getThread());
		return td.getActivationData(activation.getIdentifier());
	}

	/**
	 * @param thread
	 * @return
	 */
	private synchronized ThreadData getThreadData(IThread thread) {
		validate();
		return new ThreadData(this, thread.getID(), thread.getIdentifier());
	}

	/**
	 * @return the sketch
	 */
	public IProgramSketch getSketch() {
		return sketch;
	};
	
	public synchronized void save() throws IOException {
		File path = getPresentationPath();
		if (path == null || ! path.exists()) return;
		FileOutputStream output = new FileOutputStream(new File(path, "metadata"));
		
		Properties metaProperties = new Properties();
		metaProperties.put("isCompressingLoops", isCompressingLoops + "");
		metaProperties.put("timestamp", timestamp + "");
		try {
			metaProperties.store(output, "");
		} finally {
			output.close();
		}
		output = new FileOutputStream(new File(path, "annotations"));
		try {
			annotations.store(output, "");
		} finally {
			output.close();
		}
	}
	
	private synchronized void erase() {
		File root = getPresentationPath();
		LinkedList<File> pathsToDelete = new LinkedList<File>();
		LinkedList<File> filesToDelete = new LinkedList<File>();
		if (root.exists()) {
			//delete all the files in the path
			filesToDelete.add(root);
			while (filesToDelete.size() > 0) {
				 File f = filesToDelete.removeFirst();
				 if (f.isDirectory()) {
					 pathsToDelete.addFirst(f);
					 for (File child : f.listFiles()) {
						 filesToDelete.addLast(child);
					 }
				 } else {
					 f.delete();
				 }
			}
		}
		for (File path : pathsToDelete) {
			path.delete();
		}
	}
	
	private synchronized void load() throws IOException {
		File f = getPresentationPath();
		if (f.exists() && !f.isDirectory()) {
			if (!f.delete()) {
				throw new IOException("Could not create new presentation path");
			}
		}
		if (!f.exists()) {
			if (!f.mkdirs()) {
				throw new IOException("Could not create new presentation path");
			}
		}
		this.timestamp = System.currentTimeMillis();
		if (f != null && f.exists()) {
			File metadata = new File(f, "metadata");
			if (!metadata.exists()) {
				Properties props = new Properties();
				props.put("isCompressingLoops", "true");
				props.put("timestamp", "" + timestamp);
				FileOutputStream os = new FileOutputStream(metadata);
				try {
					props.store(os, "");
				} finally {
					os.close();
				}
			}
			File annotationsFile = new File(f, "annotations");
			if (annotationsFile.exists()) {
				FileInputStream fis = new FileInputStream(annotationsFile);
				try {
					annotations.load(fis);
				} finally {
					fis.close();
				}
			}
			//load the metadata
			Properties p = new Properties();
			FileInputStream fis = new FileInputStream(metadata);
			try {
				p.load(fis);
			} finally {
				fis.close();
			}
			this.isCompressingLoops = Boolean.parseBoolean(p.getProperty("isCompressingLoops", "true"));
			this.timestamp = Long.parseLong(p.getProperty("timestamp", "0"));
		}
	}
	


	
	
	File getPresentationPath() {
		if (sketch instanceof DBProgramSketch) {
		URL tracePath = sketch.getTracePath();
		try {
			File path = new File(tracePath.toURI());
			File f = new File(path, ".presentation");
			return f;
		} catch (URISyntaxException e) {

		}
		}
		return null;
	}

	/**
	 * @param activationElement
	 * @param grouping
	 * @param b
	 */
	public synchronized void setGroupExpanded(IActivation activation,
			ASTMessageGroupingTree grouping, boolean expanded) {
		validate();
		ActivationData ad = getActivationData(activation);
		if (ad != null) {
			ad.setGroupExpanded(grouping, expanded);
		}
	}
	
	public void setAnnotation(ITraceModel element, String annotation) {
		if (element != null && annotation != null) {
			try {
				IHandlerService hService =
					(IHandlerService) SketchPlugin.getDefault().getWorkbench().getService(IHandlerService.class);
				if (hService != null) {
					hService.executeCommand("ca.uvic.chisel.javasketch.annotate", null);
				}
			} catch (ExecutionException e) {}
			  catch (NotDefinedException e) {} 
			  catch (NotEnabledException e) {}
			  catch (NotHandledException e) {}
			annotations.put(element.getIdentifier(), annotation);	
		}



	}
	
	public String getAnnotation(ITraceModel element) {
		if (element != null) {
			return annotations.getProperty(element.getIdentifier());
		}
		return null;
	}
	
	public String getAnnotation(String elementId) {
		if (elementId != null) {
			return annotations.getProperty(elementId);
		}
		return null;
	}
	
	public ITraceModelProxy[] getAnnotatedElements() {
		LinkedList<ITraceModelProxy> proxies = new LinkedList<ITraceModelProxy>();
		for (Object id : annotations.keySet()) {
			ITrace trace = sketch.getTraceData();
			ITraceModelProxy proxy = trace.getElement(id.toString());
			if (proxy != null) {
				proxies.add(proxy);
			}		
		}
		return proxies.toArray(new ITraceModelProxy[proxies.size()]);
	}

	/**
	 * @param element
	 * @param b
	 */
	public synchronized void setActivationExpanded(IActivation element, boolean expanded) {
		validate();
		try {
			ThreadData td = getThreadData(element.getArrival().getThread());
			if (td != null) {
				td.setActivationExpanded(element, expanded);
			}
		} catch (NullPointerException e){}
		
	}

	/**
	 * @param thread
	 * @param rootActivation
	 */
	public synchronized void setThreadRoot(IThread thread, IActivation rootActivation) {
		ThreadData td = getThreadData(thread);
		td.setRoot(rootActivation);
	}

	/**
	 * @param activation
	 * @param node
	 * @return
	 */
	public synchronized boolean isGroupExpanded(IActivation activation,
			ASTMessageGroupingTree node) {
		ActivationData ad = getActivationData(activation);
		return ad.isGroupExpanded(node);
	}
	
	/**
	 * Validates the presentation to check to see whether the trace has been reset. If it has, then
	 * the presentation is invalid, and all the visible activations must be redone.
	 * @throws IOException 
	 */
	private void validate() {
		Date date = getSketch().getTraceData().getDataTime();
		if (date.after(new Date(timestamp))) {
			erase();
			try {
				load();
			} catch (IOException e) {
				SketchPlugin.getDefault().log(e);
			}
		}
	}

	
}
