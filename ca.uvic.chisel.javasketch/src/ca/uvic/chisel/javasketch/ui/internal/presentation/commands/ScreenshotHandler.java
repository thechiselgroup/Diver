package ca.uvic.chisel.javasketch.ui.internal.presentation.commands;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.zest.custom.sequence.widgets.UMLImageExporter;
import org.eclipse.zest.custom.sequence.widgets.UMLSequenceChart;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.internal.presentation.JavaThreadSequenceView;

public class ScreenshotHandler extends AbstractHandler implements IHandler, IElementUpdater {

	public static final String COMMAND_ID = "ca.uvic.chisel.javasketch.commands.screenshot";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		//grab the sequence diagram viewer
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IViewPart view = page.findView(JavaThreadSequenceView.VIEW_ID);
				if (view instanceof JavaThreadSequenceView) {
					JavaThreadSequenceView sequenceView = (JavaThreadSequenceView) view;
					UMLSequenceChart chart = sequenceView.getSequenceChartViewer().getChart();
					Image image = UMLImageExporter.createImage(chart);
					if (image != null) {
						FileDialog dialog = new FileDialog(chart.getShell(), SWT.SAVE);
						dialog.setFilterExtensions(new String[] {"*.bmp", "*.png", "*.jpg"});
						String fileName = dialog.open();
						if (fileName == null || fileName.isEmpty()) {
							image.dispose();
							return null;
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
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.commands.IElementUpdater#updateElement(org.eclipse.ui.menus.UIElement, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		element.setIcon(SketchPlugin.imageDescriptorFromPlugin("images/etool16/screenshot.png"));		
	}
}
