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
package ca.uvic.chisel.javasketch.launching.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;

import ca.uvic.chisel.javasketch.SketchPlugin;

/**
 * little utility class to locate the jvmti agent
 * @author Del Myers
 *
 */
public final class JavaAgentUtil {
	
	public static IPath getJavaAgent(ILaunchConfiguration configuration) throws CoreException {
		String os = Platform.getOS();
		String arch = Platform.getOSArch();
		URL fileUrl = null;
		
		if ("win32".equals(os) && "x86".equals(arch)) {
			fileUrl = SketchPlugin.getDefault().getBundle().getResource("sketch_win32.dll");
		} else if ("win32".equals(os)  && ("amd64".equals(arch) || "x86_64".equals(arch))) { //Was expecting amd64 but in testing x86_64 was returned. Value supposed to be Java arch not CPU arch 
				fileUrl = SketchPlugin.getDefault().getBundle().getResource("sketch_win64.dll");
		} else if ("linux".equals(os)) {
			fileUrl = SketchPlugin.getDefault().getBundle().getResource("libsketch_linux32.so");
		} else {
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Unrecognized operating system: " + os + " with architecture " + arch));
		}
		
				
		if (fileUrl == null) {			
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Unable to locate tracing library: OS = " + os + " with architecture " + arch));					
		}
		String fileName = null;
		try {
			fileUrl = FileLocator.toFileURL(fileUrl);
			fileName = fileUrl.getFile();
			File file = new File(fileName);
			if (!file.exists()) {
				fileName = null;
			} else {
				fileName = file.getCanonicalPath();
			}
			if (fileName != null) {
				IPath filePath = new Path(fileName);
//				filePath = filePath.removeFileExtension();
//				String libraryName = filePath.lastSegment();
//				if (libraryName.startsWith("lib")) {
//					libraryName = libraryName.substring(3);
//				}
//				filePath = filePath.removeLastSegments(1).append(libraryName);
//				fileName = filePath.toOSString();
				return filePath;
			}
		} catch (IOException e) {
			throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Could not locate tracing agent", e));
		}
		throw new CoreException(new Status(Status.ERROR, SketchPlugin.PLUGIN_ID, "Could not locate tracing agent"));
	}

}
