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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Item;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.data.model.imple.internal.ThreadImpl;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;
import ca.uvic.chisel.javasketch.ui.ISketchColorConstants;
import ca.uvic.chisel.widgets.RangeAnnotation;
import ca.uvic.chisel.widgets.RangeSlider;

/**
 * @author Del Myers
 *
 */
public class MarkRangeForSelectionJob extends Job {

	private IJavaElement[] elements;
	private IJavaSketchPresenter editor;
	public static class InvocationReference implements IAdaptable {
		public final IJavaElement javaElement;
		public final IActivation activation;
		private boolean highlight;
		public InvocationReference(IJavaElement element, IActivation activation) {
			this(element, activation, false);
		}
		
		/**
		 * @param je
		 * @param activation2
		 * @param b
		 */
		public InvocationReference(IJavaElement element, IActivation activation,
				boolean highlight) {
			this.javaElement = element;
			this.activation = activation;
			this.highlight = highlight;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			if (!obj.getClass().equals(InvocationReference.class)) {
				return false;
			}
			InvocationReference that = (InvocationReference) obj;
			return this.activation.equals(that.activation) && this.javaElement.equals(that.javaElement);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return this.activation.hashCode() + this.javaElement.hashCode();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
		 */
		@SuppressWarnings("unchecked")
		@Override
		public Object getAdapter(Class adapter) {
			if (IJavaElement.class.equals(adapter)) {
				return javaElement;
			} else if (IActivation.class.equals(adapter) || ITraceModel.class.equals(adapter)) {
				return activation;
			}
			return null;
		}
	}

	/**
	 * @param name
	 */
	public MarkRangeForSelectionJob(IJavaSketchPresenter editor) {
		super("Marking Range For Current Selection");
		this.editor = editor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		if (editor.getTimeRange().isDisposed()) {
			return Status.OK_STATUS;
		}
		monitor.beginTask("Refreshing Invocation Ranges", IProgressMonitor.UNKNOWN);
		if (elements == null) {
			return Status.OK_STATUS;
		}
		final HashSet<InvocationReference> invocations = new HashSet<InvocationReference>();
		IThread thread = (ThreadImpl) editor.getThread();
		try {		
			for (IJavaElement element : elements) {
				if (monitor.isCanceled()) {
					break;
				}
				IProgressMonitor subMonitor = new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN);
				List<IActivation> activations = new ArrayList<IActivation>();
				if (element instanceof IType) {
					activations = JavaSearchUtils.findActivationsForClass(thread, (IType)element, subMonitor);
				} else if (element instanceof IMethod) {
					activations = JavaSearchUtils.findActivationsForMethod(thread, (IMethod)element, subMonitor);
				}
				for (IActivation activation : activations) {
					invocations.add(new InvocationReference(element, activation));
				}
			}
			editor.getTimeRange().getDisplay().syncExec(new Runnable(){
				@Override
				public void run() {
					updateInUI(monitor, invocations);
				}
				
			});
		} catch (CoreException e) {
			SketchPlugin.getDefault().log(e);
			return e.getStatus();
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
		

	/**
	 * Set the java elements for this job, and reschedules it. This method should
	 * be used rather than schedule().
	 * @param elements the elements to mark
	 */
	public void schedule(IJavaElement[] elements) {
		cancel();
		this.elements = elements;
		schedule();
	}
	
	
	private void updateInUI(IProgressMonitor monitor, Collection<InvocationReference> invocations) {
		RangeSlider slider = editor.getTimeRange();
		if (slider.isDisposed()) {
			return;
		}
		//first, dispose all of the previous markers
		Item item = slider.getItem(slider.getSelectionIndex());
		Object selectedData = null;
		HashMap<InvocationReference, RangeAnnotation> preservedRanges = new HashMap<InvocationReference, RangeAnnotation>();
		for (RangeAnnotation range : slider.getRanges()) {
			if (item != null && !item.isDisposed() && item == range) {
				selectedData = item.getData();
			}
			if (range.getData() instanceof InvocationReference) {
				preservedRanges.put((InvocationReference) range.getData(), range);
			}
		}
		
		IStructuredSelection viewSelection = (IStructuredSelection) editor.getSequenceChartViewer().getSelection();
		Iterator<?> i = viewSelection.iterator();
		while (i.hasNext()) {
			if (monitor.isCanceled()) return;
			Object next = i.next();
			IActivation activation = null;
			if (next instanceof IActivation) {
				activation = (IActivation) next;
			} else if (next instanceof IOriginMessage) {
				try {
					IOriginMessage message = (IOriginMessage) next;
					activation = message.getTarget().getActivation();
				} catch (NullPointerException e) {}
			}
			if (activation != null) {
				try {
					IJavaElement je = JavaSearchUtils.findElement(activation, new NullProgressMonitor());
					if (je instanceof IMethod) {
						InvocationReference ref = new InvocationReference(je, activation, true);
						if (invocations.contains(ref)) {
							invocations.remove(ref);
						}
						invocations.add(ref);
					}
				} catch (InterruptedException e) {
				} catch (CoreException e) {
				}
			}
		}
		
		//now, add all the new invocations
		RangeAnnotation newSelection = null;
		for (InvocationReference ref : invocations) {
			if (monitor.isCanceled()) return;
			if (preservedRanges.containsKey(ref)) {
				if (ref.equals(selectedData)) {
					newSelection = preservedRanges.get(ref);
				}
				preservedRanges.remove(ref);
				continue;
			}
			RangeAnnotation annotation = new RangeAnnotation(slider);
			annotation.setOffset(ref.activation.getTime());
			annotation.setLength(1);
			annotation.setText("Activation of " + ref.javaElement.getElementName() + " at " + ref.activation.getTime());
			if (!ref.highlight) {
				annotation.setForeground(ISketchColorConstants.BLUE);
				annotation.setBackground(ISketchColorConstants.LIGHT_BLUE);
			} else {
				annotation.setForeground(ISketchColorConstants.PURPLE);
				annotation.setForeground(ISketchColorConstants.LIGHT_PURPLE);
			}
			annotation.setData(ref);
			if (ref.equals(selectedData)) {
				newSelection = annotation;
			}
		}
		for (RangeAnnotation annotation : preservedRanges.values()) {
			if (!annotation.isDisposed()) {
				annotation.dispose();
			}
		}
		if (newSelection != null && !newSelection.isDisposed()) {
			slider.setSelectionIndex(slider.getIndex(newSelection));
		}
	}
	
}
