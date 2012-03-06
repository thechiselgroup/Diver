package ca.uvic.chisel.diver.logging.loggers;

import org.eclipse.zest.custom.uml.viewers.SequenceViewerEvent;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerGroupEvent;
import org.eclipse.zest.custom.uml.viewers.SequenceViewerRootEvent;

import ca.uvic.chisel.logging.eclipse.ILogObjectInterpreter;

public class SequenceEventInterpreter implements ILogObjectInterpreter {

	public SequenceEventInterpreter() {
	}

	@Override
	public String toString(Object object) {
		String eventString = "data=";
		Object eventObject = null;
		if (object instanceof SequenceViewerEvent) {
			SequenceViewerEvent event = (SequenceViewerEvent) object;
			eventObject = event.getElement();
		} else if (object instanceof SequenceViewerGroupEvent) {
			SequenceViewerGroupEvent event = (SequenceViewerGroupEvent) object;
			eventObject = event.getGroup();
		} else if (object instanceof SequenceViewerRootEvent) {
			SequenceViewerRootEvent event = (SequenceViewerRootEvent) object;
			eventObject = event.getSequenceViewer().getRootActivation();
		}
		return eventString + ((eventObject == null) ? "" :
			eventObject.getClass().getName() + "@" + System.identityHashCode(eventObject));
	}

}
