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
package ca.uvic.chisel.javasketch.persistence.internal;

import static ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent.METHOD_ENTERED;
import static ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent.METHOD_EXITED;
import static ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent.PAUSED;
import static ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent.RESUMED;
import static ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent.THREAD_INIT;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.SketchDataPlugin;
import ca.uvic.chisel.javasketch.data.internal.DataUtils;
import ca.uvic.chisel.javasketch.data.internal.WriteDataUtils;
import ca.uvic.chisel.javasketch.data.model.imple.internal.TraceImpl;
import ca.uvic.chisel.javasketch.internal.DBProgramSketch;
import ca.uvic.chisel.javasketch.launching.ITraceClient;
import ca.uvic.chisel.javasketch.persistence.internal.logs.MethodEnterEvent;
import ca.uvic.chisel.javasketch.persistence.internal.logs.MethodEvent;
import ca.uvic.chisel.javasketch.persistence.internal.logs.MethodExitEvent;
import ca.uvic.chisel.javasketch.persistence.internal.logs.PauseEvent;
import ca.uvic.chisel.javasketch.persistence.internal.logs.ResumeEvent;
import ca.uvic.chisel.javasketch.persistence.internal.logs.ThreadInitEvent;
import ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLog;
import ca.uvic.chisel.javasketch.persistence.internal.logs.TraceLogEvent;

/**
 * A simple job that persists all the trace files into a database. It is assumed
 * that the sketch is completed, and not currently running. If it is running,
 * then this job will quit with an error.
 * 
 * @author Del Myers
 * 
 */
public class PersistTraceJob extends Job {
	
	private final String KEY_FILTER_DECORATION = "filter";
	private final String filtered = "fil";
	private final String filterStart = "fils";
	private final String triggered = "trig";
	private final String triggerEnd = "trige";
	private String[] inclusionFilters = null;
	private String[] exclusionFilters = null;
	
	
	private class TracePauseResumeListener implements IDebugEventSetListener {

		/* (non-Javadoc)
		 * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(org.eclipse.debug.core.DebugEvent[])
		 */
		@Override
		public void handleDebugEvents(DebugEvent[] events) {
			for (DebugEvent event : events) {
				if (event.getSource().equals(client)) {
					if (event.getKind() == DebugEvent.TERMINATE) {
						synchronized (this) {
							notifyAll();
						}
					} else if (event.getKind() == DebugEvent.MODEL_SPECIFIC) {
						if (event.getDetail() == ITraceClient.TRACE_PAUSED) {
							synchronized (this) {
								notifyAll();
							}
						}
					}
				}
			}
		}
		
	}
	
	private static enum ModelEventType {
		Thread,
		Call,
		Arrival,
		Activation,
		Reply,
		Return,
		Throw,
		Catch
	}
	/**
	 * Used for storing an internal "call stack" for each thread. This way, the returns
	 * and catches can be resolved while processing the events.
	 * @author Del Myers
	 *
	 */
	private class TraceModelEvent {
		public final ModelEventType eventType;
		public final long modelId;
		private HashMap<String, Object> decorations;
		public TraceModelEvent(ModelEventType eventType, long modelId) {
			this.eventType = eventType;
			this.modelId = modelId;
			decorations = new HashMap<String, Object>();
		}
		public void decorate(String key, Object value) {
			decorations.put(key, value);
		}
		public Object getDecoration(String key) {
			return decorations.get(key);
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return eventType.name() + ":" + modelId;
		}
	}
	
	private class ThreadLogReader {
		private LinkedList<TraceModelEvent> eventStack;
		private LinkedList<MethodEnterEvent> callEventStack;
		private TraceLog log;
		private long mark;
		private boolean finished;
		public int accessorLineNumber = -1;
		private boolean failed = false;
		//the time of the first fail, to calculate whether we should give up
		private long failure = 0;
		
		public ThreadLogReader(File file) throws IOException {
			log = new TraceLog(file);
			eventStack = new LinkedList<TraceModelEvent>();
			callEventStack = new LinkedList<MethodEnterEvent>();
		}
		
		public void mark() {
			this.mark = log.getReadLocation();
		}
		
		public long getMark() {
			return mark;
		}
		
		public long getReadLocation() {
			return log.getReadLocation();
		}
		
		public long getTraceLength() {
			return log.getTraceLength();
		}
		
