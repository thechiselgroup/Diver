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
package ca.uvic.chisel.hsqldb.server;


import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.hsqldb.Trigger;

/**
 * Any time a trigger is needed from HSQLDB, clients should register it using this
 * class to ensure that the trigger will be on the classpath of the HSQLDB server.
 * Simply 
 * @author Del Myers
 *
 */
public final class HSQLTrigger implements Trigger {
	
	private static class TriggerKey {
		public final String triggerName;
		public final String tableName;
		public TriggerKey(String triggerName, String tableName) {
			this.triggerName = triggerName;
			this.tableName = tableName;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (!obj.getClass().equals(getClass())) {
				return false;
			}
			TriggerKey that = (TriggerKey) obj;
			return this.triggerName.equals(that.triggerName) && this.tableName.equals(that.tableName);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return triggerName.hashCode() + tableName.hashCode();
		}
	}
	
	private static final HashMap<TriggerKey, LinkedList<WeakReference<Trigger>>> TRIGGERS = 
		new HashMap<TriggerKey, LinkedList<WeakReference<Trigger>>>();

	/* (non-Javadoc)
	 * @see org.hsqldb.Trigger#fire(int, java.lang.String, java.lang.String, java.lang.Object[], java.lang.Object[])
	 */
	@Override
	public void fire(int type, String triggerName, String tableName, Object[] oldRow,
			Object[] newRow) {
		TriggerKey key = new TriggerKey(triggerName, tableName);
		LinkedList<Trigger> triggers = new LinkedList<Trigger>();
		synchronized (TRIGGERS) {
			LinkedList<WeakReference<Trigger>> l = TRIGGERS.get(key);
			if (l != null) {
				for (Iterator<WeakReference<Trigger>> it = l.iterator(); it.hasNext();) {
					WeakReference<Trigger> w = it.next();
					Trigger trigger = w.get();
					if (trigger == null) {
						it.remove();
					} else {
						triggers.add(trigger);
					}
				}
				if (l.size() == 0) {
					TRIGGERS.remove(key);
				}
			}
		}
		for (Trigger trigger : triggers) {
			trigger.fire(type, triggerName, tableName, oldRow, newRow);
		}
	}
	
	/**
	 * Registers a trigger for the given table name and the given trigger name. The trigger is stored
	 * as a weak reference so that client's don't have to worry about unregistering them.
	 * They will be removed from the queue when they are garbage collected. This means
	 * that clients must be careful to keep a strong reference to the trigger as long
	 * as it is needed, for example: in a field for a class.
	 * @param triggerName
	 * @param tableName
	 * @param trigger
	 */
	public static void registerWeakTrigger(String triggerName, String tableName, Trigger trigger) {
		synchronized (TRIGGERS) {
			TriggerKey key = new TriggerKey(triggerName, tableName);
			LinkedList<WeakReference<Trigger>> triggers = TRIGGERS.get(key);
			if (triggers == null) {
				triggers = new LinkedList<WeakReference<Trigger>>();
				TRIGGERS.put(key, triggers);
			}
			if (trigger != null) {
				for (WeakReference<Trigger> w : triggers) {
					if (trigger.equals(w.get())) {
						 return;
					}
				}
				triggers.add(new WeakReference<Trigger>(trigger));
			}
		}
	}

}
