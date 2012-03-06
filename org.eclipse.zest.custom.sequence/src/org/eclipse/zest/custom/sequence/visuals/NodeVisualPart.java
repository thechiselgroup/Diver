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
package org.eclipse.zest.custom.sequence.visuals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;

/**
 * A visual part for widgets that represent "Nodes"
 * @author Del Myers
 */

public abstract class NodeVisualPart extends WidgetVisualPart {

	private List<ConnectionVisualPart> sourceConnections;
	private List<ConnectionVisualPart> targetConnections;
	
	/**
	 * @param item
	 * @param key
	 * @param parentFigure
	 */
	public NodeVisualPart(UMLItem item, String key) {
		super(item, key);
		this.sourceConnections = new ArrayList<ConnectionVisualPart>();
		this.targetConnections = new ArrayList<ConnectionVisualPart>();
	}
	
	
	/**
	 * Returns the source connections. Clients should not change the returned list.
	 * Use add/remove connection instead.
	 * @return the connections.
	 */
	public final List<ConnectionVisualPart> getSourceConnections() {
		return sourceConnections;
	}
	/**
	 * Returns the target connections. Clients should not change the returned list.
	 * Use add/remove connection instead.
	 * @return the connections.
	 */
	public final List<ConnectionVisualPart> getTargetConnections() {
		return targetConnections;
	}
	
	/**
	 * Adds the given connection to the list of connections.
	 * @param connection
	 */
	public void addSourceConnection(ConnectionVisualPart connection) {
		sourceConnections.add(connection);
		connection.setSource(this);
	}
	
	public void addTargetConnection(ConnectionVisualPart connection) {
		targetConnections.add(connection);
		connection.setTarget(this);
	}
	
	public void removeSourceConnection(ConnectionVisualPart connection) {
		if (connection.getSource() != this) return;
		sourceConnections.remove(connection);
		connection.setSource(null);
	}
	
	public void removeTargetConnection(ConnectionVisualPart connection) {
		if (connection.getTarget() != this) return;
		targetConnections.remove(connection);
		connection.setTarget(null);
	}

	/**
	 * @param part
	 * @return
	 */
	public abstract ConnectionAnchor getSourceAnchor(ConnectionVisualPart part);

	/**
	 * @param part
	 * @return
	 */
	public abstract ConnectionAnchor getTargetAnchor(ConnectionVisualPart part);

}
