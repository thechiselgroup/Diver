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
package ca.uvic.chisel.javasketch.internal.interest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.core.IJavaElement;

import ca.uvic.chisel.javasketch.IDegreeOfInterest;
import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.ISketchInterestListener;
import ca.uvic.chisel.javasketch.SketchInterestEvent;
import ca.uvic.chisel.javasketch.SketchInterestEvent.SketchInterestEventType;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.javasketch.data.model.IReply;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;

/**
 * @author Del
 *
 */
public class DegreeOfInterest implements IDegreeOfInterest {
	protected HashSet<IProgramSketch> filteredSketches = new HashSet<IProgramSketch>();
	private ReconnaissanceFiltering rFilter;
	private ListenerList listeners;
	private IThread threadSelection;
	
	
	/**
	 * 
	 */
	public DegreeOfInterest() {
		listeners = new ListenerList();
		rFilter = new ReconnaissanceFiltering();
	}
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.IDegreeOfInterest#getInterest(java.lang.Object)
	 */
	@Override
	public double getInterest(Object o) {
		if (o instanceof ICall) {
			ICall call = (ICall) o;
			if (rFilter.checkCall(call)) {
				return 1;
			} else {
				ITraceClassMethod method = call.getTarget().getActivation().getMethod();
				//System.out.println("rejected " + method);
				return 0;
			}
		} else if (o instanceof IReply) {
			try {
				ICall call = (ICall)((IReply)o).getActivation().getArrival().getOrigin();
				if (rFilter.checkCall(call)) {
					return 1;
				} else {
					return 0;
				}
			} catch (NullPointerException e) {
				return 1;
			}
		} else if (o instanceof IActivation) {
			try {
				ICall call = (ICall) ((IActivation)o).getArrival().getOrigin();
				if (rFilter.checkCall(call)) {
					return 1;
				} else {
					return 0;
				}
			} catch (NullPointerException e) {
				return 1;
			}
		} else if (o instanceof IJavaElement) {
			return getInterest((IJavaElement)o);
		}
		return 1;
	}
	
	private double getInterest(IJavaElement javaElement) {
		if (getActiveSketch() == null) {
			return 1;
		}

		//if it is still null, then there was an error. Everything is 1
	
//		try {
//			switch (javaElement.getElementType()) {
//			case IJavaElement.JAVA_PROJECT:
//				return getInterest((IJavaProject)javaElement);
//			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
//				return getInterest((IPackageFragmentRoot)javaElement);
//			case IJavaElement.PACKAGE_FRAGMENT:
//				return getInterest((IPackageFragment)javaElement);
//			case IJavaElement.COMPILATION_UNIT:
//			case IJavaElement.CLASS_FILE:
//				return getInterest((ITypeRoot)javaElement);
//			case IJavaElement.TYPE:
//				return getInterest((IType)javaElement);
//			case IJavaElement.METHOD:
//				return getInterest((IMethod)javaElement);
//				default:
//					return 0;
//			}
//		} catch (JavaModelException e) {
//			SketchPlugin.getDefault().log(e);
//		}
//		return 1;
		if (rFilter.isValid(javaElement)) {
			if (threadSelection == null) {
				return 1;
			}
			IProgramSketch active = getActiveSketch();
			if (!active.equals(SketchPlugin.getDefault().getSketch(threadSelection))) {
				return 1;
			}

			IThread[] threads = JavaSearchUtils.findThreadsForElement(active.getTraceData(), javaElement);
			for (IThread t : threads) {
				if (t.equals(threadSelection)) {
					return 1;
				}
			}

			List<IJavaElement> childElements = rFilter.getAllValidChildren(javaElement);
			for (IJavaElement element : childElements) {
				threads = JavaSearchUtils.findThreadsForElement(active.getTraceData(), element);
				for (IThread t : threads) {
					if (t.equals(threadSelection)) {
						return .5;
					}
				}
			}
		}
		return 0;
	}
	
//	private double getInterest(IJavaProject javaProject) throws JavaModelException {
//		for (IPackageFragmentRoot child : javaProject.getPackageFragmentRoots()) {
//			double interest = getInterest(child);
//			if (interest > 0) {
//				return interest;
//			}
//		}
//			
//		return 0;
//	}
//	
//	private double getInterest(IPackageFragmentRoot root) throws JavaModelException {
//		for (IJavaElement fragment : root.getChildren()) {
//			if (fragment instanceof IPackageFragment) {
//				double interest = getInterest((IPackageFragment)fragment);
//				if (interest > 0) {
//					return interest;
//				}
//			}
//		}
//		return 0;
//	}
//	
//	private double getInterest(IPackageFragment fragment) {
//		if (activeSketch == null) {
//			return 1;
//		}
//		String name = fragment.getElementName();
//		try {
//			PreparedStatement statement = activeSketch.getPortal().prepareStatement("Select * from ValidMethod where type_name like ?");
//			statement.setString(1, name + ".%");
//			ResultSet results = statement.executeQuery();
//			return (results.next()) ? 1 : 0;
//		} catch (Exception e) {
//			SketchPlugin.getDefault().log(e);
//		} 
//		return 1;
//	}
//	
//	private double getInterest(ITypeRoot typeRoot) {
//		return getInterest(typeRoot.findPrimaryType());
//	}
//	
//	private double getInterest(IType type) {
//		if (type == null) {
//			return 0;
//		}
//		String name = type.getFullyQualifiedName();
//		try {
//			PreparedStatement statement = activeSketch.getPortal().prepareStatement("Select * from ValidMethod where type_name like ?");
//			statement.setString(1, name + "%");
//			ResultSet results = statement.executeQuery();
//			while (results.next()) {
//				String typeName = results.getString("type_name");
//				if (typeName.length() == name.length()) {
//					return 1;
//				} else {
//					if (typeName.charAt(name.length()) == '$') {
//						return 1;
//					}
//				}
//			}
//			return 0;
//		} catch (Exception e) {
//			SketchPlugin.getDefault().log(e);
//		}
//		return 1;
//	}
//	
//	private double getInterest(IMethod method) {
//		String tName = method.getDeclaringType().getFullyQualifiedName();
//		String mName = method.getElementName();
//		try {
//			PreparedStatement statement = activeSketch.getPortal().prepareStatement("Select * from ValidMethod where type_name=? AND method_name=?");
//			statement.setString(1, tName);
//			statement.setString(2, mName);
//			ResultSet results = statement.executeQuery();
//			while (results.next()) {
//				String signature = results.getString("method_signature");
//				IMethod proxy = method.getDeclaringType().getMethod(mName, Signature.getParameterTypes(signature));
//				if (method.isSimilar(proxy)) {
//					return 1;
//				}
//			}
//			return 0;
//		} catch (Exception e) {
//			SketchPlugin.getDefault().log(e);
//		}
//		return 1;
//	}
//
//	/**
//	 * 
//	 */
//	
	/**
	 * @param sketch
	 */
	public void setActiveSketch(IProgramSketch sketch, IProgressMonitor progress) {
		IProgramSketch activeSketch = getActiveSketch();
		if (sketch == null && activeSketch == null) {
			return;
		} else if (sketch != null && sketch.equals(activeSketch)) {
			return;
		} else if (activeSketch != null && activeSketch.equals(sketch)) {
			return;
		}
		
		if (activeSketch != null) {
			fireInterestEvent(new SketchInterestEvent(activeSketch, SketchInterestEventType.SketchDeactivated));
		}
		rFilter.setActiveSketch(sketch, progress);
		
		if (sketch != null) {
			fireInterestEvent(new SketchInterestEvent(sketch, SketchInterestEventType.SketchActivated));
		}
		
	}

