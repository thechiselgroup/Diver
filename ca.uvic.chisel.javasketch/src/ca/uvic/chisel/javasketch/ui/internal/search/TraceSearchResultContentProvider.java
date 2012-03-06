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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.text.Match;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;

/**
 * @author Del Myers
 *
 */
public class TraceSearchResultContentProvider implements ITreeContentProvider {
	boolean isTree = true;
	private TraceSearchQueryResults input;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		LinkedList<Object> results = new LinkedList<Object>();
		if (parentElement == null) {
			return new Object[0];
		} else if (!isTree) {
			if (parentElement instanceof TraceSearchQueryResults) {
				TraceSearchQueryResults sr = (TraceSearchQueryResults) parentElement;
				for (IProgramSketch sketch : sr.getSketches()) {
					for (ITraceModelProxy result : sr.getFoundElements(sketch)) {
						Object element = result.getElement();
						if (element != null) {
							results.add(element);
						}
					}
				}
			}
		} else {
			TraceSearchQueryResults sr = input;
			
			//add annotations to the top
			if (parentElement instanceof ITraceModel) {
				List<Match> elementAnnotations = getAnnotations((ITraceModel) parentElement);
				results.addAll(elementAnnotations);
			}
			if (parentElement instanceof TraceSearchQueryResults) {
				return ((TraceSearchQueryResults)parentElement).getSketches();
			} else if (parentElement instanceof IProgramSketch) {
				IProgramSketch sketch = (IProgramSketch) parentElement;
				List<Match> annotations = input.getFoundAnnotations(sketch);
				if (annotations != null && annotations.size() > 0) {
					List<Match> sketchAnnotations = new ArrayList<Match>();
					for (Match annotation : annotations) {
						ITraceModelProxy proxy = (ITraceModelProxy) annotation.getElement();
						if (!proxy.isMessageElement()) {
							if ((proxy.getKind() != ITraceModel.ACTIVATION) &&
								(proxy.getKind() != ITraceModel.TRACE_CLASS) &&
								(proxy.getKind() != ITraceModel.TRACE_CLASS_METHOD)) {
								sketchAnnotations.add(annotation);
							}
						}
					}
					if (sketchAnnotations.size() > 0) {
						results.add(sketchAnnotations);
					}
				}
				for (ITraceModelProxy result : sr.getFoundElements(sketch)) {
					if (result.getKind() == ITraceModel.TRACE_CLASS) {
						results.add(result.getElement());
					} else {
						break;
					}
				}
			} else if (parentElement instanceof List<?>) {
				return ((List<?>)parentElement).toArray();
			} else if (parentElement instanceof ITraceClass) {
				String className = ((ITraceClass)parentElement).getName();
				IProgramSketch sketch = SketchPlugin.getDefault().getSketch((ITraceClass)parentElement);
				if (sketch == null) {
					return new Object[0];
				}
				for (ITraceModelProxy result : sr.getFoundElements(sketch)) {
					if (result.getKind() == ITraceModel.TRACE_CLASS) {
						continue;
					} else if (result.getKind() == ITraceModel.TRACE_CLASS_METHOD){
						String methodSig = result.toString();
						//check to see if it is in the same class
						int paren = methodSig.indexOf('(');
						if (paren > 0) {
							String longName = methodSig.substring(0, paren);
							int dot = longName.lastIndexOf('.');
							if (dot > 0) {
								String mclassName = longName.substring(0, dot);
								if (mclassName.equals(className)) {
									ITraceModel element = result.getElement();
									if (element != null) {
										results.add(element);
									}
								}
							}
						}
					} else {
						break;
					}
				}
			} else if (parentElement instanceof ITraceClassMethod) {
				IProgramSketch sketch = SketchPlugin.getDefault().getSketch((ITraceModel)parentElement);
				for (ITraceModelProxy result : sr.getFoundElements(sketch)) {
					if (result.getKind() == ITraceModel.ACTIVATION) {
						IActivation element = (IActivation) result.getElement();
						if (element != null && parentElement.equals(element.getMethod())) {
							results.add(element);
						}
					}
				}
			} 
		}
		return results.toArray();
	}

	/**
	 * @param element
	 * @param annotations
	 * @return
	 */
	private List<Match> getAnnotations(ITraceModel element) {
		List<Match> matches = new LinkedList<Match>();
		IProgramSketch sketch = SketchPlugin.getDefault().getSketch(element);
		if (sketch != null) {
			for (Match match : input.getFoundAnnotations(sketch)) {
				ITraceModelProxy proxy = (ITraceModelProxy) match.getElement();
				if (proxy.isMessageElement() && element instanceof IActivation) {
					IMessage message = (IMessage) proxy.getElement();
					if (message instanceof IOriginMessage) {
						//set the message to the target
						message = ((IOriginMessage)message).getTarget();
					}
					if (message != null && message.getActivation().getIdentifier().equals(element.getIdentifier())) {
						matches.add(match);
					}
				} else if (proxy.getElementId().equals(element.getIdentifier())) {
					matches.add(match);
				}
			}
		}
		return matches;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (input == null) return null;
		if (element instanceof ITraceModel) {
			switch (((ITraceModel) element).getKind()) {
			case ITraceModel.ACTIVATION:
				return ((IActivation)element).getMethod();
			case ITraceModel.TRACE_CLASS_METHOD:
				return ((ITraceClassMethod)element).getTraceClass();
			case ITraceModel.TRACE_CLASS:
				return SketchPlugin.getDefault().getSketch((ITraceModel) element);
			}
		} else if (element instanceof Match) {
			Match match = (Match) element;
			if (match.getElement() instanceof ITraceModelProxy) {
				ITraceModelProxy proxy = (ITraceModelProxy) match.getElement();
				if (proxy != null) {
					IProgramSketch sketch = SketchPlugin.getDefault().getSketch(proxy.getTrace());
					if (sketch != null) {
						return input.getFoundAnnotations(sketch);
					}
				}
			}
		} else if (element instanceof List<?>) {
			if (((List<?>) element).size() > 0) {
				Object o = ((List<?>)element).get(0);
				if (o instanceof ITraceModelProxy) {
					return SketchPlugin.getDefault().getSketch(((ITraceModelProxy) o).getTrace());
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		return (!(element instanceof Match));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		assert(inputElement == null || inputElement instanceof TraceSearchQueryResults);
		return getChildren(inputElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		assert(newInput == null || newInput instanceof TraceSearchQueryResults);
		input = (TraceSearchQueryResults)newInput;
	}
	
	public void setLayout(boolean isTree) {
		this.isTree = isTree;
	}

}
