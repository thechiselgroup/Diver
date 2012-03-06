/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers -- initial API and implementation
 *******************************************************************************/
package org.eclipse.zest.custom.uml.viewers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.zest.custom.sequence.events.SequenceEvent;
import org.eclipse.zest.custom.sequence.events.SequenceListener;
import org.eclipse.zest.custom.sequence.events.internal.ListenerList;
import org.eclipse.zest.custom.sequence.internal.AbstractSimpleProgressRunnable;
import org.eclipse.zest.custom.sequence.internal.IUIProgressService;
import org.eclipse.zest.custom.sequence.internal.SimpleProgressMonitor;
import org.eclipse.zest.custom.sequence.internal.UIJobProcessor;
import org.eclipse.zest.custom.sequence.widgets.Activation;
import org.eclipse.zest.custom.sequence.widgets.Call;
import org.eclipse.zest.custom.sequence.widgets.IExpandableItem;
import org.eclipse.zest.custom.sequence.widgets.Lifeline;
import org.eclipse.zest.custom.sequence.widgets.Message;
import org.eclipse.zest.custom.sequence.widgets.MessageGroup;
import org.eclipse.zest.custom.sequence.widgets.Return;
import org.eclipse.zest.custom.sequence.widgets.UMLColoredItem;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;
import org.eclipse.zest.custom.sequence.widgets.UMLSequenceChart;
import org.eclipse.zest.custom.sequence.widgets.UMLTextColoredItem;

/**
 * A viewer on top of a UMLSequenceChart. 
 * @author Del Myers
 *
 */
public class UMLSequenceViewer extends StructuredViewer {
	
	private UMLSequenceChart chart;
	private IMessageGrouper grouper;
	private ListenerList sequenceListeners;
	//used in runnables to limit the execution time and memory needed for the chart
	private final int TIME_LIMIT = 30000;
	private final int CHILD_LIMIT = 1000;
	
