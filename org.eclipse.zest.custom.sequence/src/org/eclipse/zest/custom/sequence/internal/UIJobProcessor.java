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
package org.eclipse.zest.custom.sequence.internal;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.widgets.Display;

/**
 * A class used to schedule long-running processes that must be run in the UI thread.
 * This class is available here in order to prevent coupling with the Eclipse runtime
 * so that the views can be used in stand-alone java applications.
 * 
 * @author Del Myers
 *
 */
public final class UIJobProcessor {
	
	private class UIJob {
		AbstractSimpleProgressRunnable runnable;
		Display display;
		IUIProgressService service;
		boolean cancelable;
		public UIJob(AbstractSimpleProgressRunnable runnable, Display display, IUIProgressService service, boolean cancelable) {
			this.display = display;
			this.runnable = runnable;
			this.service = service;
			this.cancelable = cancelable;
		}
	}
	
	/**
	 * Defines listeners for job states in this processor.
	 * @author Del Myers
	 */
	public static interface IUIJobProcessListener {
		public void jobQueued(AbstractSimpleProgressRunnable runnable);
		public void jobRemoved(AbstractSimpleProgressRunnable runnable);
		public void jobStarted(AbstractSimpleProgressRunnable runnable);
		public void jobFinished(AbstractSimpleProgressRunnable runnable);
	}
	
	
		
	/**
	 * The queue of jobs to be run.
	 */
	private LinkedList<UIJob> queue;
	
	private List<IUIJobProcessListener> listeners;
	
	private UIJob currentJob;


	private Thread thread;

	private volatile boolean quit;

	private IUIProgressService progressService;
	

	public UIJobProcessor(IUIProgressService progressService) {
		this.progressService = progressService;
		queue = new LinkedList<UIJob>();
		listeners = new LinkedList<IUIJobProcessListener>();
		quit = false;
	}
	
	public final void runInUIThread(AbstractSimpleProgressRunnable runnable, Display display, boolean cancelable) {
		run(runnable, display, progressService, cancelable);
	}
	
	/**
	 * Searches for queued jobs that have a family equal to the given family. Jobs that are not
	 * currently running, they will be removed from the queue. If the current running job is
	 * part of the family, it will be cancelled. 
	 * @param family
	 */
	public final void cancelJobsInFamily(Object family) {
		if (family == null) return;
		synchronized (queue) {
			removeJobsInFamily(family);
			if (currentJob != null && family.equals(currentJob.runnable.getFamily())) {
				if (currentJob.runnable != null) {
					currentJob.runnable.cancel();
				}
			}
		}
	}
	
	public final void quit() {
		if (quit) {
			return;
		}
		synchronized (queue) {
			quit = true;
			if (currentJob != null) {
				currentJob.runnable.cancel();
			}
			while (queue.size() > 0) {
				UIJob job = queue.removeFirst();
				fireRemoved(job.runnable);
				fireFinished(job.runnable);
			}
		}
	}
	
	/**
	 * Searches through the queued jobs that have a family equal to the given family. If such jobs are found,
	 * they are removed from the queue. Does not cancel a currently running job.
	 * @param family
	 */
	public void removeJobsInFamily(Object family) {
		if (family == null) return;
		synchronized (queue) {
			int i = 0;
			while (i < queue.size()) {
				UIJob job = queue.get(i);
				if (family.equals(job.runnable.getFamily())) {
					queue.remove(i);
					fireRemoved(job.runnable);
					fireFinished(job.runnable);
				} else {
					i++;
				}
			}
		}
		
	}

	private final void run(AbstractSimpleProgressRunnable runnable, Display display, IUIProgressService service, boolean cancelable) {
		synchronized (queue) {
			if (runnable != null && display != null && !display.isDisposed()) {
				UIJob job = new UIJob(runnable, display, service, cancelable);
				queue.add(job);
				fireQueued(job.runnable);
				if (thread == null) {
					thread = new Thread(new QueueingThreadRunnable(), "Sequence Diagram UI Job Queue");
					thread.start();
				}
			}
			
		}
	}
	
	private class QueueingThreadRunnable implements Runnable {
		/**
		 * Runs until the job queue is empty. After the job queue is empty, the thread
		 * will die, and must be rescheduled.
		 */
		public void run() {
			int queueSize;
			do {
				
				synchronized (queue) {
					queueSize = queue.size();
					if (queueSize > 0) {
						queueSize--;
						currentJob = queue.removeFirst();
						fireRemoved(currentJob.runnable);
					}
				}
				if (currentJob != null && currentJob.display != null) {
					try {
						currentJob.display.syncExec(new Runnable(){
							public void run() {
								fireStarted(currentJob.runnable);
								if (currentJob.service == null) {
									currentJob.service = new DelayedProgressMonitorDialog(
											Display.getCurrent().getActiveShell()
									);
								}
								try {
									currentJob.service.runInUIThread(currentJob.runnable, currentJob.cancelable);
								} catch (Throwable e) {
									currentJob.service.handleException(e);
								} finally {
									fireFinished(currentJob.runnable);
									currentJob = null;
								}
							}		
						});

					} catch (Throwable t) {
						//don't die because of an exception
						t.printStackTrace();
					}
				}
				synchronized (queue) {
					if (queue.size() == 0) {
						thread = null;
						break;
					}
				}
			} while (true);
		}
	}
	
	public void addJobListener(IUIJobProcessListener listener) {
		synchronized (listeners) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}
	
	public void removeJobListener(IUIJobProcessListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}
	
	private void fireStarted(AbstractSimpleProgressRunnable runnable) {
		IUIJobProcessListener[] la;
		synchronized (listeners) {
			 la = listeners.toArray(new IUIJobProcessListener[listeners.size()]);
		}
		for (IUIJobProcessListener listener : la) {
			listener.jobStarted(runnable);
		}
	}
	
	private void fireQueued(AbstractSimpleProgressRunnable runnable) {
		IUIJobProcessListener[] la;
		synchronized (listeners) {
			 la = listeners.toArray(new IUIJobProcessListener[listeners.size()]);
		}
		for (IUIJobProcessListener listener : la) {
			listener.jobQueued(runnable);
		}
	}
	
	private void fireRemoved(AbstractSimpleProgressRunnable runnable) {
		IUIJobProcessListener[] la;
		synchronized (listeners) {
			 la = listeners.toArray(new IUIJobProcessListener[listeners.size()]);
		}
		for (IUIJobProcessListener listener : la) {
			listener.jobRemoved(runnable);
		}
	}
	
	private void fireFinished(AbstractSimpleProgressRunnable runnable) {
		IUIJobProcessListener[] la;
		synchronized (listeners) {
			 la = listeners.toArray(new IUIJobProcessListener[listeners.size()]);
		}
		for (IUIJobProcessListener listener : la) {
			listener.jobFinished(runnable);
		}
	}
 
}
