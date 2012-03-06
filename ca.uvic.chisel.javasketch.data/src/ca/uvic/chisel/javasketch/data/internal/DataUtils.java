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
package ca.uvic.chisel.javasketch.data.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ca.uvic.chisel.hsqldb.server.IDataPortal;


/**
 * A simple class that contains query information for the database.
 * 
 * @author Del Myers
 * 
 */
public class DataUtils {

	private static final String METHOD_COUNT_STATEMENT = "select count(*) from Method where type_name = ?";
	private static final String METHODS_BY_TYPE_STATEMENT = "select type_name, method_name, method_signature from Method where type_name=?";
	private static final String METHODS_BY_SIGNATURE_STATEMENT = "SELECT type_name, method_name, method_signature from Method where type_name=? and method_name=? and method_signature=?";
	private static final String EVENTS_BY_TEXT_STATEMENT = "SELECT model_id, time, text FROM Event WHERE text=?";
	private static final String EVENT_BY_ID_STATEMENT = "SELECT model_id, time, text FROM Event WHERE model_id=?";
	private static final String ACTIVATION_BY_ID_STATEMENT = "select model_id, arrival_id, type_name, method_name, method_signature, thread_id, this_type, instance from Activation where model_id=?";
	private static final String ACTIVATION_BY_CALLER_STATEMENT = "SELECT " +
				 "a.model_id, " +
				 "a.arrival_id, " +
				 "a.type_name, " +
				 "a.method_name, " +
				 "a.method_signature, " +
				 "a.thread_id, " +
				 "a.this_type, " +
				 "a.instance " +
				 "FROM Activation a, " +
				 "(SELECT model_id FROM Message WHERE order_num=? AND kind='ARRIVE') as m " +
				 "WHERE a.arrival_id=m.model_id " +
				 "AND a.thread_id=?";
	private static final String MESSAGE_BY_ID_STATEMENT = "select model_id, kind, activation_id, opposite_id, order_num, time, code_line, sequence from Message where model_id=?";
	private static final String THREAD_BY_ID_STATEMENT = "select model_id, thread_id, thread_name, root_id from Thread where model_id=?";
	private static final String THREAD_STATEMENT = "select model_id, thread_id, thread_name, root_id from Thread";
	private static final String TRACE_CLASS_COUNT_STATEMENT = "Select count(*) from (select distinct type_name from Activation)";
	private static final String TRACE_CLASS_BY_NAME_STATEMENT = "Select distinct type_name from Activation where type_name=?";
	private static final String TRACE_CLASS_STATEMENT = "Select type_name from TraceClass";
	protected static final String TRACE_STATEMENT = "Select model_id, launch_id, time, data_time from Trace";
	public static final String MESSAGE_KIND_CALL = "CALL";
	public static final String MESSAGE_KIND_ARRIVE = "ARRIVE";
	public static final String MESSAGE_KIND_REPLY = "REPLY";
	public static final String MESSAGE_KIND_RETURN = "RETURN";
	public static final String MESSAGE_KIND_THROW = "THROW";
	private static final String REPLY_MESSAGES_STATEMENT = "select m.model_id, m.kind, m.activation_id, m.opposite_id, m.order_num, m.time, m.code_line, m.sequence from (" +
					"select activation_id,  max(order_num) as maxorder from Message where activation_id = ?  AND (kind = '"+MESSAGE_KIND_REPLY+"' OR kind ='" +MESSAGE_KIND_THROW+"') group by (activation_id)" +
				") as x join Message as m on x.maxorder=m.order_num AND m.activation_id = x.activation_id";