		/**
		 * Process the next event in the log, if one can be read. Returns false
		 * if no event was read, or the end of the log has been reached. If blockOnRead
		 * is set, then the reader will block until the next event can be read, or an
		 * end of file is reached. Otherwise, it will only proccess an event given that
		 * one is available.
		 * @param blockOnRead 
		 * @return
		 * @throws CoreException 
		 * @throws SQLException 
		 */
		public boolean processNextEvent(boolean blockOnRead) throws CoreException {
			if (log.isComplete() || (!blockOnRead && !log.available()))
				return false;
			try {
				TraceLogEvent event = null;
				try {
					event = log.nextEvent();
				} catch (IOException ioe) {
					closeThread(eventStack);
					log.close();
					return false;
				}
				if (event != null) {
					failed = false;
					failure = 0;
					processEvent(eventStack, callEventStack, event, this);
				} else {
					if (isClientFinished()) {
						//failed read, save it and quit
						if (!failed) {
							failed = true;
							failure = System.currentTimeMillis();
						} else {
							if ((System.currentTimeMillis() - failure) > 10000) {
								//give up to 10 seconds to try again, or quit.
								closeThread(eventStack);
								log.close();
							}
						}
					}
					return false;
				}
			} catch (Exception e) {
				try {
					log.close();
				} catch (IOException e1) {
					throw new CoreException(SketchPlugin.getDefault().createStatus(e1));
				}
				throw new CoreException(SketchPlugin.getDefault().createStatus(e));
			}
			return true;
		}
		
		public void cancel() throws IOException {
			log.close();
		}
		
		public boolean isFinished() {
			return log.isComplete() || finished;
		}

		/**
		 * @throws SQLException 
		 * @throws CoreException 
		 * 
		 */
		public void finish() throws CoreException, SQLException {
			closeThread(eventStack);
			try {
				log.close();
			} catch (IOException e) {
				throw new CoreException(SketchPlugin.getDefault().createStatus(e));
			}
			finished = true;
		}
	}

	private IProgramSketch sketch;
	private WriteDataUtils queryUtils;
	private ITraceClient client;

	public PersistTraceJob(IProgramSketch sketch) {
		super("Storing Trace Data: " + sketch.getProcessName());
		this.sketch = sketch;
		setRule(sketch.getRule());
		setPriority(LONG);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//first clear the sketch
		try {
			IProgramSketch active = SketchPlugin.getDefault().getActiveSketch();
			if (active != null && sketch.equals(active)) {
				SketchPlugin.getDefault().setActiveSketch(null);
			}
			((DBProgramSketch)sketch).reset();
		} catch (CoreException e1) {
			return e1.getStatus();
		}
		int totalWork = 7500000;
		int indexWork = 2500000;
		double workDone = 0;
		monitor.beginTask("Storing Trace Data", totalWork + indexWork);
		// create a database to store everything in
		if (queryUtils != null) {
			try {
				queryUtils.close();
			} catch (SQLException e) {
				return new Status(Status.ERROR, SketchDataPlugin.PLUGIN_ID, "Error closing database connection", e);
			}
		}
		TracePauseResumeListener pauseResumeListener = new TracePauseResumeListener();
		DebugPlugin.getDefault().addDebugEventListener(pauseResumeListener);
		HashMap<File, ThreadLogReader> readers = new HashMap<File, ThreadLogReader>();
		try {
			queryUtils = (WriteDataUtils) ((TraceImpl)sketch.getTraceData()).getDataUtils();
			queryUtils.getConnection().setAutoCommit(false);
			boolean done = false;
			
			client = sketch.getTracer();
			long events = 0;
			long totalFileLength = -1;
			
			//initialize the readers.
			getNewTracedThreads(readers);
			while (!done) {
				
				getNewTracedThreads(readers);
				boolean clientFinished = isClientFinished();
				ThreadLogReader[] readerArray = readers.values().toArray(new ThreadLogReader[0]);
				//calculate the total file length so that we can get an 
				//accurate amount of work that has been completed.
				if (clientFinished && totalFileLength < 0) {
					totalFileLength = 0;
					long readLength = 0;
					for (ThreadLogReader r: readerArray) {
						totalFileLength += r.getTraceLength();
						if (r.isFinished()) {
							readLength += r.getTraceLength();
						} else {
							readLength += r.getReadLocation();
						}
					}
					if (readLength > 0) {
						workDone = ((double)readLength)/totalFileLength;
					}
					//get the monitor caught up.
					monitor.worked((int)(workDone*totalWork));
				}
				boolean isComplete = true;
				for (ThreadLogReader reader : readerArray) {
					
					//mark the last spot read.
					reader.mark();
					if (monitor.isCanceled()) {
						reader.cancel();
						break;
					}
					if (clientFinished) {
						//process each reader until it is complete because
						//it is slightly faster than processing one event
						//at a time
						while (reader.processNextEvent(clientFinished)) {
							events++;
							monitor.subTask("(" + events + ") events");
							long mark = reader.getMark();
							long amountRead = reader.getReadLocation()-mark;
							double worked = ((double)amountRead/totalFileLength)*totalWork;
							if (worked > 0) {
								//mark for the next read, and update the monitor
								reader.mark();
								monitor.worked((int)worked);
							}
							//don't force the issue
							if (monitor.isCanceled()) {
								break;
							}
						}
					} else {
						if (client != null && !(client.isPaused() || client.isTerminated())) {
							//wait a bit, and reset
							synchronized (pauseResumeListener) {
								pauseResumeListener.wait(5000);
								break;
							}
						} else {
							//process one event from each reader, only if
							//the trace is paused
							if (reader.processNextEvent(clientFinished)) {
								events++;
								monitor.subTask("(" + events + ") events");
							}
						}
						
					}
					isComplete &= reader.isFinished();
				}
				done = (monitor.isCanceled() || isComplete & clientFinished);
			}
			
			//store the views.
			monitor.subTask("Indexing methods and types...");
			for (ThreadLogReader reader : readers.values()) {
				reader.finish();
			}
			queryUtils.commit();
			queryUtils.storeViews();
			queryUtils.compact();
			queryUtils.getConnection().setAutoCommit(true);
			monitor.worked(indexWork);
		} catch (InterruptedException e){
			Thread.interrupted();
			monitor.setCanceled(true);
			clearData();
		} catch (CoreException e) {
			monitor.setCanceled(true);
			clearData();
			return e.getStatus();
		} catch (Exception e) {
			e.printStackTrace();
			return new Status(Status.ERROR, SketchPlugin.PLUGIN_ID,
					"Fatal Error Running Persist Job", e);
		} finally {
			for (ThreadLogReader reader : readers.values()) {
				if (!reader.log.isComplete()) {
					try {
						reader.log.close();
					} catch (IOException e) {
						SketchPlugin.getDefault().log(e);
					}
				}
			}
			DebugPlugin.getDefault().removeDebugEventListener(pauseResumeListener);
		}
		if (monitor.isCanceled()) {
			clearData();
			return new Status(IStatus.WARNING, SketchPlugin.PLUGIN_ID,
					"Unable to complete file processing. Database rolled-back.");
		}
		monitor.done();
		return Status.OK_STATUS;
	}

