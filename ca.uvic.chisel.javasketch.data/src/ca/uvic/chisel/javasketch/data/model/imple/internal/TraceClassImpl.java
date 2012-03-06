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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

import ca.uvic.chisel.javasketch.data.SketchDataPlugin;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;

/**
 * @author Del Myers
 *
 */
public class TraceClassImpl extends TraceModelViewBase implements ITraceClass {

	private TreeMap<String, ITraceClassMethod> methods;
	private String qualifiedName;
	private boolean dirty;
	
	
	/**
	 * @param trace
	 * @param results
	 * @throws SQLException 
	 */
	public TraceClassImpl(TraceImpl trace, String qualifiedName)  {
		super(trace, getIdentifier(qualifiedName));
		methods = new TreeMap<String, ITraceClassMethod>();
		dirty = true;
		this.qualifiedName = qualifiedName;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#load()
	 */
	@Override
	public void load() {
		//does nothing
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceClass#findMethod(java.lang.String, java.lang.String)
	 */
	public synchronized ITraceClassMethod findMethod(String name, String signature) {
		ITraceClassMethod method = methods.get(name + signature);
		try {
			if (method == null) {
				ResultSet results = getDataUtils().findMethod(getName(), name, signature);
				if (results != null) {
					method = new TraceClassMethodImpl(this, name, signature);
					methods.put(name+signature, method);
				}
			}
		} catch (SQLException e) {
			SketchDataPlugin.getDefault().log(e);
		}
		return method;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceClass#getMethods()
	 */
	public synchronized Collection<ITraceClassMethod> getMethods() {
		try {
			if (dirty || methods.size() != getDataUtils().getMethodCount(getName())) {
				ResultSet results = getDataUtils().getMethodsByClass(getName());
				while (results.next()) {
					try {
						String name = results.getString("method_name");
						String signature = results.getString("method_signature");
						if (!methods.containsKey(name+signature)) {
							ITraceClassMethod method = new TraceClassMethodImpl(this, name, signature);
							methods.put(name+signature, method);
						}
					} catch (SQLException e) {
						SketchDataPlugin.getDefault().log(e);
					}
				}
				dirty = false;
			}
		} catch (SQLException e) {
			SketchDataPlugin.getDefault().log(e);
		}
		return Collections.unmodifiableCollection(methods.values());
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceClass#getName()
	 */
	public String getName() {
		return qualifiedName;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#unload()
	 */
	@Override
	public synchronized void unload() {
		for (ITraceClassMethod m : methods.values()) {
			((TraceClassMethodImpl)m).unload();
		}
	}

	/**
	 * @param b
	 */
	synchronized void setDirty(boolean b) {
		dirty = b;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getKind()
	 */
	public int getKind() {
		return TRACE_CLASS;
	}
	
	public static final String getIdentifier(String qualifiedName) {
		return "[TRACECLASS]," + qualifiedName;
	}
}
