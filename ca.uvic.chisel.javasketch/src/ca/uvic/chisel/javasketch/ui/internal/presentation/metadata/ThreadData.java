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
import java.io.IOException;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IThread;

/**
 * Stores presentation metadata about a particular thread.
 * 
 * @author Del Myers
 *
 */
public class ThreadData {
	private String threadID;
	private PresentationData presentation;
	private IThread thread;
	private String root;
	private int threadNum;
	private File threadDirectory;
	
	/**
	 * @param presentation2
	 * @param file
	 * @throws IOException 
	 */
	public ThreadData(PresentationData presentation, int threadNum, String threadID)  {
		this.presentation = presentation;
		this.threadID = threadID;
		this.threadNum = threadNum;
		try {
			initialize();
		} catch (IOException e) {
			SketchPlugin.getDefault().log(e);
		}
		
	}

	/**
	 * 
	 */
	private void initialize() throws IOException {
		File root = presentation.getPresentationPath();
		threadDirectory = new File (root, "" + threadNum);
		if (threadDirectory.exists() && !threadDirectory.isDirectory()) {
			throw new IOException(threadDirectory.getAbsolutePath() + " is not a directory");
		} else if (!threadDirectory.exists()) {
			if (!threadDirectory.mkdirs()) {
				throw new IOException("Could not create " + threadDirectory.getAbsolutePath());
			}
		}
	}

	/**
	 * @return the thread
	 */
	public IThread getThread() {
		if (thread == null) {
			thread = (IThread) presentation.getSketch().getTraceData().findElement(threadID);
		}
		return thread;
	}
	
	/**
	 * @return the threadID
	 */
	public String getThreadID() {
		return threadID;
	}

	/**
	 * @param activation
	 * @return
	 */
	public boolean isExpanded(IActivation activation) {
		ActivationData activationData = getActivationData(activation.getIdentifier());
		return activationData.isExpanded();
	}
	
	public ActivationData getActivationData(String activationID) {
		return new ActivationData(this, activationID);
	}


	/**
	 * @return
	 */
	public PresentationData getPresentation() {
		return presentation;
	}

	/**
	 * @param element
	 * @param expanded
	 */
	public void setActivationExpanded(IActivation element, boolean expanded) {
		ActivationData activationData = getActivationData(element.getIdentifier());
		activationData.setExpanded(expanded);
		
	}

	/**
	 * @param rootActivation
	 */
	public void setRoot(IActivation rootActivation) {
		this.root = rootActivation.getIdentifier();
	}
	
	public String getRootId() {
		return root;
	}
	
	public IActivation getRoot() {
		try {
			if (root == null || "".equals(root)) {
				return getThread().getRoot().getActivation();
			}
			return (IActivation) presentation.getSketch().getTraceData().findElement(root);
		} catch (NullPointerException e) {}
		return null;
	}

	/**
	 * @return
	 */
	File getDirectory() {
		return threadDirectory;
	}
}
