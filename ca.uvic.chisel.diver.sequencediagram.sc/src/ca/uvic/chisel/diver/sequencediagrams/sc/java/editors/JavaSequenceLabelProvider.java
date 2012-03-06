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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.zest.custom.sequence.widgets.Lifeline;
import org.eclipse.zest.custom.uml.viewers.IStylingSequenceLabelProvider;

import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaMessage;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaObject;
import ca.uvic.chisel.javasketch.ui.ISketchColorConstants;

/**
 * Provides labels for the sequence viewer.
 * @author Del Myers
 */

public class JavaSequenceLabelProvider implements IStylingSequenceLabelProvider, IColorProvider {

	private WorkbenchLabelProvider provider = new WorkbenchLabelProvider();
		

	/**
	 * This is the only method that is additional to the standard JFace label providers. 
	 * In this implementation, classes are 
	 */
	public String getStereoType(Object element) {
		if (!(element instanceof IAdaptable)) {
			return null;
		}
		IJavaElement javaElement = (IJavaElement) ((IAdaptable)element).getAdapter(IJavaElement.class);
		if (javaElement instanceof IType) {
			try {
				if (((IType)javaElement).isInterface()) {
					return "interface";
				} else if (Flags.isAbstract(((IType)javaElement).getFlags())) {
					return "abstract";
				}
			} catch (JavaModelException e) {
			}
		}
		return null;
	}

	
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		provider.dispose();
	}

	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return true;
	}

	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	public Color getBackground(Object element) {
		if (element instanceof IAdaptable) {
			IJavaElement javaElement = (IJavaElement) ((IAdaptable)element).getAdapter(IJavaElement.class);
			if (javaElement instanceof IMember) {
				try {
					int flags = ((IMember)javaElement).getFlags();
					if (Flags.isAbstract(flags) || Flags.isInterface(flags)) {
						return Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
					} 
					if (javaElement instanceof IMethod) {
						if (Flags.isPrivate(flags)) {
							return ISketchColorConstants.PRIVATE_BG;
						} else if (Flags.isProtected(flags)) {
							return ISketchColorConstants.PROTECTED_BG;
						} else if (Flags.isPackageDefault(flags)) {
							return ISketchColorConstants.LIGHT_BLUE;
						} else if (Flags.isPublic(flags)) {
							return ISketchColorConstants.PUBLIC_BG;
						}
					}
				} catch (JavaModelException e) {
					//just return null
				}
			} else if (javaElement instanceof IPackageFragment) {
				return ISketchColorConstants.PRIVATE_BG;
			}
		}
		return null;
	}

	public Color getForeground(Object element) {
		if (element instanceof JavaMessage) {
			if (((JavaMessage)element).isException()){
				return ISketchColorConstants.ERROR_FG;
			}
		}
		return null;
	}


	public Image getImage(Object element) {
		if (element instanceof IAdaptable) {
			IJavaElement javaElement = (IJavaElement) ((IAdaptable)element).getAdapter(IJavaElement.class);
			if (javaElement != null) {
				return provider.getImage(javaElement);
			}
		}
		return null;
	}


	public String getText(Object element) {
		if (element instanceof IAdaptable) {
			IJavaElement javaElement = (IJavaElement) ((IAdaptable)element).getAdapter(IJavaElement.class);
			if (javaElement != null) {
				return provider.getText(javaElement);
			} else if (element instanceof JavaObject) {
				return "USER";
			}
		}
		return element.toString();
	}


	public int getLifelineStyle(Object element) {
		if (element instanceof IAdaptable) {
			IJavaElement javaElement = (IJavaElement) ((IAdaptable)element).getAdapter(IJavaElement.class);
			if (javaElement instanceof IPackageFragment) {
				return Lifeline.PACKAGE;
			} else if (javaElement == null) {
				//it's a user type
				return Lifeline.ACTOR;
			}
		}
		return -1;
	}


	public int getMessageLineStyle(Object element) {
		return -1;
	}


	public int getMessageSourceStyle(Object element) {
		return -1;
	}


	public int getMessageTargetStyle(Object element) {
		return -1;
	}

}
