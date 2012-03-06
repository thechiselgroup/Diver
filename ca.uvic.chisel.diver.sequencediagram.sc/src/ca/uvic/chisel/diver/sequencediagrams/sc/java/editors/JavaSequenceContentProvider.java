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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.zest.custom.uml.viewers.ISequenceChartContentProvider;
import org.eclipse.zest.custom.uml.viewers.ISequenceContentExtension;

import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.IJavaActivation;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaActivation;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaCallTree;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaMessage;
import ca.uvic.chisel.diver.sequencediagrams.sc.java.model.JavaObject;

/**
 * A content provider that parses java files to generate sequence diagrams.
 * @author Del Myers
 */

public class JavaSequenceContentProvider implements
		ISequenceChartContentProvider, ISequenceContentExtension {
	private JavaCallTree tree;

	public Object getLifeline(Object activation) {
		if (activation instanceof IJavaActivation) {
			return ((IJavaActivation)activation).getLifeLine();
		}
		return null;
	}

	public Object[] getMessages(Object activation) {
		if (activation instanceof IJavaActivation) {
			return ((IJavaActivation)activation).getMessages().toArray();
		}
		return new Object[0];
	}

	public Object getTarget(Object message) {
		if (message instanceof JavaMessage) {
			return ((JavaMessage)message).getTarget();
		}
		return null;
	}

	public boolean isCall(Object message) {
		if (message instanceof JavaMessage) {
			ASTNode node = ((JavaMessage)message).getAST();
			switch (node.getNodeType()) {
			case ASTNode.RETURN_STATEMENT:
			case ASTNode.THROW_STATEMENT:
				return false;
			}
		}
		return true;
	}

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof MethodEditorInput) {
			this.tree = JavaCallTree.createTree(((MethodEditorInput)inputElement).getMethod());
			return new IJavaActivation[] {tree.getRoot()};
		}
		return new Object[0];
	}

	public void dispose() {
		if (tree != null) tree.clearCache();
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (tree != null) tree.clearCache();
	}

	public Object getContainingGroup(Object lifelineOrGroup) {
		if (lifelineOrGroup instanceof JavaObject) {
			IJavaElement element = ((JavaObject)lifelineOrGroup).getJavaElement();
			if (element instanceof IType) {
				return ((IType)element).getPackageFragment();
			}
		} else if (lifelineOrGroup instanceof IPackageFragment) {
			IPackageFragment fragment = (IPackageFragment) lifelineOrGroup;
			IPackageFragmentRoot root = (IPackageFragmentRoot) fragment.getParent();
			int lastDot = fragment.getElementName().lastIndexOf('.');
			if (lastDot > 0) {
				String name = fragment.getElementName().substring(0, lastDot);
				return root.getPackageFragment(name);
			}
		}
		return null;
	}

	public boolean hasContainingGroup(Object lifelineOrGroup) {
		if (lifelineOrGroup instanceof JavaObject) {
			IJavaElement element = ((JavaObject)lifelineOrGroup).getJavaElement();
			if (element instanceof IType) {
				return ((IType)element).getPackageFragment() != null;
			}
		} else if (lifelineOrGroup instanceof IPackageFragment) {
			IPackageFragment fragment = (IPackageFragment) lifelineOrGroup;
			IPackageFragmentRoot root = (IPackageFragmentRoot) fragment.getParent();
			int lastDot = fragment.getElementName().lastIndexOf('.');
			if (lastDot > 0) {
				String name = fragment.getElementName().substring(0, lastDot);
				return root.getPackageFragment(name) != null;
			}
		}
		return false;
	}

}
