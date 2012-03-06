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

import ca.uvic.chisel.javasketch.data.SketchDataPlugin;
import ca.uvic.chisel.javasketch.data.internal.DataUtils;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IArrival;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;

/**
 * @author Del Myers
 *
 */
public class ThreadImpl extends TraceModelIDBase implements IThread {

	private ArrivalImpl root;

	/**
	 * @param trace
	 * @param results
	 * @throws SQLException
	 */
	public ThreadImpl(TraceImpl trace, ResultSet results) throws SQLException {
		super(trace, results);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#load()
	 */
	@Override
	public void load() {
		try {
			loadFromResults(getDataUtils().getThread(getModelID()));
		} catch (SQLException e) {
			SketchDataPlugin.getDefault().log(e);
		}
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IThread#getID()
	 */
	public int getID() {
		return getInt("thread_id");
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IThread#getName()
	 */
	public String getName() {
		return getString("thread_name");
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IThread#getRoot()
	 */
	public IArrival getRoot() {
		try {
			if (this.root == null) {
				long rootID = getLong("root_id");
				if (rootID > 0) {
					ResultSet results = getDataUtils().getMessage(rootID);
					this.root = new ArrivalImpl(this, results);
				}
			}
		} catch (SQLException e) {
			SketchDataPlugin.getDefault().log(e);
		}
		return root;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IThread#getBySequence()
	 */
	public IActivation getByOrder(long order) {
		DataUtils utils = ((TraceImpl)getTrace()).getDataUtils();
		try {
			ResultSet results = utils.getActivationByCallerOrder(order, getModelID());
			if (results != null) {
				long model_id = results.getLong("model_id");
				//try and find it if it has already been loaded
				ITraceModel element = ((TraceImpl)getTrace()).findElement("[ACTIVATION],"+model_id);
				if (element instanceof IActivation) {
					return (IActivation) element;
				}
				IActivation activation = new ActivationImpl(this, results);
				return activation;
				
			}
		} catch (SQLException e) {
			SketchDataPlugin.getDefault().log(e);
		}
		return null;
		
	}
	
	public IActivation getByModelID(long model_id) {
		DataUtils utils = ((TraceImpl)getTrace()).getDataUtils();
		try {
			ITraceModel element = ((TraceImpl)getTrace()).findElement("[ACTIVATION],"+model_id);
			if (element instanceof IActivation) {
				return (IActivation) element;
			}
			ResultSet results = utils.getActivation(model_id);
			if (results != null) {
				IActivation activation = new ActivationImpl(this, results);
				return activation;
			}
		} catch (SQLException e) {
			SketchDataPlugin.getDefault().log(e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getKind()
	 */
	public int getKind() {
		return THREAD;
	}

}
