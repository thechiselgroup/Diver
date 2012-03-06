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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IMemento;

import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;
import ca.uvic.chisel.logging.eclipse.internal.Log;
import ca.uvic.chisel.logging.eclipse.internal.LoggingCategory;

/**
 * Uploads the data in a log to its category's server.
 * @author Del
 *
 */
public class LogUploadRunnable implements IRunnableWithProgress {
	

	private Log log;

	public LogUploadRunnable(Log log) {
		super();
		this.log = log;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor sendMonitor) throws InterruptedException, InvocationTargetException{
		File[] filesToUpload = log.getCategory().getFilesToUpload();
		sendMonitor.beginTask("Uploading Log " + log.getCategory().getName(), filesToUpload.length);
		LoggingCategory category = log.getCategory();
		IMemento memento = category.getMemento();
		for (File file : filesToUpload) {
			if (sendMonitor.isCanceled()) {
				throw new InterruptedException();
			}
			PostMethod post = new PostMethod(category.getURL().toString());
					
			try {
				Part[] parts = { 
					new StringPart("KIND", "workbench-log"),
					new StringPart("CATEGORY", category.getCategoryID()),
					new StringPart("USER", WorkbenchLoggingPlugin.getDefault().getLocalUser()),
					new FilePart("WorkbenchLogger", file.getName(), file, "application/zip", null)
					};
				post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
				HttpClient client = new HttpClient();
				int status = client.executeMethod(post);
				String resp = getData(post.getResponseBodyAsStream());
				if (status != 200 || !resp.startsWith("Status: 200 Success")) {
					IOException ex = new IOException(resp);
					throw (ex);
				}
				memento.putString("lastUpload", file.getName());
				file.delete();
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			} finally {
				sendMonitor.worked(1);
			}
		}
		sendMonitor.done();
	}
	
	private static String getData(InputStream i) {
		String s = "";
		String data = "";
		BufferedReader br = new BufferedReader(new InputStreamReader(i));
		try {
			while ((s = br.readLine()) != null)
				data += s;
		} catch (IOException e) {
			WorkbenchLoggingPlugin.getDefault().log(e);
		}
		return data;
	}
}
