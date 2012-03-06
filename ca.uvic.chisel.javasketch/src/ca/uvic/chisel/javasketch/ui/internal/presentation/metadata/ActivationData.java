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
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;
import ca.uvic.chisel.javasketch.internal.ast.groups.ASTLoopGroupCalculator;
import ca.uvic.chisel.javasketch.internal.ast.groups.ASTMessageGroupingTree;

/**
 * @author Del Myers
 *
 */
public class ActivationData {
	
	private String activationID;
	private IActivation activation;
	private ThreadData threadData;
	private File activationDirectory;
	
	ActivationData(ThreadData td, String activationID)  {
		this.threadData = td;
		this.activationID = activationID;
		try {
			initialize();
		} catch (IOException e) {
			SketchPlugin.getDefault().log(e);
		}
	}

	/**
	 * @throws IOException 
	 * 
	 */
	private void initialize() throws IOException {
		File threadDirectory = threadData.getDirectory();
		File activationDirectory = new File (threadDirectory, activationID);
		if (activationDirectory.exists() && !activationDirectory.isDirectory()) {
			throw new IOException(activationDirectory.getAbsolutePath() + " is not a directory");
		} else if (!activationDirectory.exists()) {
			if (!activationDirectory.mkdirs()) {
				throw new IOException("Could not create directory " + activationDirectory.getAbsolutePath());
			}
		}
		this.activationDirectory = activationDirectory;
	}

	/**
	 * @return
	 */
	public ASTMessageGroupingTree getGroups() {
		return ASTLoopGroupCalculator.calculateGroups(getActivation());
	}
	
	/**
	 * @return the activation
	 */
	public IActivation getActivation() {
		if (activation == null) {
			activation = (IActivation) threadData.getPresentation().getSketch().getTraceData().findElement(activationID);
		}
		return activation;
	}
	
	public String getActivationID() {
		return activationID;
	}

	/**
	 * @param node
	 * @return
	 */
	public boolean isGroupVisible(ASTMessageGroupingTree node) {
		GroupData groupData = getGroupData(node);
		return groupData.isVisible();
	}
	
	/**
	 * @param node
	 * @return
	 */
	private GroupData getGroupData(ASTMessageGroupingTree node) {
		return new GroupData(this, node);
	}

	File getActivationDirectory() {
		return activationDirectory;
	}
	

	public boolean isGroupExpanded(ASTMessageGroupingTree node) {
		GroupData groupData = getGroupData(node);
		return groupData.isExpanded();
	}
	
	
	/**
	 * @param sibling
	 * @param b
	 */
	public void setGroupVisible(ASTMessageGroupingTree node, boolean visible) {
		GroupData groupData = getGroupData(node);
		groupData.setVisible(visible);
		
	}

	/**
	 * @param node
	 */
	public void swapLoop(ASTMessageGroupingTree node, boolean firstNonempty) {
		ASTMessageGroupingTree[] nonEmpty = new ASTMessageGroupingTree[2];
		boolean past = false;
		for (ASTMessageGroupingTree sibling : node.getIterations()) {
			if (sibling.equals(node)) {
				past = true;
			}
			if (sibling.isLoop() && sibling.getNodeID().equals(node.getNodeID())) {
				if (firstNonempty) {
					if (!isGroupEmpty(sibling)) {
						if (!past) {
							nonEmpty[0] = sibling;
						} else {
							if (nonEmpty[1] == null) {
								nonEmpty[1] = sibling;
							}
						}
					}
				}
				setGroupVisible(sibling, false);
			}
		}
		if (firstNonempty) {
			if (nonEmpty[0] != null) {
				if (nonEmpty[1] != null) {
					if (Math.abs(node.getIteration() - nonEmpty[0].getIteration()) <
						(Math.abs(node.getIteration() - nonEmpty[1].getIteration()))) {
						setGroupVisible(nonEmpty[0], true);
					} else {
						setGroupVisible(nonEmpty[1], true);
					}
				} else {
					setGroupVisible(nonEmpty[0], true);
				}
			} else {
				if (nonEmpty[1] != null) {
					setGroupVisible(nonEmpty[1], true);
				}
			}
		}
		else {
			setGroupVisible(node, true);
		}
	}

	boolean isGroupEmpty(ASTMessageGroupingTree group) {
		for (String id : group.getMessageIdentifiers()) {
			ITraceModelProxy proxy = getActivation().getTrace().getElement(id);
			IOriginMessage message = (IOriginMessage) proxy.getElement();
			double interest = SketchPlugin.getDefault().getDOI().getInterest(message);
			if (interest > .3) {
				return false;
			}
		}
		return true;
	}

	

	/**
	 * @param grouping
	 * @param expanded2
	 */
	public void setGroupExpanded(ASTMessageGroupingTree grouping,
			boolean expanded) {
		GroupData groupData = getGroupData(grouping);
		groupData.setExpanded(expanded);
	}

	/**
	 * @return
	 */
	public boolean isExpanded() {
		File file = new File(activationDirectory, "expanded");
		return file.exists();
	}

	/**
	 * @param expanded2
	 */
	public void setExpanded(boolean expanded) {
		File file = new File(activationDirectory, "expanded");
		if (expanded) {
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					SketchPlugin.getDefault().log(e);
				}
			}
		} else {
			if (file.exists()) {
				file.delete();
			}
		}
	}

}
