/*******************************************************************************
 * Copyright 2005-2006, CHISEL Group, University of Victoria, Victoria, BC,
 * Canada. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: The Chisel Group, University of Victoria
 *******************************************************************************/
package org.eclipse.zest.custom.sequence.widgets;

import java.util.HashMap;

import org.eclipse.swt.graphics.Image;
import org.eclipse.zest.custom.sequence.widgets.internal.IUMLImageAdapter;
import org.eclipse.zest.custom.sequence.widgets.internal.SequenceDiagramImageAdapter;

/**
 * Generates an image from a UMLChart
 * 
 * @author Del Myers
 * 
 */
public class UMLImageExporter {

	private static final HashMap<Class<?>, IUMLImageAdapter> adapters = 
		new HashMap<Class<?>, IUMLImageAdapter>();
	
	static {
		installAdapter(UMLSequenceChart.class, new SequenceDiagramImageAdapter());
	}

	/**
	 * Returns true if the exporter is able to export an image for the given
	 * chart.
	 * 
	 * @param chart
	 *            the chart to export
	 * @return true if the exporter is able to export an image for the given
	 *         chart.
	 */
	public static boolean isChartSupported(UMLChart chart) {
		return chart != null && getAdapter(chart.getClass()) != null;
	}

	/**
	 * @param class1
	 * @param sequenceDiagramImageAdapter
	 */
	private static void installAdapter(Class<UMLSequenceChart> class1,
			SequenceDiagramImageAdapter adapter) {
		adapters.put(class1, adapter);		
	}

	/**
	 * @param class1
	 * @return
	 */
	private synchronized static IUMLImageAdapter getAdapter(
			Class<? extends UMLChart> clazz) {
		return adapters.get(clazz);
	}
	
	/**
	 * Creates an image from the given chart, or null if one could not be created.
	 * The returned image must be disposed.
	 * @param chart the chart to create an image from.
	 * @return the new image or null. The image must be disposed by the client.
	 */
	public static Image createImage(UMLChart chart) {
		if (isChartSupported(chart)) {
			return getAdapter(chart.getClass()).getImage(chart);
		}
		return null;
	}

}
