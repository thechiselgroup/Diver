package ca.uvic.chisel.javasketch.data.internal;
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

/**
 * A simple reference class for getting the names of the columns in the activation table.
 * @author Del Myers
 *
 */
public interface IActivationTable {
	
	final int MODEL_ID = 1;
	final int ARRIVAL_ID = 2;
	final int TYPE_NAME = 3;
	final int METHOD_NAME = 4;
	final int METHOD_SIGNATURE = 5;
	final int THREAD_ID = 6;
	final int THIS_TYPE = 7;
	final int INSTANCE = 8;
	
	final String[] columnNames = {
		null,
		"model_id",
		"arrival_id",
		"type_name",
		"method_name",
		"method_signature",
		"thread_id",
		"this_type",
		"instance"
	};

}
