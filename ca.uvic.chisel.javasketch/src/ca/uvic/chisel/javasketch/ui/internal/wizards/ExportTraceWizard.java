package ca.uvic.chisel.javasketch.ui.internal.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;

public class ExportTraceWizard extends Wizard implements IExportWizard {

	private ITrace trace;
	private ExportTraceWizardPage1 exportPage;

	private static class ExportTraceJob extends Job {

		private int[] threadIDs;
		private ITrace trace;
		private File file;

		ExportTraceJob(ITrace trace, int[] threadIDs, File file) {
			super("Exporting trace " + trace.toString());
			this.trace = trace;
			this.threadIDs = threadIDs;
			this.file = file;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime
		 * .IProgressMonitor)
		 */
		@Override
		public IStatus run(IProgressMonitor monitor) {
			if (trace == null || file == null || threadIDs == null
					|| file.isDirectory() || threadIDs.length == 0) {
				return Status.OK_STATUS;
			}
			IProgramSketch sketch = SketchPlugin.getDefault().getSketch(trace);
			if (sketch == null) {
				return new Status(IStatus.WARNING, SketchPlugin.PLUGIN_ID,
					"Unable to find metadata for trace");
			}
			URL dataPath = sketch.getTracePath();
			boolean error = false;
			try {
				File dataDirectory = new File(dataPath.toURI());
				ZipOutputStream zipStream = new ZipOutputStream(
					new FileOutputStream(file));
				LinkedList<File> filesToProcess = new LinkedList<File>();
				filesToProcess.add(new File(dataDirectory, ".filters"));
				filesToProcess
					.add(new File(dataDirectory, "process.properties"));
				filesToProcess.add(new File(dataDirectory,
					"launch.configuration"));
				filesToProcess.add(new File(dataDirectory, "agent.log"));
				for (int t : threadIDs) {
					filesToProcess.add(new File(dataDirectory, t + ".trace"));
				}
				monitor.beginTask("Exporting " + sketch.getLabel(),
					filesToProcess.size());
				for (File file : filesToProcess) {
					try {
						
						FileInputStream is = new FileInputStream(file);
						ZipEntry entry = new ZipEntry(
							file.getParentFile().getParentFile().getName() +
							"/" + file.getParentFile().getName()
								+ "/" + file.getName());
						monitor.subTask(entry.getName());
						zipStream.putNextEntry(entry);
						byte[] buf = new byte[1024];
						int read = -1;
						while ((read = is.read(buf)) >= 0) {
							zipStream.write(buf, 0, read);
						}
						is.close();
						zipStream.closeEntry();
					} catch (FileNotFoundException e) {
						SketchPlugin
							.getDefault()
							.getLog()
							.log(
								new Status(
									IStatus.WARNING,
									SketchPlugin.PLUGIN_ID,
									"Unable to export trace file "
											+ file.getAbsolutePath()
											+ ". The exported data may be corrupt."));
					}
				}
				zipStream.close();
				monitor.done();
			} catch (URISyntaxException e) {
				return new Status(IStatus.ERROR, SketchPlugin.PLUGIN_ID,
					"Unable to resolve metadata for trace", e);
			} catch (FileNotFoundException e) {
				return new Status(IStatus.ERROR, SketchPlugin.PLUGIN_ID,
					"Unable to create export file for trace", e);
			} catch (IOException e) {
				return new Status(IStatus.ERROR, SketchPlugin.PLUGIN_ID,
					"Unable to export trace", e);
			}
			if (!error) {
				return Status.OK_STATUS;
			} else {
				return new Status(IStatus.WARNING, SketchPlugin.PLUGIN_ID,
					"One or more trace files could not be exported. See error log for details");
			}
		}

	}

	public ExportTraceWizard() {
		exportPage = new ExportTraceWizardPage1();
	}

	@Override
	public boolean performFinish() {
		exportPage.save();
		File file = exportPage.getDestinationFile();
		if (file.exists()) {
			if (!MessageDialog.openQuestion(getShell(), "File Exists", "The selected file already exists. Would you like to overwrite it?")) {
				return false;
			}
		}
		Job job = new ExportTraceJob(exportPage.getTrace(),
			exportPage.getThreadIDs(), exportPage.getDestinationFile());
		PlatformUI.getWorkbench().getProgressService().showInDialog(
			getShell(),
			job
			);
		job.schedule();
		return true;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		IDialogSettings pluginSettings = SketchPlugin.getDefault()
			.getDialogSettings();
		IDialogSettings exportSettings = pluginSettings
			.getSection("diver.trace.export");
		if (exportSettings == null) {
			exportSettings = pluginSettings.addNewSection("diver.trace.export");
		}
		setDialogSettings(exportSettings);
		setSelection(selection);
		addPage(exportPage);
	}

	/**
	 * @param selection
	 */
	private void setSelection(IStructuredSelection selection) {
		for (Iterator<?> it = selection.iterator(); it.hasNext();) {
			Object next = it.next();
			if (next instanceof ITraceModel) {
				this.trace = ((ITraceModel) next).getTrace();
			} else if (next instanceof IProgramSketch) {
				this.trace = ((IProgramSketch)next).getTraceData();
			}
		}
	}

	/**
	 * @return the trace
	 */
	public ITrace getTrace() {
		return trace;
	}

}
