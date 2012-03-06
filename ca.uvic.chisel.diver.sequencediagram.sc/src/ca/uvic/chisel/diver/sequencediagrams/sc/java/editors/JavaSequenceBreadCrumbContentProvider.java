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
package ca.uvic.chisel.diver.sequencediagrams.sc.java.editors;

import java.util.LinkedList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.IJavaActivation;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaActivation;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaMessage;

/**
 * Gives a path to the root from a JavaActivation
 * @author Del Myers
 */

public class JavaSequenceBreadCrumbContentProvider implements
		IStructuredContentProvider {

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof JavaActivation) {
			LinkedList<IJavaActivation> elements = new LinkedList<IJavaActivation>();
			IJavaActivation a = (JavaActivation) inputElement;
			JavaMessage m = a.getCallingMessage();
			while (a != null) {
				elements.addFirst(a);
				m = a.getCallingMessage();
				if (m != null) {
					a = m.getSource();
				} else {
					a = null;
				}
			}
			return elements.toArray();
		}
		return new Object[0];
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
