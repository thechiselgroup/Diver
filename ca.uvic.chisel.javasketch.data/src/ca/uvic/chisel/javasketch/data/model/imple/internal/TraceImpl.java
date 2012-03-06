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

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TreeMap;

import ca.uvic.chisel.hsqldb.server.IDataPortal;
import ca.uvic.chisel.javasketch.data.SketchDataPlugin;
import ca.uvic.chisel.javasketch.data.internal.DataUtils;
import ca.uvic.chisel.javasketch.data.internal.IDataTriggerListener;
import ca.uvic.chisel.javasketch.data.internal.WriteDataUtils;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceEventListener;
import ca.uvic.chisel.javasketch.data.model.ITraceMetaEvent;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;

/**
 * @author Del Myers
 *
 */
public class TraceImpl extends TraceModelIDImpl implements ITrace, IDataTriggerListener {
	
	private WriteDataUtils dataUtils;
	private TreeMap<String, ITraceClass> classes;
	private List<IThread> threads;
	private List<ITraceMetaEvent> events;
	private TreeMap<String, ITraceModel> modelCache;
	private boolean classesDirty;
	private boolean threadsDirty;
	private boolean disposed;
	private String launchID;
	private Date launchTime;
	
	private static final Timer EVENT_THREAD = new Timer("Trace Model Events", true);
	private static final EventAgrigatorTask EVENT_AGGRIGATOR;
	static {
		EVENT_AGGRIGATOR = new EventAgrigatorTask();
		EVENT_THREAD.schedule(EVENT_AGGRIGATOR, 1000, 2000);
	}

	private TraceImpl(String launchID, java.util.Date launchTime, WriteDataUtils utils) throws SQLException {
		this.dataUtils = utils;
		this.launchID = launchID;
		this.launchTime = new Date(launchTime.getTime());
		classes = new TreeMap<String, ITraceClass>();
		threads = new ArrayList<IThread>();
		modelCache = new TreeMap<String, ITraceModel>();
		load();
		dataUtils.addTriggerListener(this);
		classesDirty = true;
		threadsDirty = true;
		register(this);
	}
	
	private TraceImpl(IDataPortal portal) {
		
	}
	
	/**
	 * Checks to see if a trace with the given launch id and time exists for the
	 * given data portal.
	 * @param launchID
	 * @param date
	 * @param portal
	 * @return
	 */
	public static boolean exists(String launchID, java.util.Date date, IDataPortal portal) {
		try {
			PreparedStatement s = portal.prepareStatement("Select * from Trace");
			ResultSet results = s.executeQuery();
			if (results.next()) {
				return (results.getString("launch_id") != null) && (results.getString("time") != null);
			} else {
				return false;
			}
		} catch (SQLException e) {
			return false;
		}
	}
	
	/**
	 * Loads the trace with the given launch id and time from the given data portal.
	 * If no such trace can be loaded, then null will be returned.
	 * @param launchID
	 * @param date
	 * @param portal
	 * @return
	 */
	public static ITrace load(String launchID, java.util.Date date, IDataPortal portal) {
		if (!exists(launchID, date, portal)) {
			return null;
		}
		try {
			WriteDataUtils utils = new WriteDataUtils(portal);
			TraceImpl trace = new TraceImpl(launchID, date, utils);
			return trace;
		} catch (SQLException e) {
			return null;
		}
	}

