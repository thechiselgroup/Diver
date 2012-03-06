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
package ca.uvic.chisel.javasketch.internal.adapters;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;

import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;

/**
 * Adapters for trace class methods.
 * @author Del Myers
 *
 */
public class TraceClassMethodAdapterFactory implements IAdapterFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof ITraceClassMethod) {
			if (IJavaElement.class.isAssignableFrom(adapterType)) {
				try {
					return JavaSearchUtils.findElement(
						((ITraceClassMethod) adaptableObject), new NullProgressMonitor());
				} catch (InterruptedException e) {
					return null;
				} catch (CoreException e) {
					return null;
				}
			} else if (ITraceModel.class.isAssignableFrom(adapterType)) {
				return ((ITraceClassMethod)adaptableObject);
			}
		} 
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Class[] getAdapterList() {
		return new Class[] {IJavaElement.class, IMethod.class, ITraceModel.class, ITraceClassMethod.class};
	}

}