	/**
	 * @return
	 */
	protected boolean isClientFinished() {
		return (client == null || (client.isTerminated() && client.getLaunch().isTerminated()));
	}

	/**
	 * @param readers2 
	 * @return
	 * @throws IOException
	 */
	private void getNewTracedThreads(HashMap<File, ThreadLogReader> readers)
			throws IOException {
		File[] files = getTraceFiles();
		for (File file : files) {
			ThreadLogReader reader = readers.get(file);
			if (reader == null) {
				reader = new ThreadLogReader(file);
				readers.put(file, reader);
			}
		}
	}


	
	/**
	 * 
	 */
	private void clearData() {
		// TODO Auto-generated method stub

	}

	/*
	 * Processes the file.
	 * 
	 * @param file
	 * @param monitor
	 * @throws CoreException
	 *
	private void process(String taskName, File file, IProgressMonitor monitor)
			throws CoreException {
		
		//source.getResourceSet().getR
		
	// read the file one line at a time, processing each event.
		try {
			TraceLog log = new TraceLog(file);
			// figure out how many lines are in the file
			int worked = 0;
			monitor.beginTask(taskName, (int)log.getTraceLength());
			//this contains a list of the model ids for
			LinkedList<TraceModelEvent> eventStack = new LinkedList<TraceModelEvent>();
			List<MethodEnterEvent> callEventStack = new LinkedList<MethodEnterEvent>();
			TraceLogEvent event = null;
			long lastPosition = log.getReadLocation();
			int eventCount = 0;
			while ((event = log.nextEvent()) != null) {
				processEvent(eventStack, callEventStack, event);
						
				if (monitor.isCanceled()) {
					break;
				}
				long position = log.getReadLocation();
				monitor.worked((int)(position-lastPosition));
				monitor.subTask(file.getName() + " (" +eventCount+" events)");
				eventCount++;
				lastPosition = position;
			}
			closeThread(eventStack);
			monitor.worked((int)(file.length()-worked));
			monitor.done();
			queryUtils.commit();
		} catch (IOException e) {
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Error reading trace file", e));
		} catch (SQLException e) {
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Error accessing database", e));
		}
		
	}
*/
	/**
	 * Finishes off the thread by adding a final reply to the top activation.
	 * @param eventStack 
	 * @throws CoreException 
	 * @throws SQLException 
	 * 
	 */
	private void closeThread(LinkedList<TraceModelEvent> eventStack) throws CoreException, SQLException {
		Long time = -1L;
		long longestTime = -1;
		//move up the call chain, sending a return for each call.
		while (eventStack.size() > 3) {
			TraceModelEvent parentActivation = eventStack.removeLast();
			time = (Long)parentActivation.getDecoration("end");
			if (time > longestTime) {
				longestTime = time;
			}
			TraceModelEvent parentArrival = eventStack.removeLast();
			TraceModelEvent parentCall = eventStack.removeLast();
			TraceModelEvent callingActivation = eventStack.getLast();
			if (parentActivation.eventType != ModelEventType.Activation ||
					parentArrival.eventType != ModelEventType.Arrival) {
				throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Error creating return: malformed call stack"));
			}
			long replyID = queryUtils.nextMessageID();
			long returnID = replyID + 1;
			long result = queryUtils.createMessage(DataUtils.MESSAGE_KIND_REPLY, parentActivation.modelId, returnID, time, -1, null);
			Integer line = (Integer) parentCall.getDecoration("line");
			checkModel("reply", result, replyID);
			result = queryUtils.createMessage(DataUtils.MESSAGE_KIND_RETURN, callingActivation.modelId, replyID, time, (line != null) ? line : -1, null);
			checkModel("return", result, returnID);
		}
		if (eventStack.size() > 0) {
			// create the message for the final return.
			long replyID = queryUtils.nextMessageID();
			TraceModelEvent parentActivation = eventStack.removeLast();
			time = (Long)parentActivation.getDecoration("end");
			if (time == null || time < longestTime) {
				time = longestTime;
			}
			long result = queryUtils.createMessage(DataUtils.MESSAGE_KIND_REPLY, parentActivation.modelId, null, time, -1, null);
			checkModel("reply", result, replyID);
		}
	}

