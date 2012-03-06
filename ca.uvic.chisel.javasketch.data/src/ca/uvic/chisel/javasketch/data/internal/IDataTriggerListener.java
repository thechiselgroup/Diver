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

/**
 * @author Del Myers
 *
 */
public interface IDataTriggerListener {
	
	/**
	 * A row was added to the activation table
	 * @param tableName the name of the table for which to row was added
	 * @param row the new row
	 */
	public void rowAdded(String tableName, Object[] row);

}
