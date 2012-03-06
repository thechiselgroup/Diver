/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.ui.internal.presentation;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.internal.operations.TimeTriggeredProgressMonitorDialog;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.zest.custom.sequence.widgets.Activation;
import org.eclipse.zest.custom.sequence.widgets.Call;
import org.eclipse.zest.custom.sequence.widgets.Message;
import org.eclipse.zest.custom.sequence.widgets.MessageGroup;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;
import org.eclipse.zest.custom.sequence.widgets.UMLSequenceChart;
import org.eclipse.zest.custom.uml.viewers.BreadCrumbViewer;
import org.eclipse.zest.custom.uml.viewers.IMessageGrouping;
import org.eclipse.zest.custom.uml.viewers.ISequenceContentExtension2;
import org.eclipse.zest.custom.uml.viewers.ISequenceViewerListener;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerEvent;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerGroupEvent;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerRootEvent;
import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.ISketchEventListener;
import ca.uvic.chisel.javasketch.ISketchInterestListener;
import ca.uvic.chisel.javasketch.SketchEvent;
import ca.uvic.chisel.javasketch.SketchInterestEvent;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IActivationEvent;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.ITargetMessage;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.data.model.ITraceEvent;
import ca.uvic.chisel.javasketch.data.model.ITraceEventListener;
import ca.uvic.chisel.javasketch.data.model.ITraceMetaEvent;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;
import ca.uvic.chisel.javasketch.data.model.imple.internal.ActivationImpl;
import ca.uvic.chisel.javasketch.data.model.imple.internal.ThreadImpl;
import ca.uvic.chisel.javasketch.data.model.imple.internal.TraceImpl;
import ca.uvic.chisel.javasketch.internal.ast.groups.ASTMessageGroupingTree;
import ca.uvic.chisel.javasketch.ui.ISketchImageConstants;
import ca.uvic.chisel.javasketch.ui.internal.preferences.ISketchPluginPreferences;
import ca.uvic.chisel.javasketch.ui.internal.presentation.MarkRangeForSelectionJob.InvocationReference;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.CollapseAllHandler;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.ExpandAllHandler;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.FocusInHandler;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.FocusUpHandler;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.ScreenshotHandler;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.SelectIterationAction;
import ca.uvic.chisel.javasketch.ui.internal.presentation.metadata.PresentationData;
import ca.uvic.chisel.widgets.RangeAnnotation;
import ca.uvic.chisel.widgets.RangeSlider;
import ca.uvic.chisel.widgets.TimeField;

