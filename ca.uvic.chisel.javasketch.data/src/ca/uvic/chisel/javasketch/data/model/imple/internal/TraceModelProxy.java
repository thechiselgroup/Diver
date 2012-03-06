/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.data.model.imple.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.uvic.chisel.javasketch.data.internal.DataUtils;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;

/**
 * Concrete implementation of proxies. Used as a factory to proxies as well.
 * 
 * @author Del Myers
 * 
 */
public class TraceModelProxy implements ITraceModelProxy {

	private ITraceModel element;
	private ITrace trace;
	private Object key;
	private int kind;
	private String elementId;

	private TraceModelProxy(ITrace trace, Object key, int kind, String elementId) {
		assert (key instanceof MethodKey || key instanceof Long || key instanceof String);
		assert (trace != null);
		assert (checkKind(kind));
		element = null;
		this.trace = trace;
		this.key = key;
		this.kind = kind;
		this.elementId = elementId;
	}

	/**
	 * 
	 */
	private boolean checkKind(int kind) {
		switch (kind) {
		case ITraceModel.ACTIVATION:
		case ITraceModel.ARRIVAL:
		case ITraceModel.CALL:
		case ITraceModel.CATCH:
		case ITraceModel.EVENT:
		case ITraceModel.REPLY:
		case ITraceModel.RETURN:
		case ITraceModel.THREAD:
		case ITraceModel.TRACE:
		case ITraceModel.TRACE_CLASS:
		case ITraceModel.TRACE_CLASS_METHOD:
		case ITraceModel.MESSAGE:
			return true;
		}
		return false;

	}

	/*
	 * (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModelProxy#getElement()
	 */
	public ITraceModel getElement() {
		if (element == null) {
			try {
				load();
			} catch (SQLException e) {
				element = null;
			}
		}
		return element;
	}

	/**
	 * 
	 */
	private void load() throws SQLException {
		String identifier = getIdentifer();
		element = trace.findElement(identifier);
		if (element == null) {
			DataUtils utils = ((TraceImpl) trace).getDataUtils();
			if ((getKind() & ITraceModel.MESSAGE) == ITraceModel.MESSAGE) {
				ResultSet results = utils.getMessage((Long) key);
				if (results != null) {
					long activationId = results.getLong("activation_id");
					TraceModelProxy activationProxy = new TraceModelProxy(
						trace, activationId, ITraceModel.ACTIVATION,
						"[ACTIVATION]," + activationId);
					IActivation activation = (IActivation) activationProxy
						.getElement();
					if (activation != null) {
						IThread thread = activation.getThread();
						// try and find the element again, just in case it has
						// already been loaded
						element = trace.findElement(identifier);
						if (element == null) {
							element = MessageImpl.createFromResults(
								(ThreadImpl) thread, results);
						}
					}
				}

			} else {
				ResultSet results = null;
				switch (getKind()) {
				case ITraceModel.ACTIVATION:
					results = utils.getActivation((Long) key);
					if (results != null) {
						Long threadId = results.getLong("thread_id");
						TraceModelProxy threadProxy = new TraceModelProxy(
							trace, threadId, ITraceModel.THREAD, "[THREAD],"
									+ threadId);
						ThreadImpl thread = (ThreadImpl) threadProxy
							.getElement();
						if (thread != null) {
							element = new ActivationImpl(thread, results);
						}
					}
					break;
				case ITraceModel.TRACE_CLASS:
					element = trace.forName((String) key);
					break;
				case ITraceModel.TRACE_CLASS_METHOD:
					MethodKey mkey = (MethodKey) key;
					ITraceClass clazz = trace.forName(mkey.type);
					if (clazz != null) {
						element = clazz.findMethod(mkey.name, mkey.signature);
					}
					break;
				case ITraceModel.THREAD:
					for (IThread t : trace.getThreads()) {
						if (((ThreadImpl) t).getModelID() == (Long) key) {
							element = t;
							break;
						}
					}
					break;
				case ITraceModel.TRACE:
					element = trace;
					break;
				case ITraceModel.EVENT:
					results = utils.getEvent((Long) key);
					element = new TraceEventImpl((TraceImpl) trace, results);
					break;
				}
			}
		}
	}

	/**
	 * @return
	 */
	private String getIdentifer() {
		String tableName = getTableName();
		return "[" + tableName + "]," + key.toString();
	}

