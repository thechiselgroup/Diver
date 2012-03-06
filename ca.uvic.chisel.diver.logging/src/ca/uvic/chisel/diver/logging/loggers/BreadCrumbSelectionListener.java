package ca.uvic.chisel.diver.logging.loggers;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import ca.uvic.chisel.logging.eclipse.IPartLogger;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

public class BreadCrumbSelectionListener implements IPartLogger,
		ISelectionChangedListener {

	public BreadCrumbSelectionListener() {}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		WorkbenchLoggingPlugin.getDefault().getEventLogger().logPartEvent(this, "breadCrumbEvent", event.getSelection());
	}

}
