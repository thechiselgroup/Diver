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

import java.util.LinkedList;

import org.hsqldb.Trigger;

/**
 * @author Del Myers
 *
 */
public class DataTrigger implements Trigger {
	
	private final LinkedList<IDataTriggerListener> listeners = new LinkedList<IDataTriggerListener>();
	
	DataTrigger() {};
	
	/* (non-Javadoc)
	 * @see org.hsqldb.Trigger#fire(int, java.lang.String, java.lang.String, java.lang.Object[], java.lang.Object[])
	 */
	public void fire(int type, String name, String tableName, Object[] oldRow,
			Object[] newRow) {
		fireNewRow(tableName, newRow);
	}
	
	public void addDataListener(IDataTriggerListener listener) {
		//make sure that the trigger matches the actual trigger name in the
		//database
		synchronized (listeners) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
			}
		}
	}
	
	public void removeDataListener(IDataTriggerListener listener) {
		synchronized (listeners) {
				listeners.remove(listener);
		}
	}
	
	private void fireNewRow(String table, Object[] newRow) {
		Object[] row = new Object[newRow.length];
		//won't allow any changes to the database itself.
		System.arraycopy(newRow, 0, row, 0, row.length);
		IDataTriggerListener[] array = null;
		synchronized (listeners) {
			if (listeners != null) {
				//avoid concurrent modification
				array = listeners.toArray(new IDataTriggerListener[listeners.size()]);
			} 
		}
		if (array != null) {
			for (IDataTriggerListener listener : array) {
				listener.rowAdded(table, row);
			}
		}
	}

}
