/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch;

public interface IFilterChangedListener {
	public void inclusionChanged(String[] old, FilterSettings settings);
	public void exclusionChanged(String[] old, FilterSettings settings);
	public void referenceChanged(IProgramSketch old, FilterSettings settings);
	public void projectClassesChanged(boolean old, FilterSettings settings);
}