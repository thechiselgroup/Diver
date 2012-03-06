package ca.uvic.chisel.diver.mylyn.logger;

import java.io.FileNotFoundException;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ca.uvic.chisel.diver.mylyn.logger.logging.SimpleLogger;

/**
 * The activator class controls the plug-in life cycle
 */
public class MylynLogger extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ca.uvic.chisel.diver.mylyn.logger"; //$NON-NLS-1$

	// The shared instance
	private static MylynLogger plugin;

	private SimpleLogger logger;
	
	/**
	 * The constructor
	 */
	public MylynLogger() {
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
	public static MylynLogger getDefault() {
		return plugin;
	}

	/**
	 * @return
	 */
	private synchronized SimpleLogger getLogger() {
		if (logger == null) {
			try {
				logger = new SimpleLogger();
			} catch (FileNotFoundException e) {
				return null;
			}
		}
		return logger;
	}
	
	public void logEvent(String event) {
		SimpleLogger logger = getLogger();
		if (logger != null) {
			logger.logLine(event);
		}
	}

}
