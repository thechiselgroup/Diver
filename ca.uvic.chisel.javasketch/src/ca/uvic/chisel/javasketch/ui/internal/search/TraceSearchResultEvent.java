package ca.uvic.chisel.javasketch.ui.internal.search;

import java.util.LinkedList;

import org.eclipse.search.ui.SearchResultEvent;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;

/**
 * @author Del Myers
 *
 */
final class TraceSearchResultEvent extends SearchResultEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private transient IProgramSketch sketch;
	private transient LinkedList<ITraceModelProxy> results;

	/**
	 * @param query
	 * @param sketch
	 * @param results
	 */
	public TraceSearchResultEvent(TraceSearchQueryResults result, IProgramSketch sketch,
			LinkedList<ITraceModelProxy> results) {
		super(result);
		this.sketch = sketch;
		this.results = results;
		
	}

	/**
	 * @return the sketch
	 */
	public IProgramSketch getSketch() {
		return sketch;
	}
	
	/**
	 * @return the results
	 */
	public LinkedList<ITraceModelProxy> getResults() {
		return results;
	}
}