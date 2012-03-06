package ca.uvic.chisel.diver.logging.loggers;

import org.eclipse.zest.custom.uml.viewers.ISequenceViewerListener;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerEvent;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerGroupEvent;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerRootEvent;

import ca.uvic.chisel.logging.eclipse.IPartLogger;
import ca.uvic.chisel.logging.eclipse.WorkbenchLoggingPlugin;

public class SequenceChartLogger implements IPartLogger,
		ISequenceViewerListener {
	
	public SequenceChartLogger() {
	}

	@Override
	public void elementCollapsed(SequenceViewerEvent event) {
		WorkbenchLoggingPlugin.getDefault().getEventLogger().logPartEvent(this, "chartElementCollapsed", event);

	}

	@Override
	public void elementExpanded(SequenceViewerEvent event) {
		WorkbenchLoggingPlugin.getDefault().getEventLogger().logPartEvent(this, "chartElementExpanded", event);

	}

	@Override
	public void groupCollapsed(SequenceViewerGroupEvent event) {
		WorkbenchLoggingPlugin.getDefault().getEventLogger().logPartEvent(this, "chartGroupCollapsed", event);


	}

	@Override
	public void groupExpanded(SequenceViewerGroupEvent event) {
		WorkbenchLoggingPlugin.getDefault().getEventLogger().logPartEvent(this, "chartGroupExpanded", event);


	}

	@Override
	public void rootChanged(SequenceViewerRootEvent event) {
		WorkbenchLoggingPlugin.getDefault().getEventLogger().logPartEvent(this, "chartRootChanged", event);

	}

}
