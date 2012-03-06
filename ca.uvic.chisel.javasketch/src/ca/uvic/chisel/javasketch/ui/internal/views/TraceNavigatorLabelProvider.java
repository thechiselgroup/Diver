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
package ca.uvic.chisel.javasketch.ui.internal.views;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import ca.uvic.chisel.javasketch.IDegreeOfInterest;
import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.ISketchInterestListener;
import ca.uvic.chisel.javasketch.SketchInterestEvent;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;
import ca.uvic.chisel.javasketch.ui.ISketchImageConstants;
import ca.uvic.chisel.javasketch.ui.internal.OverlayIcon;

public class TraceNavigatorLabelProvider extends CellLabelProvider {
	
	ListenerList labelListeners;
	private SketchInterestListener activationListener;
	
	private class UpdateJob extends UIJob {
		
		private Object[] elements;
		public UpdateJob(Object[] elements) {
			super("Updating Labels");
			this.elements = elements;
		}
		/* (non-Javadoc)
		 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			for (Object o : labelListeners.getListeners()) {
				ILabelProviderListener listener = (ILabelProviderListener) o;
				LabelProviderChangedEvent event = new LabelProviderChangedEvent(TraceNavigatorLabelProvider.this, elements);
				listener.labelProviderChanged(event);
			}
			return Status.OK_STATUS;
		}
		
	}
	
	private class SketchInterestListener implements ISketchInterestListener {


		/* (non-Javadoc)
		 * @see ca.uvic.chisel.javasketch.ISketchInterestListener#sketchInterestChanged(ca.uvic.chisel.javasketch.SketchInterestEvent)
		 */
		@Override
		public void sketchInterestChanged(SketchInterestEvent event) {
			ILaunchConfiguration lc = event.getSketch().getTracedLaunchConfiguration();
			ArrayList<Object> elements = new ArrayList<Object>();
			for (IProgramSketch sketch : SketchPlugin.getDefault().getStoredSketches(lc.getName())) {
				elements.add(sketch);
				if (sketch.isConnected()) {
					ITrace trace = sketch.getTraceData();
					if (trace != null) {
						elements.addAll(trace.getThreads());
					}
				}
			}
			new UpdateJob(elements.toArray()).schedule();
		}
		
	}
	/**
	 * 
	 */
	public TraceNavigatorLabelProvider() {
		labelListeners = new ListenerList();
		activationListener = new SketchInterestListener();
		SketchPlugin.getDefault().getDOI().addSketchInterestListener(activationListener);
	}
	
