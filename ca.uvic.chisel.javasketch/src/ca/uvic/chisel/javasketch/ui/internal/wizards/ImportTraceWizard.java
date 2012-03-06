package ca.uvic.chisel.javasketch.ui.internal.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.javasketch.FilterSettings;
import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.launching.LocalDataBaseTraceClient;
import ca.uvic.chisel.javasketch.launching.ui.FilterTab;
import ca.uvic.chisel.javasketch.persistence.internal.PersistTraceJob;
import ca.uvic.chisel.javasketch.utils.LaunchConfigurationUtilities;

public class ImportTraceWizard extends Wizard implements IImportWizard {
	private class ImportTraceJob extends Job {

		private File file;
		private File installDirectory;
		private ILaunchConfiguration configuration;
		private String newName;
		private String traceID;

		/**
		 * @param name
		 */
		public ImportTraceJob(File file, File installDirectory, ILaunchConfiguration configuration, String newName, String traceID) {
			super("Importing Trace");
			this.file = file;
			this.installDirectory = installDirectory;
			this.configuration =  configuration;
			this.newName = newName;
			this.traceID = traceID;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			// copy all of the files in the zip file
			ZipFile zip = null;
			IStatus status = Status.OK_STATUS;
			try {
				zip = new ZipFile(file);
				monitor.beginTask("Importing Trace...", zip.size() + 3);
				Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String entryName = entry.getName();
					Path entryPath = new Path(entryName);
					String entryFileName = entryPath.lastSegment();
					File entryFile = new File(installDirectory, entryFileName);
					FileOutputStream out = new FileOutputStream(entryFile);
					InputStream in = zip.getInputStream(entry);
					byte[] buf = new byte[1024];
					int read = -1;
					while ((read = in.read(buf)) != -1) {
						out.write(buf, 0, read);
					}
					out.close();
					monitor.worked(1);
				}
				
				//now, we have to update all of the properties files if 
				//there is a new name for the launch
				if (newName != null) {
					Properties props = new Properties();
					File propsFile = new File(installDirectory, "process.properties");
					props.load(new FileInputStream(propsFile));
					props.setProperty("id", traceID);
					props.setProperty(LocalDataBaseTraceClient.HOST_PROPERTY, newName);
					props.setProperty(LocalDataBaseTraceClient.LABEL_PROPERTY, newName);
					props.setProperty(LocalDataBaseTraceClient.PROCESS_PROPERTY, newName);
					props.store(new FileWriter(propsFile), null);
					//save the launch configuration
					String lcMemento = configuration.getMemento();
					File lcFile = new File(installDirectory, "launch.configuration");
					FileWriter printer = new FileWriter(lcFile);
					printer.write(lcMemento);
					printer.close();
				}
				IProgramSketch sketch = SketchPlugin.getDefault().getSketch(traceID);
				monitor.worked(3);
				if (sketch != null) {
					PersistTraceJob persist = new PersistTraceJob(sketch);
					persist.schedule();
				}
			} catch (Exception e) {
				status = SketchPlugin.getDefault().createStatus(e);
			} finally {
				monitor.done();
				if (zip != null) {
					try {
						zip.close();
					} catch (IOException e) {
						status = SketchPlugin.getDefault().createStatus(e);
					}
				}
			}
			return status;
		}
		
	}

	private ImportTraceWizardPage1 page;

	public ImportTraceWizard() {
		page = new ImportTraceWizardPage1();
	}

	@Override
	public boolean performFinish() {
		String installLocation = page.getTempInstallLocation();
		ILaunchConfiguration configuration = page.getTempLaunchConfiguration();
		FilterSettings settings = page.getTempFilterSettings();
		String launchType = settings.getLaunchType();
		File file = page.getFile();
		return importTrace(installLocation, configuration, settings,
			launchType, file);
	}

	/**
	 * @param installLocation
	 * @param configuration
	 * @param settings
	 * @param launchType
	 * @param file
	 * @return
	 */
	private boolean importTrace(String installLocation,
			ILaunchConfiguration configuration, FilterSettings settings,
			String launchType, File file) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		String newName = null;
		boolean createNewConfiguration = false;
		try {
			if (manager.isExistingLaunchConfigurationName(configuration
				.getName())) {
				System.out.println();
				// make sure that they are of the same type
				for (ILaunchConfiguration cf : manager
					.getLaunchConfigurations()) {
					if (cf.getName().equals(configuration.getName())) {
						ILaunchConfigurationType type = cf.getType();
						if (!type.getIdentifier().equals(launchType)) {
							newName = manager
								.generateUniqueLaunchConfigurationNameFrom(configuration
									.getName());
							createNewConfiguration = true;
							break;
						}
					}
				}
			} else {
				createNewConfiguration = true;
			}
			if (createNewConfiguration) {
				// create a new launch configuration
				ILaunchConfigurationType traceType = manager
					.getLaunchConfigurationType(launchType);
				ILaunchConfigurationWorkingCopy wc = traceType
					.newInstance(null, (newName != null) ? newName
							: configuration.getName());
				// initialize the configuration
				IJavaProject[] javaProjects = settings.getJavaProjects();
				if (javaProjects.length > 0) {
					LinkedList<String> projectNames = new LinkedList<String>();
					for (IJavaProject jp : javaProjects) {
						projectNames.add(jp.getProject().getName());
					}
					wc.setAttribute(
						LaunchConfigurationUtilities.ATTR_PROJECT_NAMES,
						projectNames);
					wc.setAttribute(
						IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
						javaProjects[0].getProject().getName());
					wc.setAttribute(FilterTab.PAUSE_ON_START, true);
					wc.setAttribute(FilterSettings.EXCLUSION_FILTERS, Arrays
						.asList(settings.getExclusionFilters()));
					wc.setAttribute(FilterSettings.INCLUSION_FILTERS, Arrays
						.asList(settings.getInclusionFilters()));
					wc.setAttribute(FilterSettings.USE_PROJECT_CLASSES,
						settings.isUsingProjectClassesOnly());
					wc.doSave();
					//it's really silly that I have to do this.
					for (ILaunchConfiguration find : manager.getLaunchConfigurations(traceType)) {
						if (find.getName().equals(wc.getName())) {
							configuration = find;
							break;
						}
					}
				}
			}
			// now copy all the files
			String newID = null;
			String traceID = null;
			String[] locationSegments = installLocation.split("/");
			// the second one will contain the launch id, it may have to change.
			traceID = locationSegments[1];
			if (newName != null) {
				String[] idSegments = locationSegments[1].split("\\.");
				newID = newName + '.' + idSegments[1];
				traceID = newID;
				installLocation = newName + "/" + newID;
			}
			File stateLocation = SketchPlugin.getDefault().getStateLocation()
				.toFile();
			File installDirectory = new File(stateLocation, installLocation);
			if (installDirectory.exists()) {
				boolean cont = MessageDialog.openQuestion(getShell(),
					"Overwrite current data",
					"A trace with the same ID as the imported trace"
							+ " already exists. Continuing the import will"
							+ " over-write the data. Continue?");
				if (!cont) {
					return true;
				} else {
					// get the sketch with the id
					IProgramSketch sketch = SketchPlugin.getDefault()
						.getSketch(traceID);
					if (sketch != null) {
						SketchPlugin.getDefault().deleteSketch(sketch, false);
					}
				}
			}
			if (!installDirectory.isDirectory()) {
				if (!installDirectory.mkdirs()) {
					MessageDialog
						.openError(
							getShell(),
							"Error Creating Storage Area",
							"An error occured while creating the storage area for "
									+ "the imported trace. The trace has not been imported.");
					return true;
				}
			}
			ImportTraceJob job = new ImportTraceJob(file, installDirectory, configuration, newName, traceID);
			PlatformUI.getWorkbench().getProgressService().showInDialog(
				getShell(),
				job
				);
			job.schedule();
		} catch (CoreException e) {
			SketchPlugin.getDefault().log(e);
			MessageDialog
				.openError(getShell(), "Error Importing Trace",
					"An error occurred while importing trace. See error log for details.");
		}
		return true;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		addPage(page);
		IDialogSettings settings = SketchPlugin.getDefault()
			.getDialogSettings();
		IDialogSettings importSettings = settings
			.getSection("diver.trace.import");
		if (importSettings == null) {
			importSettings = settings.addNewSection("diver.trace.import");
		}
		setDialogSettings(importSettings);

	}

}
