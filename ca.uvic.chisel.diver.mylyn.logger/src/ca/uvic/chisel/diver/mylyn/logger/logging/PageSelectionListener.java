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
package ca.uvic.chisel.diver.mylyn.logger.logging;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jface.action.ExternalActionManager.IActiveChecker;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.context.core.IInteractionContextManager;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

import ca.uvic.chisel.diver.mylyn.logger.MylynLogger;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;

/**
 * @author Del
 *
 */
public class PageSelectionListener implements ISelectionListener {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		//log the selection.
		String partId = part.getSite().getId();
		Object o = getSelectionObject(part, selection);
		if (o == null) {
			return;
		}
		String elementSelection = translate(o) + "\tpart=" + partId;
		MylynLogger.getDefault().logEvent(elementSelection);
		
	}

	
	/**
	 * @param o
	 * @return
	 */
	private String translate(Object o) {
		String eventString = "";
		eventString += "\tevent=selection";
		if (o instanceof ITraceModel) {
			ITraceModel model = (ITraceModel) o;
			if (model instanceof IMessage) {
				return eventString + translateMessage((IMessage)o);
			} else if (model instanceof IActivation) {
				ITraceClassMethod method = ((IActivation)model).getMethod();
				ITraceClass c = method.getTraceClass();
				eventString += "\tkind=activation\telement=" + c.getName() + "." + method.getName() + "." + method.getSignature();
				
			} else if (model instanceof ITraceClass) {
				eventString += "\tkind=traceClass\telement=" + ((ITraceClass)model).getName();
			} else if (model instanceof ITraceClassMethod) {
				ITraceClassMethod method = (ITraceClassMethod) model;
				ITraceClass c = method.getTraceClass();
				eventString += "\tkind=traceClassMethod\telement=" + c.getName() + "." + method.getName() + "." + method.getSignature();
			} else if (model instanceof IThread) {
				eventString += "\tkind=traceThread\telement=" + ((IThread)model).getName();
			}
		} else if (o instanceof IJavaElement) {
			IJavaElement je = (IJavaElement) o;
			IMethod method = getContainingMethod(je);
			String methodName = "null";
			if (method != null) {
				try {
					methodName = method.getElementName() + "." +
							method.getSignature();
				} catch (JavaModelException e) {
					methodName = "null";
				}
			}
			IType type = getContainingType(je);
			String elementType = getElementType(je);
			eventString += "\tkind=javaElement\telementType=" + elementType +
				"\telement=" + je.getElementName() +
				"\tmethod=" + methodName +
				"\tclass=" + 
					((type != null) ? type.getFullyQualifiedName() : "null");
						
		}
		return eventString;
	}

	/**
	 * @param je
	 * @return
	 */
	private String getElementType(IJavaElement je) {
		switch (je.getElementType()) {
		case IJavaElement.ANNOTATION:
			return "annotation";
		case IJavaElement.CLASS_FILE:
			return "classfile";
		case IJavaElement.COMPILATION_UNIT:
			return "compilationunit";
		case IJavaElement.FIELD:
			return "field";
		case IJavaElement.IMPORT_CONTAINER:
			return "importcontainer";
		case IJavaElement.IMPORT_DECLARATION:
			return "importdeclaration";
		case IJavaElement.INITIALIZER:
			return "initializer";
		case IJavaElement.JAVA_MODEL:
			return "javamodel";
		case IJavaElement.JAVA_PROJECT:
			return "javaproject";
		case IJavaElement.LOCAL_VARIABLE:
			return "localvariable";
		case IJavaElement.METHOD:
			return "method";
		case IJavaElement.PACKAGE_DECLARATION:
			return "packagedeclaration";
		case IJavaElement.PACKAGE_FRAGMENT:
			return "packagefragment";
		case IJavaElement.TYPE:
			return "type";
		case IJavaElement.TYPE_PARAMETER:
			return "typeparameter";
		}
		return "null";
	}

	/**
	 * @param je
	 * @return
	 */
	private IType getContainingType(IJavaElement je) {
		IJavaElement parent = je;
		while (!(parent instanceof IType)) {
			parent = parent.getParent();
			if (parent instanceof ITypeRoot) {
				parent = ((ITypeRoot) parent).findPrimaryType();
			} if (parent == null) {
				return null;
			}
		}
		if (parent instanceof IType) {
			return (IType) parent;
		}
		return null;
	}

	/**
	 * @param je
	 * @return
	 */
	private IMethod getContainingMethod(IJavaElement je) {
		while (!(je instanceof IMethod)) {
			if (je instanceof IType) {
				return null;
			} else if (je == null) {
				return null;
			}
			je = je.getParent();
		}
		return (IMethod)je;
	}

	/**
	 * @param o
	 * @return
	 */
	private String translateMessage(IMessage m) {
		
		return null;
	}

	/**
	 * @param selection
	 * @return
	 */
	private Object getSelectionObject(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (!ss.isEmpty()) {
				Object o = ss.getFirstElement();
				if (o instanceof IJavaElement) {
					return o;
				} else if (o instanceof ITraceModel) {
					return o;
				}
			} 
		} else if (selection instanceof ITextSelection) {
			ITextSelection ts = (ITextSelection) selection;
			if (part instanceof ITextEditor) {
				ITextEditor editor = (ITextEditor) part;
				ITypeRoot typeRoot = null;
				IEditorInput input = editor.getEditorInput();
				//using internal stuff, but I don't care
				if (input instanceof IClassFileEditorInput) {
					typeRoot = ((IClassFileEditorInput) input).getClassFile();
				} else if (input instanceof IFileEditorInput){
					IFile file = ((IFileEditorInput)input).getFile();
					IJavaElement element = JavaCore.create(file);
					if (element instanceof ITypeRoot) {
						typeRoot = (ITypeRoot) element;
					}
				}
				if (typeRoot != null) {
					IType type = typeRoot.findPrimaryType();
					if (type != null) {
						try {
							IJavaElement je = typeRoot.getElementAt(ts.getOffset());
							return je;
						} catch (JavaModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		return null;
	}

}
