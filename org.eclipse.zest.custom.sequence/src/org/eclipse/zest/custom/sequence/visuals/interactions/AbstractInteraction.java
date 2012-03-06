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
 * Abstract implementation of IVisualInteraction.
 * @author Del Myers
 */

public abstract class AbstractInteraction implements IVisualInteraction {
	
	private WidgetVisualPart part;
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.interactions.IVisualInteraction#hookInteraction(org.eclipse.mylar.zest.custom.sequence.visuals.WidgetVisualPart)
	 */
	public final void hookInteraction(WidgetVisualPart part) {
		unhookInteraction();
		this.part = part;
		doHook();
	}
	
	protected abstract void doHook();
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.interactions.IVisualInteraction#unhookInteraction()
	 */
	public final void unhookInteraction() {
		if (this.part != null) {
			doUnhook();
		}
		this.part = null;
	}
	
	protected abstract void doUnhook();
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.interactions.IVisualInteraction#getPart()
	 */
	public final WidgetVisualPart getPart() {
		return part;
	}

}
