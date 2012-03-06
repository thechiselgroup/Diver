/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers -- initial API and implementation
 *******************************************************************************/
package org.eclipse.zest.custom.uml.viewers;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * A filter for UML Sequence Viewers. The filter is activation-centric in that when {@link #filter(Viewer, Object, Object[])},
 * is called the <code>parent</code> and <code>element</code> objects are expected to represent an activations. Undefined results
 * occur if this is not the case. 
 * @author Del Myers
 */
@Deprecated
public abstract class UMLSequenceFilter extends ViewerFilter {
	
	/**
	 * Filters the activations for the given viewer, starting at the given activation element, with the given child elements.
	 * @param viewer the viewer. Will be an instance of UMLSequenceViewer
	 * @param parent the parent element representing activations.
	 * @param elements the child activations of the parent element.
	 */
	@Override
	public final Object[] filter(Viewer viewer, Object parent, Object[] elements) {
		return super.filter(viewer, parent, elements);
	}
	
	/**
	 * Assumes that the given elements are the children of the element in the last sement of the given tree path and
	 * filters them using {@link #filter(Viewer, Object, Object[])}.
	 * @param viewer the viewer.
	 * @param parentPath the path to the parent element
	 * @param elements the children of the parent. 
	 */
	@Override
	public final Object[] filter(Viewer viewer, TreePath parentPath, Object[] elements) {
		return super.filter(viewer, parentPath, elements);
	}

	/**
	 * Checks to see if the given activation element passes the filter. The element will only pass the filter if:
	 * 1) selectActivation(viewer, parentElement, element) returns true
	 * 2) selectTargetObject(viewer, targetObject) returns true for the given element, and object that
	 * it is activated on. activationTarget is retrieved from the content provider.
	 * 3) selectPackage(viewer, containingPackage) (if applicable) returns true for all of the containing packages
	 * for the target object of the activation (retrieved from the content provider).
	 */
	@Override
	public final boolean select(Viewer viewer, Object parentElement, Object element) {
		if (selectActivation(viewer, parentElement, element)) {
			ISequenceContentProvider provider = (ISequenceContentProvider) ((UMLSequenceViewer)viewer).getContentProvider();
			Object targetObject = provider.getTargetObject(element);
			if (selectTargetObject(viewer, targetObject)) {
				if (provider instanceof ISequenceContentExtension) {
					ISequenceContentExtension ext = (ISequenceContentExtension) provider;
					Object containingPackage = ext.getContainingGroup(targetObject);
					while (containingPackage != null) {
						if (!selectPackage(viewer, containingPackage)) {
							return false;
						}
						if (!ext.hasContainingGroup(containingPackage)) {
							break;
						}
						containingPackage = ext.getContainingGroup(containingPackage);
					}
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Checks to see if the given package should be displayed in the viewer. If not, then no activation or object contained in that
	 * package will be displayed either. Default implementation always returns true.
	 * @param viewer the viewer containing the package.
	 * @param containingPackage the containing package.
	 * @return true iff the given package should be displayed in the viewer.
	 */
	public boolean selectPackage(Viewer viewer, Object containingPackage) {
		return true;
	}

	/**
	 * Checks to see if the given object/class should be shown in the viewer. If not, then no activation on that object/class will be
	 * shown either.
	 * @param viewer the viewer containing the object. 
	 * @param targetObject the target object element
	 * @return true iff the given target object should be shown in the viewer.
	 */
	public boolean selectTargetObject(Viewer viewer, Object targetObject) {
		return true;
	}

	/**
	 * Checks to see if the given activation element should be shown in the viewer. The result should be independant of the 
	 * target object/class for the activation. That condition will be checked by the {@link #filter(Viewer, Object, Object[])} method.
	 * @param viewer the viewer that the activation element is contained in.
	 * @param parentActivation the parent activation for the activation element.
	 * @param activationElement the activation element to check.
	 * @return true if the activation should be shown, independant of its containers.
	 */
	public abstract boolean selectActivation(Viewer viewer, Object parentActivation, Object activationElement);

}
