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
package ca.uvic.chisel.javasketch.data.model.imple.internal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

import ca.uvic.chisel.javasketch.data.internal.IActivationTable;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.ITargetMessage;
import ca.uvic.chisel.javasketch.data.model.ITraceEvent;
import ca.uvic.chisel.javasketch.data.model.ITraceEventListener;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;

class EventAgrigatorTask extends TimerTask {
	private HashMap<TraceImpl, LinkedList<TraceEvent>> aggrigator;
	private HashMap<TraceImpl, LinkedList<ITraceEventListener>> listeners;
	
	EventAgrigatorTask() {
		aggrigator = new HashMap<TraceImpl, LinkedList<TraceEvent>>();
		listeners = new HashMap<TraceImpl, LinkedList<ITraceEventListener>>();
	}
	
	
	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		
		HashMap<TraceImpl, List<TraceEvent>> clone;
		synchronized (aggrigator) {
			clone = new HashMap<TraceImpl, List<TraceEvent>>();
			for (TraceImpl trace : aggrigator.keySet()) {
				List<TraceEvent> events = aggrigator.get(trace);
				clone.put(trace, new LinkedList<TraceEvent>(events));
				events.clear();
			}
		}
		for (TraceImpl trace : clone.keySet()) {
			List<TraceEvent> events = clone.get(trace);
			if (events.size() > 0) {
				synchronized (listeners) {
					List<ITraceEventListener> list = listeners.get(trace);
					if (list != null) {
						ITraceEvent[] array = events.toArray(new ITraceEvent[events.size()]);
						for (ITraceEventListener listener : list) {
							listener.handleEvents(array);
						}
					}
				}
			}
		}
			
		
	}
	
	/**
	 * Aggrigates changes in the database into basic events that may be fired periodically.
	 * @param trace
	 * @param columns
	 * @return the events that exist currently for the trace, and have not yet been fired.
	 */
	public void aggrigate(TraceImpl trace, Object[] columns) {
		LinkedList<TraceEvent> events;
		synchronized (aggrigator) {
			events = aggrigator.get(trace);
			if (events == null) {
				events = new LinkedList<TraceEvent>();
				aggrigator.put(trace, events);
			}
			IActivation activation = findActivation(trace, columns);
			boolean threadAffected = isThreadAffected(trace, columns);
			ActivationEvent activationEvent = null;
			TypeEvent typeEvent = null;
			ThreadEvent threadEvent = null;
			MethodEvent methodEvent = null;
			for (TraceEvent event : events) {
				if (event instanceof ActivationEvent) {
					activationEvent = (ActivationEvent) event;
				} else if (event instanceof TypeEvent) {
					typeEvent = (TypeEvent) event;
				} else if (event instanceof ThreadEvent) {
					threadEvent = (ThreadEvent) event;
				} else if (event instanceof MethodEvent) {
					methodEvent = (MethodEvent) event;
				}
			}
			if (isClassAffected(trace, columns)) {
				if (typeEvent == null) {
					typeEvent = new TypeEvent(trace);
					events.add(typeEvent);
				}
				String className = getClassName(trace, columns);
				if (typeEvent.addClassName(className)) {
					trace.classesChanged();
				}
			}
			if (isMethodAffected(trace, columns)) {
				String methodID = getMethodID(trace, columns);
				if (methodEvent == null) {
					methodEvent = new MethodEvent(trace);
					events.add(methodEvent);
				}
				if (methodEvent.addMethod(methodID)) {
					trace.methodsChanged(getClassName(trace, columns));
				}
			}
			if (threadAffected) {
				if (threadEvent == null) {
					threadEvent = new ThreadEvent(trace);
					events.add(threadEvent);
					trace.threadsChanged();
				}
			}
			if (activation != null) {
				if (activationEvent == null) {
					activationEvent = new ActivationEvent(trace);
					events.add(activationEvent);
				}
				if (activationEvent.addActivation(activation)) {
					trace.activationChanged(activation);
				}
			}
		}
		
	}


	/**
	 * @param trace
	 * @param columns
	 * @return
	 */
	private boolean isThreadAffected(TraceImpl trace, Object[] columns) {
		if (columns[IActivationTable.THREAD_ID-1] instanceof Long) {
			Long threadID = (Long) columns[IActivationTable.THREAD_ID-1];
			if (trace.findElement("[THREAD],"+threadID) == null) {
				return true;
			}
				
		}
		return false;
	}


	/**
	 * @param trace
	 * @param columns
	 * @return
	 */
	private boolean isMethodAffected(TraceImpl trace, Object[] columns) {
		String className = getClassName(trace, columns);
		//only return true if the class already exists in the trace.
		if (!isClassAffected(trace, columns)) {
			String methodID = getMethodID(trace, columns);
			return (trace.findElement("[MEATHOD],"+className +"."+ methodID) == null);
		}
		return false;
	}


	/**
	 * @param trace
	 * @param columns
	 * @return
	 */
	private String getMethodID(TraceImpl trace, Object[] columns) {
		return getClassName(trace, columns) + "." + ((String)columns[IActivationTable.METHOD_NAME-1]) + 
			((String)columns[IActivationTable.METHOD_SIGNATURE-1]);
	}


	/**
	 * @param trace
	 * @param columns
	 * @return
	 */
	private boolean isClassAffected(TraceImpl trace, Object[] columns) {
		String className = getClassName(trace, columns);
		return (trace.findElement("[TRACECLASS],"+className) == null);
	}


	/**
	 * @param trace
	 * @param columns
	 * @return
	 */
	private String getClassName(TraceImpl trace, Object[] columns) {
		return ((String)columns[IActivationTable.THIS_TYPE-1]);
	}


	/**
	 * @param trace
	 * @param columns
	 * @return
	 */
	private IActivation findActivation(TraceImpl trace, Object[] columns) {
		if (columns[IActivationTable.ARRIVAL_ID-1] instanceof Long) {
			Long arrivalID = (Long) columns[IActivationTable.ARRIVAL_ID-1];
			ITraceModel arrival = trace.findElement("[MESSAGE],"+arrivalID);
			if (arrival instanceof ITargetMessage) {
				IOriginMessage origin = ((ITargetMessage)arrival).getOrigin();
				if (origin != null) {
					return origin.getActivation();
				} else {
					return ((ITargetMessage)arrival).getActivation();
				}
			}
				
		}
		return null;
	}
	
	public void addListener(TraceImpl trace, ITraceEventListener listener) {
		synchronized (listeners) {
			LinkedList<ITraceEventListener> list = listeners.get(trace);
			if (list == null) {
				list = new LinkedList<ITraceEventListener>();
				listeners.put(trace, list);
			}
			if (!list.contains(listener)) {
				list.add(listener);
			}
		}
	}
	
	public void removeListener(TraceImpl trace, ITraceEventListener listener) {
		synchronized (listeners) {
			LinkedList<ITraceEventListener> list = listeners.get(trace);
			if (list != null) {
				list.remove(listener);
				if (list.size() == 0) {
					listeners.remove(trace);
				}
			}
			
		}
	}


	/**
	 * @param traceImpl
	 */
	public void removeListeners(TraceImpl trace) {
		synchronized (listeners) {
			listeners.remove(trace);
		}
		synchronized (aggrigator) {
			aggrigator.remove(trace);
		}
	}
	
}