	/**
	 * @param event
	 * @param threadLogReader 
	 * @throws IOException
	 * @throws CoreException 
	 */
	private void processEvent(LinkedList<TraceModelEvent> eventStack,
			List<MethodEnterEvent> callEventStack, 
			TraceLogEvent event, ThreadLogReader reader)
			throws SQLException, CoreException {
		//if (true) return;
		switch (event.type) {
		case THREAD_INIT:
			processThread(eventStack, callEventStack, (ThreadInitEvent)event);
			break;
		case METHOD_ENTERED:
			processEntry(eventStack, callEventStack, (MethodEnterEvent)event, reader);
			break;
		case METHOD_EXITED:
			processExit(eventStack, callEventStack, (MethodExitEvent)event);
			break;
		case PAUSED:
			processPause(eventStack, callEventStack, (PauseEvent)event);
			break;
		case RESUMED:
			processResume(eventStack, callEventStack, (ResumeEvent)event, reader);
			break;
		}
	}

	/**
	 * Checks the entry event to see if it should be filtered. If so, then the entry
	 * is placed on a stack for later processing in case a later entry should trigger
	 * it to no longer be filtered (we must track the calls from the originally filtered
	 * call, to the new triggered call).
	 * @param eventStack
	 * @param event
	 * @param reader 
	 * @throws CoreException 
	 * @throws SQLException 
	 */
	private void processEntry(LinkedList<TraceModelEvent> eventStack, 
			List<MethodEnterEvent> callEventStack, 
			MethodEnterEvent event, ThreadLogReader reader) throws SQLException, CoreException {
		if (event.methodName.startsWith("access$")) {
			//just add the method to the stack, and ignore it
			callEventStack.add(event);
			return;
		}
		//check the top of the stack to see if it is an accessor.
		//if it is, then set this one's line number accordingly
		if (callEventStack.size() > 0) {
			MethodEnterEvent top = callEventStack.get(callEventStack.size()-1);
			if (top.methodName.startsWith("access$")) {
				event.lineNumber = top.lineNumber;
			}
		}
		if (isFiltered(event)) {
			if (callEventStack.size() > 0) {
				//check the top of the stack to see if filtering is
				//already occurring. If it is not, set this one as
				//the start of the filtering, and still process it
				MethodEnterEvent top = callEventStack.get(callEventStack.size()-1);
				String dec = top.getDecoration(KEY_FILTER_DECORATION);
				if (dec == null) {
					event.decorate(KEY_FILTER_DECORATION, filterStart);
					doProcessEntry(eventStack, event, reader);
				} else {
					event.decorate(KEY_FILTER_DECORATION, filtered);
				}
			} else {
				event.decorate(KEY_FILTER_DECORATION, filtered);
				//doProcessEntry(eventStack, event);
			}
			
			
		} else {
			//check the event on the top of call stack
			//if it was filtered, then we have to run backward, to the
			//root of the filtering and process each entry event
			//so that we have a path to the new triggered event
			if (callEventStack.size() > 0) {
				ListIterator<MethodEnterEvent> iterator = callEventStack.listIterator(callEventStack.size());
				LinkedList<MethodEnterEvent> eventsToProcess = new LinkedList<MethodEnterEvent>();
				while (iterator.hasPrevious()) {
					MethodEnterEvent top = iterator.previous();
					String filterState = top.getDecoration(KEY_FILTER_DECORATION);
					if (filtered.equals(filterState)) {
						top.decorate(KEY_FILTER_DECORATION, triggered);
						eventsToProcess.addFirst(top);
					} else if (filterStart.equals(filterState)) {
						//it has already been processed, so mark it as the end
						//of the triggers, and don't process
						top.decorate(KEY_FILTER_DECORATION, triggerEnd);
						break;
					} else {
						//we don't want to process anything that was not just
						//previously filtered.
						break;
					}
				}
				while (eventsToProcess.size() > 0) {
					//process the events
					doProcessEntry(eventStack, eventsToProcess.removeFirst(), reader);
				}
			}
			//finally, process this event
			doProcessEntry(eventStack, event, reader);
		}
		
		//add the event to the top of the callstack
		callEventStack.add(event);
	}
	
