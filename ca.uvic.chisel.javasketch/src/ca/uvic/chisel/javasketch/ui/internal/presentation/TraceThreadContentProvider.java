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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.zest.custom.uml.viewers.ISequenceChartContentProvider;
import org.eclipse.zest.custom.uml.viewers.ISequenceContentExtension;
import org.eclipse.zest.custom.uml.viewers.ISequenceContentExtension2;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.internal.ast.groups.ASTMessageGroupingTree;
import ca.uvic.chisel.javasketch.ui.internal.preferences.ISketchPluginPreferences;
import ca.uvic.chisel.javasketch.ui.internal.presentation.metadata.PresentationData;

/**
 * @author Del Myers
 *
 */
public class TraceThreadContentProvider implements
		ISequenceChartContentProvider, ISequenceContentExtension, ISequenceContentExtension2 {
	private PresentationData presentation;
	private JavaThreadSequenceView view = null;

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceChartContentProvider#getLifeline(java.lang.Object)
	 */
	@Override
	public Object getLifeline(Object activation) {
		if (activation instanceof IActivation) {
			return ((IActivation)activation).getThisClass();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceChartContentProvider#getMessages(java.lang.Object)
	 */
	@Override
	public Object[] getMessages(Object activation) {
		if (activation instanceof IActivation) {
			List<IOriginMessage> messages = ((IActivation)activation).getOriginMessages();
			List<IOriginMessage> filtered = new LinkedList<IOriginMessage>();
			boolean compactLoops =
				SketchPlugin.getDefault().getPreferenceStore().getBoolean(ISketchPluginPreferences.COMPACT_LOOPS_PREFERENCE);
			boolean reconHide =
				SketchPlugin.getDefault().getPreferenceStore().getBoolean(ISketchPluginPreferences.DIAGRAM_RECON_HIDE);
			boolean reconEnabled =
				SketchPlugin.getDefault().getPreferenceStore().getBoolean(ISketchPluginPreferences.DIAGRAM_RECONNAISSANCE);
			
			//try filtering the messages based on their loop index
			if (presentation != null) {
				ASTMessageGroupingTree tree = presentation.getGroups((IActivation) activation);
				
				if (tree != null) {
					if (compactLoops && reconHide && reconEnabled) {
						LinkedList<ASTMessageGroupingTree> organizer = new LinkedList<ASTMessageGroupingTree>();
						HashSet<String> swaped = new HashSet<String>();
						organizer.addAll(tree.getChildren());
						while (!organizer.isEmpty()) {
							ASTMessageGroupingTree node = organizer.removeFirst();
							if (presentation.isGroupVisible((IActivation) activation, node)) {
								if (node.isLoop()) {
									String id = node.getNodeID();
									if (!swaped.contains(id)) {
										swaped.add(id);
										presentation.swapLoop((IActivation) activation, node, true);
										organizer.addAll(node.getChildren());
									}

								} else {
									organizer.addAll(node.getChildren());
								}
							}
						}
					}
					for (IOriginMessage message : messages) {
//						ITraceClassMethod method = message.getTarget().getActivation().getMethod();
						boolean selected = true;
//						try {
//							IJavaElement jElement = JavaSearchUtils.findElement(method, new NullProgressMonitor());
//							if (jElement instanceof IMethod) {
//								IMethod jMethod = (IMethod) jElement;
//								if (Flags.isPrivate(jMethod.getFlags())) {
//									//make sure that it is in the same package
//									ITraceClass caller = ((IActivation)activation).getMethod().getTraceClass();
//									ITraceClass callee = method.getTraceClass();
//									int callerIndex = caller.getName().lastIndexOf('.');
//									int calleeIndex = callee.getName().lastIndexOf('.');
//									String calleePackage = callee.getName().substring(0, calleeIndex);
//									String callerPackage = caller.getName().substring(0, callerIndex);
//									if (callerIndex > 0 && calleeIndex > 0) {
//										selected = callerPackage.equals(calleePackage);
//									}
//								}
//							}
//						} catch (Exception e) {
//						}
						ASTMessageGroupingTree localContainer = tree.getMessageContainer(message);
						if (compactLoops) {
							selected &= (localContainer != null);
							while (localContainer != null & selected) {
								if (!presentation.isGroupVisible((IActivation)activation, localContainer)) {
									selected = false;
								}
								localContainer = localContainer.getParent();
							}
						}
						if (selected) {
							filtered.add(message);
						}
					}
					return filtered.toArray();
				}
			}

			return messages.toArray();
		}
		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceChartContentProvider#getTarget(java.lang.Object)
	 */
	@Override
	public Object getTarget(Object message) {
		if (message instanceof IOriginMessage) {
			IOriginMessage m = (IOriginMessage) message;
			if (m.getTarget() != null) {
				return m.getTarget().getActivation();
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceChartContentProvider#isCall(java.lang.Object)
	 */
	@Override
	public boolean isCall(Object message) {
		return (message instanceof ICall);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IThread) {
			return new Object[] {((IThread)inputElement).getRoot().getActivation()};
		}
		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		if (presentation != null) {
			presentation.disconnect();
			presentation = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof ITraceModel) {
			if (presentation != null) {
				presentation.disconnect();
				presentation = null;
			}
			IProgramSketch sketch = SketchPlugin.getDefault().getSketch((ITraceModel)newInput);
			if (sketch != null) {
				presentation = PresentationData.connect(sketch);
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceContentExtension#getContainingGroup(java.lang.Object)
	 */
	@Override
	public Object getContainingGroup(Object lifelineOrGroup) {
		String name = null;
		if (lifelineOrGroup instanceof ITraceClass) {
			ITraceClass tc = (ITraceClass) lifelineOrGroup;
			String className = tc.getName();
			name = className;
		} else if (lifelineOrGroup instanceof String) {
			name = (String) lifelineOrGroup;
		}
		int dot = name.lastIndexOf('.');
		if (dot > 0 && name.length() > 1) {
			return name.substring(0, dot);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceContentExtension#hasContainingGroup(java.lang.Object)
	 */
	@Override
	public boolean hasContainingGroup(Object lifelineOrGroup) {
		if (lifelineOrGroup instanceof ITraceClass) {
			ITraceClass tc = (ITraceClass) lifelineOrGroup;
			String className = tc.getName();
			return (className.lastIndexOf('.') > 0);
		} else if (lifelineOrGroup instanceof String) {
			String packageName = (String) lifelineOrGroup;
			return packageName.lastIndexOf('.') > 0;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceContentExtension2#getCall(java.lang.Object)
	 */
	@Override
	public Object getCall(Object activation) {
		try {
			if (activation instanceof IActivation) {
				return ((IActivation)activation).getArrival().getOrigin();
			}
		} catch (NullPointerException e) {}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.uml.viewers.ISequenceContentExtension2#getOriginActivation(java.lang.Object)
	 */
	@Override
	public Object getOriginActivation(Object message) {
		if (message instanceof IOriginMessage) {
			return ((IOriginMessage)message).getActivation();
		}
		return null;
	}

}
