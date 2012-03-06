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

import org.eclipse.jface.viewers.IBaseLabelProvider;

/**
 * A tooltip provider for sequence diagrams. 
 * 
 * @author Marco Savard
 *
 */
public interface ITooltipLabelProvider extends IBaseLabelProvider {
  /**
   * Returns a tooltip for an item in a viewer when the tooltip should be different than the 
   * item's text.
   * @return a tooltip for an item in a viewer when the tooltip should be different than the 
   * item's text.
   */
  String getTooltipText(Object element);

}
