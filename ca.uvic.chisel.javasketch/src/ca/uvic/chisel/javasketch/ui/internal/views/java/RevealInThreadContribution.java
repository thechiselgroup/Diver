package ca.uvic.chisel.javasketch.ui.internal.views.java;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;
import ca.uvic.chisel.javasketch.ui.ISketchImageConstants;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.CommandAction;
import ca.uvic.chisel.javasketch.ui.internal.presentation.commands.RevealActivationHandler;

public class RevealInThreadContribution extends ContributionItem {

//	public class RevealInThreadAction extends Action {
//
//		private IJavaElement element;
//		private IThread thread;
//
//		/**
//		 * @param thread
//		 * @param selection
//		 */
//		public RevealInThreadAction(IThread thread, IJavaElement element) {
//			this.element = element;
//			this.thread = thread;
//		}
//
//		/*
//		 * (non-Javadoc)
//		 * @see org.eclipse.jface.action.Action#run()
//		 */
//		@Override
//		public void run() {
//			IWorkbenchPage page = PlatformUI.getWorkbench()
//				.getActiveWorkbenchWindow().getActivePage();
//			IViewPart part = page.findView(JavaSketchView.VIEW_ID);
//			ICommandService cs = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
//			IHandlerService hs = (IHandlerService) PlatformUI.getWorkbench().getService(ICommandService.class);
//			
//			if (part instanceof JavaSketchView) {
//				JavaSketchView sv = (JavaSketchView) part;
//				List<IActivation> activations = null;
//				try {
//					if (element instanceof IMethod) {
//						activations = JavaSearchUtils.findActivationsForMethod(
//							thread, (IMethod) element,
//							new NullProgressMonitor());
//					} else if (element instanceof IType) {
//						activations = JavaSearchUtils.findActivationsForClass(
//							thread, (IType) element, new NullProgressMonitor());
//					}
//				} catch (CoreException e) {
//				}
//				if (activations != null && !activations.isEmpty()) {
//					IActivation first = activations.get(0);
//					page.bringToTop(sv);
//					sv.reveal(first);
//				}
//
//			}
//		}
//
//		/*
//		 * (non-Javadoc)
//		 * @see org.eclipse.jface.action.Action#getText()
//		 */
//		@Override
//		public String getText() {
//			return thread.getName();
//		}
//	}

	public RevealInThreadContribution() {
	}

	public RevealInThreadContribution(String id) {
		super(id);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jface.action.ContributionItem#fill(org.eclipse.swt.widgets
	 * .Menu, int)
	 */
	@Override
	public void fill(Menu menu, int index) {
		MenuManager manager = new MenuManager();
		fill(manager);
		IContributionItem items[] = manager.getItems();
		for (int i = 0; i < items.length; i++) {
			items[i].fill(menu, index++);
		}
	}

	/**
	 * @param manager
	 */
	private void fill(MenuManager manager) {
		IProgramSketch sketch = SketchPlugin.getDefault().getActiveSketch();
		if (sketch != null && !sketch.isAnalysing() && !sketch.isRunning()
				&& sketch.isConnected()) {
			IJavaElement selection = getActiveSelection();
			IThread[] threads = JavaSearchUtils.findThreadsForElement(sketch
				.getTraceData(), selection);
			for (IThread thread : threads) {
				Map<String, String> parameters = new HashMap<String, String>();
				parameters.put(RevealActivationHandler.TRACE_PARAMETER, sketch.getID());
				parameters.put(RevealActivationHandler.THREAD_PARAMETER, thread.getIdentifier());
				IAction action = new CommandAction(RevealActivationHandler.COMMAND_ID, parameters);
				action.setImageDescriptor(SketchPlugin.getDefault().getImageRegistry().getDescriptor(ISketchImageConstants.ICON_TRACE_EDITOR));
				action.setText(thread.getName());
				manager.add(action);
			}
		}
//		Separator separator = new Separator(org.eclipse.ui.IWorkbenchActionConstants.MB_ADDITIONS);
//		separator.setId(org.eclipse.ui.IWorkbenchActionConstants.MB_ADDITIONS);
//		manager.add(separator);

	}

	/**
	 * @return
	 */
	private IJavaElement getActiveSelection() {
		IWorkbenchPartSite site = PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow().getActivePage().getActivePart()
			.getSite();
		ISelection selection = site.getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			for (Iterator<?> it = ss.iterator(); it.hasNext();) {
				Object o = it.next();
				if (o instanceof IAdaptable) {
					return (IJavaElement) ((IAdaptable) o)
						.getAdapter(IJavaElement.class);
				} else if (o != null) {
					Object element = Platform.getAdapterManager().getAdapter(o,
						IJavaElement.class);
					if (element instanceof IJavaElement) {
						return (IJavaElement) element;
					}
				}
			}
		}
		return null;
	}

}
