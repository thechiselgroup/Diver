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
package ca.uvic.chisel.javasketch.ui.internal.presentation;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.ui.ISketchImageConstants;

/**
 * @author Del Myers
 *
 */
public class ProgramSketchEditorInput implements IEditorInput {

	private IThread thread;

	/**
	 * @param o
	 */
	public ProgramSketchEditorInput(IThread o) {
		this.thread = o;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	@Override
	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		return SketchPlugin.getDefault().getImageRegistry().getDescriptor(ISketchImageConstants.ICON_TRACE_EDITOR);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	@Override
	public String getName() {
		return thread.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		return getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.isInstance(thread)) {
			return thread;
		}
		return null;
	}

	/**
	 * @return
	 */
	public IThread getThread() {
		return thread;
	}

}
