/*******************************************************************************
 * Copyright (c) 2010 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers - initial API and implementation
 *******************************************************************************/
/**
 * 
 */
package ca.uvic.chisel.logging.eclipse.internal.network;

import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import ca.uvic.chisel.logging.eclipse.ILoggingCategory;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;
import ca.uvic.chisel.logging.eclipse.internal.Log;
import ca.uvic.chisel.logging.eclipse.internal.LoggingCategory;

public class UploadJob extends Job {

	private String[] categories;
	/**
	 * Preference store key for the upload. Stores the upload date as a long.
	 */
	public static final String LAST_UPLOAD_KEY = "last.upload";
	public static final String UPLOAD_INTERVAL_KEY = "upload.interval.key";
	
	public static final long UPLOAD_INTERVAL_DAILY = 1000*60*60*24;
	public static final long UPLOAD_INTERVAL_WEEKLY = UPLOAD_INTERVAL_DAILY*7;
	public static final long UPLOAD_INTERVAL_MONTHLY = UPLOAD_INTERVAL_DAILY*30;
	

	public UploadJob(String[] categories) {
		super("Uploading Workbench Log Data");
		this.categories = categories;
	}

	public IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Uploading Workbench Log Data", categories.length*2);
		for (String categoryID : categories) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			ILoggingCategory category = 
				WorkbenchLoggingPlugin.getDefault().getCategoryManager().getCategory(categoryID);
			if (category instanceof LoggingCategory) {
				Log log = ((LoggingCategory)category).getLog();
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
				log.archiveLog(subMonitor);
				subMonitor = new SubProgressMonitor(monitor, 1);
				try {
					new LogUploadRunnable(log).run(subMonitor);
				} catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				} catch (InvocationTargetException e) {
					return WorkbenchLoggingPlugin.getDefault().createStatus((Exception)e.getTargetException());
				}
			}
		}
		return Status.OK_STATUS;
	}
	
	@Override
	public boolean belongsTo(Object family) {
		return family.equals(getClass());
	}

	public static long today() {
		long currentTime = System.currentTimeMillis();
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis(currentTime);
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		//get rid of the day precision
		long calendarTime = cal.getTimeInMillis();
		return calendarTime;
	}
}