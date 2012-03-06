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

import java.util.LinkedList;

import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.zest.custom.sequence.widgets.Activation;
import org.eclipse.zest.custom.sequence.widgets.Call;
import org.eclipse.zest.custom.sequence.widgets.Message;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;

/**
 * Sets the hover state for the activation on a mouse-over.
 * @author Del Myers
 */

public class ActivationHoverInteraction extends AbstractInteraction {

	private class ActivationMouseListener implements MouseMotionListener {
		public void mouseDragged(MouseEvent me) {}
		public void mouseHover(MouseEvent me) {}
		public void mouseMoved(MouseEvent me) {}

		public void mouseEntered(MouseEvent me) {
			setHoverState(true);
	
		}
		public void mouseExited(MouseEvent me) {
			setHoverState(false);
		}
	}

	private ActivationMouseListener listener;
	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.interactions.AbstractInteraction#doHook()
	 */
	@Override
	protected void doHook() {
		if (this.listener == null) {
			this.listener = new ActivationMouseListener();
		}
		getPart().getFigure().addMouseMotionListener(listener);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.visuals.interactions.AbstractInteraction#doUnhook()
	 */
	@Override
	protected void doUnhook() {
		if (this.listener != null) {
			getPart().getFigure().removeMouseMotionListener(listener);
		}
	}
	
	private void setHoverState(boolean state) {
		LinkedList<UMLItem> stack = new LinkedList<UMLItem>();
		stack.add(getPart().getWidget());
		while (stack.size() > 0) {
			UMLItem current = stack.removeFirst();
			if (current.isHighlighted() != state) {
				current.setHighlight(state);
				if (current instanceof Activation) {
					for (Message m : ((Activation)current).getMessages()) {
						if (m.isVisible()) {
							stack.add(m);
							
						}
					}
				} else if (current instanceof Call) {
					Activation target = ((Call)current).getTarget();
					stack.add(target);
				}
			}
		}
	}

}