@SuppressWarnings("restriction")
public class JavaThreadSequenceView extends ViewPart implements IJavaSketchPresenter,
		ITabbedPropertySheetPageContributor, ISketchInterestListener {

	public static final String VIEW_ID = "ca.uvic.chisel.javasketch.threadView";
	private static final String LAST_TRACE = "last_trace";
	private static final String LAST_THREAD = "last_thread";
	
	private class InternalSelectionProvider implements ISelectionProvider {
		ListenerList selectionListeners = new ListenerList();
		private ISelection selection = StructuredSelection.EMPTY;

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		@Override
		public void addSelectionChangedListener(
				ISelectionChangedListener listener) {
			selectionListeners.add(listener);
			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
		 */
		@Override
		public ISelection getSelection() {
			return selection;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
		 */
		@Override
		public void removeSelectionChangedListener(
				ISelectionChangedListener listener) {
			selectionListeners.remove(listener);			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
		 */
		@Override
		public void setSelection(ISelection selection) {
			if (selection == null) {
				return;
			}
			if (this.selection != null) {
				if (this.selection.equals(selection)) {
					return;
				}	
			}
			this.selection = selection;
			SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
			for (Object o : selectionListeners.getListeners()) {
				((ISelectionChangedListener)o).selectionChanged(event);
			}
			//update commands
			ICommandService service = (ICommandService) SketchPlugin.getDefault().getWorkbench().getService(ICommandService.class);
			if (service != null) {
				service.refreshElements(FocusInHandler.COMMAND_ID, null);
			}
		}
		
		public void dispose() {
			selectionListeners.clear();
		}
		
	}

	/**
	 * @author Del Myers
	 * 
	 */
	private final class InternalSelectionChangedListener implements
			ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			IJavaElement[] elements = new IJavaElement[0];
			if (javaSelection != null) {
				elements = javaSelection.toArray(elements);
			}
			rangeJob.cancel();
			rangeJob.schedule(elements);
			selectionProvider.setSelection(event.getSelection());
		}
	}

	/**
	 * @author Del Myers
	 * 
	 */
	private final class PresentationListener implements ISequenceViewerListener, ISchedulingRule {
		private IProgressMonitor progress;

		@Override
		public void rootChanged(SequenceViewerRootEvent event) {
			if (isCancelled()) {
				reset();
				return;
			}
			PresentationData pd = PresentationData.connect(getSketch());
			if (pd != null) {
				try {
					pd.setThreadRoot(thread, (IActivation) event
						.getSequenceViewer().getRootActivation());
				} finally {
					pd.disconnect();
				}

			}
		}

		@Override
		public void groupExpanded(SequenceViewerGroupEvent event) {
			if (isCancelled()) {
				reset();
				return;
			}
			setGroupExpansion(event, true);

		}

		private void setGroupExpansion(SequenceViewerGroupEvent event, boolean b) {
			if (isCancelled()) {
				reset();
				return;
			}
			PresentationData pd = PresentationData.connect(getSketch());
			if (pd != null) {
				try {
					ASTMessageGroupingTree grouping = getGroupIng(event
						.getGroup());
					if (grouping != null) {
						pd.setGroupExpanded((IActivation) event.getGroup()
							.getActivationElement(), grouping, b);
					}
				} finally {
					pd.disconnect();
				}
			}
		}

		@Override
		public void groupCollapsed(SequenceViewerGroupEvent event) {
			if (isCancelled()) {
				reset();
				return;
			}
			setGroupExpansion(event, false);
		}

		@Override
		public void elementExpanded(SequenceViewerEvent event) {
			if (isCancelled()) {
				reset();
				return;
			}
			setActivationExpanded(event, true);
			internalResetExpansionStates((IActivation) event.getElement());
		}

				
		/**
		 * 
		 */
		private void reset() {
			this.progress = null;		
		}

		@Override
		public void elementCollapsed(SequenceViewerEvent event) {
			if (isCancelled()) {
				reset();
				return;
			}
			setActivationExpanded(event, false);
		}

		/**
		 * @return
		 */
		private boolean isCancelled() {
			return (progress != null && progress.isCanceled());
		}

		private void setActivationExpanded(SequenceViewerEvent event, boolean b) {
			if (event.getElement() instanceof IActivation) {
				PresentationData pd = PresentationData.connect(getSketch());
				if (pd != null) {
					try {
						pd.setActivationExpanded((IActivation) event
							.getElement(), b);

					} finally {
						pd.disconnect();
					}
				}

			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
		 */
		@Override
		public boolean contains(ISchedulingRule rule) {
			if (isConflicting(rule)) {
				return true;
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
		 */
		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return (rule instanceof PresentationListener);
		}

		/**
		 * @param monitor
		 */
		public void setProgress(IProgressMonitor monitor) {
			this.progress = monitor;			
		}
	}

	/**
	 * @author Del Myers
	 * 
	 */
	private final class ISketchEventListenerImplementation implements
			ISketchEventListener {
		@Override
		public void handleSketchEvent(final SketchEvent fevent) {
			getViewSite().getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					try {
						IProgramSketch sketch = SketchPlugin.getDefault()
							.getSketch(thread);
						SketchEvent event = fevent;
						if (sketch != null && sketch.equals(event.getSketch())) {
							switch (event.getType()) {
							case SketchAnalysisStarted:
							case SketchAnalysisEnded:
							case SketchAnalysisInterrupted:
							case SketchRefreshed:
								if (thread != null && !thread.isValid()) {
									for (IThread test : sketch.getTraceData()
										.getThreads()) {
										if (test.getID() == thread.getID()
												&& test.getName().equals(
													thread.getName())) {
											setInput(test);
											return;
										}
									}
									thread = null;
									setInput(thread);
								} else {
									resetViewers();
								}
								break;
							case SketchDeleted:
								setInput(null);
								break;
							}
						}
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}

			});
		}
	}

	private final class RefreshActivationListener implements
			ITraceEventListener {

		/*
		 * (non-Javadoc)
		 * @see
		 * ca.uvic.chisel.javasketch.data.model.ITraceEventListener#handleEvents
		 * (ca.uvic.chisel.javasketch.data.model.ITraceEvent[])
		 */
		@Override
		public void handleEvents(ITraceEvent[] events) {
			for (ITraceEvent event : events) {
				switch (event.getType()) {
				case ActivationEventType:
					IActivation[] activations = ((IActivationEvent) event)
						.getActivations();
					refreshActivationsJob.cancel();
					refreshActivationsJob.schedule(activations);
					break;

				}
			}
		}

	}

	private class RefreshActivationsJob extends UIJob {
		private IActivation[] activations;

		public RefreshActivationsJob() {
			super("Refreshing Activations");
			activations = new IActivation[0];
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime
		 * .IProgressMonitor)
		 */
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			synchronized (activations) {
				for (IActivation activation : activations) {
					viewer.refresh(activation);
				}
			}
			return Status.OK_STATUS;
		}

		public void schedule(IActivation[] activations) {
			if (activations == null) {
				return;
			}
			synchronized (this.activations) {
				if (activations.length != this.activations.length) {
					this.activations = new IActivation[activations.length];
				}
				System.arraycopy(activations, 0, this.activations, 0,
					activations.length);
			}
			if (this.activations.length > 0) {
				schedule();
			}
		}

	}
	
	private class RequestReconnaissanceJob extends Job {
		
		private class FilterAnimator implements Runnable {
			
			private int index = 0;
			private boolean up;
			private boolean running = false;
			FilterAnimator() {
			}

			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				if (running) {
					enableReconnaissanceAction.setImageDescriptor(filterImages[index]);
					if (up) {
						index++;
						if (index >= filterImages.length) {
							index = filterImages.length-2;
							up = false;
						}
					} else {
						index--;
						if (index < 0) {
							index = 1;
							up = true;
						}
					}
					PlatformUI.getWorkbench().getDisplay().timerExec(150, this);
				} else {
					if (enableReconnaissanceAction.isChecked()) {
						enableReconnaissanceAction.setImageDescriptor(filterImages[3]);
					} else {
						enableReconnaissanceAction.setImageDescriptor(filterImages[0]);
					}
				}
			}
			
			public void stop() {
				running = false;
			}
			
			public void start() {
				if (!running) {
					running = true;
					index = 0;
					PlatformUI.getWorkbench().getDisplay().asyncExec(this);
				}
			}
			
		}
		
		private FilterAnimator animator;
		/**
		 * @param name
		 */
		public RequestReconnaissanceJob() {
			super("Filtering Thread");
			animator = new FilterAnimator();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			animator.start();
			SketchPlugin.getDefault().getDOI().requestFiltering(thread, monitor);
			IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
			if (monitor.isCanceled()) {
				store.setValue(ISketchPluginPreferences.DIAGRAM_RECONNAISSANCE, false);
				enableReconnaissanceAction.setChecked(false);
				animator.stop();
				return Status.CANCEL_STATUS;
			} else {
				store.setValue(ISketchPluginPreferences.DIAGRAM_RECONNAISSANCE, true);
				animator.start();
				enableReconnaissanceAction.setChecked(true);
			}
			animator.stop();
			if (store.getBoolean(ISketchPluginPreferences.DIAGRAM_RECON_HIDE)) {
				refreshJob.cancel();
				refreshJob.schedule();
			} else {
				getSite().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){

					@Override
					public void run() {
						updateForReconnaissanceFiltering();
						updateAll();
					}

				});
			}
			return Status.OK_STATUS;
		}

		

		
		
	}
	
	private class RequestReconnaissanceAction extends Action {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.Action#run()
		 */
		
		/**
		 * 
		 */
		public RequestReconnaissanceAction() {
			super("Filter", IAction.AS_CHECK_BOX);
		}
		@Override
		public void run() {
			IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
			
			if (isChecked()) {
				requestReconnaissanceJob.cancel();
				requestReconnaissanceJob.schedule();
				setImageDescriptor(filterImages[3]);
			} else {
				store.setValue(ISketchPluginPreferences.DIAGRAM_RECONNAISSANCE, false);
				setImageDescriptor(filterImages[0]);
				requestReconnaissanceJob.cancel();
				if (store.getBoolean(ISketchPluginPreferences.DIAGRAM_RECON_HIDE)) {
					refreshJob.cancel();
					refreshJob.schedule();
				} else {
					updateForReconnaissanceFiltering();
					updateAll();
				}
			}
		}
	}
	
	public class ToggleReconFilterAction extends Action {
		
		/**
		 * 
		 */
		public ToggleReconFilterAction() {
			super("Exclude Hidden Method Calls", IAction.AS_CHECK_BOX); 
		}
		
		
		public void run() {
			IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
			store.setValue(ISketchPluginPreferences.DIAGRAM_RECON_HIDE, isChecked());
			if (store.getBoolean(ISketchPluginPreferences.DIAGRAM_RECONNAISSANCE)) {
				refreshJob.cancel();
				refreshJob.schedule();
			}
		}
	}
	

	private class SwapLoopAction extends Action {
		private IActivation activation;
		private ASTMessageGroupingTree nodeToSwap;

		public SwapLoopAction(IActivation a, ASTMessageGroupingTree nodeToSwap) {
			this.activation = a;
			this.nodeToSwap = nodeToSwap;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public void run() {
			PresentationData pd = PresentationData.connect(SketchPlugin
				.getDefault().getSketch(activation));
			if (pd != null) {
				try {
					pd.swapLoop(activation, nodeToSwap, reconnaissanceEnabled() && hidingUnusedCalls());
				} finally {
					pd.disconnect();
				}
				viewer.refresh(activation);
				resetExpansionStates(activation);
			}
			super.run();
		}
	}
	
	private class InternalPreferenceListener implements IPropertyChangeListener{

		/* (non-Javadoc)
		 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			String property = event.getProperty();
			if (ISketchPluginPreferences.COMPACT_LOOPS_PREFERENCE.equals(property) ||
				ISketchPluginPreferences.DISPLAY_GROUPS_PREFERENCE.equals(property)) {
				refreshJob.cancel();
				refreshJob.schedule();
			}
		}
		
	}

	UMLSequenceViewer viewer;
	private InternalSelectionProvider selectionProvider;
	private UIJob refreshJob;
	private RangeSlider timeRange;

	private ContributionItem collapseAllAction;
	private ContributionItem expandAllAction;
	private ContributionItem focusInAction;
	private ContributionItem focusUpAction;
	private RequestReconnaissanceAction enableReconnaissanceAction;
	private ThumbnailOutlinePage outlinePage;
	private BreadCrumbViewer breadcrumb;
	private ISelectionListener javaSelectionListener;
	private MarkRangeForSelectionJob rangeJob;
	private ISketchEventListener sketchListener;
	private IThread thread;
	private RefreshActivationsJob refreshActivationsJob;
	private RequestReconnaissanceJob requestReconnaissanceJob;
	private ITraceEventListener activationChangeListener;
	private PresentationListener presentationListener;
	private ArrayList<IJavaElement> javaSelection;
	private ISelectionChangedListener internalSelectionListener;
	private TimeField maxTime;
	private TimeField minTime;
	private InternalPreferenceListener preferenceListener;
	private ImageDescriptor[] filterImages;
	private ToggleReconFilterAction toggleReconFilterAction;

	public JavaThreadSequenceView() {
		refreshJob = new UIJob("Refreshing sequence viewer") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					viewer.refresh();
				} catch (Exception e) {
					IStatus status = new Status(IStatus.ERROR,
						SketchPlugin.PLUGIN_ID,
						"Error refreshing sequence viewer", e);
					SketchPlugin.getDefault().getLog().log(status);
				}
				return Status.OK_STATUS;
			}
		};
		requestReconnaissanceJob = new RequestReconnaissanceJob();
		activationChangeListener = new RefreshActivationListener();
		this.refreshActivationsJob = new RefreshActivationsJob();
		preferenceListener = new InternalPreferenceListener();
		javaSelectionListener = new ISelectionListener() {

			@Override
			public void selectionChanged(IWorkbenchPart part,
					ISelection selection) {
				updateForSelection(selection);

			}
		};
	}

	/**
	 * @param selection
	 */
	protected void updateForSelection(ISelection selection) {
		ArrayList<IJavaElement> javaElements = new ArrayList<IJavaElement>();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			Iterator<?> iterator = ss.iterator();
			while (iterator.hasNext()) {
				Object o = iterator.next();
				if (o instanceof IAdaptable) {
					IJavaElement element = (IJavaElement) ((IAdaptable) o)
						.getAdapter(IJavaElement.class);
					if (element instanceof IType) {
						javaElements.add(element);
					} else if (element instanceof IMethod) {
						javaElements.add(element);
					}
				}
			}
		}
		if (javaElements.size() > 0) {
			this.javaSelection = javaElements;
			rangeJob.cancel();
			rangeJob.schedule(javaElements
				.toArray(new IJavaElement[javaElements.size()]));
		}
	}

	public void setInput(IThread newInput) {
		if (newInput == thread) {
			return;
		}
		if (thread != null) {
			thread.getTrace().removeListener(activationChangeListener);
		}

		thread = newInput;
		if (thread != null) {
			thread.getTrace().addListener(activationChangeListener);
			setPartName(thread.getName());
		}
		final IActivation activation = thread.getRoot().getActivation();
		resetExpansionStates(activation);

	}

	/**
	 * 
	 */
	public void resetExpansionStates(final IActivation activation) {
		
		IRunnableWithProgress initializer = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) {
				monitor.beginTask("Initializing Sequence Diagram", IProgressMonitor.UNKNOWN);
				presentationListener.setProgress(monitor);
				if (viewer != null) {
					resetViewers();
					PresentationData pd = PresentationData.connect(getSketch());
					if (pd != null) {
						try {
							if (pd.isExpanded(activation)) {
								viewer.setExpanded(activation,
									true);
							}
						} finally {
							pd.disconnect();
						}
					}
					if (enableReconnaissanceAction.isChecked()) {
						requestReconnaissanceJob.cancel();
						requestReconnaissanceJob.schedule();
					}
				}
				
				updateForReconnaissanceFiltering();
				monitor.done();
			}
		};
		try {
			
			getSite().getWorkbenchWindow().getWorkbench().getProgressService().runInUI(getSite().getWorkbenchWindow(), initializer, presentationListener);
		} catch (InvocationTargetException e) {
			SketchPlugin.getDefault().log(e);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Just sets the background color for when reconnaissance filtering is enabled
	 * to make sure that the user can know whether the current visualized thread
	 * is in the active trace.
	 */
	private void updateForReconnaissanceFiltering() {
		UMLSequenceChart chart = viewer.getChart();
		IProgramSketch activeSketch = SketchPlugin.getDefault().getActiveSketch();
		chart.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		if (reconnaissanceEnabled()) {
			if (activeSketch != null) {
				if (!activeSketch.equals(SketchPlugin.getDefault().getSketch(thread))) {
					chart.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
				}
			}
		}
	}

	/**
	 * 
	 */
	protected void internalResetExpansionStates(IActivation activation) {
		if (!viewer.getExpanded(activation))
			return;
		PresentationData pd = PresentationData.connect(getSketch());
		if (pd != null) {
			try {
				// use the widget itself because it will be faster than
				// querying the content provider again
				viewer.getChart().setRedraw(false);
				Widget w = viewer.testFindItem(activation);
				if (w instanceof Activation) {
					Activation aw = (Activation) w;
					for (Message m : aw.getMessages()) {
						//read and dispatch so things don't get look like they
						//hang
						if (m instanceof Call) {
							Call c = (Call) m;
							Activation target = c.getTarget();
							if (target != null && !target.isDisposed()
									&& target.getData() instanceof IActivation) {
								IActivation ta = (IActivation) target.getData();
								viewer.setExpanded(ta, pd.isExpanded(ta));
								viewer.getChart().getDisplay().readAndDispatch();
							}
						}
					}
					for (MessageGroup group : aw.getMessageGroups()) {
						IMessageGrouping o = (IMessageGrouping) group.getData();
						if (o instanceof IAdaptable) {
							ASTMessageGroupingTree node = (ASTMessageGroupingTree) ((IAdaptable) o)
								.getAdapter(ASTMessageGroupingTree.class);
							if (node != null) {

								viewer.setGroupingExpanded(o, pd
									.isGroupExpanded(activation, node));
								viewer.getChart().getDisplay().readAndDispatch();

							}
						}
					}
				}
			} catch (Exception e) {
				SketchPlugin.getDefault().log(e);
			} finally {
				pd.disconnect();
				viewer.getChart().setRedraw(true);
			}
		}
	}

	protected ASTMessageGroupingTree getGroupIng(IMessageGrouping group) {
		if (group instanceof IAdaptable) {
			IAdaptable adapt = (IAdaptable) group;
			return (ASTMessageGroupingTree) adapt
				.getAdapter(ASTMessageGroupingTree.class);
		}
		return null;
	}

	protected IProgramSketch getSketch() {
		return SketchPlugin.getDefault().getSketch(thread);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.ViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		if (memento == null) {
			return;
		}
		if (thread == null) {
			return;
		}
		memento.putString(LAST_TRACE, thread.getTrace().getLaunchID());
		memento.putString(LAST_THREAD, thread.getIdentifier());
		super.saveState(memento);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite,
	 * org.eclipse.ui.IMemento)
	 */
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		SketchPlugin.getDefault().getDOI().addSketchInterestListener(this);
		// if (memento != null) {
		// String lastTrace = memento.getString(LAST_TRACE);
		// String lastThread = memento.getString(LAST_THREAD);
		// if (lastTrace != null && lastThread != null) {
		// IProgramSketch sketch =
		// SketchPlugin.getDefault().getSketch(lastTrace);
		// if (sketch != null) {
		// ITrace trace = sketch.getTraceData();
		// for (IThread thread : trace.getThreads()) {
		// if (thread.getIdentifier().equals(lastThread)) {
		// setInput(thread);
		// return;
		// }
		// }
		// }
		// }
		// }
	}

	@Override
	public void createPartControl(Composite parent) {
		filterImages = new ImageDescriptor[4];
		ImageRegistry reg = SketchPlugin.getDefault().getImageRegistry();
		filterImages[0] = reg.getDescriptor(ISketchImageConstants.ICON_ELEMENT_VISIBLE);
		filterImages[1] = reg.getDescriptor(ISketchImageConstants.ICON_ELEMENT_VISIBLE+"2-3");
		filterImages[2] = reg.getDescriptor(ISketchImageConstants.ICON_ELEMENT_VISIBLE+"1-3");
		filterImages[3] = reg.getDescriptor(ISketchImageConstants.ICON_ELEMENT_FILTERED);
		Composite page = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		page.setLayout(layout);

		breadcrumb = new BreadCrumbViewer(page, SWT.BORDER);
		breadcrumb.setContentProvider(new BreadCrumbContentProvider());
		breadcrumb.setLabelProvider(new TraceThreadLabelProvider());

		breadcrumb.getControl().setLayoutData(
			new GridData(SWT.FILL, SWT.FILL, true, false));

		viewer = new UMLSequenceViewer(page, SWT.V_SCROLL | SWT.H_SCROLL
				| SWT.VIRTUAL);

		viewer.setUseHashlookup(true);
		viewer.setContentProvider(new TraceThreadContentProvider());
		viewer.setLabelProvider(new TraceThreadLabelProvider());
		viewer.setMessageGrouper(new ASTMessageGrouper());
		viewer.addFilter(new ViewerFilter(){

			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				boolean reconFilter =
					SketchPlugin.getDefault().getPreferenceStore().getBoolean(ISketchPluginPreferences.DIAGRAM_RECON_HIDE);
				
				if (reconFilter && enableReconnaissanceAction.isChecked() && element instanceof ICall) {
					ICall call = (ICall) element;
					double interest = SketchPlugin.getDefault().getDOI().getInterest(call);
					if (interest < .3) {
						return false;
					}
				}
				return true;
			}});
		internalSelectionListener = new InternalSelectionChangedListener();

		viewer.addSelectionChangedListener(internalSelectionListener);

		viewer.getControl().addMouseListener(new NavigateToCodeListener());
		viewer.getControl().setBackground(
			parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		viewer.getControl().setLayoutData(
			new GridData(SWT.FILL, SWT.FILL, true, true));
		presentationListener = new PresentationListener();
		viewer.addSequenceListener(presentationListener);

		new BreadCrumbHook(breadcrumb, viewer);
		createTimeRange(page, thread);

		viewer.setInput(thread);

		createActions();
		createContextMenu();
		getViewSite().getActionBars().getToolBarManager().add(enableReconnaissanceAction);
		getViewSite().getActionBars().getMenuManager().add(toggleReconFilterAction);
		//add the drop-down menu
		IMenuManager manager = getViewSite().getActionBars().getMenuManager();
		manager.add(new Separator("presentation"));
		
		getViewSite().getPage().addSelectionListener(javaSelectionListener);
		selectionProvider = new InternalSelectionProvider();
		getViewSite().setSelectionProvider(selectionProvider);
		sketchListener = new ISketchEventListenerImplementation();
		SketchPlugin.getDefault().addSketchEventListener(sketchListener);
		SketchPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(preferenceListener);
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		viewer.removeSequenceListener(presentationListener);
		viewer.removeSelectionChangedListener(internalSelectionListener);
		selectionProvider.dispose();
		if (thread != null) {
			thread.getTrace().removeListener(activationChangeListener);
		}
		getViewSite().getPage().removeSelectionListener(javaSelectionListener);
		SketchPlugin.getDefault().removeSketchEventListener(sketchListener);
		SketchPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(preferenceListener);
		SketchPlugin.getDefault().getDOI().removeSketchInterestListener(this);
		super.dispose();
	}
	

	/**
	 * @param page
	 */
	private void createTimeRange(Composite page, IThread thread) {
		Composite rangeComposite = new Composite(page, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		rangeComposite.setLayoutData(gd);
		GridLayout layout = new GridLayout(3, false);
		layout.horizontalSpacing = 3;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		rangeComposite.setLayout(layout);
		minTime = new TimeField(rangeComposite, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = 15;
		minTime.setLayoutData(gd);
		timeRange = new RangeSlider(rangeComposite, SWT.NONE);
		timeRange.setForeground(page.getDisplay().getSystemColor(
			SWT.COLOR_DARK_GRAY));
		timeRange.setBackground(page.getDisplay().getSystemColor(
			SWT.COLOR_GREEN));
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.heightHint = 15;
		timeRange.setLayoutData(gd);
		if (thread != null) {
			long min = thread.getRoot().getActivation().getTime();
			long max = thread.getRoot().getActivation().getDuration() + min;
			timeRange.setMinimum(min);
			timeRange.setMaximum(max);
			timeRange.setSelectedMinimum(min);
			timeRange.setSelectedMaximum(max);
		}
		maxTime = new TimeField(rangeComposite, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = 15;
		maxTime.setLayoutData(gd);
		minTime.setTime(timeRange.getMinimum());
		maxTime.setTime(timeRange.getMaximum());
		timeRange.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				if (e.width < 0 || e.height < 0) {
					if (e.data instanceof InvocationReference) {
						InvocationReference ref = (InvocationReference) e.data;
						StructuredSelection selection = new StructuredSelection(ref.activation);
						selectionProvider.setSelection(selection);
					}
					return;
				}
				boolean changed = false;
				if (minTime.getTime() != timeRange.getSelectedMinimum()) {
					minTime.setTime(timeRange.getSelectedMinimum());
					changed = true;
				}
				if (maxTime.getTime() != timeRange.getSelectedMaximum()) {
					maxTime.setTime(timeRange.getSelectedMaximum());
					changed = true;
				}
				if (changed) {
					refreshJob.cancel();
					refreshJob.schedule(2000);
				}
			}
		});
		viewer.addFilter(new TimeFilter(timeRange));
		ModifyListener typedTimeListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (e.widget == maxTime) {
					timeRange.setSelectedMaximum(maxTime.getTime());
				} else if (e.widget == minTime) {
					timeRange.setSelectedMinimum(minTime.getTime());
				}
				refreshJob.cancel();
				refreshJob.schedule(2000);
			}
		};
		minTime.addModifyListener(typedTimeListener);
		maxTime.addModifyListener(typedTimeListener);
		rangeComposite.pack();

		rangeJob = new MarkRangeForSelectionJob(this);
		new TimeLineTooltip(this);
		new TimeLineAnnotationHook(this);
	}

	/**
	 * 
	 */
	protected void resetViewers() {
		if (viewer == null || viewer.getChart().isDisposed()) {
			return;
		}
		if (viewer.getInput() != thread) {
			viewer.setInput(thread);
		}
		if (thread == null) {
			minTime.setTime(0);
			maxTime.setTime(0);
			return;
		}
		IWorkbenchPage page = getViewSite().getPage();
		Display display = getSite().getShell().getDisplay();
		// add annotations for the pause/resume events
		List<ITraceMetaEvent> events = thread.getTrace().getEvents();

		IProgramSketch sketch = SketchPlugin.getDefault().getSketch(thread);
		RangeAnnotation annotation = null;
		// first remove all of the old annotations
		for (RangeAnnotation a : timeRange.getRanges()) {
			if ("PAUSE".equals(a.getText())) {
				a.dispose();
			}
		}
		if (sketch != null) {
			// search for the newest thread for the sketch, as it may have
			// changed
			for (IThread t : sketch.getTraceData().getThreads()) {
				if (t.getName().equals(thread.getName())) {
					if (t.getID() == thread.getID()) {
						if (this.thread != t) {
							this.thread = t;
						}
					}
					break;
				}
			}
		}

		if (thread != null) {
			long min = thread.getRoot().getActivation().getTime();
			long max = thread.getRoot().getActivation().getDuration() + min;
			timeRange.setMinimum(min);
			timeRange.setMaximum(max);
			timeRange.setSelectedMinimum(min);
			timeRange.setSelectedMaximum(max);
			minTime.setTime(min);
			maxTime.setTime(max);
		} else {
			minTime.setTime(0);
			maxTime.setTime(0);
		}
		for (ITraceMetaEvent event : events) {
			String text = event.getText();
			if (text.equals("PAUSE IN THREAD " + thread.getID())) {
				if (annotation != null) {
					annotation.setLength(event.getTime()
							- annotation.getOffset());
				}
				annotation = new RangeAnnotation(timeRange);
				annotation.setOffset(event.getTime());
				annotation.setText("PAUSE");
				annotation.setBackground(display
					.getSystemColor(SWT.COLOR_YELLOW));
			} else if (text.equals("RESUME IN THREAD " + thread.getID())) {
				if (annotation == null) {
					annotation = new RangeAnnotation(timeRange);
					annotation.setText("PAUSE");
					annotation.setBackground(display
						.getSystemColor(SWT.COLOR_YELLOW));
					annotation.setOffset(thread.getRoot().getTime());
				}
				annotation.setLength(event.getTime() - annotation.getOffset());
				annotation = null;
			}
		}
		updateForSelection(page.getSelection());
		if (thread != viewer.getInput()) {
			viewer.setInput(thread);
		}
		viewer.refresh();
	}

	/**
	 * 
	 */
	private void createActions() {

		focusInAction = createContributionItem(FocusInHandler.COMMAND_ID);
		expandAllAction = createContributionItem(ExpandAllHandler.COMMAND_ID);
		collapseAllAction = createContributionItem(CollapseAllHandler.COMMAND_ID);
		focusUpAction = createContributionItem(FocusUpHandler.COMMAND_ID);
		enableReconnaissanceAction = new RequestReconnaissanceAction();
		enableReconnaissanceAction.setText("Filter");
		enableReconnaissanceAction.setImageDescriptor(SketchPlugin.getDefault().getImageRegistry().getDescriptor(ISketchImageConstants.ICON_ELEMENT_VISIBLE));
		IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
		enableReconnaissanceAction.setChecked(store.getBoolean(ISketchPluginPreferences.DIAGRAM_RECONNAISSANCE));
		
		this.toggleReconFilterAction = new ToggleReconFilterAction();
		toggleReconFilterAction.setChecked(store.getBoolean(ISketchPluginPreferences.DIAGRAM_RECON_HIDE));
		toggleReconFilterAction.setText("Hide Unnecessary Calls");
		// focusUpAction = new FocusUpHandler(viewer);
		// focusUpAction.setText("Focus On Parent");
		// focusUpAction.setImageDescriptor(
		// SketchPlugin.imageDescriptorFromPlugin("images/etool16/up.gif")
		// );
		// expandAllAction = new ExpandAllHandler(viewer);
		// expandAllAction.setText("Expand All Children");
		// expandAllAction.setImageDescriptor(
		// SketchPlugin.imageDescriptorFromPlugin("images/etool16/expandAll.gif")
		// );
		// collapseAllAction = new CollapseAllHandler(viewer);
		// collapseAllAction.setText("Collapse All Children");
		// collapseAllAction.setImageDescriptor(
		// SketchPlugin.imageDescriptorFromPlugin("images/etool16/collapseAll.gif")
		// );
	}

	private CommandContributionItem createContributionItem(String commandId) {
		CommandContributionItemParameter parameters = new CommandContributionItemParameter(
			SketchPlugin.getDefault().getWorkbench(), null, commandId, SWT.PUSH);
		return new CommandContributionItem(parameters);
	}

	private void createContextMenu() {

		MenuManager manager = new MenuManager("JavaSketchEditor",
			"#JavaSketchEditorPopUp");
		Menu menu = manager.createContextMenu(viewer.getChart());
		// getSite().registerContextMenu(manager, viewer);
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				Point location = Display.getCurrent().getCursorLocation();
				location = viewer.getChart().toControl(location);
				Object element = viewer.elementAt(location.x, location.y);
				fillContextMenu(manager, element);
			}
		});
		viewer.getChart().setMenu(menu);
		getSite().registerContextMenu("#JavaSketchEditorPopUp", manager, selectionProvider);
	}

	/**
	 * Opens the context menu to be filled with actions.
	 * 
	 * @param manager
	 */
	private void fillContextMenu(IMenuManager manager, Object elementUnderCursor) {
		if (elementUnderCursor instanceof IMessageGrouping) {
			IMessageGrouping grouping = (IMessageGrouping) elementUnderCursor;
			if (elementUnderCursor instanceof IAdaptable) {
				ASTMessageGroupingTree node = (ASTMessageGroupingTree) ((IAdaptable) elementUnderCursor)
					.getAdapter(ASTMessageGroupingTree.class);
				if (node != null) {
					IActivation activation = (IActivation) grouping
						.getActivationElement();
					ASTMessageGroupingTree[] iterations = node.getIterations();
					int selected = 0;
					for (int i = 0; i < iterations.length; i++) {
						ASTMessageGroupingTree iteration = iterations[i];
						if (!isEmptyIteration(iteration)) {
							selected++;
							IAction swapAction = new SwapLoopAction(activation,
								iteration);
							swapAction.setText("Iteration "
									+ iteration.getIteration());
							manager.add(swapAction);
							if (selected >= 9) {
								SelectIterationAction selector = new SelectIterationAction(
									activation, iterations, this);
								selector.setText("Select Iteration...");
								manager.add(selector);
								break;
							}
						}
						
					}
				}
				return;
			}

		}
		if (focusInAction.isEnabled()) {
			manager.add(focusInAction);
		}
		if (focusUpAction.isEnabled()) {
			manager.add(focusUpAction);
		}
		if (collapseAllAction.isEnabled()) {
			manager.add(collapseAllAction);
		}
		if (expandAllAction.isEnabled()) {
			manager.add(expandAllAction);
		}
		
		manager.add(createContributionItem(ScreenshotHandler.COMMAND_ID));
	}

	/**
	 * @param iteration
	 * @return
	 */
	private boolean isEmptyIteration(ASTMessageGroupingTree iteration) {
		if (!reconnaissanceEnabled() || !hidingUnusedCalls()) {
			return false;
		}
		for (String id : iteration.getMessageIdentifiers()) {
			ITraceModelProxy proxy = getThread().getTrace().getElement(id);
			IOriginMessage message = (IOriginMessage) proxy.getElement();
			double interest = SketchPlugin.getDefault().getDOI().getInterest(message);
			if (interest > .3) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setFocus() {}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
			if (outlinePage == null) {
				outlinePage = new ThumbnailOutlinePage(viewer.getChart());
			}
			return outlinePage;
		} else if (IPropertySheetPage.class.equals(adapter)) {
			return new TabbedPropertySheetPage(this);
		}
		return super.getAdapter(adapter);
	}

	public RangeSlider getTimeRange() {
		return timeRange;
	}

	/**
	 * @return
	 */
	public UMLSequenceViewer getSequenceChartViewer() {
		return viewer;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ca.uvic.chisel.javasketch.ui.internal.presentation.IJavaSketchPresenter
	 * #getThread()
	 */
	@Override
	public IThread getThread() {
		return thread;
	}

	/**
	 * @param thread2
	 * @param first
	 */
	public void reveal(ITraceModel element, String threadIdentifier) {
		if (element != null) {
			if (viewer.isVisible(element)) {
				//simply reveal it
				viewer.reveal(element);
				return;
			}
			LinkedList<Object> pathToRoot = new LinkedList<Object>();
			
			
			final IActivation activation = findFirstActivation(element, threadIdentifier);
			if (activation == null) return;

			Object newInput = activation.getArrival().getThread();
			Object input = viewer.getInput();
			if (input != newInput) {
				setInput((IThread)newInput);
			}
			input = newInput;
			if (newInput == null)
				return;
			ISequenceContentExtension2 provider = (ISequenceContentExtension2) viewer
			.getContentProvider();
			Object[] roots = ((IStructuredContentProvider) provider)
			.getElements(input);
			if (roots.length <= 0)
				return;
			Object currentRoot = roots[0];
			Object currentParent = activation;
			while (currentParent != null && !currentParent.equals(currentRoot)) {
				pathToRoot.addFirst(currentParent);
				Object call = provider.getCall(currentParent);
				if (call != null) {
					currentParent = provider.getOriginActivation(call);
				} else {
					currentParent = null;
				}
			}
			pathToRoot.addFirst(currentRoot);
			TimeTriggeredProgressMonitorDialog progress = new TimeTriggeredProgressMonitorDialog(
				viewer.getControl().getShell(), 1000);
			try {
				progress.run(false, true, new ExpandToRootRunnable(viewer,
					pathToRoot, false) {
					/*
					 * (non-Javadoc)
					 * @seeca.uvic.chisel.javasketch.ui.internal.presentation.
					 * ExpandToRootRunnable
					 * #run(org.eclipse.core.runtime.IProgressMonitor)
					 */
					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						super.run(monitor);
						viewer.reveal(activation);
					}
				});

			} catch (InvocationTargetException ex) {
				SketchPlugin.getDefault().log(ex);
			} catch (InterruptedException e1) {
			}
		}
	}
	
	/**
	 * @param event
	 * @param tm
	 */
	private IActivation findFirstActivation(ITraceModel tm, String threadId) {
		if (tm != null) {
			try {
				Connection c = ((TraceImpl) tm.getTrace()).getConnection();
				Statement s = c.createStatement();
				if (tm instanceof IActivation) {
					return (IActivation) tm;
				} else if (tm instanceof IThread) {
					try {
						return ((IThread) tm).getRoot().getActivation();
					} catch (NullPointerException e) {
					}
				} else if (tm instanceof ITraceClass) {
					ITraceClass tc = (ITraceClass) tm;
					String queryString = 
						"SELECT a.model_id FROM Activation a, Message m WHERE a.type_name = '"
						+ tc.getName()
						+ "' AND a.model_id = m.activation_id"
						+ " ORDER BY m.time, a.model_id";
					if (threadId != null) {
						ITraceModelProxy proxy = tm.getTrace().getElement(threadId);
						if (proxy != null) {
							ITraceModel thread = (IThread) proxy.getElement();
							if (thread instanceof ThreadImpl) {
								queryString = "SELECT a.model_id FROM Activation a, Message m WHERE a.type_name = '"
								+ tc.getName()
								+ "' AND a.model_id = m.activation_id"
								+ " AND thread_id = " + ((ThreadImpl)thread).getModelID()
								+ " ORDER BY m.time, a.model_id";
							}
						}
					}
					
					ResultSet results = s
						.executeQuery(queryString);
					if (results.next()) {
						String identifier = ActivationImpl
							.getIdentifierFromModel(results.getString(1));
						ITraceModelProxy proxy = tm.getTrace().getElement(
							identifier);
						if (proxy != null) {
							return (IActivation) proxy.getElement();
						}
					}
				} else if (tm instanceof ITraceClassMethod) {
					ITraceClassMethod tcm = (ITraceClassMethod) tm;
					String queryString = 
						"SELECT a.model_id FROM Activation a, Message m WHERE a.type_name = '"
						+ tcm.getTraceClass().getName()
						+ "' AND a.method_name = '"
						+ tcm.getName()
						+ "'  AND a.method_signature = '"
						+ tcm.getSignature()
						+ "' AND a.model_id = m.activation_id"
						+ " ORDER BY m.time, a.model_id";
					if (threadId != null) {
						ITraceModelProxy proxy = tm.getTrace().getElement(threadId);
						if (proxy != null) {
							ITraceModel thread = (IThread) proxy.getElement();
							if (thread instanceof ThreadImpl) {
								queryString = 
									"SELECT a.model_id FROM Activation a, Message m WHERE a.type_name = '"
									+ tcm.getTraceClass().getName()
									+ "' AND a.method_name = '"
									+ tcm.getName()
									+ "'  AND a.method_signature = '"
									+ tcm.getSignature()
									+ "' AND thread_id = "
									+ ((ThreadImpl)thread).getModelID()
									+ " AND a.model_id = m.activation_id"
									+ " ORDER BY m.time, a.model_id";
							}
						}
					}
					ResultSet results = s
						.executeQuery(queryString);
					if (results.next()) {
						String modelId = results.getString(1);
						String identifier = ActivationImpl
							.getIdentifierFromModel(modelId);
						ITraceModelProxy proxy = tm.getTrace().getElement(
							identifier);
						if (proxy != null) {
							return (IActivation) proxy.getElement();
						}
					}
				} else if (tm instanceof ITargetMessage) {
					return ((IMessage) tm).getActivation();
				} else if (tm instanceof IOriginMessage) {
					return ((IOriginMessage)tm).getTarget().getActivation();
				} else if (tm instanceof IActivation) {
					return (IActivation) tm;
				}
			} catch (SQLException e) {
				SketchPlugin.getDefault().log(e);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor
	 * #getContributorId()
	 */
	@Override
	public String getContributorId() {
		return "ca.uvic.chisel.javasketch.modelProperties";
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.ISketchInterestListener#sketchInterestChanged(ca.uvic.chisel.javasketch.SketchInterestEvent)
	 */
	@Override
	public synchronized void sketchInterestChanged(SketchInterestEvent event) {
		if (requestReconnaissanceJob != null) {
			requestReconnaissanceJob.cancel();
		}
		if (enableReconnaissanceAction != null && enableReconnaissanceAction.isChecked()) {
			requestReconnaissanceJob.schedule();
		}
	}

	public boolean reconnaissanceEnabled() {
		boolean reconEnabled =
			SketchPlugin.getDefault().getPreferenceStore().getBoolean(ISketchPluginPreferences.DIAGRAM_RECONNAISSANCE);
		return reconEnabled;
	}
	
	public boolean hidingUnusedCalls() {
		boolean reconHide =
			SketchPlugin.getDefault().getPreferenceStore().getBoolean(ISketchPluginPreferences.DIAGRAM_RECON_HIDE);
		return reconHide;
	}
	
	/**
	 * 
	 */
	protected void updateAll() {
		if (Display.getCurrent() == null) {
			return;
		}
		UMLSequenceChart chart = viewer.getChart();
		if (!chart.isDisposed()) {
			UMLItem[] items = chart.getItems();
			for (UMLItem item : items) {
				if (chart.isDisposed()) {
					break;
				}
				if (item != null && !item.isDisposed()) {
					viewer.update(item.getData(), null);
				}
				Display.getCurrent().readAndDispatch();
			}
		}
	}
}
