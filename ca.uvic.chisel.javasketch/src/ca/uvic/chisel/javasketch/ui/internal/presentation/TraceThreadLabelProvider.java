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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.zest.custom.sequence.widgets.Lifeline;
import org.eclipse.zest.custom.uml.viewers.IStylingSequenceLabelProvider;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IArrival;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.IReply;
import ca.uvic.chisel.javasketch.data.model.IThrow;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;
import ca.uvic.chisel.javasketch.ui.ISketchColorConstants;
import ca.uvic.chisel.javasketch.ui.internal.preferences.ISketchPluginPreferences;

/**
 * @author Del Myers
 *
 */
public class TraceThreadLabelProvider implements IStylingSequenceLabelProvider, IColorProvider {
	
	private WorkbenchLabelProvider workbenchLabelProvider;

	/**
	 * 
	 */
	public TraceThreadLabelProvider() {
		this.workbenchLabelProvider = new WorkbenchLabelProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		try {
			IJavaElement je = null;
			if (element instanceof ICall) {
				IMessage target = ((ICall)element).getTarget();
				if (target != null) {
					je = JavaSearchUtils.findElement(target.getActivation(), new NullProgressMonitor());
				}
			} else if (element instanceof IReply) {
				return null;
			} else if (element instanceof ITraceModel) {
				je = JavaSearchUtils.findElement((ITraceModel) element, new NullProgressMonitor());
			}
			if (je != null) {
				return workbenchLabelProvider.getImage(je);
			}
		} catch (InterruptedException e) {}
		catch (CoreException e) {}
		catch (Exception e) {}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		try {
			IJavaElement je = null;
			
			if (element instanceof ICall) {
				IMessage target = ((ICall)element).getTarget();
				if (target != null) {
					je = JavaSearchUtils.findElement(target.getActivation(), new NullProgressMonitor());
				}
			} else if (element instanceof IThrow) {
				return "Exception";
			} else if (element instanceof IReply) {
				String sig = ((IReply)element).getActivation().getMethod().getSignature();
				return Signature.getSimpleName(Signature.toString(Signature.getReturnType(sig)));				
			} else if (element instanceof ITraceModel) {
				je = JavaSearchUtils.findElement((ITraceModel) element, new NullProgressMonitor());
			}
			if (je != null) {
				if (je instanceof IType) {
					IType type = (IType) je;
					//qualify with all the parent type names
					String name = type.getElementName();

					if (type.isAnonymous()) {
						name = type.getTypeQualifiedName();
					} else if (type.getOccurrenceCount() > 1) {
						name = type.getOccurrenceCount() + name;
					}
					IJavaElement parent = type.getParent();
					while (parent != null) {
						if (parent instanceof IType) {
							IType pt = (IType) parent;
							int occurrence = pt.getOccurrenceCount();
							if (pt.isAnonymous()) {
								name = occurrence + "$" + name; 
							} else {
								name = ((occurrence > 1) ? occurrence : "") + pt.getElementName() + "$" + name;
							}
						}
						parent = parent.getParent();
					}
					return name;
				}
				return workbenchLabelProvider.getText(je);
			} else if (element instanceof ITraceModel) {
				return uresolvedModelElement((ITraceModel) element);
			}
		} catch (InterruptedException e) {}
		catch (CoreException e) {}
		catch (Exception e) {}
		return (element != null) ? element.toString() : "";
	}

