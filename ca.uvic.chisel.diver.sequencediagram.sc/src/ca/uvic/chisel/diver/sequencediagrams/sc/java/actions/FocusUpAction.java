/*******************************************************************************
 * Copyright 2005-2007, CHISEL Group, University of Victoria, Victoria, BC, Canada.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Chisel Group, University of Victoria
 *******************************************************************************/
package ca.uvic.chisel.diver.sequencediagrams.sc.java.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;

import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaActivation;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaMessage;

/**
 * Focuses on the parent of the current root activation.
 * @author Del Myers
 */

public class FocusUpAction extends Action {
	
	private UMLSequenceViewer viewer;
	
	public FocusUpAction(UMLSequenceViewer viewer) {
		this.viewer = viewer;
	}
	
	@Override
	public void run() {
		Object root = viewer.getRootActivation();
		if (root instanceof JavaActivation) {
			JavaMessage m = ((JavaActivation)root).getCallingMessage();
			if (m != null && m.getSource() != null) {
				viewer.setRootActivation(m.getSource());
			}
		}
	}

}
