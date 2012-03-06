package ca.uvic.chisel.feature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class DiverPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ca.uvic.chisel.feature";

	// The shared instance
	private static DiverPlugin plugin;
	
	/**
	 * The constructor
	 */
	public DiverPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DiverPlugin getDefault() {
		return plugin;
	}

	/**
	 * @param e1
	 */
	public void log(Throwable e) {
		getLog().log(createStatus(e));		
	}

	/**
	 * @param e
	 * @return
	 */
	public IStatus createStatus(Throwable e) {
		
		if (e instanceof CoreException) {
			return ((CoreException)e).getStatus();
		}
		int severity = IStatus.WARNING;
		if (e instanceof Exception) {
			severity = IStatus.ERROR;
		}
		String message = e.getMessage();
		if (message == null) {
			message = "";
		}
		return new Status(severity, PLUGIN_ID, message);
	}

}
