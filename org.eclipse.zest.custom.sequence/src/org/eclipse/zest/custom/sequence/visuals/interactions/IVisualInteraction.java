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
package org.eclipse.zest.custom.sequence.visuals.interactions;

import org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart;

/**
 * Interface for visual interactions based on widget visual parts.
 * @author Del Myers
 */

public interface IVisualInteraction {
	
	/**
	 * Hooks this interaction to the given part. May listen to the visuals or the model
	 * for the part. The interaction may change both as well. Typically called in the
	 * activate() method of the part.
	 * @param part
	 */
	void hookInteraction(WidgetVisualPart part);
	
	/**
	 * Unhooks the interaction from the part set in hookInteraction(). Typically
	 * called in the deactivate method of the visual part.
	 *
	 */
	void unhookInteraction();
	
	/**
	 * Returns the currently hooked part.
	 * @return
	 */
	WidgetVisualPart getPart();

}
