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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.uvic.chisel.javasketch.data.SketchDataPlugin;
import ca.uvic.chisel.javasketch.data.internal.WriteDataUtils;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IArrival;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.IReply;
import ca.uvic.chisel.javasketch.data.model.ITargetMessage;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;

/**
 * @author Del Myers
 *
 */
public class ActivationImpl extends TraceModelIDBase implements IActivation {

	private ArrivalImpl arrival;
	private TraceClassImpl traceClass;
	private TraceClassMethodImpl method;
	private ArrayList<IOriginMessage> originMessages;
	private ArrayList<ITargetMessage> targetMessages;
	private IThread thread;
	private IOriginMessage replyMessage;
	private TraceClassImpl thisClass;
	private boolean dirty;

	/**
	 * @param trace
	 * @param results
	 * @throws SQLException
	 */
	public ActivationImpl(IThread thread, ResultSet results)
			throws SQLException {
		super((TraceImpl)thread.getTrace(), results);
		this.thread = thread;
		dirty = true;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#load()
	 */
	@Override
	public void load() {
		try {
			loadFromResults(getDataUtils().getActivation(getModelID()));
		} catch (SQLException e) {
			SketchDataPlugin.getDefault().log(e);
		}
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IActivation#getCaller()
	 */
	public IArrival getArrival() {
		if (arrival == null) {
			try {
				long arrival_id = getLong("arrival_id");
				ITraceModel element = ((TraceImpl)getTrace()).findElement("[Message],"+arrival_id);
				if (element == null) {
					ResultSet results = ((TraceImpl)getTrace()).getDataUtils().getMessage(arrival_id);
					arrival = (ArrivalImpl)MessageImpl.createFromResults((ThreadImpl)getThread(), results);
				} else {
					arrival = (ArrivalImpl) element;
				}
			} catch (SQLException e) {
				SketchDataPlugin.getDefault().log(e);
			}
		}
		return arrival;
	}

	/**
	 * @return
	 */
	public IThread getThread() {
		return thread;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IActivation#getInstanceID()
	 */
	public String getInstanceID() {
		return getString("instance");
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IActivation#getMethod()
	 */
	public ITraceClassMethod getMethod() {
		if (method == null) {
			String methodName = getString("method_name");
			String methodSignature = getString("method_signature");
			ITraceClass traceClass = getTraceClass();
			ITraceModel element = ((TraceImpl)getTrace()).findElement("[METHOD]," + traceClass.getName() +"."+methodName+methodSignature);
			if (element == null) {
				method = new TraceClassMethodImpl((TraceClassImpl) traceClass, methodName, methodSignature);
			} else {
				method = (TraceClassMethodImpl) element;
			}
			
		}
		return method;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IActivation#getOriginMessages()
	 */
	public synchronized List<IOriginMessage> getOriginMessages() {
		if (dirty || originMessages == null) {
			IOriginMessage lastMessage = null;
			originMessages = new ArrayList<IOriginMessage>();
			try {
				ResultSet results = getDataUtils().getOriginMessages(getModelID());
				while (results.next()) {
					MessageImpl message = MessageImpl.createFromResults((ThreadImpl)getArrival().getThread(), results);
					originMessages.add((IOriginMessage)message);
					lastMessage = (IOriginMessage) message;
				}
				if (lastMessage instanceof IReply) {
					replyMessage = lastMessage;
				}
			} catch (SQLException e) {
				SketchDataPlugin.getDefault().log(e);
			}
			dirty = false;
		}
		// TODO Auto-generated method stub
		return Collections.unmodifiableList(originMessages);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IActivation#getTargetMessages()
	 */
	public synchronized List<ITargetMessage> getTargetMessages() {
		if (targetMessages == null) {
			targetMessages = new ArrayList<ITargetMessage>();
			try {
				ResultSet results = getDataUtils().getTargetMessages(getModelID());
				while (results.next()) {
					MessageImpl message = MessageImpl.createFromResults((ThreadImpl)getArrival().getThread(), results);
					targetMessages.add((ITargetMessage)message);
				}
			} catch (SQLException e) {
				SketchDataPlugin.getDefault().log(e);
			}
		}
		return Collections.unmodifiableList(targetMessages);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IActivation#getTraceClass()
	 */
	public ITraceClass getTraceClass() {
		if (traceClass == null) {
			String className = getString("type_name");
			ITraceModel element = ((TraceImpl)getTrace()).findElement("[TraceClass]," + className);
			if (element == null) {
				//create a new one
				traceClass = new TraceClassImpl((TraceImpl)getTrace(), className);
			} else {
				traceClass = (TraceClassImpl) element;
			}
		}
		return traceClass;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IActivation#getDuration()
	 */
	public long getDuration() {
		return getLastMessageTime() - getArrival().getTime();
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IActivation#getTime()
	 */
	public long getTime() {
		return getArrival().getTime();
	}
	
	/**
	 * Gets the last message sent from this activation, which should be either
	 * a reply or a throw.
	 * @return
	 */
	private long getLastMessageTime() {
		getOriginMessages();
		if (replyMessage != null) {
			return replyMessage.getTime();
		}
		if (replyMessage == null) {
			try {
				Statement s = ((TraceImpl)getTrace()).getConnection().createStatement();
				ResultSet results = s.executeQuery("SELECT MAX(order_num) FROM Message WHERE activation_id=" + getModelID() + " AND (kind='REPLY' OR kind='THROW')");
				if (results.next()) {
					return results.getLong(1);
				}
			} catch (SQLException e) {
				SketchDataPlugin.getDefault().log(e);
			}
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IActivation#getThisClass()
	 */
	public ITraceClass getThisClass() {
		if (thisClass == null) {
			String className = getString("this_type");
			ITraceModel element = ((TraceImpl)getTrace()).findElement("[TraceClass]," + className);
			if (element == null) {
				//create a new one
				thisClass = new TraceClassImpl((TraceImpl)getTrace(), className);
			} else {
				thisClass = (TraceClassImpl) element;
			}
		}
		return thisClass;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String sequence = "";
		if (getArrival() != null) {
			ITargetMessage arrival = getArrival();
			if (arrival.getOrigin() instanceof ICall) {
				ICall call = (ICall) arrival.getOrigin();
				sequence = WriteDataUtils.fromStoredSequenceString(call.getSequence());
			}
		}
		String method = getMethod().getName() + getMethod().getSignature();
		return sequence + " " + method;
	}

	/**
	 * @param b
	 */
	public void setDirty(boolean b) {
		dirty = b;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getKind()
	 */
	public int getKind() {
		return ACTIVATION;
	}

	public static final String getIdentifierFromModel(String modelID) {
		return "[ACTIVATION]," + modelID;
	}
}