	private static final String ORIGIN_MESSAGES_STATEMENT = "select model_id, kind, activation_id, opposite_id, order_num, time, code_line, sequence from Message where activation_id = ? AND (kind = '"+MESSAGE_KIND_CALL+"' OR kind = '"+MESSAGE_KIND_REPLY+"' OR kind ='" +MESSAGE_KIND_THROW+"')";
	public static final String MESSAGE_KIND_CATCH = "CATCH";
	private static final String TARGE_MESSAGES_STATEMENT = "select model_id, kind, activation_id, opposite_id, order_num, time, code_line, sequence from Message where activation_id = ? AND (kind = '"+MESSAGE_KIND_ARRIVE+"' OR kind = '"+MESSAGE_KIND_RETURN+"' OR kind ='" +MESSAGE_KIND_CATCH+"')";
	
	
	protected class ExecutableQuery {
		private String query;
		private int batchCounter = 0;

		public ExecutableQuery(String query) {
			this.query = query;
		}
		public ResultSet executeQuery() throws SQLException {
			return getStatement().executeQuery();
		}
		public boolean execute() throws SQLException {
				getStatement().addBatch();
				batchCounter++;
				if ((batchCounter % 10000) == 0) {
					getStatement().executeBatch();
					batchCounter = 0;
					return true;
				}
			
			return true;
		}
		
		public boolean commit() throws SQLException {
			if (batchCounter > 0) {
				getStatement().executeBatch();
				return true;
			}
			return true;
		}
		
			
		/**
		 * @param i
		 * @param qualifiedName
		 * @throws SQLException 
		 */
		public void setString(int i, String value) throws SQLException {
			getStatement().setString(i, value);
		}
		
		private PreparedStatement getStatement() throws SQLException {
			return getPortal().prepareStatement(query);
		}
		/**
		 * @param i
		 * @param id
		 * @throws SQLException 
		 */
		public void setLong(int i, long value) throws SQLException {
			getStatement().setLong(i, value);
		}
		/**
		 * @param i
		 * @param order
		 * @throws SQLException 
		 */
		public void setInt(int i, int value) throws SQLException {
			getStatement().setInt(i, value);
		}
		/**
		 * @param i
		 * @param oppositeId
		 * @throws SQLException 
		 */
		public void setObject(int i, Object value) throws SQLException {
			getStatement().setObject(i, value);
			
		}
	}
		

	private ExecutableQuery activationByIDStatement;
	private ExecutableQuery eventByIDStatement;
	private ExecutableQuery eventsByTextStatement;
	private ExecutableQuery messageByIDStatement;
	protected ExecutableQuery methodBySignatureStatement;
	protected ExecutableQuery methodsByTypeStatement;
	private ExecutableQuery methodCountStatement;
	private ExecutableQuery threadByIDStatement;
	private ExecutableQuery threadStatement;
	private ExecutableQuery traceClassByNameStatement;
	private ExecutableQuery traceClassCountStatement;
	private ExecutableQuery traceClassStatement;
	private ExecutableQuery traceStatement;
	private ExecutableQuery activationByCallerStatement;
	private ExecutableQuery originMessagesStatement;
	private ExecutableQuery targetMessagesStatement;
	private ExecutableQuery replyMessagesStatement;
	
	
	protected long order_num;
	private IDataPortal portal;
	public DataUtils(IDataPortal portal) throws SQLException {
		this.portal = portal;
		prepareStatements();
	}

	
		
	
	/**
	 * @throws SQLException
	 */
	synchronized void prepareStatements() throws SQLException {
		
		traceStatement = new ExecutableQuery(TRACE_STATEMENT);
		traceClassStatement = new ExecutableQuery(TRACE_CLASS_STATEMENT);
		traceClassByNameStatement = new ExecutableQuery(TRACE_CLASS_BY_NAME_STATEMENT);
		traceClassCountStatement =
			new ExecutableQuery(TRACE_CLASS_COUNT_STATEMENT);
		threadStatement = new ExecutableQuery(THREAD_STATEMENT);
		threadByIDStatement = new ExecutableQuery(THREAD_BY_ID_STATEMENT);
		/* for all messages
		"model_id", "kind", "activation_id", "opposite_id", "order_num", "time",
				"code_line",
				
				"sequence"*/
		messageByIDStatement = new ExecutableQuery(MESSAGE_BY_ID_STATEMENT);
		originMessagesStatement = new ExecutableQuery(ORIGIN_MESSAGES_STATEMENT);
		replyMessagesStatement = new ExecutableQuery(REPLY_MESSAGES_STATEMENT);
		targetMessagesStatement = new ExecutableQuery(TARGE_MESSAGES_STATEMENT);
		activationByCallerStatement = new ExecutableQuery(ACTIVATION_BY_CALLER_STATEMENT);
		activationByIDStatement = new ExecutableQuery(ACTIVATION_BY_ID_STATEMENT);
	
		eventByIDStatement = new ExecutableQuery(EVENT_BY_ID_STATEMENT);
		eventsByTextStatement = new ExecutableQuery(EVENTS_BY_TEXT_STATEMENT);
		methodBySignatureStatement = new ExecutableQuery(METHODS_BY_SIGNATURE_STATEMENT);
		methodsByTypeStatement = new ExecutableQuery(METHODS_BY_TYPE_STATEMENT);
		methodCountStatement = new ExecutableQuery(METHOD_COUNT_STATEMENT);
	}

