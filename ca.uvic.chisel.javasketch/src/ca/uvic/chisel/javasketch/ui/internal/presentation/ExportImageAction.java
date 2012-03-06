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
package ca.uvic.chisel.javasketch.ui.internal.presentation;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.zest.custom.sequence.widgets.UMLImageExporter;
import org.eclipse.zest.custom.sequence.widgets.UMLSequenceChart;
import org.eclipse.zest.custom.uml.viewers.UMLSequenceViewer;

/**
 * Exports an image of the sequence diagram in the sequence diagram view.
 * @author Del Myers
 *
 */
public class ExportImageAction extends Action {
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		IViewPart part = 
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(JavaThreadSequenceView.VIEW_ID);
		if (!(part instanceof JavaThreadSequenceView)) {
			return;
		}
		
		UMLSequenceViewer viewer = ((JavaThreadSequenceView)part).getSequenceChartViewer();
		UMLSequenceChart chart = viewer.getChart();
		Image image = UMLImageExporter.createImage(chart);
		if (image != null) {
			FileDialog dialog = new FileDialog(chart.getShell(), SWT.SAVE);
			dialog.setFilterExtensions(new String[] {"*.bmp", "*.png", "*.jpg"});
			String fileName = dialog.open();
			if (fileName == null || fileName.isEmpty()) {
				image.dispose();
				return;
			}
			//set the format based on the file name
			String extension = "";
			int lastDot = fileName.lastIndexOf('.');
			if (lastDot > 0) {
				extension = fileName.substring(lastDot); 
			}
			int filterIndex = dialog.getFilterIndex();
			int format = -1;
			if (!extension.isEmpty()) {
				//check if it makes sense
				String lc = extension.toLowerCase();
				if (lc.equals(".bmp")) {
					format = SWT.IMAGE_BMP;
				} else if (lc.equals(".png")) {
					format = SWT.IMAGE_PNG;
				} else if (lc.equals(".jpg") || lc.equals(".jpeg")) {
					format = SWT.IMAGE_JPEG;
				}
			}
			if (format == -1) {
				switch (filterIndex) {
				case -1:
				case 0:
					if (!extension.toLowerCase().equals(".bmp")) {
						extension = ".bmp";
						fileName += extension;
					}
					format = SWT.IMAGE_BMP;
					break;
				case 1:
					if (!extension.toLowerCase().equals(".png")) {
						extension = ".png";
						fileName += extension;
					}
					format = SWT.IMAGE_PNG;
					break;
				case 2:
					if (!(extension.toLowerCase().equals(".jpg") || extension.toLowerCase().equals(".jpeg"))) {
						extension = ".jpg";
						fileName += extension;
					}
					format = SWT.IMAGE_JPEG;
					break;
				}
			}

			ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[] {image.getImageData()};
			loader.save(fileName, format);
			image.dispose();
		}
		
	}

}