	public Image getImage(Object element) {
			
			if (element instanceof IProgramSketch) {
				return buildSketchImage((IProgramSketch)element);
			} else if (element instanceof IThread) {
				return SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_THREAD_TRACE);
			} else if (element instanceof ILaunchConfiguration) {
				return SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_TRACE);
			} else if (element instanceof ParentedCalendar) {
				return SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_CALENDAR);
			} else if (element instanceof ITraceModel) {
				try {
					IJavaElement je = JavaSearchUtils.findElement((ITraceModel) element, new NullProgressMonitor());
					if (je != null) {
						return (Image) je.getAdapter(Image.class);
					}
				} catch (InterruptedException e) {
				} catch (CoreException e) {
				}
			}
		
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param element
	 * @return
	 */
	private Image buildSketchImage(IProgramSketch element) {
		String key = ISketchImageConstants.ICON_PROCESS_TRACE;
		ImageDescriptor baseImage = SketchPlugin.getDefault().getImageRegistry().getDescriptor(key);
		if (element.isAnalysing()) {
			key = key + "." + ISketchImageConstants.OVERLAY_ANALYSE;
			baseImage = overlayAnalyze(baseImage, key);
		}
		if (element.isRunning()) {
			key = key + "." + ISketchImageConstants.OVERLAY_PLAY;
			baseImage = overlayPlay(baseImage, key);
		} else {
			key = key + "." + ISketchImageConstants.OVERLAY_STOP;
			baseImage = overlayStop(baseImage, key);
		}
		return SketchPlugin.getDefault().getImageRegistry().get(key);
	}

	/**
	 * @param baseImage
	 * @param key
	 * @return
	 */
	private ImageDescriptor overlayStop(ImageDescriptor baseImage, String key) {
		ImageDescriptor image = SketchPlugin.getDefault().getImageRegistry().getDescriptor(key);
		if (image == null) {
			ImageDescriptor overlay = SketchPlugin.getDefault().getImageRegistry().getDescriptor(ISketchImageConstants.OVERLAY_STOP);
			image = new OverlayIcon(baseImage, overlay, OverlayIcon.LEFT, OverlayIcon.BOTTOM);
			SketchPlugin.getDefault().getImageRegistry().put(key, image);
		}
		return image;
	}

	/**
	 * @param baseImage
	 * @param key
	 * @return
	 */
	private ImageDescriptor overlayPlay(ImageDescriptor baseImage, String key) {
		ImageDescriptor image = SketchPlugin.getDefault().getImageRegistry().getDescriptor(key);
		if (image == null) {
			ImageDescriptor overlay = SketchPlugin.getDefault().getImageRegistry().getDescriptor(ISketchImageConstants.OVERLAY_PLAY);
			image = new OverlayIcon(baseImage, overlay, OverlayIcon.LEFT, OverlayIcon.BOTTOM);
			SketchPlugin.getDefault().getImageRegistry().put(key, image);
		}
		return image;
	}

	/**
	 * @param baseImage
	 * @param key
	 * @return
	 */
	private ImageDescriptor overlayAnalyze(ImageDescriptor baseImage, String key) {
		ImageDescriptor image = SketchPlugin.getDefault().getImageRegistry().getDescriptor(key);
		if (image == null) {
			ImageDescriptor overlay = SketchPlugin.getDefault().getImageRegistry().getDescriptor(ISketchImageConstants.OVERLAY_ANALYSE);
			image = new OverlayIcon(baseImage, overlay, OverlayIcon.RIGHT, OverlayIcon.TOP);
			SketchPlugin.getDefault().getImageRegistry().put(key, image);
		}
		return image;
	}

	public String getText(Object element) {

		if (element instanceof IThread) {
			IThread thread = (IThread) element;
			return thread.getName();
		} else if (element instanceof IProgramSketch) {
			IProgramSketch sketch = (IProgramSketch) element;
			DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
			return sketch.getLabel() + " " + timeFormat.format(sketch.getProcessTime());
		} else if (element instanceof IProject) {
			return ((IProject)element).getName();
		} else if (element instanceof ParentedCalendar) {
			Calendar day = ((ParentedCalendar) element).getCalendar();
			DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
			return format.format(day.getTime());
		} else if (element instanceof ITraceModel) {
			try {
				IJavaElement je = JavaSearchUtils.findElement((ITraceModel) element, new NullProgressMonitor());
				if (je != null) {
					return je.getElementName();
				}
			} catch (InterruptedException e) {
			} catch (CoreException e) {
			}
		}
		return element.toString();
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		labelListeners.add(listener);
	}

	@Override
	public void dispose() {
		labelListeners.clear();
		SketchPlugin.getDefault().getDOI().removeSketchInterestListener(activationListener);
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		labelListeners.remove(listener);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.CellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
	 */
	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		IProgramSketch sketch = null;
		Image image = null;
		if (element instanceof IProgramSketch) {
			sketch = (IProgramSketch) element;
		} else if (element instanceof IThread) {
			IThread thread = (IThread) element;
			sketch = SketchPlugin.getDefault().getSketch(thread);
		}
		switch (cell.getColumnIndex()) {
		case TraceNavigator.ACTIVE_TRACE_COLUMN:
			if (sketch != null) {
				if (sketch.equals(SketchPlugin.getDefault().getActiveSketch())) {
					cell.setImage(SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_TRACE_ACTIVE));
				} else {
					cell.setImage(SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_TRACE_INACTIVE));
				}
			}
			break;
		case TraceNavigator.LABEL_COLUMN:
			cell.setText(getText(element));
			cell.setImage(getImage(element));
			break;
		case TraceNavigator.VISIBLE_TRACE_COLUMN:
			if (sketch != null) {
				IDegreeOfInterest doi = SketchPlugin.getDefault().getDOI();
				IProgramSketch activeSketch = SketchPlugin.getDefault().getActiveSketch();
				if (activeSketch != null) {
					if (activeSketch.getTracedLaunchConfiguration().equals(sketch.getTracedLaunchConfiguration())) {
						TraceNavigator navigator = 
							(TraceNavigator) PlatformUI.
								getWorkbench().
								getActiveWorkbenchWindow().
								getActivePage().
								findView(TraceNavigator.VIEW_ID);
						if (navigator != null) {
							if (doi.isSketchHidden(sketch)) {
								image = SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_ELEMENT_FILTERED);
							} else {
								image = SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_ELEMENT_VISIBLE);
							}
						}
					}
				}
			}
			cell.setImage(image);
			break;
		}
	}

}