	/**
	 * Returns the result set representing the class for the given qualified
	 * name, or null if it could not be found.
	 * 
	 * @param qualifiedName
	 * @return
	 * @throws SQLException
	 */
	public synchronized ResultSet findTraceClass(String qualifiedName)
			throws SQLException {
		traceClassByNameStatement.setString(1, qualifiedName);
		ResultSet results = traceClassByNameStatement.executeQuery();
		if (results.next()) {
			return results;
		}
		return null;
	}

//	public synchronized long getOrCreateMethod(String type, String name,
//			String signature) throws SQLException {
//		ResultSet results = findMethod(type, name, signature);
//		if (results != null) {
//			return results.getLong(1);
//		}
//		long modelID = getNextModelID();
//		// try and find the class
//		long typeID = getOrCreateTraceClass(type);
//		createMethodStatement.setLong(1, modelID);
//		createMethodStatement.setLong(2, typeID);
//		createMethodStatement.setString(3, name);
//		createMethodStatement.setString(4, signature);
//		createMethodStatement.execute();
//		incrementModelNumStatement.execute();
//		getConnection().commit();
//		return modelID;
//	}

	/**
	 * @param arrivalId
	 * @return
	 * @throws SQLException 
	 */
	public synchronized ResultSet getMessage(long id) throws SQLException {
		messageByIDStatement.setLong(1, id);
		ResultSet results = messageByIDStatement.executeQuery();
		if (results.next()) {
			return results;
		}
		return null;
	}

	/**
	 * @param type
	 * @param name
	 * @param signature
	 * @return
	 * @throws SQLException
	 */
	public synchronized ResultSet findMethod(String type, String name, String signature)
			throws SQLException {
		methodBySignatureStatement.setString(1, type);
		methodBySignatureStatement.setString(2, name);
		methodBySignatureStatement.setString(3, signature);
		ResultSet results = methodBySignatureStatement.executeQuery();
		if (results.next()) {
			return results;
		}
		return null;
	}
	
	/**
	 * Closes the underlying connection for this utility class. Changes to the database are
	 * committed first.
	 * @throws SQLException if an error occurred committing results and closing the database.
	 */
	public void close() throws SQLException {
		getConnection().commit();
		getConnection().close();
	}

	/**
	 * @return
	 * @throws SQLException 
	 */
	public ResultSet getTrace() throws SQLException {
		
		ResultSet results = traceStatement.executeQuery();
		if (results.next()) {
			return results;
		}
		return null;
	}

	/**
	 * @return
	 * @throws SQLException 
	 */
	public int getClassCount() throws SQLException {
		
		ResultSet results = traceClassCountStatement.executeQuery();
		if (results.next()) {
			return results.getInt(1);
		}
		return 0;
	}

	/**
	 * @return all of the current classes in the database. The result set is set before
	 * the first row of the query.
	 * @throws SQLException 
	 */
	public ResultSet getTraceClasses() throws SQLException {
		
		return traceClassStatement.executeQuery();
	}

