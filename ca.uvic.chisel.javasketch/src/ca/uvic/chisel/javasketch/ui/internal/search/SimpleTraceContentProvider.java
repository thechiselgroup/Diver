/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.ui.internal.search;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeSet;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.ui.internal.views.ParentedCalendar;
import ca.uvic.chisel.javasketch.ui.internal.views.TreeNode;
import ca.uvic.chisel.javasketch.utils.LaunchConfigurationUtilities;

/**
 * @author Del Myers
 * 
 */
public class SimpleTraceContentProvider implements ITreeContentProvider {
	TreeNode tree;
	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.
	 * Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (tree == null) {
			return new Object[0];
		}
		TreeNode parent = tree.findNode(parentElement);
		if (parent != null) {
			if (!parent.isLoaded()) {
				// load the children
				if (parentElement instanceof IWorkspaceRoot) {
					IProgramSketch[] storedSketches = SketchPlugin.getDefault()
						.getStoredSketches();
					TreeSet<ILaunchConfiguration> configs = new TreeSet<ILaunchConfiguration>(
						new Comparator<ILaunchConfiguration>() {
							@Override
							public int compare(ILaunchConfiguration o1,
									ILaunchConfiguration o2) {
								return o1.getName().compareTo(o2.getName());
							}
						});
					for (IProgramSketch sketch : storedSketches) {
						ILaunchConfiguration slc = sketch
							.getTracedLaunchConfiguration();
						// get the configuration in the workspace that matches
						// the name first
						ILaunchConfiguration lc = LaunchConfigurationUtilities
							.getLaunchConfiguration(slc.getName());
						if (lc == null) {
							lc = slc;
						}
						configs.add(lc);
					}
					for (Object o : configs) {
						parent.addChild(o);
					}
				} else if (parentElement instanceof ILaunchConfiguration) {
					IProgramSketch[] storedSketches = SketchPlugin.getDefault()
						.getStoredSketches(
							((ILaunchConfiguration) parentElement).getName());
					// check the dates for each of them and return calendars.
					TreeSet<Calendar> days = new TreeSet<Calendar>();
					for (IProgramSketch sketch : storedSketches) {
						Calendar calendar = Calendar.getInstance();
						Date time = sketch.getProcessTime();
						calendar.setTime(time);
						int day = calendar.get(Calendar.DAY_OF_YEAR);
						int year = calendar.get(Calendar.YEAR);
						Calendar dayCalendar = Calendar.getInstance();
						dayCalendar.setTimeInMillis(0);
						dayCalendar.set(Calendar.YEAR, year);
						dayCalendar.set(Calendar.DAY_OF_YEAR, day);
						days.add(dayCalendar);
					}
					for (Calendar day : days) {
						parent
							.addChild(new ParentedCalendar(parentElement, day));
					}
				} else if (parentElement instanceof ParentedCalendar) {
					// do the above in reverse
					ILaunchConfiguration lc = (ILaunchConfiguration) parent
						.getParent().data;
					Calendar day = ((ParentedCalendar) parent.data)
						.getCalendar();

					IProgramSketch[] storedSketches = SketchPlugin.getDefault()
						.getStoredSketches(lc.getName());
					ArrayList<IProgramSketch> sketches = new ArrayList<IProgramSketch>();

					for (IProgramSketch sketch : storedSketches) {
						Date time = sketch.getProcessTime();
						Calendar sketchDay = Calendar.getInstance();
						sketchDay.setTime(time);
						if (sketchDay.get(Calendar.YEAR) == day
							.get(Calendar.YEAR)) {
							if (sketchDay.get(Calendar.DAY_OF_YEAR) == day
								.get(Calendar.DAY_OF_YEAR)) {
								sketches.add(sketch);
							}
						}
					}
					// make sure that they always come in the same order.
					Collections.sort(sketches,
						new Comparator<IProgramSketch>() {
							@Override
							public int compare(IProgramSketch o1,
									IProgramSketch o2) {
								return o1.getID().compareTo(o2.getID());
							}
						});
					for (IProgramSketch sketch : sketches) {
						parent.addChild(sketch);
					}
				} else if (parentElement instanceof IProgramSketch) {
					// try to return the threads
					IProgramSketch sketch = (IProgramSketch) parentElement;
					ITrace trace = sketch.getTraceData();
					if (trace != null) {
						for (IThread thread : trace.getThreads()) {
							parent.addChild(thread);
						}
					}

				}
			}
			return parent.getChildElements();
		}
		return new Object[0];
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object
	 * )
	 */
	@Override
	public Object getParent(Object element) {
		if (tree == null) {
			return null;
		}
		TreeNode node = tree.findNode(element);
		if (node != null && node.getParent() != null) {
			return node.getParent().data;
		}
		return node;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.
	 * Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		return (element instanceof IWorkspaceRoot)
				|| (element instanceof ILaunchConfiguration)
				|| (element instanceof ParentedCalendar);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java
	 * .lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		tree = null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface
	 * .viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null) {
			tree = null;
		} else {
			tree = new TreeNode(null, newInput);

		}
	}

}
