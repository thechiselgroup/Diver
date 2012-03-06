/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.ui.internal.search;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;
import ca.uvic.chisel.javasketch.ui.ISketchImageConstants;
import ca.uvic.chisel.javasketch.ui.internal.presentation.metadata.PresentationData;
import ca.uvic.chisel.javasketch.ui.internal.views.ParentedCalendar;
import ca.uvic.chisel.widgets.TimeField;

/**
 * @author Del Myers
 *
 */
public class TraceSearchResultLabelProvider extends StyledCellLabelProvider
		implements IBaseLabelProvider {

	private WorkbenchLabelProvider wbProvider;

	/**
	 * 
	 */
	public TraceSearchResultLabelProvider() {
		wbProvider = new WorkbenchLabelProvider();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	public Image getColumnImage(Object element, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return getImage(element);
		}
		return null;
	}
	
	public Image getImage(Object element) {
		
		if (element instanceof IProgramSketch) {
			String key = ISketchImageConstants.ICON_PROCESS_TRACE;
			return SketchPlugin.getDefault().getImageRegistry().get(key);
		} else if (element instanceof IThread) {
			return SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_THREAD_TRACE);
		} else if (element instanceof ILaunchConfiguration) {
			return SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_TRACE);
		} else if (element instanceof ParentedCalendar) {
			return SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_CALENDAR);
		} else if (element instanceof ITraceModel) {
			switch (((ITraceModel)element).getKind()) {
			case ITraceModel.TRACE_CLASS:
				return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
			case ITraceModel.TRACE_CLASS_METHOD:
				return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PUBLIC);
			case ITraceModel.ACTIVATION:
				return SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_ACTIVATION);
			case ITraceModel.TRACE:
				return SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_TRACE);
			}
		} else if (element instanceof List<?>) {
			return SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_ANNOTATIONS);
		} else if (element instanceof Match) {
			return SketchPlugin.getDefault().getImageRegistry().get(ISketchImageConstants.ICON_ANNOTATION);
		}
	return null;
}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */

	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof ITraceModel) {
			ITraceModel mElement = (ITraceModel) element;
			switch (columnIndex) {
			case 0:
				return getText(element);
			case 1:
				switch (mElement.getKind()) {
				case ITraceModel.ACTIVATION:
					return "Activation";
				case ITraceModel.TRACE_CLASS:
					return "Class";
				case ITraceModel.TRACE_CLASS_METHOD:
					return "Method";
				case ITraceModel.THREAD:
					return "Thread";
				case ITraceModel.TRACE:
					return "Trace";
				}
				if ((mElement.getKind() & ITraceModel.MESSAGE) != 0) {
					return "Message";
				}
				break;
			case 2: return getText(mElement.getTrace());
			}
		} else if (element instanceof IProgramSketch) {
			IProgramSketch sketch = (IProgramSketch) element;
			switch (columnIndex) {
			case 0: return getText(element);
			case 1:	return "Traced Launch";
			case 2: return getText(sketch.getTraceData()); 
			}
		} else if (element instanceof List<?>) {
			if (columnIndex == 0) {
				return "Notes";
			}
		} 
		return "";
	}
	
	public String getText(Object element) {

		if (element instanceof IThread) {
			IThread thread = (IThread) element;
			return thread.getName() + " (" + thread.getID() + ")";
		} else if (element instanceof IProgramSketch) {
			IProgramSketch sketch = (IProgramSketch) element;
			DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
			return sketch.getLabel() + " " + timeFormat.format(sketch.getProcessTime());
		} else if (element instanceof IProject) {
			return ((IProject)element).getName();
		} else if (element instanceof ParentedCalendar) {
			Calendar day = ((ParentedCalendar) element).getCalendar();
			DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM);
			return format.format(day.getTime());
		} else if (element instanceof ITraceModel) {
			switch (((ITraceModel)element).getKind()) {
			case ITraceModel.ACTIVATION:
				long time = ((IActivation)element).getTime();
				return "Activated at " + TimeField.toString(time);
			case ITraceModel.TRACE:
				String lid = ((ITrace)element).getLaunchID();
				int dot = lid.lastIndexOf('.');
				if (dot > 0) {
					lid = lid.substring(0, dot);
				}
				return lid;
			case ITraceModel.TRACE_CLASS_METHOD:
				return ((ITraceClassMethod)element).getName();
			}
			
		}
		return element.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		wbProvider.dispose();
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.CellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
	 */
	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		int columnIndex = cell.getColumnIndex();
		cell.setImage(getColumnImage(element, columnIndex));
		if (element instanceof Match) {
			String text = "";
			switch (columnIndex) {
			case 0:
				setMatchText(cell);
				break;
			case 1:
				ITraceModelProxy mElement = (ITraceModelProxy) ((Match)element).getElement();
				switch (mElement.getKind()) {
				case ITraceModel.ACTIVATION:
					text = "Activation";
					break;
				case ITraceModel.TRACE_CLASS:
					text = "Class";
					break;
				case ITraceModel.TRACE_CLASS_METHOD:
					text = "Method";
					break;
				case ITraceModel.THREAD:
					text = "Thread";
					break;
				case ITraceModel.TRACE:
					text = "Trace";
					break;
				}
				if ((mElement.getKind() & ITraceModel.MESSAGE) == ITraceModel.MESSAGE) {
					text = "Message";
				}
				cell.setText(text);
			}
		} else {
			cell.setText(getColumnText(element, columnIndex));
		}
	}

	/**
	 * @param cell
	 */
	private void setMatchText(ViewerCell cell) {
		Match match = (Match) cell.getElement();
		ITraceModelProxy proxy = (ITraceModelProxy) match.getElement();
		IProgramSketch sketch = SketchPlugin.getDefault().getSketch(proxy.getTrace());
		PresentationData pd = PresentationData.connect(sketch);
		try {
			String annotation = pd.getAnnotation(proxy.getElementId());
			if (annotation != null) {
				annotation = annotation.replaceAll("\\s+", " ");
				if (annotation.length() >= match.getOffset()+match.getLength()) {
				int summaryOffset = 0;
				String summary = annotation.substring(match.getOffset(), match.getOffset() + match.getLength());
				StringBuilder builder = new StringBuilder(summary);
				int wordCount = 0;
				int index = match.getOffset() -1;
				while (wordCount < 3 && index >= 0) {
					char c = annotation.charAt(index);
					if (Character.isWhitespace(c)) {
						wordCount++;
						if (wordCount > 3) {
							break;
						}
					}
					builder.insert(0, c);
					summaryOffset++;
					index--;
				}
				if (index >= 0) {
					builder.insert(0, "...");
					summaryOffset += 3;
				}
				index = match.getOffset() + match.getLength();
				wordCount = 0;
				while (wordCount < 3 && index < annotation.length()) {
					char c = annotation.charAt(index);
					if (Character.isWhitespace(c)) {
						wordCount++;
						if (wordCount > 3) {
							break;
						}
					}
					builder.append(c);
					index++;
				}
				if (index < annotation.length()) {
					builder.append("...");
				}
				cell.setText(builder.toString());
				Color fg = cell.getForeground();
				Color bg = cell.getFont().getDevice().getSystemColor(SWT.COLOR_YELLOW);
				StyleRange range = new StyleRange(summaryOffset, match.getLength(), fg, bg);
				cell.setStyleRanges(new StyleRange[]{range});
			}
			}
		} finally {
			pd.disconnect();
		}
		
	}
}
