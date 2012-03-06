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
package org.eclipse.zest.custom.sequence.figures.internal;

import java.util.Map;

import org.eclipse.draw2d.DeferredUpdateManager;
import org.eclipse.draw2d.UpdateListener;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * An update manager that allows all updates to be suspended. Work will still get
 * queued, but updates will not be performed until the manager is resumed.
 * @author Del Myers
 *
 */
public class SuspendableDeferredUpdateManager extends DeferredUpdateManager {
	
	/**
	 * State variable indicated whether or not this update manager is suspended.
	 */
	private volatile boolean suspended;
	
	private UpdateListener[] listeners;
	
	public SuspendableDeferredUpdateManager() {
		super();
		listeners = new UpdateListener[0];
		suspended = false;
	}
	
	/**
	 * Sets the state of this update manager to suspended. Updates will not be
	 * performed until resume() is called.
	 */
	public void suspend() {
		this.suspended = true;
	}
	
	
	/**
	 * Performs queued updates, and allows new updates to be performed.
	 */
	public void resume() {
		if (!isSuspended()) {
			return;
		}
		suspended = false;
		sendUpdateRequest();
	}
	
	/**
	 * This implementation only sends update requests if the manager is not suspended.
	 */
	@Override
	protected void sendUpdateRequest() {
		if (isSuspended()) return;
		super.sendUpdateRequest();
	}
	
	/**
	 * Returns true if this update manager is not sending update requests.
	 * @return true if this update manager is not sending update requests.
	 */
	public boolean isSuspended() {
		return suspended;
	}
	
	/**
	 * Adds the given listener to the list of listeners to be notified of painting and 
	 * validation.
	 * @param listener the listener to add
	 */
	public void addUpdateListener(UpdateListener listener) {
		if (listener == null)
			throw new IllegalArgumentException();
	    if (listeners == null) {
	        listeners = new UpdateListener[1];
	        listeners[0] = listener;
	    } else {
	    	int oldSize = listeners.length;
	    	UpdateListener newListeners[] = new UpdateListener[oldSize + 1];
	    	System.arraycopy(listeners, 0, newListeners, 0, oldSize);
	    	newListeners[oldSize] = listener;
	    	listeners = newListeners;
	    }
	}	
	
	/**
	 * Removes one occurence of the given UpdateListener by identity.
	 * @param listener the listener to remove
	 */
	public void removeUpdateListener(UpdateListener listener) {
		if (listener == null)
			throw new IllegalArgumentException();
		for (int index = 0; index < listeners.length; index++)
			if (listeners[index] == listener) {
				int newSize = listeners.length - 1;
				UpdateListener newListeners[] = new UpdateListener[newSize];
				if (newSize != 0) {
					System.arraycopy(listeners, 0, newListeners, 0, index);
					System.arraycopy(listeners, index + 1, newListeners, index, newSize - index);
				}
				listeners = newListeners;
				return;
			}
	}
	
	/**
	 * Notifies listeners that painting is about to occur, passing them the damaged rectangle
	 * and the map of dirty regions.
	 * @param damage the damaged rectangle
	 * @param dirtyRegions map of dirty regions to figures
	 */
	@SuppressWarnings("unchecked")
	protected void firePainting(Rectangle damage, Map dirtyRegions) {
		UpdateListener localListeners[] = listeners;
		for (int i = 0; i < localListeners.length; i++)
			localListeners[i].notifyPainting(damage, dirtyRegions);
	}

	/**
	 * Notifies listeners that validation is about to occur.
	 */
	protected void fireValidating() {
		UpdateListener localListeners[] = listeners;
		for (int i = 0; i < localListeners.length; i++)
			localListeners[i].notifyValidating();
	}

}
