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
package ca.uvic.chisel.javasketch.ui.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.texteditor.MarkerUtilities;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;

/**
 * @author Del Myers
 *
 */
public class MarkTypeJob extends WorkspaceJob {

	
	private ITypeRoot typeRoot;

	/**
	 * @param name
	 */
	public MarkTypeJob(ITypeRoot typeRoot) {
		super("Marking Touched Locations in " + typeRoot.getElementName());
		this.typeRoot = typeRoot;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {
		try {
			monitor.beginTask("Marking Touched Locations", IProgressMonitor.UNKNOWN);
			
			IResource resource = typeRoot.getCorrespondingResource();
			if (resource == null) {
				IType type = typeRoot.findPrimaryType();
				if (type != null) {
					ICompilationUnit unit = type.getCompilationUnit();
					if (unit != null) {
						resource = unit.getResource();
					}
				}
			}
			if (resource == null) {
				resource = ResourcesPlugin.getWorkspace().getRoot();
			}
			deleteMarkers(resource, typeRoot);
			
			IProgramSketch active = SketchPlugin.getDefault().getActiveSketch();
			if (active == null) {
				return Status.OK_STATUS;
			}
			
			if (resource.getLocalTimeStamp() > active.getTraceData().getTraceTime().getTime()) {
				//no point in trying to look it up if it has changed recently.
				return Status.OK_STATUS;
			}
			PreparedStatement s = active.getPortal().prepareStatement("SELECT * FROM Activation LEFT OUTER JOIN Message ON Message.activation_id = Activation.model_id WHERE Activation.type_name LIKE ? AND " +
					"(Message.kind IN ('CALL', 'REPLY', 'THROW'))");
			for (IJavaElement child : typeRoot.getChildren()) {
				if (!(child instanceof IType)) {
					continue;
				}
				IType type = (IType) child;
				String qualifiedName = type.getFullyQualifiedName() + '%';
				s.setString(1, qualifiedName);
				ResultSet results = s.executeQuery();
				TreeSet<Integer> codeLines = new TreeSet<Integer>();
				while (results.next()) {
					//create a new marker
					Object codeLine = results.getObject("CODE_LINE");
					if (codeLine instanceof Integer) {
						codeLines.add((Integer)codeLine);
					}

				}
				for (Integer line : codeLines) {
					if (line > 0) {
						Map<Object,Object> attributes = new HashMap<Object, Object>();
						JavaCore.addJavaElementMarkerAttributes(attributes, typeRoot);
						attributes.put(IMarker.LINE_NUMBER, line);
						MarkerUtilities.createMarker(resource, attributes, "ca.uvic.chisel.javasketch.markers.touched");
					}
				}
			}
		} catch (Exception e) {
			SketchPlugin.getDefault().log(e);
			return SketchPlugin.getDefault().createStatus(e);
		
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private void deleteMarkers(IResource resource, ITypeRoot typeRoot) {
		
		try {
			IMarker[] markers = resource.findMarkers("ca.uvic.chisel.javasketch.markers.touched", true, IResource.DEPTH_ONE);
			for (IMarker marker : markers) {
				if (marker.exists()) {
					if (JavaCore.isReferencedBy(typeRoot, marker)) {
						marker.delete();
					}
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
