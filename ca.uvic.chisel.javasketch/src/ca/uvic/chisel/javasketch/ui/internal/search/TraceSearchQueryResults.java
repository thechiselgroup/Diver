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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultListener;
import org.eclipse.search.ui.text.Match;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;
import ca.uvic.chisel.javasketch.data.model.imple.internal.MethodKey;
import ca.uvic.chisel.javasketch.data.model.imple.internal.TraceModelProxy;

/**
 * @author Del Myers
 *
 */
public class TraceSearchQueryResults implements ISearchResult {
	
	
	
	private TraceSearchQuery query;
	private ListenerList listeners;
	private HashMap<IProgramSketch, Result> results;
	
	private static class Result {
		final List<ITraceModelProxy> proxies;
		final List<Match> annotations;
		/**
		 * 
		 */
		public Result(List<ITraceModelProxy> proxies, List<Match> annotations) {
			this.proxies = proxies;
			this.annotations = annotations;
		}
	}

	/**
	 * 
	 */
	TraceSearchQueryResults(TraceSearchQuery query) {
		this.query = query;
		this.listeners = new ListenerList();
		results = new HashMap<IProgramSketch, Result>();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#addListener(org.eclipse.search.ui.ISearchResultListener)
	 */
	@Override
	public void addListener(ISearchResultListener l) {
		listeners.add(l);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		return SketchPlugin.imageDescriptorFromPlugin("images/trace_search.png");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getLabel()
	 */
	@Override
	public String getLabel() {
		return query.getLabel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getQuery()
	 */
	@Override
	public ISearchQuery getQuery() {
		return query;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getTooltip()
	 */
	@Override
	public String getTooltip() {
		return getLabel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#removeListener(org.eclipse.search.ui.ISearchResultListener)
	 */
	@Override
	public void removeListener(ISearchResultListener l) {
		listeners.remove(l);
	}
	
	void updateSearch(IProgramSketch sketch, SortedSet<String> classes, SortedSet<MethodKey> methods, SortedSet<Long> activations, List<Match> matches) {
		LinkedList<ITraceModelProxy> proxies = new LinkedList<ITraceModelProxy>();
		if (classes != null) {
			for (String c : classes) {
				ITraceModelProxy proxy = TraceModelProxy.forClass(sketch.getTraceData(), c);
				proxies.add(proxy);
			}
			if (methods != null) {
				for (MethodKey mk : methods) {
					proxies.add(TraceModelProxy.forMethod(sketch.getTraceData(), mk));
				}
				if (activations != null) {
					for (Long a : activations) {
						proxies.add(TraceModelProxy.forActivation(sketch.getTraceData(), a.longValue()));
					}
				}
			}
		}
		
		this.results.put(sketch, new Result(proxies, matches));
		for (Object o : listeners.getListeners()) {
			ISearchResultListener listener = (ISearchResultListener) o;
			listener.searchResultChanged(new TraceSearchResultEvent(this, sketch, proxies));
		}
	}
	
	
	/**
	 * Returns all of the search results for the given sketch in order of classes
	 * first, then methods, then activations, or null if none exist.
	 * @param sketch
	 * @return
	 */
	public List<ITraceModelProxy> getFoundElements(IProgramSketch sketch) {
		Result result = results.get(sketch);
		if (result != null) {
			return result.proxies;
		}
		return null;
	}
	
	public List<Match> getFoundAnnotations(IProgramSketch sketch) {
		Result result = results.get(sketch);
		if (result != null) {
			if (result.annotations != null) {
				return result.annotations;
			}
		}
		return Collections.emptyList();
	}
	
	public IProgramSketch[] getSketches() {
		Set<IProgramSketch> keys = results.keySet();
		return keys.toArray(new IProgramSketch[results.keySet().size()]);
	}

}
