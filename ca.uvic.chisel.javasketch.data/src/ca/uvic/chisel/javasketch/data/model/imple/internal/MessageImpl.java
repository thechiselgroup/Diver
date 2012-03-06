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
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.IThread;

/**
 * @author Del Myers
 *
 */
public abstract class MessageImpl extends TraceModelIDBase implements IMessage {

	private MessageImpl opposite;
	private ThreadImpl thread;
	private IActivation activation;
	
	/**
	 * @param trace
	 * @param results
	 * @throws SQLException
	 */
	public MessageImpl(ThreadImpl thread, ResultSet results) throws SQLException {
		super((TraceImpl)thread.getTrace(), results);
		this.thread = thread;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#load()
	 */
	@Override
	public void load() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IMessage#codeLine()
	 */
	public int codeLine() {
		return getInt("code_line");
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IMessage#getActivation()
	 */
	public IActivation getActivation() {
		if (activation == null) {
			long activation_id = getLong("activation_id");
			try {
				IActivation a = (IActivation)((TraceImpl)getTrace()).findElement("[ACTIVATION],"+activation_id);
				if (a == null) {
					//create it from a results set.
					ResultSet results = getDataUtils().getActivation(activation_id);
					activation = new ActivationImpl(getThread(), results);
				} else {
					activation = a;
				}
			} catch (SQLException e) {
				SketchDataPlugin.getDefault().log(e);
			}
		}
		return activation;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IMessage#getOrder()
	 */
	public long getOrder() {
		return getLong("order_num");
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IMessage#getTime()
	 */
	public long getTime() {
		return getLong("time");
	}
	
	protected MessageImpl getOpposite() {
		if (opposite == null) {
			long opposite_id = getLong("opposite_id");
			try {
				//check to see if the opposite has been registered yet
				IMessage message = (IMessage) ((TraceImpl)getTrace()).findElement("[MESSAGE],"+opposite_id);
				if (message == null) { 
					ResultSet results = getDataUtils().getMessage(opposite_id);
					opposite = createFromResults((ThreadImpl) getThread(), results);
				} else {
					opposite = (MessageImpl) message;
				}
			} catch (SQLException e) {
				SketchDataPlugin.getDefault().log(e);
			}
		}
		return opposite;
	}
	
	protected static MessageImpl createFromResults(ThreadImpl thread, ResultSet results) throws SQLException {
		MessageImpl message = null;
		if (results != null) {
			String kind = results.getString("kind");
			if (DataUtils.MESSAGE_KIND_ARRIVE.equals(kind)) {
				message = new ArrivalImpl(thread, results);
			} else if (DataUtils.MESSAGE_KIND_CALL.equals(kind)) {
				message = new CallImpl(thread, results);
			} else if (DataUtils.MESSAGE_KIND_REPLY.equals(kind)) {
				message = new ReplyImpl(thread, results);
			} else if (DataUtils.MESSAGE_KIND_RETURN.equals(kind)) {
				message = new ReturnImpl(thread, results);
			} else if (DataUtils.MESSAGE_KIND_THROW.equals(kind)) {
				message = new ThrowImpl(thread, results);
			} else if (DataUtils.MESSAGE_KIND_CATCH.equals(kind)) {
				message = new CatchImpl(thread, results);
			} else {
				throw new UnsupportedOperationException(kind + " not supported");
			}
		}
		return message;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.IMessage#getThread()
	 */
	public IThread getThread() {
		return thread;
	}

}