	/**
	 * @return
	 */
	public IProgramSketch getActiveSketch() {
		return rFilter.getActiveSketch();
	}
	
	public void addSketchInterestListener(ISketchInterestListener listener) {
		listeners.add(listener);
	}
	
	public void removeSketchInterestListener(ISketchInterestListener listener) {
		listeners.remove(listener);
	}
	
	private void fireInterestEvent(SketchInterestEvent event) {
		Object[] array = listeners.getListeners();
		for (Object l : array) {
			((ISketchInterestListener)l).sketchInterestChanged(event);
		}
	}
	
	public void setSketchHidden(IProgramSketch sketch, boolean hidden, IProgressMonitor progress) {
		if (sketch == null)
			return;
		if (sketch.equals(getActiveSketch())) {
			return;
		}
		if (getActiveSketch() == null) {
			return;
		}
		SketchInterestEvent event = null;
		synchronized(filteredSketches) {
			if (hidden) {
				if (!isSketchHidden(sketch)) {
					filteredSketches.add(sketch);
					rFilter.setHidden(sketch, hidden, progress);
					event = new SketchInterestEvent(sketch, SketchInterestEventType.SketchHidden);
				} 
			} else {
				if (isSketchHidden(sketch)) {
					filteredSketches.remove(sketch);
					rFilter.setHidden(sketch, hidden, progress);
					event = new SketchInterestEvent(sketch, SketchInterestEventType.SketchShown);
				}
			}
		}
		if (event != null) {
			fireInterestEvent(event);
		}
	}

	
	public boolean isSketchHidden(IProgramSketch sketch) {
		if (sketch == null)
			return false;
		if (sketch.equals(getActiveSketch())) {
			return false;
		}
		synchronized (filteredSketches) {
			return filteredSketches.contains(sketch);
		}
	}
	
	public IProgramSketch[] getHiddenSketches() {
		ArrayList<IProgramSketch> filteredArray = new ArrayList<IProgramSketch>();
		if (getActiveSketch() == null) {
			return new IProgramSketch[0];
		}
		synchronized (filteredSketches) {
			String lcName = getActiveSketch().getTracedLaunchConfiguration().getName();
			for (IProgramSketch f : filteredSketches) {
				if (!f.equals(getActiveSketch())) {
					String fName = f.getTracedLaunchConfiguration().getName();
					if (fName.equals(lcName)) {
						filteredArray.add(f);
					}
				}
			}
		}
		return filteredArray.toArray(new IProgramSketch[filteredArray.size()]);
	}
	
	
	
	
	@Override
	public boolean requestFiltering(IThread thread, IProgressMonitor monitor) {
		return rFilter.filter(thread, monitor);
	}
	
	public void setThreadSelection(IThread thread) {
		this.threadSelection = thread;
	}
	
	/**
	 * @return the rFilter
	 */
	public ReconnaissanceFiltering getReconnaissanceFilter() {
		return rFilter;
	}
}
