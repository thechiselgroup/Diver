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
package ca.uvic.chisel.javasketch.ui.internal.presentation.metadata;

import java.io.File;
import java.io.IOException;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.internal.ast.groups.ASTMessageGroupingTree;

/**
 * @author Del Myers
 *
 */
public class GroupData {

	private ActivationData activationData;
	private File groupDirectory;

	/**
	 * @param activationData
	 * @param node
	 */
	public GroupData(ActivationData activationData, ASTMessageGroupingTree node) {
		this.activationData = activationData;
		try {
			initializeGroup(node);
		} catch (IOException e) {
			SketchPlugin.getDefault().log(e);
		}
	}
	
	/**
	 * @param identifier
	 * @throws IOException 
	 */
	private void initializeGroup(ASTMessageGroupingTree node) throws IOException {
		File activationDirectory = activationData.getActivationDirectory();
		groupDirectory = new File(activationDirectory, node.getIdentifier());
		if (groupDirectory.exists() && !groupDirectory.isDirectory()) {
			throw new IOException(groupDirectory.getAbsolutePath() + " is not a directory");
		} else if (!groupDirectory.exists()) {
			if (!groupDirectory.mkdirs()) {
				throw new IOException("Could not create directory " + groupDirectory.getAbsolutePath());
			}
			File expandedMarker = new File(groupDirectory, "expanded");
			expandedMarker.createNewFile();
			if (node.isLoop()) {
				File loopMarker = new File(groupDirectory, "loop");
				loopMarker.createNewFile();
				if (node.getIteration() == 1) {
					File visibleMarker = new File(groupDirectory, "visible");
					visibleMarker.createNewFile();
				}
			} else {
				File visibleMarker = new File(groupDirectory, "visible");
				visibleMarker.createNewFile();
			}
		}
	}

	/**
	 * @param expanded
	 */
	public void setExpanded(boolean expanded) {
		File file = new File(groupDirectory, "expanded");
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

	/**
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		File file = new File(groupDirectory, "visible");
		if (visible) {
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

	/**
	 * @return
	 */
	public boolean isExpanded() {
		File file = new File(groupDirectory, "expanded");
		return file.exists();
	}

	/**
	 * @return
	 */
	public boolean isVisible() {
		File file = new File(groupDirectory, "visible");
		return file.exists();
	}

}
