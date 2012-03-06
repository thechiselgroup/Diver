package ca.uvic.chisel.javasketch.ui.internal.views;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.internal.JavaSearchUtils;
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
//					//sv.reveal(first);
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
			try {
				ITraceModel selection = getActiveSelection();
				if (selection == null) return;
				IJavaElement element = JavaSearchUtils.findElement(selection, new NullProgressMonitor());
				IThread[] threads = JavaSearchUtils.findThreadsForElement(selection.getTrace(), element);
				for (IThread thread : threads) {
					Map<String, String> parameters = new HashMap<String, String>();
					parameters.put(RevealActivationHandler.TRACE_PARAMETER, selection.getTrace().getIdentifier());
					parameters.put(RevealActivationHandler.THREAD_PARAMETER, thread.getIdentifier());
					CommandContributionItemParameter parameter = 
						new CommandContributionItemParameter(
							PlatformUI.getWorkbench(), 
							null, 
							RevealActivationHandler.COMMAND_ID, 
							parameters, 
							null, 
							null, 
							null, 
							thread.getName(), 
							null, 
							null, 
							SWT.PUSH, 
							null, 
							true
						);
					CommandContributionItem item = new CommandContributionItem(parameter);
					manager.add(item);
				}
			} catch (InterruptedException e) {
				
			} catch (CoreException e) {
				SketchPlugin.getDefault().log(e);
			}
		
	}

	/**
	 * @return
	 */
	private ITraceModel getActiveSelection() {
		IWorkbenchPartSite site = PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow().getActivePage().getActivePart()
			.getSite();
		ISelection selection = site.getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection) {
			return getTraceModelForSelection((IStructuredSelection) selection);
		}
		return null;
	}

	private ITraceModel getTraceModelForSelection(IStructuredSelection ss) {
		ITraceModel tm = null;
		for (Iterator<?> it = ss.iterator(); it.hasNext() && tm == null;) {
			Object o = it.next();
			if (o instanceof ITraceModel) {
				tm = (ITraceModel) o;
			} else if (o instanceof IAdaptable) {
				tm = (ITraceModel) ((IAdaptable)o).getAdapter(ITraceModel.class);
			} else {
				tm = (ITraceModel) Platform.getAdapterManager().getAdapter(o, ITraceModel.class);
			}
			if (tm != null) {
				return tm;
			}
		}
		return tm;
	}

}
