package ca.uvic.chisel.javasketch.ui.internal;

import java.text.DateFormat;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.javasketch.data.model.ICatch;
import ca.uvic.chisel.javasketch.data.model.IReply;
import ca.uvic.chisel.javasketch.data.model.IReturn;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.IThrow;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.ui.ISketchImageConstants;

public class TraceWorkbenchAdapterFactory implements IAdapterFactory {
	
	private static class TraceWorkbenchAdapter implements IWorkbenchAdapter {

		/* (non-Javadoc)
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
		 */
		@Override
		public Object[] getChildren(Object o) {
			if (o instanceof ITraceModel) {
				ITraceModel model = (ITraceModel) o;
				switch (model.getKind()) {
				case ITraceModel.ACTIVATION:
				case ITraceModel.ARRIVAL:
				case ITraceModel.CALL:
				case ITraceModel.CATCH:
				case ITraceModel.REPLY:
				case ITraceModel.RETURN:
				case ITraceModel.THROW:
				case ITraceModel.THREAD:
					return new Object[0];
				case ITraceModel.TRACE:
					return ((ITrace)model).getThreads().toArray();
				}
			}
			return new Object[0];
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
		 */
		@Override
		public ImageDescriptor getImageDescriptor(Object o) {
			if (o instanceof ITraceModel) {
				ITraceModel model = (ITraceModel) o;
				switch (model.getKind()) {
				case ITraceModel.ACTIVATION:
					return SketchPlugin.getDefault().getImageRegistry().getDescriptor(ISketchImageConstants.ICON_ACTIVATION);
				case ITraceModel.ARRIVAL:
				case ITraceModel.CALL:
				case ITraceModel.CATCH:
				case ITraceModel.REPLY:
				case ITraceModel.RETURN:
				case ITraceModel.THROW:
				case ITraceModel.THREAD:
					return SketchPlugin.getDefault().getImageRegistry().getDescriptor(ISketchImageConstants.ICON_THREAD_TRACE);
				case ITraceModel.TRACE:
					return SketchPlugin.getDefault().getImageRegistry().getDescriptor(ISketchImageConstants.ICON_TRACE);
				}
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
		 */
		@Override
		public String getLabel(Object o) {
			if (o instanceof ITraceModel) {
				try {
					ITraceModel model = (ITraceModel) o;
					switch (model.getKind()) {
					case ITraceModel.ACTIVATION:
						return ((IActivation)model).getMethod().getName();
					case ITraceModel.ARRIVAL:
						return "";
					case ITraceModel.CALL:
						return ((ICall)model).getTarget().getActivation().getMethod().getName();
					case ITraceModel.CATCH:
						return "catch " + ((IThrow)((ICatch)model).getOrigin()).getReturnValue();
					case ITraceModel.REPLY:
						return ((IReply)model).getReturnValue();
					case ITraceModel.RETURN:
						return ((IReply)((IReturn)model).getOrigin()).getReturnValue();
					case ITraceModel.THROW:
						return "throw " + ((IThrow)model).getReturnValue();
					case ITraceModel.THREAD:
						return ((IThread)model).getName();
					case ITraceModel.TRACE:
						IProgramSketch sketch = SketchPlugin.getDefault().getSketch(model);
						DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
						return sketch.getLabel() + " " + timeFormat.format(sketch.getProcessTime());
					}
				} catch (Exception e) {
					return "";
				}
			}
			return "";
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
		 */
		@Override
		public Object getParent(Object o) {
			if (o instanceof IThread) {
				return ((IThread)o).getTrace();
			}
			return null;
		}
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof ITraceModel && IWorkbenchAdapter.class.isAssignableFrom(adapterType)) {
			return new TraceWorkbenchAdapter();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class[] getAdapterList() {
		return new Class[] {IWorkbenchAdapter.class};
	}

}