	/**
	 * @param element
	 * @return
	 */
	private String uresolvedModelElement(ITraceModel element) {
		if (element instanceof ICall) {
			ICall call = (ICall) element;
			//try and get the method.
			if (call.getTarget() instanceof IArrival) {
				IActivation activation = call.getTarget().getActivation();
				if (activation != null) {
					ITraceClassMethod method = activation.getMethod();
					if (method != null) {
						return Signature.toString(method.getSignature(), method.getName(), null, false, false);
					}
				}
			}
		} else if (element instanceof ITraceClass) {
			ITraceClass clazz = (ITraceClass) element;
			String name = clazz.getName();
			int dot = name.lastIndexOf('.');
			if (dot > 0 && dot < name.length()-1) {
				name = name.substring(dot+1);
			}
			return name;
		} else if (element instanceof IReply) {
			IReply rep = (IReply) element;
			IActivation activation = rep.getActivation();
			if (activation != null) {
				ITraceClassMethod method = activation.getMethod();
				if (method != null) {
					return Signature.getSimpleName(Signature.getReturnType(method.getSignature()));
				}
			}
		}
		return "";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		workbenchLabelProvider.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.IStylingSequenceLabelProvider#getLifelineStyle(java.lang.Object)
	 */
	@Override
	public int getLifelineStyle(Object lifelineElement) {
		if (lifelineElement instanceof ITraceClass) {
			ITraceClass tc = (ITraceClass) lifelineElement;
			if (tc.getName().equals("USER")) {
				return Lifeline.ACTOR;
			}
		} else if (lifelineElement instanceof String) {
			return Lifeline.PACKAGE;
		}
		return Lifeline.CLASS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.IStylingSequenceLabelProvider#getMessageLineStyle(java.lang.Object)
	 */
	@Override
	public int getMessageLineStyle(Object messageElement) {
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.IStylingSequenceLabelProvider#getMessageSourceStyle(java.lang.Object)
	 */
	@Override
	public int getMessageSourceStyle(Object messageElement) {
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.IStylingSequenceLabelProvider#getMessageTargetStyle(java.lang.Object)
	 */
	@Override
	public int getMessageTargetStyle(Object messageElement) {
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceLabelProvider#getStereoType(java.lang.Object)
	 */
	@Override
	public String getStereoType(Object element) {
		if (element instanceof ITraceClass) {
			ITraceClass tc = (ITraceClass) element;
			if (tc.getName().equals("USER")) {
				return "Actor";
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	@Override
	public Color getBackground(Object element) {
		if (element instanceof IActivation) {
			try {
				IJavaElement je = JavaSearchUtils.findElement((IActivation)element, new NullProgressMonitor());
				if (je instanceof IMethod) {
					IMethod method = (IMethod) je;
					int flags = method.getFlags();
					if ((flags & Flags.AccPrivate) != 0) {
						return ISketchColorConstants.PRIVATE_BG;
					} else if ((flags & Flags.AccProtected) != 0) {
						return ISketchColorConstants.PROTECTED_BG;
					} else if ((flags & Flags.AccDefault) != 0) {
						return ISketchColorConstants.FRIEND_BG;
					} else if ((flags & Flags.AccPublic) != 0) {
						return ISketchColorConstants.PUBLIC_BG;
					}
				}
			} catch (JavaModelException e) {
			} catch (InterruptedException e) {
			} catch (CoreException e) {
			} catch (Exception e) {}
		} else if (element instanceof String) {
			return ISketchColorConstants.PACKAGE;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	@Override
	public Color getForeground(Object element) {
		IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
		boolean recon = store.getBoolean(ISketchPluginPreferences.DIAGRAM_RECONNAISSANCE);
		if (recon) {
			if (SketchPlugin.getDefault().getDOI().getInterest(element) < .3) {
				return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_GRAY);
			}
		} 
		if (element instanceof IActivation) {
			try {
				IJavaElement je = JavaSearchUtils.findElement((IActivation)element, new NullProgressMonitor());
				if (je instanceof IMethod) {
					IMethod method = (IMethod) je;
					int flags = method.getFlags();
					if ((flags & Flags.AccPrivate) != 0) {
						return ISketchColorConstants.PRIVATE_FG;
					} else if ((flags & Flags.AccProtected) != 0) {
						return ISketchColorConstants.PROTECTED_FG;
					} else if ((flags & Flags.AccDefault) != 0) {
						return ISketchColorConstants.FRIEND_FG;
					} else {
						return ISketchColorConstants.PUBLIC_FG;
					}
				}
			} catch (JavaModelException e) {
			} catch (InterruptedException e) {
			} catch (CoreException e) {
			} catch (Exception e) {}
		} else if (element instanceof IThrow) {
			return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED);
		} else if (element instanceof IMessage) {
			return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK);
			
		} 
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ItemLabelProvider#style(org.eclipse.zest.custom.uml.viewers.ItemLabelProvider.ItemLabel, java.lang.Object)
	 */
//	@Override
//	public void style(ItemLabel label, Object element) {
//		if (element instanceof ASTMessageGrouping) {
//			ASTMessageGrouping grouping = (ASTMessageGrouping) element;
//			IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
//			if (store.getBoolean(ISketchPluginPreferences.DIAGRAM_RECONNAISSANCE)) {
//				Object a = grouping.getActivationElement();
//				if (a instanceof IActivation) {
//					PresentationData data = PresentationData.connect(SketchPlugin.getDefault().getSketch((IActivation)a));
//					try {
//						if (data != null) {
//							if (data.isGroupEmpty((IActivation) a, grouping.getNode())) {
//								Display disp = PlatformUI.getWorkbench().getDisplay();
//								label.foreground = disp.getSystemColor(SWT.COLOR_GRAY);
//								label.background = disp.getSystemColor(SWT.COLOR_WHITE);
//							}
//						}
//					} finally {
//						if (data != null) {
//							data.disconnect();
//						}
//					}
//				}
//			}
//		}
//		
//	}

}
