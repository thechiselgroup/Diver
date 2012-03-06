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
package ca.uvic.chisel.logging.eclipse.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

/**
 * Simple class for logging events.
 * @author Del Myers
 *
 */
public class Log {
	
	private LoggingCategory category;
	private PrintStream fStream;
	
	public Log(LoggingCategory category) {
		this.category = category;
	}
	
	/**
	 * @return the category
	 */
	public LoggingCategory getCategory() {
		return category;
	}
	
	public synchronized void logLine(String info) {
		if (!WorkbenchLoggingPlugin.isEnabled()) return;
		if (getCategory().isEnabled()) {
			PrintStream stream = getStream();
			stream.println(info);
		}
	}

	private synchronized PrintStream getStream() {
		if (fStream == null) {
			fStream = newStream();
		}
		return fStream;
	}

	private PrintStream newStream() {
		File logFile = getLogFile();
		boolean insertHeader = !logFile.exists();
		try {
			FileOutputStream os = new FileOutputStream(logFile, true);
			PrintStream stream = new PrintStream(os, true);
			if (insertHeader) {
				stream.println("#Eclipse UI Log");
				stream.println("#User ID:" + WorkbenchLoggingPlugin.getDefault().getLocalUser());
			}
			return stream;
		} catch (FileNotFoundException e) {
			WorkbenchLoggingPlugin.getDefault().log(e);
			return null;
		}
	}

	public synchronized File getLogFile() {
		File categoryLocation = category.getLogLocation();
		if (!categoryLocation.exists()){
			if (!categoryLocation.mkdirs()) {
				WorkbenchLoggingPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, WorkbenchLoggingPlugin.PLUGIN_ID, "Could not create log location"));
			}
		} else if (!categoryLocation.isDirectory()) {
			WorkbenchLoggingPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, WorkbenchLoggingPlugin.PLUGIN_ID, "Could not create log location"));
		}
		File logFile = new File(categoryLocation, "eclipse.log");
		return logFile;
	}
	/**
	 * Copies the log to a backup file, and resets the log.
	 * @return the file backed-up.
	 */
	public File[] archiveLog(IProgressMonitor monitor) {
		File logFile = getLogFile();
		File[] backup = backupLog(monitor);
		if (!monitor.isCanceled()) {
			if (fStream != null) {
				fStream.close();
			}
			logFile.delete();
			fStream = null;
		}
		return backup;
	}
	
	/**
	 * Returns all the files that are used as backup.
	 * @return
	 */
	public File[] getBackupFiles() {
		File[] children = category.getLogLocation().listFiles(new FileFilter(){
			public boolean accept(File pathname) {
				String fileName = pathname.getName();
				return fileName.endsWith(".zip");
			}});
		return children;
	}
	
	/**
	 * Copies the current log to a backup file. The backup has a size limit of
	 * approximately 1 megabyte. If a file exceeds 1 megabyte, it will be split
	 * across multiple files. The file name of subsequent files will have
	 * "-1", "-2", etc. appended to their name. The zip files will simply contain
	 * a continuation of the text represented in the log. All lines of text in
	 * the output file will be complete.
	 * @return an array of files of size less than 1.5 megabytes that represent the
	 *  backup.
	 */
	public File[] backupLog(IProgressMonitor monitor) {
		long time = System.currentTimeMillis();
		File logFile = getLogFile();
		File tempFile = new File(logFile.getAbsolutePath() + ".tmp");
		synchronized (this){
			if (!logFile.exists()) {
				return null;
			}
			//copy to a temporary file.
			FileInputStream copyIn = null;
			FileOutputStream copyOut = null;
			try {
				copyIn = new FileInputStream(logFile);
				copyOut = new FileOutputStream(tempFile);
				int length = -1;
				byte[] buff = new byte[2048];
				while ((length = copyIn.read(buff)) != -1) {
					copyOut.write(buff, 0, length);
				}
			}  catch (IOException e) {
				return null;
			} finally {
				if (copyIn != null) {
					try {
						copyIn.close();
					} catch (IOException e) {}
				}
				if (copyOut != null) {
					try {
						copyOut.close();
					} catch (IOException e) {}
				}
			}
		}
		ArrayList<File> files = new ArrayList<File>();
		File outputFile = new File(tempFile.getParentFile(), time + ".zip");
		float scale = 1;
		if (tempFile.length() > Integer.MAX_VALUE){
			scale = ((float)Integer.MAX_VALUE)/tempFile.length();
		}
		String separator = System.getProperty("line.separator");
		if (separator == null) {
			separator = "\n";
		}
		int sbl = separator.getBytes().length;
		monitor.beginTask("Backing up log", (int)(tempFile.length()*scale));
		int fileCount = 0;
		try {
			ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(outputFile));
			PrintStream ps = new PrintStream(zo);
			ZipEntry entry = new ZipEntry(time + ".log");
			zo.putNextEntry(entry);
			long linesRead = 0;
			BufferedReader reader = new BufferedReader(new FileReader(tempFile));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (monitor.isCanceled()) break;
				if (outputFile.length() >= 1024*1024) {
					files.add(outputFile);
					linesRead = 0;
					//create a new file.
					zo.closeEntry();
					ps.close();
					fileCount++;
					outputFile = new File(tempFile.getParentFile(), time + "-" + fileCount + ".zip");
					zo = new ZipOutputStream(new FileOutputStream(outputFile));
					ps = new PrintStream(zo);
					entry = new ZipEntry(time + "-" + fileCount + ".log");
					zo.putNextEntry(entry);
				}
				int bytesRead = line.getBytes().length + sbl;
				monitor.worked((int)(bytesRead*scale));
				linesRead++;
				ps.println(line);
				if ((linesRead % 100) ==0) {
					ps.flush();
				}
			}
			
			zo.closeEntry();
			reader.close();
			ps.close();
			if (linesRead > 0){
				files.add(outputFile);
			}
			tempFile.delete();
		} catch (IOException e) {
			WorkbenchLoggingPlugin.getDefault().log(e);
			return null;
		}
		return files.toArray(new File[files.size()]);
	}


}
