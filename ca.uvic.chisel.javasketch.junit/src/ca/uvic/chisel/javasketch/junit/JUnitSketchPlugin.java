package ca.uvic.chisel.javasketch.junit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.framework.adaptor.StatusException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class JUnitSketchPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ca.uvic.chisel.javasketch.junit"; //$NON-NLS-1$

	// The shared instance
	private static JUnitSketchPlugin plugin;
	
	/**
	 * The constructor
	 */
	public JUnitSketchPlugin() {
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
	public static JUnitSketchPlugin getDefault() {
		return plugin;
	}

	/**
	 * Logs the given status to plugin log.
	 * @param status the status to log.
	 */
	public void log(IStatus status) {
		getLog().log(status);		
	}
	
	public void log(Exception ex) {
		if (ex instanceof CoreException) {
			log(((CoreException)ex).getStatus());
		} else {
			log(new Status(IStatus.ERROR, PLUGIN_ID, ex.getMessage(), ex));
		}
	}

}
