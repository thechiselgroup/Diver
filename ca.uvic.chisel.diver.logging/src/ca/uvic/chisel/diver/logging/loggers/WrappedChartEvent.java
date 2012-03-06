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
package ca.uvic.chisel.diver.logging.loggers;

import org.eclipse.zest.custom.uml.viewers.SequenceViewerEvent;


class WrappedChartEvent {
	public final SequenceViewerEvent event;
	public final EventKind kind;
	public WrappedChartEvent(SequenceViewerEvent event, EventKind kind) {
		this.event = event;
		this.kind = kind;
	}
}