	private class InternalSequenceListener implements SequenceListener {
		/* (non-Javadoc)
		 * @see org.eclipse.zest.custom.sequence.events.SequenceListener#itemCollapsed(org.eclipse.zest.custom.sequence.events.SequenceEvent)
		 */
		public void itemCollapsed(SequenceEvent event) {
			if (event.item instanceof MessageGroup) {
				SequenceViewerGroupEvent sEvent = new SequenceViewerGroupEvent(UMLSequenceViewer.this, (IMessageGrouping) event.item.getData());
				fireCollapseEvent(sEvent);
				return;
			}
			SequenceViewerEvent sEvent = new SequenceViewerEvent(UMLSequenceViewer.this, event.item.getData());
			fireCollapseEvent(sEvent);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.zest.custom.sequence.events.SequenceListener#itemExpanded(org.eclipse.zest.custom.sequence.events.SequenceEvent)
		 */
		public void itemExpanded(SequenceEvent event) {
			if (event.item instanceof Activation) {
				Activation a = (Activation) event.item;
				if (a.hasChildren() && a.getMessages().length == 0) {
					//we have to load the messages.
					//could possibly do this in a runnable.
					internalRefreshActivation(a, null);
				}
			}
			if (event.item instanceof MessageGroup) {
				SequenceViewerGroupEvent sEvent = new SequenceViewerGroupEvent(UMLSequenceViewer.this, (IMessageGrouping) event.item.getData());
				fireExpandEvent(sEvent);
				return;
			}
			SequenceViewerEvent sEvent = new SequenceViewerEvent(UMLSequenceViewer.this, event.item.getData());
			fireExpandEvent(sEvent);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.zest.custom.sequence.events.SequenceListener#rootChanged(org.eclipse.zest.custom.sequence.events.SequenceEvent)
		 */
		public void rootChanged(SequenceEvent event) {
			SequenceViewerRootEvent sEvent = new SequenceViewerRootEvent(UMLSequenceViewer.this);
			fireRootEvent(sEvent);
		}
		
	}
	
	

	private final class ExpandActivationsUnderRunnable extends AbstractSimpleProgressRunnable {
		
		private final Object activationElement;
		//used to count the number of children that have been loaded or
		//displayed for this runnable.
		//private int childCount;
		//private long startTime;
		private boolean limit;

		/**
		 * Creates a new runnable for expanding children.
		 * @param activationElement element to expand.
		 * @param limit true if there should be a limit on the number of children / time it takes to expand.
		 */
		private ExpandActivationsUnderRunnable(Object activationElement, boolean limit) {
			this.activationElement = activationElement;
			this.limit = limit;
		}

		@Override
		protected void doRunInUIThread(SimpleProgressMonitor monitor)
				throws InvocationTargetException {
			try {
				getChart().setRedraw(false);
				monitor.beginTask("Expanding Activations", IUIProgressService.UNKNOWN_WORK);
				Object root = activationElement;
				Widget[] results = findItems(root);
				Widget item = null;
				for (int i = 0; i < results.length; i++) {
					if(results[i] instanceof Activation){
						item = results[i];
						break;
					}
				}
				if (item instanceof Activation) {
					//startTime = System.currentTimeMillis();
					recursiveExpandActivations((Activation)item, monitor);
				}
				readAndDispatch();
			} finally {
				if (!monitor.isCancelled()) {
					monitor.done();
				}
				getChart().setRedraw(true);
			}
		}

		private void recursiveExpandActivations(Activation activation, SimpleProgressMonitor monitor) {
			readAndDispatch();
			if (monitor.isCancelled()) {
				return;
			}
			monitor.beginTask("Expanding children", IUIProgressService.UNKNOWN_WORK);
			if (activation.hasChildren() && activation.getMessages().length == 0) {
				//we have to load the messages.
				//could possibly do this in a runnable.
				internalRefreshActivation(activation, null);
			}
			long startTime = System.currentTimeMillis();
			int childCount = 0;
			LinkedList<Activation> activationStack = new LinkedList<Activation>();
			activationStack.add(activation);
			while (activationStack.size() > 0) {
				readAndDispatch();
				long elapsedTime = (limit) ? System.currentTimeMillis() - startTime : 0;
				if (childCount > CHILD_LIMIT || elapsedTime > TIME_LIMIT) {
					boolean result = MessageDialog.openQuestion(
						getChart().getShell(), 
						"Large Sequence Diagram Warning", 
						"You are trying to display an extremely large sequence diagram which " +
							"will likely degrade system performance. Are you sure you would like to continue?"
					);
					if (result) {
						childCount = 0;
						startTime = System.currentTimeMillis();
					} else {
						monitor.cancel();
					}
				}
				if (monitor.isCancelled()) {
					break;
				}
				Activation a = activationStack.removeFirst();
				monitor.setSubTask(a.getText());
				boolean wasExpanded = a.isExpanded();
				a.setExpanded(true);
				for (Message m : a.getMessages()) {
					if (m instanceof Call) {
						Activation target = m.getTarget();
						if (target != null && !target.isDisposed()) {
							activationStack.add(target);
							if (limit && !wasExpanded) ++childCount;
						}
					}
				}
				readAndDispatch();
			}				
		}
	}
	
	/**
	 * Collapses all activations under the activation element given in the constructor.
	 * @author Del Myers
	 *
	 */
	private final class CollapseActivationsUnderRunnable extends AbstractSimpleProgressRunnable {
		private final Object activationElement;

		private CollapseActivationsUnderRunnable(Object activationElement) {
			this.activationElement = activationElement;
		}

		private void recursiveCollapseActivations(Activation activation, SimpleProgressMonitor monitor) {
			for (Message m : activation.getMessages()) {
				if (!(m instanceof Call)) continue;
				Call call = (Call) m;
				if (monitor.isCancelled()) return;
				monitor.setSubTask(call.getText());
				readAndDispatch();
				recursiveCollapseActivations(call.getTarget(), monitor);
			}
			activation.setExpanded(false);
		}

		@Override
		protected void doRunInUIThread(SimpleProgressMonitor monitor)
				throws InvocationTargetException {

			try {
				getChart().setRedraw(false);
				Object root = activationElement;
				Widget[] results = findItems(root);
				Widget item = null;
				for (int i = 0; i < results.length; i++) {
					if(results[i] instanceof Activation){
						item = results[i];
						break;
					}
				}
				if (item instanceof Activation) {
					monitor.beginTask("Collapsing Activations Under " + 
							((Activation)item).getText(), IUIProgressService.UNKNOWN_WORK);
					recursiveCollapseActivations((Activation)item, monitor);
				}
			} finally {
				readAndDispatch();
				monitor.done();
				getChart().setRedraw(true);
			}
		}
	}
	
	/**
	 * Runnable for expanding activations that are within a lifeline. Only expands those that are
	 * currently reachable on the lifeline, and that become reachable after the current activations
	 * are expanded.
	 * @author Del Myers
	 *
	 */
	private final class ExpandActivationsInRunnable extends AbstractSimpleProgressRunnable {

		private Object lifelineElement;
		private boolean limit;

		public ExpandActivationsInRunnable(Object lifelineElement, boolean limit) {
			this.lifelineElement = lifelineElement;
			this.limit = limit;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.zest.custom.sequence.internal.AbstractSimpleProgressRunnable#doRunInUIThread(org.eclipse.zest.custom.sequence.internal.SimpleProgressMonitor)
		 */
		@Override
		protected void doRunInUIThread(SimpleProgressMonitor monitor)
				throws InvocationTargetException {
			try {
				getChart().setRedraw(false);
				Widget item = findItem(lifelineElement);
				if (!(item instanceof Lifeline)) {
					return;
				}
				Lifeline ll = (Lifeline) item;
				monitor.beginTask("Expading Activations In " + ll.getText(), IUIProgressService.UNKNOWN_WORK);
				Activation[] activationArray = ll.getAllActivations();
				List<Activation> activationList = new LinkedList<Activation>(Arrays.asList(activationArray));
				int childCount = 0;
				long startTime = System.currentTimeMillis();
				while (activationList.size() > 0) {
					readAndDispatch();
					Activation a = activationList.remove(0);
					monitor.setSubTask(a.getText());
					if (!a.isExpanded()) {
						a.setExpanded(true);
						//search children for messages that land on the current lifeline.
						LinkedList<Message> messages = new LinkedList<Message>();
						messages.addAll(Arrays.asList(a.getMessages()));
						while (messages.size() > 0) {
							readAndDispatch();
							if (monitor.isCancelled()) {
								break;
							}
							Message m = messages.removeFirst();
							if (m instanceof Return) continue;
							Activation target = m.getTarget();
							if (target != null && !target.isDisposed()) {
								if (!target.isExpanded() && target.getLifeline() == ll) {
									childCount++;
									activationList.add(target);
								}
								if (target.isExpanded()) {
									//search the children
									messages.addAll(Arrays.asList(target.getMessages()));
								}
							}
						}
						for (Message m : a.getMessages()) {
							if (m.getTarget() != null && !m.getTarget().isDisposed() && m.getTarget().getLifeline() == ll) {
								childCount++;
								activationList.add(m.getTarget());
							}
						}
					}
					if (limit && (childCount > CHILD_LIMIT || (System.currentTimeMillis() - startTime > TIME_LIMIT))) {
						boolean result = MessageDialog.openQuestion(
								getChart().getShell(), 
								"Large Sequence Diagram Warning", 
								"You are trying to display an extremely large sequence diagram which " +
								"will likely degrade system performance. Are you sure you would like to continue?"
						);
						if (result) {
							childCount = 0;
							startTime = System.currentTimeMillis();
						} else {
							monitor.cancel();
						}
					}
					if (monitor.isCancelled()) {
						break;
					}
				}
			} finally {
				readAndDispatch();
				monitor.done();
				getChart().setRedraw(true);
			}

		}
		
	}

	private final class CollapseActivationsInRunnable extends AbstractSimpleProgressRunnable {

		private Object lifelineElement;

		public CollapseActivationsInRunnable(Object lifelineElement) {
			this.lifelineElement = lifelineElement;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.zest.custom.sequence.internal.AbstractSimpleProgressRunnable#doRunInUIThread(org.eclipse.zest.custom.sequence.internal.SimpleProgressMonitor)
		 */
		@Override
		protected void doRunInUIThread(SimpleProgressMonitor monitor)
		throws InvocationTargetException {
			try {
				getChart().setRedraw(false);
				Widget item = findItem(lifelineElement);
				if (!(item instanceof Lifeline)) {
					return;
				}
				Lifeline ll = (Lifeline) item;
				monitor.beginTask("Expanding Activations In " + ll.getText(), IUIProgressService.UNKNOWN_WORK);
				
				Activation[] activationArray = ll.getAllActivations();
				for (Activation a : activationArray) {
					a.setExpanded(false);
				}
			} finally {
				readAndDispatch();
				monitor.done();
				getChart().setRedraw(true);
			}

		}

	}
	
	/**
	 * Creates a sequence viewer on the given parent, using the given style.
	 * No layout is set on the chart. Clients should call {@link #getChart()} or
	 * {@link #getControl()} to set its layout or layout data.
	 */
	public UMLSequenceViewer(Composite parent, int style) {
		this.chart = new UMLSequenceChart(parent, style);
		sequenceListeners = new ListenerList();
		setUseHashlookup(true);
		chart.addSequenceListener(new InternalSequenceListener());
		hookControl(chart);
	}
	
	/**
	 * Fires the given event object based on its type.
	 * @param event
	 */
	protected void fireCollapseEvent(Object event) {
		for (Object o : sequenceListeners.getListeners()) {
			ISequenceViewerListener l = (ISequenceViewerListener) o;
			if (event instanceof SequenceViewerGroupEvent) {
				l.groupCollapsed((SequenceViewerGroupEvent)event);
			} else {
				l.elementCollapsed((SequenceViewerEvent) event);
			}
		}
	}
	
	/**
	 * Fires the given event object based on its type.
	 * @param event
	 */
	protected void fireExpandEvent(Object event) {
		for (Object o : sequenceListeners.getListeners()) {
			ISequenceViewerListener l = (ISequenceViewerListener) o;
			if (event instanceof SequenceViewerGroupEvent) {
				l.groupExpanded((SequenceViewerGroupEvent)event);
			} else {
				l.elementExpanded((SequenceViewerEvent) event);
			}
		}
	}
	

	/**
	 * Fires the given event object based on its type.
	 * @param event
	 */
	protected void fireRootEvent(Object event) {
		for (Object o : sequenceListeners.getListeners()) {
			ISequenceViewerListener l = (ISequenceViewerListener) o;
			l.rootChanged((SequenceViewerRootEvent) event);
		}
	}
	
	/**
	 * Creates a sequence viewer using the given chart. Note that the chart will
	 * be cleared of all widgets when used in this constructor.
	 */
	public UMLSequenceViewer(UMLSequenceChart chart) {
		chart.setRedraw(false);
		chart.clearChart();
		chart.setRedraw(true);
		this.chart = chart;
		sequenceListeners = new ListenerList();
		setUseHashlookup(true);
		chart.addSequenceListener(new InternalSequenceListener());
		hookControl(chart);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#doFindInputItem(java.lang.Object)
	 */
	@Override
	protected Widget doFindInputItem(Object element) {
		if (getInput() != null) {
			if (getInput().equals(element)) {
				return getChart();
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#doFindItem(java.lang.Object)
	 */
	@Override
	protected Widget doFindItem(Object element) {
		if (element == null) {
			return null;
		}
		if (getChart() != null && !getChart().isDisposed()) {
			UMLItem[] items = getChart().getItems();
			for (UMLItem item : items) {
				if (!item.isDisposed() && element.equals(item.getData())) {
					return item;
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#doUpdateItem(org.eclipse.swt.widgets.Widget, java.lang.Object, boolean)
	 */
	@Override
	protected void doUpdateItem(Widget widget, Object element, boolean fullMap) {
		if (widget.isDisposed()) return;
		if (widget instanceof UMLItem) {
			UMLItem item = (UMLItem) widget;
			if (fullMap) {
				associate(element, item);
			} else {
				Object data = item.getData();
				if (data != null) {
					unmapElement(data, item);
				}
				item.setData(element);
				mapElement(element, item);
			}
			
			IBaseLabelProvider provider = getLabelProvider();
			if (widget instanceof MessageGroup) {
				MessageGroup group = (MessageGroup) widget;
				if (provider instanceof ItemLabelProvider) {
					ItemLabelProvider.ItemLabel label = new ItemLabelProvider.ItemLabel();
					label.background = group.getBackground();
					label.foreground = group.getForeground();
					label.text = group.getText();
					((ItemLabelProvider)provider).style(label, element);
				}
				return;
			}
			if (provider instanceof ILabelProvider) {
				String text = ((ILabelProvider)provider).getText(element);
				item.setText((text != null) ? text : "");
				Image image = ((ILabelProvider)provider).getImage(element);
				item.setImage(image);
			}
			if (provider instanceof IColorProvider) {
				Color fc = ((IColorProvider)provider).getForeground(element);
				Color bc = ((IColorProvider)provider).getBackground(element);
				if (item instanceof UMLColoredItem) {
					((UMLColoredItem)item).setForeground(fc);
					((UMLColoredItem)item).setBackground(bc);
				}
			}
			if (provider instanceof ITextColorProvider) {
				Color fc = ((ITextColorProvider)provider).getTextForeground(element);
				Color bc = ((ITextColorProvider)provider).getTextBackground(element);
				if (item instanceof UMLTextColoredItem) {
					((UMLTextColoredItem)item).setTextForeground(fc);
					((UMLTextColoredItem)item).setTextBackground(bc);
				}
			}
			if (widget instanceof Lifeline) {
				if (provider instanceof ISequenceLabelProvider) {
					String stereotype = ((ISequenceLabelProvider)provider).getStereoType(element);
					((Lifeline)widget).setStereotype((stereotype != null) ? stereotype : "");
					if (provider instanceof IStylingSequenceLabelProvider) {
						int lifelineStyle = ((IStylingSequenceLabelProvider)provider).getLifelineStyle(element);
						if (lifelineStyle == -1) {
							lifelineStyle = Lifeline.CLASS;
						}
						((Lifeline)widget).setClassStyle(lifelineStyle);
					}
				}
			} else if (widget instanceof Message) {
				int lineStyle = SWT.LINE_SOLID;
				int targetStyle = Message.CLOSED_ARROW | Message.FILL_MASK;
				int sourceStyle = Message.NONE;
				Message m = (Message) widget;
				if (widget instanceof Return) {
					lineStyle = SWT.LINE_DASH;
					targetStyle = Message.OPEN_ARROW;
				}
				if (provider instanceof IStylingSequenceLabelProvider) {
					IStylingSequenceLabelProvider sslp = (IStylingSequenceLabelProvider) provider;
					int temp = sslp.getMessageLineStyle(element);
					if (temp != -1) {
						lineStyle = temp;
					}
					temp = sslp.getMessageSourceStyle(element);
					if (temp != -1) {
						sourceStyle = temp;
					}
					temp = sslp.getMessageTargetStyle(element);
					if (temp != -1) {
						targetStyle = temp;
					}
				}
				m.setLineStyle(lineStyle);
				m.setSourceStyle(sourceStyle);
				m.setTargetStyle(targetStyle);
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#getSelectionFromWidget()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected List getSelectionFromWidget() {
		UMLItem[] items = getChart().getSelection();
		HashSet<Object> elementSelection = new HashSet<Object>();
		for (UMLItem item : items) {
			if (!item.isDisposed()) {
				elementSelection.add(item.getData());
			}
		}
		List<?> list = new ArrayList<Object>(elementSelection);
		return list;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#internalRefresh(java.lang.Object)
	 */
	@Override
	protected void internalRefresh(Object element) {
		try {
			getChart().setRedraw(false);
			if (element == null && getInput() == null) {
				getChart().clearChart();
				unmapAllElements();
				return;
			}
			if (element == null) {
				return;
			}
			if (element.equals(getInput())) {
				LinkedList<Object> expanded = new LinkedList<Object>();
				LinkedList<Activation> activations = new LinkedList<Activation>();
				ISequenceChartContentProvider provider = (ISequenceChartContentProvider) getContentProvider();
				Object[] roots = provider.getElements(getInput());
				Activation rootActivation = null;
				if (roots != null && roots.length == 1) {
					Widget w = findItem(roots[0]);
					if (w instanceof Activation) {
						rootActivation = (Activation) w;
					}
				}
				if (rootActivation == null) {
					rootActivation = getChart().getRootActivation();
				}
				if (rootActivation != null && rootActivation.isExpanded()) {
					activations.addFirst(rootActivation);
				}
				//store the expanded activations
				while (activations.size() > 0) {
					Activation a = activations.removeFirst();
					expanded.add(a.getData());
					for (Message m :a.getMessages()) {
						if (m instanceof Call) {
							if (m.getTarget() != null && m.getTarget().isExpanded()) {
								activations.add(m.getTarget());
							}
						}
					}
				}
				//store the root
				Object root = getRootActivation();
				//clear the chart
				getChart().clearChart();
				unmapAllElements();
				//create a new activation for the root.
				Object[] elements = provider.getElements(getInput());
				if (elements.length == 1) {
					Activation a = new Activation(getChart());
					updateItem(a, elements[0]);
					internalRefreshActivation(a, null);
					getChart().setRootActivation(a);
				}
				while (expanded.size() > 0) {
					Object next = expanded.removeFirst();
					Widget[] items = findItems(next);
					for (Widget item : items) {
						if (item instanceof Activation) {
							((Activation)item).setExpanded(true);
						}
					}
					
				}
				//try to restore the root
				if (root != null) {
					Widget rootA = findItem(root);
					if (rootA instanceof Activation) {
						getChart().setRootActivation((Activation) rootA);
					}
				}
			} else {
				//refresh for the different kinds of elements.
				Widget[] items = findItems(element);
				if (items != null) {
					for (Widget item : items) {
						if (item instanceof Activation) {
							internalRefreshActivation((Activation)item, null);
						}
					}
				}
			}
		} catch (Exception e) { 
			e.printStackTrace();
		} finally {
			getChart().setRedraw(true);
		}
	}
	
	/**
	 * Refreshes from the given activation, rebuilding lifelines and messages as it goes.
	 * @param activationElement
	 * @param callStack a list of parent activations where the first element is the most
	 * recent parent.
	 */
	private void internalRefreshActivation(Activation a, LinkedList<Activation> callStack) {
		try {
			getChart().setRedraw(false);
			if (callStack == null) {
				callStack = new LinkedList<Activation>();
			}
			if (callStack.isEmpty()) {
				if (a.getSourceCall() != null && a.getSourceCall().getTarget() != null) {
					Activation parent = a.getSourceCall().getTarget();
					while (parent != null) {
						callStack.addLast(parent);
						if (parent.getSourceCall() != null) {
							parent = parent.getSourceCall().getSource();
						} else {
							parent = null;
						}
					}
				}
			}
			if (a == null || a.isDisposed()) return;
			//make sure that the activation should be shown
			if (!select(a.getData())) {
				//it shouldn't be. So, delete the sub tree
				deleteSubTree(a);
				return;
			}
			ISequenceChartContentProvider provider = (ISequenceChartContentProvider) getContentProvider();
			internalRefreashActivationLifeline(a);
			//the activation may have been disposed in the above process.
			if (a.isDisposed()) {
				return;
			}
			
			//update the messages
			Message[] oldMessages = a.getMessages();
			Object[] filteredMessages = filter(provider.getMessages(a.getData()));
			//run through a filtering to make sure that the targets of the messages will be visible as well
			List<Object> filteredMessageList = new LinkedList<Object>();
			for (Object m : filteredMessages) {
				if (selectActivation(provider.getTarget(m))) {
					filteredMessageList.add(m);
				}
			}
			Object[] newMessageElements = filteredMessageList.toArray();
			ArrayList<Message> newMessages = new ArrayList<Message>();
			newMessages.addAll(Arrays.asList(oldMessages));
			HashSet<Object> mappedElements = new HashSet<Object>();
			LinkedList<Activation> activationsToLoad = new LinkedList<Activation>();
			for (Message m : newMessages) {
				if (!m.isDisposed()) {
					mappedElements.add(m.getData());
				}
			}
			int i = 0;
			for (; i < newMessageElements.length; i++) {
				Object messageElement = newMessageElements[i];
				Object target = provider.getTarget(messageElement);
				Message oldMessage = null;
				if (mappedElements.contains(messageElement)) {
					for (int j = i; j < oldMessages.length; j++) {
						Message temp = newMessages.get(j);
						if (!temp.isDisposed()) {
							if (temp.getData().equals(messageElement)) {
								if (i != j) {
									//swap the messages to put them in order.
									Message replacement = newMessages.get(i);
									newMessages.set(i, temp);
									newMessages.set(j, replacement);
								}
								oldMessage = temp;
								break;
							}
						}
					}
				}
				Message newMessage = null;
				Activation newTarget = null;
				if (oldMessage != null) {
					newMessage = oldMessage;
					if (provider.isCall(messageElement)) {
						if (!(oldMessage instanceof Call)) {
							//remove this message from the current list and throw it
							//at the end as trash.
							newMessages.remove(i);
							newMessages.add(oldMessage);
							newMessage = null;
						}
					} else if (!(oldMessage instanceof Return)) {
						//remove this message from the current list and throw it
						//at the end as trash.
						newMessages.remove(i);
						newMessages.add(oldMessage);
						newMessage = null;
					}
					if (newMessage != null) {
						Activation oldTarget = newMessage.getTarget();
						if (oldTarget != null && !oldTarget.isDisposed()) {
							if (oldTarget.getData().equals(target)) {
								newTarget = oldTarget;
							} else {
								deleteSubTree(oldTarget);
							}
						}
					}

				}
				if (newMessage == null) {
					//have to create a new message.
					if (provider.isCall(messageElement)) {
						newMessage = new Call(getChart());
					} else {
						newMessage = new Return(getChart());
					}
					updateItem(newMessage, messageElement);
					if (i == newMessages.size()) {
						newMessages.add(newMessage);
					} else {
						newMessages.add(i, newMessage);
					}
				}
				if (newTarget == null) {
					if (newMessage instanceof Return) {
						//try and use the fastest ways to get a match.
						//first, check the immediate parent.
						if (a.getSourceCall() != null && target.equals(a.getSourceCall().getSource().getData())) {
							//found a match, no need to continue.
							newTarget = a.getSourceCall().getSource();
						} else 	{
							if (usingElementMap()) {
								//otherwise, check the map for a single match
								Widget[] widgets = findItems(target);
								if (widgets.length == 1 && widgets[0] instanceof Activation) {
									newTarget = (Activation)widgets[0];
								}
							}
							if (newTarget == null) {
								//finally, try and find a previous match on the call stack.
								Iterator<Activation> stackIterator = callStack.iterator();
								while (stackIterator.hasNext()) {
									Activation parent = stackIterator.next();
									if (parent.getData().equals(target)) {
										if (parent != a) {
											newTarget = parent;
										}
										break;
									}
								}
							}
						}
					} else {
						//create a new target to call.
						newTarget = new Activation(getChart());
						updateItem(newTarget, target);
						if (!isLazyLoading()) {
							activationsToLoad.add(newTarget);
						}
					}
				}
				if (newTarget != null) {
					//make sure that the lifeline is correct.
//					if (newTarget.getLifeline() == null) {
//						Object lifelineElement = provider.getLifeline(target);
//						Lifeline lifeline = (Lifeline) findItem(lifelineElement);
//						if (lifeline == null) {
//							lifeline = new Lifeline(getChart());
//							updateItem(lifeline, lifelineElement);
//						}
//						newTarget.setLifeline(lifeline);
//					}
					internalRefreashActivationLifeline(newTarget);
				}
				if (newMessage != null && newTarget != null) {
					if (!newMessage.isDisposed() && !newTarget.isDisposed()) {
						a.addMessage(i, newMessage, newTarget);
					}
				}
				
			}
			if (a.isDisposed()) {
				System.out.println("disposed here");
			}
			//remove the trash
			for (; i < newMessages.size(); i++) {
				Message m = newMessages.get(i);
				if (!m.isDisposed()) {
					if (m instanceof Call) {
						deleteSubTree(m.getTarget());
					}
					m.dispose();
				}
			}
			if (a.isDisposed()) {
				System.out.println("disposed here!");
			}
			refreshGroups(a);
			if (activationsToLoad.size() > 0) {
				callStack.addFirst(a);
				while (activationsToLoad.size() > 0) {
					internalRefreshActivation(activationsToLoad.removeFirst(), callStack);
				}
				callStack.removeFirst();
			}
		} finally {
			getChart().setRedraw(true);
		}

	}

		


	/**
	 * Checks the activation to see if it passes the filters.
	 * @param target
	 * @return
	 */
	private boolean selectActivation(Object target) {
		if (target == null) return false;
		ISequenceChartContentProvider provider = (ISequenceChartContentProvider) getContentProvider();
		if (select(target)) {
			return selectLifeline(provider.getLifeline(target));
		}
		return false;
	}

	/**
	 * @param lifeline
	 * @return
	 */
	private boolean selectLifeline(Object lifeline) {
		if (select(lifeline)) {
			if (getContentProvider() instanceof ISequenceContentExtension) {
				ISequenceContentExtension provider = (ISequenceContentExtension) getContentProvider();
				Object current = lifeline;
				while (provider.hasContainingGroup(current)) {
					if (select(current)) {
						current = provider.getContainingGroup(current);
					} else {
						return false;
					}
				}
				return true;
			}
			return true;
		}
		return false;
	}

	/**
	 * @param a
	 */
	private void internalRefreashActivationLifeline(Activation a) {
		ISequenceChartContentProvider provider = (ISequenceChartContentProvider) getContentProvider();
		
		//update the lifeline
		Object llElement = provider.getLifeline(a.getData());
		
		//make sure that the lifeline should be shown.
		if (!select(llElement)) {
			//delete this subtree, and all of the subtrees on the lifeline
			Widget w = findItem(llElement);
			//deleting all of the subtrees on the lifeline will result
			//in the deletion of the lifeline itself.
			if (!a.isDisposed()) {
				deleteSubTree(a);
			}
			if (w instanceof Lifeline && !w.isDisposed()) {
				Lifeline ll = (Lifeline) w;
				for (Activation a2 : ll.getAllActivations()) {
					if (!a2.isDisposed()) {
						deleteSubTree(a2);
					}
				}
			}
			return;
		}
		Lifeline ll = (Lifeline) findItem(llElement);
		if (ll == null) {
			ll = new Lifeline(getChart());
			updateItem(ll, llElement);
		}
		if (a.getLifeline() != ll) {
			if (a.getLifeline() != null) {
				a.setLifeline(null);
				if (ll.getAllActivations().length == 0) {
					//dispose the lifeline
					disassociate(a.getLifeline());
					a.getLifeline().dispose();
				}
			}

			a.setLifeline(ll);
		}
		if (provider instanceof ISequenceContentExtension) {
			internalRefreshLifelineGroup(a.getLifeline());
		}
	}

	/**
	 * Walks up the parent three of the given lifeline to refresh its groups
	 * @param lifeline
	 */
	private void internalRefreshLifelineGroup(Lifeline lifeline) {
		ISequenceContentExtension provider = (ISequenceContentExtension) getContentProvider();
		Object element = lifeline.getData();
		if (provider.hasContainingGroup(element)) {
			Object parentElement = provider.getContainingGroup(element);
			if (parentElement != null) {
				if (lifeline.getParent() == null || !lifeline.getParent().getData().equals(parentElement)) {
					//create a new lifeline group for this lifeline
					Lifeline oldParent = lifeline.getParent();
					Lifeline group = (Lifeline) findItem(parentElement);
					if (group == null) {
						group = new Lifeline(getChart());
					}
					group.addChild(lifeline);
					if (oldParent != null && oldParent.getAllActivations().length == 0) {
						//delete the group
						disassociate(group);
						group.dispose();
					}
					updateItem(group, parentElement);
					internalRefreshLifelineGroup(group);
				}
			}
		}
	}

	/**
	 * Disposes the given activation, all of its messages, and all of the activations called by it.
	 * If the activation's lifeline is empty after this operation, the lifeline is disposed as well.
	 * @param a
	 */
	private void deleteSubTree(Activation a) {
		if (a == null || a.isDisposed()) return;
		Lifeline ll = a.getLifeline();
		Message[] messages = a.getMessages();
		for (Message m : messages) {
			if (m instanceof Call) {
				deleteSubTree(m.getTarget());
			}
			if (!m.isDisposed()) {
				disassociate(m);
				m.dispose();
			}
		}
		//delete the calling message
		if (a.getSourceCall() != null && !a.getSourceCall().isDisposed()) {
			disassociate(a.getSourceCall());
			a.getSourceCall().dispose();
		}
		disassociate(a);
		a.dispose();
		if (ll != null && !ll.isDisposed() && ll.getAllActivations().length == 0) {
			disassociate(ll);
			ll.dispose();
		}
	}
	
	/**
	 * Uses the IMessageGrouper set on this viewer to refresh the groups on the given activation.
	 * It is expected that the activation is refreshed and has all of its messages
	 * set.
	 * @param a the activation to refresh.
	 */
	private void refreshGroups(Activation a) {
		try {
			getChart().setRedraw(false);	

			IMessageGrouper grouper = getMessageGrouper();
			if (grouper == null) {
				return;
			}
			Object element = a.getData();
			Message[] messages = a.getMessages();
			Object[] messageElements = new Object[messages.length];
			for (int i = 0; i < messages.length; i++) {
				messageElements[i] = messages[i].getData();
			}
			MessageGroup[] oldGroups = a.getMessageGroups();
			HashMap<IMessageGrouping, MessageGroup> groupingWidgetMap = new HashMap<IMessageGrouping, MessageGroup>();
			for (int i = 0; i < oldGroups.length; i++) {
				Object groupData = oldGroups[i].getData();
				if (!(groupData instanceof IMessageGrouping)) {
					throw new IllegalArgumentException("Message grouping set outside of the viewer");
				} else {
					groupingWidgetMap.put((IMessageGrouping)groupData, oldGroups[i]);
				}
			}
			IMessageGrouping[] newGroupings = getMessageGrouper().calculateGroups(this,element,messageElements);
			for (int i = 0; i < newGroupings.length; i++) {
				IMessageGrouping newGrouping = newGroupings[i];
				MessageGroup groupWidget = groupingWidgetMap.get(newGrouping);
				if (groupWidget == null) {
					groupWidget = new MessageGroup(getChart());
					associate(newGrouping, groupWidget);
				} else {
					groupingWidgetMap.remove(newGrouping);
				}
				groupWidget.setRange(a, newGrouping.getOffset(), newGrouping.getLength());
				groupWidget.setText(newGrouping.getName());
				groupWidget.setForeground(newGrouping.getForeground());
				groupWidget.setBackground(newGrouping.getBackground());
			}
			//delete all the unused groups
			for (Iterator<IMessageGrouping> it = groupingWidgetMap.keySet().iterator(); it.hasNext();) {
				IMessageGrouping key = it.next();
				MessageGroup widget = groupingWidgetMap.get(key);
				disassociate(widget);
				widget.dispose();
			}
		} finally {
			chart.setRedraw(true);
		}
	}
	
	/**
	 * Returns the message grouper set on this viewer.
	 * @return the message grouper set on this viewer.
	 */
	public IMessageGrouper getMessageGrouper() {
		return grouper;
	}
	
	/**
	 * Sets the message grouper on this viewer. To save on execution time, it is
	 * suggested that the message grouper be set before the input is set on the viewer.
	 * Setting the message grouper will force a full refresh of all of the activation
	 * groups currently on the chart. The earlier the grouper is set, the quicker this
	 * operation will be.
	 * @param grouper the new grouper.
	 */
	public void setMessageGrouper(IMessageGrouper grouper) {
		if (this.grouper != null) {
			this.grouper.dispose();
		}
		this.grouper = grouper;
		if (getInput() != null) {
			//need to refresh.
			try {
				getChart().setRedraw(false);
				UMLItem[] items = getChart().getItems();
				for (int i = 0; i < items.length; i++) {
					if (items[i] instanceof Activation) {
						refreshGroups((Activation)items[i]);
					}
				}
			} finally {
				getChart().setRedraw(true);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#reveal(java.lang.Object)
	 */
	@Override
	public void reveal(final Object element) {
		//put it on a work queue to ensure that any layout, etc. is done first.
		getJobProcessor().runInUIThread(new AbstractSimpleProgressRunnable() {
			
			@Override
			protected void doRunInUIThread(SimpleProgressMonitor monitor)
					throws InvocationTargetException {
				monitor.beginTask("Revealing item", IUIProgressService.UNKNOWN_WORK);
				Widget item = findItem(element);
				if (item instanceof UMLItem) {
					getChart().reveal((UMLItem)item);
				}
				monitor.done();
				
			}
		}, getChart().getDisplay(), false);
		
	}
	
	/**
	 * Checks to see if the given element currently exists and is visible in
	 * the viewer.
	 * @param element the element to check
	 * @return true if the given element exists and is visible in the viewer.
	 */
	public boolean isVisible(Object element) {
		Widget item = findItem(element);
		if (item instanceof UMLItem) {
			UMLItem umlItem = (UMLItem) item;
			return umlItem.isVisible() && !umlItem.isHidden(); 
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#setSelectionToWidget(java.util.List, boolean)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void setSelectionToWidget(List l, boolean reveal) {
		List<UMLItem> items = new ArrayList<UMLItem>();
		for (Object o : l) {
			Widget[] widgets = findItems(o);
			for (Widget widget : widgets) {
				if (widget instanceof UMLItem) {
					items.add((UMLItem)widget);
				}
			}
		}
		if (reveal && l.size() > 0) {
			getChart().reveal(items.get(0));
		}
		getChart().setSelection(items.toArray(new UMLItem[items.size()]));
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	@Override
	public Control getControl() {
		return chart;
	}
	
	public UMLSequenceChart getChart() {
		return chart;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#assertContentProviderType(org.eclipse.jface.viewers.IContentProvider)
	 */
	@Override
	protected void assertContentProviderType(IContentProvider provider) {
		assert(provider instanceof ISequenceChartContentProvider);
	}
	
	private boolean isLazyLoading() {
		return (chart.getStyle() & SWT.VIRTUAL) != 0;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#inputChanged(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void inputChanged(Object input, Object oldInput) {
		refresh();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#handleDispose(org.eclipse.swt.events.DisposeEvent)
	 */
	@Override
	protected void handleDispose(DisposeEvent event) {
		super.handleDispose(event);
		if (getMessageGrouper() != null) {
			getMessageGrouper().dispose();
		}
		sequenceListeners.clear();
	}
	
	/**
	 * Returns an array of elements that passed the viewer's filters.
	 * @param elements the elements to filter.
	 * @return an array of elements that passed the viewer's filters.
	 */
	protected Object[] filter(Object[] elements) {
		LinkedList<Object> filtered = new LinkedList<Object>();
		for (Object e : elements) {
			if (select(e)) {
				filtered.add(e);
			}
		}
		return filtered.toArray();
	}

	/**
	 * Checks the filters to see if the given object has passed. Returns true if there
	 * are no filters.
	 * @param e the element to check
	 * @return true if there are no filters on this viewer, or the element passes the filters.
	 */
	private boolean select(Object e) {
		ViewerFilter[] filters = getFilters();
		Object root = getRoot();
		for (int j = 0; j < filters.length; j++) {
			if (!((ViewerFilter) filters[j]).select(this, root, e)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Sets the expanded states for all items that represent the given element to
	 * the expanded state (if they are expandable). This method is effective for
	 * activations returned by the content provider and message groups returned by
	 * the supplied message grouper.
	 * @param element the element to set the expanded state to
	 * @param expanded the new expanded state
	 * @see setMessageGrouper
	 */
	public void setExpanded(Object element, boolean expanded) {
		Widget[] items = findItems(element);
		try {
			chart.setRedraw(false);
			for (Widget item : items) {
				if (item instanceof IExpandableItem) {
					((IExpandableItem)item).setExpanded(expanded);
				}
			}
		} finally {
			chart.setRedraw(true);
		}

	}

	/**
	 * Returns the element found at the given point in the chart.
	 * @param x the x location of the point.
	 * @param y the y location of the point.
	 * @return the element found at the point, or null if none.
	 */
	public Object elementAt(int x, int y) {
		Widget w = chart.getItemAt(x,y);
		if (w != null && !w.isDisposed()) {
			return w.getData();
		}
		return null;
	}

	/**
	 * Sets the focus to the activation represented by the given object. If the object
	 * doesn't represent an activation, this method does nothing.
	 * @param activation the element to focus on.
	 */
	public void setRootActivation(Object activation) {
		if (activation == null) return;
		Widget[] items = findItems(activation);
		for (Widget item : items) {
			if (item instanceof Activation) {
				if (item.isDisposed()) {
					unmapElement(activation, item);
				} else {
					getChart().setRootActivation((Activation)item);
					return;
				}
				
			}
		}
	}

	/**
	 * Returns the element representing the root activation, or null if not present.
	 * @return the element representing the root activation, or null if not present.
	 */
	public Object getRootActivation() {
		Activation root = getChart().getRootActivation();
		if (root != null && !root.isDisposed()) {
			return root.getData();
		}
		return null;
	}
	
	/**
	 * Adds the given listener to the list of sequence listeners.
	 * @param listener the listener to add.
	 */
	public void addSequenceListener(ISequenceViewerListener listener) {
		sequenceListeners.add(listener);
	}
	
	/**
	 * Removes the given listener from the list of sequence listeners.
	 * @param listener the listener to remove.
	 */
	public void removeSequenceListener(ISequenceViewerListener listener) {
		sequenceListeners.remove(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#setContentProvider(org.eclipse.jface.viewers.IContentProvider)
	 */
	@Override
	public void setContentProvider(IContentProvider provider) {
		super.setContentProvider(provider);
		if (!(provider instanceof ISequenceContentExtension)) {
			getChart().setLifelineGroupsVisible(false);
		} else {
			getChart().setLifelineGroupsVisible(true);
		}
	}
	
	
	/**
	 * Expands all activations in the chart. Updates on the chart will
	 * be dispabled during the expansion, so that the user isn't bothered by unnessary re-layouts.
	 * @param enforceLimit set to true if there should be a limit to the amount of time that is used to expand
	 * children. This is highly recommended, as some sequence charts may have infinite recursions and not
	 * enforcing a limit may never terminate.
	 */
	public void expandAllActivations(boolean enforceLimit) {
		expandActivationsUnder(((ISequenceChartContentProvider)getContentProvider()).getElements(getInput())[0], enforceLimit);
	}
	
	
	/**
	 * Expands the given activations, and all of its sub-activations.
	 * @param activationElement the activation to expand.
	 * @param enforceLimit set to true if there should be a limit to the amount of time that is used to expand
	 * children. This is highly recommended, as some sequence charts may have infinite recursions and not
	 * enforcing a limit may never terminate.
	 */
	public void expandActivationsUnder(final Object activationElement, boolean enforceLimit) {
		getJobProcessor().runInUIThread(
			new ExpandActivationsUnderRunnable(activationElement, enforceLimit), 
			getChart().getDisplay(),
			true
		);
	}
	
	/**
	 * Expands all of the activations within the given lifeline. Only activations that are currently
	 * reachable from the root are expanded. No attempt is made to search the contents for new
	 * activations that may be on the lifeline given different expansion states. Such an effort may
	 * produce an infinite loop, and so is avoided. If #enforceLimit is set to true, then the process
	 * will warn the user after a period of time, or a large number of new activations have been reached.
	 * It is recommended that limit is always set to true in order to avoid large graphs. 
	 * @param lifelineElement the lifeline to expand in.
	 * @param enforceLimit set to true to warn users when the chart might be getting too large to display efficiently.
	 */
	public void expandActivationsIn(Object lifelineElement, boolean enforceLimit) {
		getJobProcessor().runInUIThread(
				new ExpandActivationsInRunnable(lifelineElement, enforceLimit), 
				getChart().getDisplay(),
				true
			);		
	}
	
	/**
	 * Collapses all of the activations within the givin lifeline. Only activations that are currently
	 * reachable from the root are collapsed. No attempt is made to search the contents for
	 * activations that may be reacable given different expansion states.
	 * @param lifelineElement
	 */
	public void collapseActivationsIn(Object lifelineElement) {
		getJobProcessor().runInUIThread(
				new CollapseActivationsInRunnable(lifelineElement), 
				getChart().getDisplay(),
				true
			);	
	}

	/**
	 * Collapses all activations in the chart. Updates on the chart will
	 * be disabled during the collapse, so that the user isn't bothered by unnecessary re-layouts.
	 *
	 */
	public void collapseAllActivations() {
		collapseActivationsUnder(((ISequenceChartContentProvider)getContentProvider()).getElements(getInput())[0]);
	}
	
	
	/**
	 * Collapses the given activations, and all of its sub-activations.
	 * @param activationElement the activation to collapse.
	 */
	public void collapseActivationsUnder(final Object activationElement) {
		getJobProcessor().runInUIThread(new CollapseActivationsUnderRunnable(
				activationElement), getChart().getDisplay(),
				true);
	}

	/**
	 * Returns the expanded state of the given element. Always returns false for elements that
	 * are not expandable (eg. messages).
	 * @param element the expanded state of the given element. Always returns false for elements that
	 * are not expandable (eg. messages).
	 * @return 
	 */
	public boolean getExpanded(Object element) {
		Widget[] items = findItems(element);
		if (items != null && items.length > 0) {
			//just choose the first item. There is really no better way to do it.
			if (!items[0].isDisposed() && items[0] instanceof IExpandableItem) {
				return ((IExpandableItem)items[0]).isExpanded();
			}
		}
		return false;
	}

	/**
	 * Sets the expansion state for the given message grouping. If the grouping doesn't currently exist
	 * int the chart, no state is changed.
	 * @param group the grouping to set.
	 * @param expanded the new expanded state
	 */
	public void setGroupingExpanded(IMessageGrouping grouping, boolean expanded) {
		Widget[] items = findItems(grouping);
		if (items != null) {
			for (Widget item : items) {
				if (item instanceof MessageGroup) {
					((MessageGroup)item).setExpanded(expanded);
				}
			}
		}
		
	}

	/**
	 * Returns the filtered message groupings for the given activation. If the activation element
	 * doesn't currently exist in the chart, the grouper supplied to the viewer is queried to see
	 * what the groupings would be if the activation element did exist in the chart. For this reason,
	 * clients should not expect that the groupings returned will have object identity with the groupings
	 * that may or may not exist in the chart.
	 * @param activationElement the element to check.
	 * @return the groupings on the element.
	 */
	public IMessageGrouping[] getMessageGroupingsFor(Object activationElement) {
		Widget[] items = findItems(activationElement);
		List<IMessageGrouping> groupings = null;
		if (items != null && items.length > 0) {
			for (Widget item : items) {
				if (item instanceof Activation) {
					groupings = new LinkedList<IMessageGrouping>();
					for (MessageGroup group : ((Activation)item).getMessageGroups()) {
						groupings.add((IMessageGrouping)group.getData());
					}
					break;
				}
			}
		}
		if (groupings == null) {
			IMessageGrouper grouper = getMessageGrouper();
			Object[] messages = ((ISequenceChartContentProvider)getContentProvider()).getMessages(activationElement);
			if (messages != null) {
				messages = filter(messages);
				return grouper.calculateGroups(this, activationElement, messages);
			}	
		}
		return new IMessageGrouping[0];
	}
	
	private UIJobProcessor getJobProcessor() {
		return (UIJobProcessor) chart.getData("jobs");
	}

	public void updateAll() {
		getChart().setRedraw(false);
		UMLItem[] items = getChart().getItems();
		for (UMLItem item : items) {
			updateItem(item, item.getData());
		}
		getChart().setRedraw(true);
	}
	
	
	
}