	/**
	 * @param event
	 * @return
	 */
	private boolean isFiltered(MethodEvent event) {
		//if (true) return false;
		return !(isIncluded(event) && !isExcluded(event));
	}

	/**
	 * @param event
	 * @return
	 */
	private boolean isExcluded(MethodEvent event) {
		String[] inclusion = getInclusionFilters();
		String[] exclusion = getExclusionFilters();
		if (inclusion.length == 0 && exclusion.length == 0) {
			return false;
		} else if (exclusion.length == 0) {
			return false;
		}
		String typeName = simplifyJavaType(event.className);
		String methodSig = typeName + "." + event.methodName + event.methodSignature;
		for (String p : exclusionFilters) {
			//check for the default package
			if (".*".equals(p)) {
				if (typeName.indexOf('.') < 0) {
					return true;
				}
			} else if (p.charAt(0) == '*') {
				p = p.substring(1);
				if (p.length() != 0) {
					if (p.charAt(p.length()-1) == '*') {
						p = p.substring(0, p.length()-1);
						if (methodSig.contains(p)) {
							return true;
						}
					} else {
						if (methodSig.endsWith(p)) {
							return true;
						}
					}
				} else {
					return true;
				}
			} else if (p.charAt(p.length()-1) == '*') {
				p = p.substring(0, p.length()-1);
				if (methodSig.startsWith(p)) {
					return true;
				}
			} else {
				if (methodSig.equals(p)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param event
	 * @return
	 */
	private boolean isIncluded(MethodEvent event) {
		String[] inclusion = getInclusionFilters();
		String[] exclusion = getExclusionFilters();
		if (inclusion.length == 0 && exclusion.length == 0) {
			return true;
		} else if (inclusion.length == 0) {
			return true;
		}
		String typeName = simplifyJavaType(event.className);
		String methodSig = typeName + "." + event.methodName + event.methodSignature;
		for (String p : inclusionFilters) {
			//check for the default package
			if (".*".equals(p)) {
				if (typeName.indexOf('.') < 0) {
					return true;
				}
			} else if (p.charAt(0) == '*') {
				p = p.substring(1);
				if (p.isEmpty()) return true;
				if (p.charAt(p.length()-1) == '*') {
					p = p.substring(0, p.length()-1);
					if (methodSig.contains(p)) {
						return true;
					}
				} else {
					if (methodSig.endsWith(p)) {
						return true;
					}
				}
			} else if (p.charAt(p.length()-1) == '*') {
				p = p.substring(0, p.length()-1);
				if (methodSig.startsWith(p)) {
					return true;
				}
			} else {
				if (methodSig.equals(p)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return
	 */
	private String[] getExclusionFilters() {
		if (exclusionFilters == null) {
			//load the exclusion filters.
			exclusionFilters = 
				getSketch().getFilterSettings().getResolvedExclusionFilters();
		}
		return exclusionFilters;
	}

	/**
	 * @return
	 */
	private String[] getInclusionFilters() {
		if (inclusionFilters == null) {
			//load the inclusion filters.
			inclusionFilters  = 
				getSketch().getFilterSettings().getResolvedInclusionFilters();
		}
		return inclusionFilters;
	}

	private void processExit(
			LinkedList<TraceModelEvent> eventStack, 
			List<MethodEnterEvent> callEventStack, 
			MethodExitEvent event) 
	throws CoreException, SQLException {
		//size should never be less than one on an exit process
		if (callEventStack.size() <= 0) return;
		MethodEnterEvent topCall = callEventStack.remove(callEventStack.size()-1);
		if (!topCall.methodName.startsWith("access$")) {
			String filteredState = topCall.getDecoration(KEY_FILTER_DECORATION);
			if (filteredState == null || !filteredState.equals(filtered)) {
				//process the exit
				doProcessExit(eventStack, event);
			}
		}
	}

	/**
	 * @param eventStack
	 * @param callEventStack 
	 * @param event
	 * @throws SQLException 
	 * @throws CoreException 
	 */
	private void processPause(LinkedList<TraceModelEvent> eventStack,
			List<MethodEnterEvent> callEventStack, PauseEvent event) throws CoreException, SQLException {
		//run through the stack frames, and send an exit event for each one
		if (event.stackTrace == null) {
			return;
		}
		int thread_id = (Integer)eventStack.getFirst().getDecoration("thread_id");
		queryUtils.createEvent(event.time, "PAUSE IN THREAD " + thread_id);
		
		//clear the stack
		if (callEventStack.size() <= 0) return;
		ListIterator<MethodEnterEvent> iterator = callEventStack.listIterator(callEventStack.size());
		//we have to generate the events before  processing them in order to avoid a concurrent
		//modification.
		LinkedList<MethodExitEvent> exitEvents = new LinkedList<MethodExitEvent>();
		while (iterator.hasPrevious()) {
			MethodEnterEvent topEvent = iterator.previous();
			MethodExitEvent exitEvent = topEvent.simulateExit(event.time);
			exitEvents.addFirst(exitEvent);
		}
		while (exitEvents.size() > 0) {
			processExit(eventStack, callEventStack, exitEvents.removeLast());
		}
		queryUtils.commit();
//		for (String frame : stackFrames) {
//			int methodNameIndex = frame.indexOf(';') + 1;
//			int methodSigIndex = frame.indexOf('(');
//			int lineNumberIndex = frame.lastIndexOf('[');
//			int lineNumberEndIndex = frame.length();
//			if (methodNameIndex <= 0 || lineNumberIndex <= 0) {
//				continue;
//			}
//			String className = frame.substring(0, methodNameIndex);
//			String methodName = frame.substring(methodNameIndex, methodSigIndex);
//			String methodSig = frame.substring(methodSigIndex, lineNumberIndex);
//			//line number isn't needed
//			HashMap<String, String> exitEvent = new HashMap<String, String>();
//			exitEvent.put("event", "X");
//			exitEvent.put(KEY_METHOD_CLASS, className.replace('/', '.'));
//			exitEvent.put(KEY_METHOD_NAME, methodName);
//			exitEvent.put(KEY_METHOD_SIGNATURE, methodSig.replace('/', '.'));
//			exitEvent.put(KEY_METHOD_TIME, event.get(KEY_METHOD_TIME));
//			exitEvent.put(KEY_METHOD_RETURN, "?");
//			processExit(eventStack, callEventStack, exitEvent);
//		}
		
	}

	/**
	 * @param eventStack
	 * @param callEventStack 
	 * @param event
	 * @throws SQLException 
	 * @throws CoreException 
	 */
	private void processResume(LinkedList<TraceModelEvent> eventStack,
			List<MethodEnterEvent> callEventStack, ResumeEvent event, ThreadLogReader reader) throws CoreException, SQLException {
		if (event.stackTrace == null) {
			return;
		}
		String stackFrames[] = event.stackTrace.split("\\n+");
		int thread_id = (Integer)eventStack.getFirst().getDecoration("thread_id");
		queryUtils.createEvent(event.time, "RESUME IN THREAD " + thread_id);
		//no need to process the last one on the frame: it will be the same as the next
		//entry call.
		for (int i = 0; i < stackFrames.length-1; i++) {
			String frame = stackFrames[i].trim();
			int methodNameIndex = frame.indexOf(';') + 1;
			int methodSigIndex = frame.indexOf('(');
			int lineNumberIndex = frame.lastIndexOf('[');
			int lineNumberEndIndex = frame.length()-1;
			if (methodNameIndex <= 0 || lineNumberIndex <= 0) {
				continue;
			}
			try {
			String className = frame.substring(0, methodNameIndex);
			String methodName = frame.substring(methodNameIndex, methodSigIndex);
			String methodSig = frame.substring(methodSigIndex, lineNumberIndex);
			String lineString = frame.substring(lineNumberIndex+1, lineNumberEndIndex);
			int line = Integer.parseInt(lineString);
			//line number isn't needed
			MethodEnterEvent exitEvent = MethodEnterEvent.simulateEnter(
				className.replace('/','.'),
				methodName,
				methodSig.replace('/','.'),
				line,
				event.time);
			processEntry(eventStack, callEventStack, exitEvent, reader);
			} catch (StringIndexOutOfBoundsException e) {
				
				throw new CoreException(new Status(IStatus.ERROR, SketchPlugin.PLUGIN_ID, "Bad stack trace: \n " + e.getMessage(), e));
				
			}
			
		}
	}

	/**
	 * @param parent
	 * @param event
	 * @throws IOException
	 * @throws CoreException 
	 * @throws SQLException 
	 */
	private void doProcessExit(LinkedList<TraceModelEvent> eventStack, MethodExitEvent event)
			throws CoreException, SQLException {
		//some threads may start with a stack greater than
		//0 because they join in from a different thread
		//unfortunately, there is no graceful way to deal with this.
		if (eventStack.size() <= 3) return;
		TraceModelEvent parentActivation = eventStack.removeLast();
		TraceModelEvent parentArrival = eventStack.removeLast();
		TraceModelEvent parentCall = eventStack.removeLast();
		TraceModelEvent callingActivation = eventStack.getLast();
		if (parentActivation.eventType != ModelEventType.Activation ||
			parentArrival.eventType != ModelEventType.Arrival ||
			parentCall.eventType != ModelEventType.Call ||
			callingActivation.eventType != ModelEventType.Activation) {
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Error creating return: malformed call stack"));
		}
		
		 		
		// create the message for the return.
		long replyID = queryUtils.nextMessageID();
		long returnID = replyID + 1;
		String replyString = DataUtils.MESSAGE_KIND_REPLY;
		String returnString = DataUtils.MESSAGE_KIND_RETURN;
		if (event.isException) {
			replyString = DataUtils.MESSAGE_KIND_THROW;
			returnString = DataUtils.MESSAGE_KIND_CATCH;
		}
		long result = queryUtils.createMessage(replyString, parentActivation.modelId, returnID, event.time, event.lineNumber, null);
		checkModel("reply", result, replyID);
		result = queryUtils.createMessage(returnString, callingActivation.modelId, replyID, event.time, (Integer)parentCall.getDecoration("line"), null);
		checkModel("return", result, returnID);
		//set the time for last activation
		callingActivation.decorate("end", event.time);
		//debugString = debugString.substring(1);
		TraceModelEvent firstEvent = eventStack.get(2);
		firstEvent.decorate("end", event.time);
		//System.out.println(debugString + "<" + "." + methodName);
		//TODO: add data for the return
	}

	/**
	 * @param parent
	 * @param event
	 * @throws IOException
	 * @throws CoreException 
	 */
	private void doProcessEntry(LinkedList<TraceModelEvent> eventStack, MethodEnterEvent event, ThreadLogReader reader)
			throws SQLException, CoreException {
		//get the top of the call stack, to make sure that it is an activation
		TraceModelEvent parent = eventStack.getLast();
		long thread_id = eventStack.getFirst().modelId;
		if (parent.eventType != ModelEventType.Activation) {
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Error creating call: invalid parent"));
		}
		
		
		//use java type separators, and get rid of the language type identifiers for classes
		String this_type = simplifyJavaType(event.className);
		String type_name = simplifyJavaType(event.className);
		String signature = event.methodSignature.replace('/', '.');
		String thisInstance = "?";
		if (event.variableValues.length > 0) {
			//TODO process variables
		}
		//this method call may have come from an accessor.. get the line number
		//from it.
		
//		if (event.getDecoration(accessor) != null)
//		if (reader.accessorLineNumber != -1) {
//			if (event.lineNumber == -1) {
//				event.lineNumber = reader.accessorLineNumber;
//			}
//			//clear the accessor line number
//			reader.accessorLineNumber = -1;
//		}
		
		String sequence = "";
		if (eventStack.size() > 3) {
			TraceModelEvent parentCall = eventStack.get(eventStack.size()-3);
			if (parentCall.eventType != ModelEventType.Call) {
				throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Illegal format in trace file: missing parent call"));
			}
			sequence = (String) parentCall.getDecoration("sequence") + ".";
		}
		Long parentSize = (Long) parent.getDecoration("children");
		if (parentSize == null) {
			parentSize = 0L;
		}
		parentSize = parentSize + 1;
		sequence = sequence + parentSize;
		//create the call
		long callID = queryUtils.nextMessageID();
		long arrivalID = callID + 1;
		long result = queryUtils.createMessage(DataUtils.MESSAGE_KIND_CALL, parent.modelId, arrivalID, event.time, event.lineNumber, sequence);
		//make sure that the id matches
		checkModel("call", result, callID);
		//make sure to store the new number of children
		parent.decorate("children", parentSize);
		
		//create the arrival
		long activationID = queryUtils.nextActivationID();
		result = queryUtils.createMessage(DataUtils.MESSAGE_KIND_ARRIVE, activationID, callID, event.time, -1, sequence);
		checkModel("arrival", result, arrivalID);
				
		//create the activation
		result = queryUtils.createActivation(arrivalID, type_name, event.methodName, signature, thread_id, this_type, thisInstance);
		checkModel("activation", result, activationID);
		
		//TODO: add data for the parameters
		
		//add to the call stack
		TraceModelEvent call = new TraceModelEvent(ModelEventType.Call, callID);
		call.decorate("line", event.lineNumber);
		call.decorate("time", event.time);
		call.decorate("method", event.methodName);
		if (sequence.length() > 40000) {
			throw new CoreException(new Status(IStatus.ERROR, SketchPlugin.PLUGIN_ID, "Call depth too large"));
		}
		call.decorate("sequence", sequence);
		eventStack.add(call);
		eventStack.add(new TraceModelEvent(ModelEventType.Arrival, arrivalID));
		eventStack.add(new TraceModelEvent(ModelEventType.Activation, activationID));
		eventStack.getLast().decorate("end", event.time);
		TraceModelEvent firstEvent = eventStack.get(2);
		firstEvent.decorate("end", event.time);
		//.out.println(debugString + ">" + type_name + "." + methodName);
		//debugString = debugString + " ";
	}

	/**
	 * Converts names of classes as defined by the bytecode into simplified
	 * Java names as they would appear in source code.
	 * @param thisType
	 * @return
	 */
	private String simplifyJavaType(String typeName) {
		typeName = typeName.replace('/', '.');
		if (typeName.endsWith(";")) {
			if (typeName.length() > 2) {
				typeName = typeName.substring(1, typeName.length() -1);
			}
		}
		return typeName;
	}

	/**
	 * @param callEventStack 
	 * @param parent
	 * @param event
	 * @throws CoreException 
	 * @throws IOException 
	 */
	private void processThread(LinkedList<TraceModelEvent> eventStack, List<MethodEnterEvent> callEventStack, ThreadInitEvent event) throws SQLException, CoreException {
		//create the user class if needed
//		queryUtils.getOrCreateTraceClass("USER");
//		long methodID = queryUtils.getOrCreateMethod("USER", "start", "()V");
		long arrivalID = queryUtils.nextMessageID();
		long threadID = queryUtils.createThread(event.threadID+"", event.threadName, arrivalID);
		long activationID = queryUtils.nextActivationID();
		long result = queryUtils.createMessage(DataUtils.MESSAGE_KIND_ARRIVE, activationID, null, 0, -1, "");
		checkModel("message", result, arrivalID);
		result = queryUtils.createActivation(arrivalID, "USER", "start", "()V", threadID, "USER", "?");
		checkModel("activation", result, activationID);
		//set up the call stack
		eventStack.addLast(new TraceModelEvent(ModelEventType.Thread, threadID));
		eventStack.addLast(new TraceModelEvent(ModelEventType.Arrival, arrivalID));
		eventStack.addLast(new TraceModelEvent(ModelEventType.Activation, activationID));
		eventStack.getLast().decorate("end", 0L);
		Integer thread_id = event.threadID;
		eventStack.getFirst().decorate("thread_id", thread_id);
	}
	
	private void checkModel(String modelType, long id1, long id2) throws CoreException {
		if (id1 != id2) {
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Mismatched id for " + modelType + ": " + id1 + "/" + id2));
		}
	}

	/**
	 * @return
	 */
	private File[] getTraceFiles() {
		File file;
		try {
			file = new File(sketch.getTracePath().toURI());
			return file.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".trace");
				}
			});
		} catch (URISyntaxException e) {
			return new File[0];
		}
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
	 */
	@Override
	public boolean belongsTo(Object family) {
		return (IProgramSketch.class.equals(family));
	}

	/**
	 * @return
	 */
	public IProgramSketch getSketch() {
		return sketch;
	}

}