	/**
	 * @return
	 */
	private String getTableName() {
		String tableName = "";
		if ((getKind() & ITraceModel.MESSAGE) == ITraceModel.MESSAGE) {
			tableName = "MESSAGE";
		} else {
			switch (getKind()) {
			case ITraceModel.ACTIVATION:
				tableName = "ACTIVATION";
				break;
			case ITraceModel.TRACE_CLASS:
				tableName = "TRACECLASS";
				break;
			case ITraceModel.TRACE_CLASS_METHOD:
				tableName = "METHOD";
				break;
			case ITraceModel.THREAD:
				tableName = "THREAD";
				break;
			case ITraceModel.TRACE:
				tableName = "TRACE";
				break;
			case ITraceModel.EVENT:
				tableName = "EVENT";
				break;
			}
		}
		return tableName;
	}

	/*
	 * (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModelProxy#getKind()
	 */
	public int getKind() {
		return kind;
	}

	/**
	 * @param traceData
	 * @param c
	 * @return
	 */
	public static ITraceModelProxy forClass(ITrace traceData, String c) {
		return new TraceModelProxy(traceData, c, ITraceModel.TRACE_CLASS,
			"[TRACECLASS]," + c);
	}

	/**
	 * @param traceData
	 * @param mk
	 * @return
	 */
	public static ITraceModelProxy forMethod(ITrace traceData, MethodKey mk) {
		return new TraceModelProxy(traceData, mk,
			ITraceModel.TRACE_CLASS_METHOD, "[METHOD]," + mk.toString());
	}

	/**
	 * @param traceData
	 * @param longValue
	 * @return
	 */
	public static ITraceModelProxy forActivation(ITrace traceData, long modelId) {
		return new TraceModelProxy(traceData, modelId, ITraceModel.ACTIVATION,
			"[ACTIVATION]," + modelId);
	}

	/**
	 * Creates a trace model proxy for the given identifier. The actual model
	 * object is not guaranteed to exist in the model. Null will be returned if
	 * the identifier is not valid.
	 * 
	 * @param trace
	 * @param identifier
	 * @return
	 */
	public static ITraceModelProxy forIdentifier(ITrace trace, String identifier) {
		Pattern p = Pattern.compile("^\\[([a-zA-Z_-]+)\\],(.*)$");
		Matcher m = p.matcher(identifier);
		if (m.find()) {
			String table = m.group(1).toUpperCase();
			String id = m.group(2);
			if ("TRACECLASS".equals(table)) {
				return forClass(trace, id);
			} else if ("METHOD".equals(table)) {
				int paren = id.indexOf('(');
				if (paren > 0) {
					String sig = id.substring(paren);
					id = id.substring(0, paren);
					int dot = id.lastIndexOf('.');
					if (dot > 0) {
						String cname = id.substring(0, dot);
						String mname = id.substring(dot + 1);
						return forMethod(trace,
							new MethodKey(cname, mname, sig));
					}
				}
			} else {
				int kind = ITraceModel.ACTIVATION;
				try {
					long modelId = Long.parseLong(id);
					if ("MESSAGE".equals(table)) {
						kind = ITraceModel.MESSAGE;
					} else if ("ACTIVATION".equals(table)) {
						kind = ITraceModel.ACTIVATION;
					} else if ("EVENT".equals(table)) {
						kind = ITraceModel.EVENT;
					} else if ("THREAD".equals(table)) {
						kind = ITraceModel.THREAD;
					} else if ("TRACE".equals(table)) {
						kind = ITraceModel.TRACE;
					}
					return new TraceModelProxy(trace, modelId, kind, "["
							+ table + "]," + modelId);
				} catch (NumberFormatException e) {
				}
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (key instanceof Long) {
			ITraceModel element = getElement();
			if (element != null) {
				return element.toString();
			}
			return "Anable to load element";
		}
		return key.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ca.uvic.chisel.javasketch.data.model.ITraceModelProxy#isDynamicElement()
	 */
	public boolean isDynamicElement() {
		return ((kind & ITraceModel.DYNAMIC_ELEMENT) == ITraceModel.DYNAMIC_ELEMENT);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ca.uvic.chisel.javasketch.data.model.ITraceModelProxy#isMessageElement()
	 */
	public boolean isMessageElement() {
		return ((kind & ITraceModel.MESSAGE) == ITraceModel.MESSAGE);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ca.uvic.chisel.javasketch.data.model.ITraceModelProxy#isStaticElement()
	 */
	public boolean isStaticElement() {
		return !isDynamicElement();
	}

	/*
	 * (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModelProxy#getElementId()
	 */
	public String getElementId() {
		return elementId;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModelProxy#getTrace()
	 */
	public ITrace getTrace() {
		return trace;
	}

}
