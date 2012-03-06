package ca.uvic.chisel.javasketch.ui.internal.views.java;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.ISketchColorConstants;

public class LightWeightJavaDOILabelDecorator implements
		ILightweightLabelDecorator {
	

	public LightWeightJavaDOILabelDecorator() {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable(){
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				//make sure that the colors are loaded
				Color c =ISketchColorConstants.GRAY;
				c = ISketchColorConstants.BLACK;
			}
		});
	}
	
	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return (element instanceof IJavaElement);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof IJavaElement) {
			IJavaElement je = (IJavaElement) element;
			double interest = SketchPlugin.getDefault().getDOI().getInterest(je);
			if (interest <= .6) {
				decoration.setForegroundColor(ISketchColorConstants.GRAY);
			} else {
				decoration.setForegroundColor(ISketchColorConstants.BLACK);
			}
			
		}
	}

}
