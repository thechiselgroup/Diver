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
package org.eclipse.zest.custom.sequence.figures;

/**
 * Constraints for figures that are meant to be placed on life
 * lines. Offers a time of execution for the child, and 
 * the length of execution.
 * @author Del Myers
 *
 */
public interface ILifeLineConstraints {
	/**
	 * Returns the absolute offset of execution.
	 */
	float getExecutionOffset();
	
	/**
	 * The amount of time in the parent that this child will
	 * execute for.
	 * @return
	 */
	float getExecutionLength();
	
	/**
	 * The time of the start for the entire sequence chart. It is up to
	 * the implementor to make sure that this is the same accross all
	 * constrained elements in the same sequence chart.
	 * @return the global start time.
	 */
	float getGlobalStartTime();
	
	/**
	 * The total amount of time that it takes for the entire sequence to run.
	 * It is up to the implementor to make sure that this is the same accross
	 * all constrained elements in the same sequence chart.
	 * @return the total elapsed time for the entire sequence chart.
	 */
	float getGlobalElapsedTime();
}
