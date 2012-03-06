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

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;


/**
 * Editor input for methods.
 * @author Del Myers
 */

public class MethodEditorInput implements IEditorInput {
	
	private IMethod method;

	public MethodEditorInput(IMethod method) {
		this.method = method;
	}


	public boolean exists() {
		return true;
	}

	public ImageDescriptor getImageDescriptor() {
		return JavaUI.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_PUBLIC);
	}

	/**
	 * @return the method
	 */
	public IMethod getMethod() {
		return method;
	}
	
	public String getName() {
		return method.getElementName();
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return getName();
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if (adapter.isInstance(method)) {
			return method;
		}
		return null;
	}

}