	/**
	 * @return
	 * @throws SQLException 
	 */
	public ResultSet getThreads() throws SQLException {
		
		return threadStatement.executeQuery();
	}

	/**
	 * @param modelID
	 * @return
	 * @throws SQLException 
	 */
	public synchronized ResultSet getThread(long modelID) throws SQLException {
		
		threadByIDStatement.setLong(1, modelID);
		ResultSet results = threadByIDStatement.executeQuery();
		if (results.next()) {
			return results;
		}
		return null;
	}

	/**
	 * @param modelID
	 * @return
	 * @throws SQLException 
	 */
	public synchronized ResultSet getMethodsByClass(String name) throws SQLException {
		
		methodsByTypeStatement.setString(1, name);
		ResultSet results = methodsByTypeStatement.executeQuery();
		if (results.next()) {
			return results;
		}
		return null;
	}

	/**
	 * @param modelID
	 * @return
	 * @throws SQLException 
	 */
	public synchronized int getMethodCount(String name) throws SQLException {
		methodCountStatement.setString(1, name);
		ResultSet results = methodCountStatement.executeQuery();
		if (results.next())
			return results.getInt(1);
		return 0;
	}

	/**
	 * @param modelID
	 * @return
	 * @throws SQLException 
	 */
	public synchronized ResultSet getActivation(long modelID) throws SQLException {
		
		activationByIDStatement.setLong(1, modelID);
		ResultSet results = activationByIDStatement.executeQuery();
		if (results.next()) {
			return results;
		}
		return null;
	}

	/**
	 * @param modelID
	 * @return
	 * @throws SQLException 
	 */
	public synchronized ResultSet getOriginMessages(long modelID) throws SQLException {
		
		originMessagesStatement.setLong(1, modelID);
		ResultSet results = originMessagesStatement.executeQuery();
		return results;
	}
	
	/**
	 * @param modelID
	 * @return
	 * @throws SQLException 
	 */
	public synchronized ResultSet getTargetMessages(long modelID) throws SQLException {
		
		targetMessagesStatement.setLong(1, modelID);
		ResultSet results = targetMessagesStatement.executeQuery();
		return results;
	}

	/**
	 * @param modelID
	 * @return
	 * @throws SQLException 
	 */
	public synchronized ResultSet getReplyMessage(long activationID) throws SQLException {
		
		replyMessagesStatement.setLong(1, activationID);
		ResultSet results = replyMessagesStatement.executeQuery();
		if (results.next()) {
			return results;
		}
		return null;
	}

	/**
	 * @param modelID
	 * @return
	 * @throws SQLException 
	 */
	public synchronized ResultSet getEvent(long modelID) throws SQLException {
		
		eventByIDStatement.setLong(1, modelID);
		ResultSet results = eventByIDStatement.executeQuery();
		if (results.next()) {
			return results;
		}
		return null;
	}
	
	public synchronized ResultSet getEventByText(String text) throws SQLException {
		
		eventsByTextStatement.setString(1, text);
		return eventsByTextStatement.executeQuery();
	}
	
	public synchronized ResultSet getActivationByCallerOrder(long order, long thread_id) throws SQLException {
		
		
//		Statement s = getConnection().createStatement();
//		ResultSet results1 = s.executeQuery("SELECT * FROM Message WHERE order_num = " + order + " AND kind='ARRIVE'");
//		while (results1.next()) {
//			long message_id = results1.getLong("model_id");
//			ResultSet results2 = s.executeQuery("SELECT * FROM Activation WHERE thread_id = " + thread_id + " AND arrival_id = " + message_id);
//			if (results2.next()) {
//				return results2;
//			}
//		}
		activationByCallerStatement.setLong(1, order);
		activationByCallerStatement.setLong(2, thread_id);
		ResultSet results = activationByCallerStatement.executeQuery();
		if (results.next()) {
			return results;
		}
		return null;
	}
	
	public Connection getConnection() throws SQLException {
		return portal.getDefaultConnection();
	}
	
	public void reset() {
		
	}
	
	protected IDataPortal getPortal() {
		return portal;
	}

}
