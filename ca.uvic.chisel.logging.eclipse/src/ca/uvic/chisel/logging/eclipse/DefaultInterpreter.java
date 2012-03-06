/*******************************************************************************
 * Copyright (c) 2010 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.logging.eclipse;


/**
 * @author Del Myers
 *
 */
public class DefaultInterpreter implements
		ILogObjectInterpreter {
	public String toString(Object object) {
		if (object == null) return "null";
		return object.getClass().getCanonicalName() + "@" + System.identityHashCode(object);
	}
}