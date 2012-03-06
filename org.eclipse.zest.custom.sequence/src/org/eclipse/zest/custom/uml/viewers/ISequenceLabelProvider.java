/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers -- initial API and implementation
 *******************************************************************************/
package org.eclipse.zest.custom.uml.viewers;

import org.eclipse.jface.viewers.ILabelProvider;

/**
 * A label provider for sequence diagrams. Offers extra information like
 * stereotypes, etc.
 * @author Del Myers
 *
 */
public interface ISequenceLabelProvider extends ILabelProvider {
	/**
	 * Returns a stereo type for a class, activation, or return value.
	 * @return a stereo type for a class, activation, or return value.
	 */
	String getStereoType(Object element);
}
