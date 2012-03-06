package ca.uvic.chisel.javasketch.ui.internal.views.java;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.internal.SketchUI;

public class PackageExplorerFilterCommand extends AbstractHandler implements
		IElementUpdater {
	
	/**
	 * 
	 */
	public PackageExplorerFilterCommand() {
		
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
		boolean enabled = store.getBoolean(SketchUI.PREFERENCE_FILTER_PACKAGE_EXPLORER);
		store.setValue(SketchUI.PREFERENCE_FILTER_PACKAGE_EXPLORER, !enabled);
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#setEnabled(java.lang.Object)
	 */
	@Override
	public void setEnabled(Object evaluationContext) {
		super.setEnabled(evaluationContext);
		IProgramSketch sketch = SketchPlugin.getDefault().getActiveSketch();
		if (sketch == null) {
			setBaseEnabled(false);
		} else {
			setBaseEnabled(true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.commands.IElementUpdater#updateElement(org.eclipse.ui.menus.UIElement, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void updateElement(UIElement element, Map parameters) {
		IProgramSketch sketch = SketchPlugin.getDefault().getActiveSketch();
		if (sketch == null) {
			setBaseEnabled(false);
		} else {
			setBaseEnabled(true);
		}
		IPreferenceStore store = SketchPlugin.getDefault().getPreferenceStore();
		boolean toggle = store.getBoolean(SketchUI.PREFERENCE_FILTER_PACKAGE_EXPLORER);
		element.setChecked(toggle);
	}

}
