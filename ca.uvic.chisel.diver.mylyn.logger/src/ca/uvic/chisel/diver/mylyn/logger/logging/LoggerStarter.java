package ca.uvic.chisel.diver.mylyn.logger.logging;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

public class LoggerStarter implements IStartup {

	private WindowListener windowListener;

	@Override
	public void earlyStartup() {
		this.windowListener = new WindowListener();
		windowListener.windowActivated(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		PlatformUI.getWorkbench().addWindowListener(windowListener);

	}

}