	/**
	 * Creates a new trace for the given launch ID and time on the given data portal. All
	 * previous data will be deleted.
	 * @param launchID
	 * @param date
	 * @param portal
	 * @return
	 */
	public static ITrace create(String launchID, java.util.Date date, IDataPortal portal) {
		TraceImpl trace = null;
		try {
			WriteDataUtils utils = new WriteDataUtils(portal);
			utils.initializeDB(launchID, date);
			trace = new TraceImpl(launchID, date, utils);
		} catch (SQLException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
		return trace;
	}
	
	
	/**
	 * 
	 */
	public Connection getConnection() {
		try {
			return dataUtils.getConnection();
		} catch (SQLException e) {
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITrace#forName(java.lang.String)
	 */
	public synchronized ITraceClass forName(String name) {
		if (disposed) {
			return null;
		}
		ITraceClass clazz = null;
		try {
			clazz = classes.get(name);
			if (clazz == null) {
				ResultSet results = dataUtils.findTraceClass(name);
				if (results != null) {
					clazz = new TraceClassImpl(this, name);
					classes.put(name, clazz);
				}
			}
		} catch (SQLException e) {
			SketchDataPlugin.getDefault().log(e);
		}
		return clazz;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITrace#getClasses()
	 */
	public synchronized Collection<ITraceClass> getClasses() {
		if (disposed) {
			return Collections.emptyList();
		}
		
		try {
			if (classesDirty) {
				ResultSet results = dataUtils.getTraceClasses();
				while (results.next()) {
					try {
						String name = results.getString("type_name");
						if (!classes.containsKey(name)) {
							ITraceClass clazz = new TraceClassImpl(this, name);
							classes.put(name, clazz);
						}
					} catch (SQLException e) {
						SketchDataPlugin.getDefault().log(e);
					}
				}
				classesDirty = false;
			}
		} catch (SQLException e) {}
		return Collections.unmodifiableCollection(classes.values());
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITrace#getLaunchID()
	 */
	public String getLaunchID() {
		return launchID;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITrace#getThreads()
	 */
	public synchronized Collection<IThread> getThreads() {
		if (disposed) {
			return Collections.emptyList();
		}
		
		try {
			if (threadsDirty) {
				threads = new ArrayList<IThread>();
				ResultSet results = getDataUtils().getThreads();
				while (results.next()) {
					try {
						IThread thread = new ThreadImpl(this, results);
						threads.add(thread);
					} catch (SQLException e) {
						SketchDataPlugin.getDefault().log(e);
					}
				}
				threadsDirty = false;
			}
		} catch (SQLException e) {
			SketchDataPlugin.getDefault().log(e);
		}
		return Collections.unmodifiableCollection(threads);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITrace#getTime()
	 */
	public Date getTraceTime() {
		return launchTime;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getTrace()
	 */
	public ITrace getTrace() {
		return this;
	}
	
	public DataUtils getDataUtils() {
		try {
		} catch (IllegalStateException e) {
			SketchDataPlugin.getDefault().log(e);
		}
		return dataUtils;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#load()
	 */
	@Override
	public synchronized void load() {
		if (disposed) {
			return;
		}
		try {
			ResultSet results = getDataUtils().getTrace();
			loadFromResults(results);
		} catch (SQLException e) {
			isValid = false;
			dispose();
			SketchDataPlugin.getDefault().log(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#unload()
	 */
	@Override
	public void unload() {
		for (ITraceClass c : classes.values()) {
			((TraceClassImpl)c).unload();
		}
		for (IThread t : threads) {
			((ThreadImpl)t).unload();
		}
		super.unload();
	}

	/**
	 * @param traceModelBase
	 */
	public synchronized void register(TraceModelImpl traceModel) {
		String identifier = traceModel.getIdentifier().toUpperCase();
		modelCache.put(identifier, traceModel);		
	}
	

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#getModelID()
	 */
	@Override
	public long getModelID() {
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getIdentifier()
	 */
	public String getIdentifier() {
		return "[TRACE]," + getModelID();
	}

	
	public ITraceModel findElement(String identifier) {
		if (disposed) {
			return null;
		}
		try {
			if (identifier == null)
				return null;
			identifier = identifier.toUpperCase();
		} catch (IllegalStateException e) {
			return null;
		}
		return modelCache.get(identifier);
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITrace#getEvents()
	 */
	public synchronized List<ITraceMetaEvent> getEvents() {
		if (disposed) {
			return Collections.emptyList();
		}
		try {
			if (events != null) {
				return Collections.unmodifiableList(events);
			} else {
				events = new ArrayList<ITraceMetaEvent>();
				try {
					Statement eventsStatement = getConnection().createStatement();
					ResultSet results = eventsStatement.executeQuery("SELECT * FROM Event ORDER BY time");
					while (results.next()) {
						events.add(new TraceEventImpl(this, results));
					}
				} catch (SQLException e) {
					SketchDataPlugin.getDefault().log(e);
				}
			}
		} catch (IllegalStateException e) {}
		return Collections.unmodifiableList(events);
	}

	/**
	 * Clears all the data in this trace.
	 */
	private synchronized void clear() {
		classes.clear();
		modelCache.clear();
		if (events != null) {
			events.clear();
			events = null;

		}
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.internal.IDataTriggerListener#rowAdded(java.lang.String[], java.lang.Object[])
	 */
	public void rowAdded(String tableName, Object[] row) {
		if (!disposed) {
			EVENT_AGGRIGATOR.aggrigate(this, row);
		}
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#invalidate()
	 */
	@Override
	public void invalidate() {
		dispose();
		synchronized (this) {
			for (ITraceModel element : modelCache.values()) {
				if (element instanceof TraceModelImpl && element != this) {
					((TraceModelImpl)element).invalidate();
				}
			}
			dataUtils.reset();
			clear();
			isValid = false;
		}
	}
	
	public synchronized void initialize() throws SQLException, IOException {
		invalidate();
		unload();
		dataUtils.initializeDB(launchID, launchTime);
		dataUtils.addTriggerListener(this);
		disposed = false;
		load();
	}
	
	public void addListener(ITraceEventListener listener) {
		if (!disposed && isValid()) {
			EVENT_AGGRIGATOR.addListener(this, listener);
		}
	}
	
	public void removeListener(ITraceEventListener listener) {
		if (!disposed) {
			EVENT_AGGRIGATOR.removeListener(this, listener);
		}
	}

	synchronized void classesChanged() {
		classesDirty = true;
	}
	
	synchronized void threadsChanged() {
		threadsDirty = true;
	}

	/**
	 * @param className
	 */
	void methodsChanged(String className) {
		TraceClassImpl type = (TraceClassImpl) forName(className);
		type.setDirty(true);
	}

	/**
	 * @param activation
	 */
	void activationChanged(IActivation activation) {
		((ActivationImpl)activation).setDirty(true);
	}
	
	

	/**
	 * 
	 */
	public void dispose() {
		EVENT_AGGRIGATOR.removeListeners(this);
		dataUtils.removeTriggerListener(this);
		this.disposed = true;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITrace#getDataTime()
	 */
	public Date getDataTime() {
		if (disposed) {
			return new Date(0);
		}
		try {
			Timestamp stamp = getDate("data_time");
			if (stamp != null) {
				return new Date(stamp.getTime());
			}
		} catch (IllegalStateException e) {}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITraceModel#getKind()
	 */
	public int getKind() {
		return TRACE;
	}

	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.ITrace#getElement(java.lang.String)
	 */
	public ITraceModelProxy getElement(String identifier) {
		return TraceModelProxy.forIdentifier(this, identifier);
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().equals(obj.getClass())) {
			return false;
		}
		TraceImpl that = (TraceImpl) obj;
		String id = getIdentity();
		if (id == null) {
			return false;
		}
		return id.equals(that.getIdentity());
	}
	
	/* (non-Javadoc)
	 * @see ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelImpl#hashCode()
	 */
	@Override
	public int hashCode() {
		return getIdentity().hashCode();
	}
	
	private String getIdentity() {
		return launchID + "@" + launchTime;
	}

}
