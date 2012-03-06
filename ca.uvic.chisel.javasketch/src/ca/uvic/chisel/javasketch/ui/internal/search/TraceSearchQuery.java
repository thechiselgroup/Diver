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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.Match;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.ITargetMessage;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.data.model.ITraceModelProxy;
import ca.uvic.chisel.javasketch.data.model.imple.internal.ActivationImpl;
import ca.uvic.chisel.javasketch.data.model.imple.internal.MethodKey;
import ca.uvic.chisel.javasketch.ui.internal.presentation.metadata.PresentationData;

/**
 * @author Del Myers
 *
 */
public class TraceSearchQuery implements ISearchQuery {
	public static final int CASE_SENSITIVE = 1;
	public static final int CLASSES = 1 << 1;
	public static final int METHODS = 1 << 2;
	public static final int ACTIVATIONS = 1 << 3;
	public static final int PROPERTIES = 1 << 4;

	private TraceSearchQueryResults searchResults;
	private IProgramSketch[] scope;
	private String searchString;
	private int searchMask;

	TraceSearchQuery(IProgramSketch[] scope, String searchString, int searchMask) {
		this.scope = scope;
		this.searchString = searchString;
		if ((searchMask & CASE_SENSITIVE) == 0) {
			this.searchString = searchString.toUpperCase();
		}
		this.searchMask = searchMask;
		this.searchResults = new TraceSearchQueryResults(this);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#canRerun()
	 */
	@Override
	public boolean canRerun() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#canRunInBackground()
	 */
	@Override
	public boolean canRunInBackground() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	@Override
	public String getLabel() {
		return "Searching Traces for " + searchString;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#getSearchResult()
	 */
	@Override
	public ISearchResult getSearchResult() {
		return searchResults;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus run(IProgressMonitor monitor)
			throws OperationCanceledException {
		int work = scope.length * 100;
		monitor.beginTask("Searching Traces...", work);
		MultiStatus s = new MultiStatus(SketchPlugin.PLUGIN_ID, 0, "Trace Search", null);
		for (int i = 0; i < scope.length; i++) {
			try {
				SortedSet<String> classes = null;
				SortedSet<MethodKey> methods = null;
				SortedSet<Long> activations = null;
				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				monitor.subTask(scope[i].getLabel());
				if ((searchMask & CLASSES) != 0) {
					classes = new TreeSet<String>();
					searchForClasses(classes, scope[i]);
				}
				if ((searchMask & METHODS) != 0) {
					methods = new TreeSet<MethodKey>();
					searchForMethods(methods, scope[i]);
				}
				if ((searchMask & ACTIVATIONS) != 0) {
					SortedSet<String> activationClasses = new TreeSet<String>();
					SortedSet<MethodKey> activationMethods = new TreeSet<MethodKey>();
					if (classes != null) {
						activationClasses.addAll(classes);
					}
					if (methods != null) {
						activationMethods.addAll(methods);
					}
					activations = findActivations(activationClasses, activationMethods, scope[i]);
					//make sure that the methods and classes are included
					if (classes == null) {
						classes = activationClasses;
					}
					if (methods == null) {
						methods = activationMethods;
					}
				}
				if (methods != null) {
					//make sure that all of the classes are included
					//in the search results, if they have methods
					//associated with them.
					if (classes == null) {
						classes = new TreeSet<String>();
					}
					for (MethodKey mk : methods) {
						classes.add(mk.type);
					}
				}
				List<Match> matches = null;
				if ((searchMask & PROPERTIES) != 0) {
					matches = searchForAnnotations(scope[i]);
					for (Match match : matches) {
						ITraceModelProxy proxy = (ITraceModelProxy) match.getElement();
						IActivation activation = null;
						ITraceClassMethod method = null;
						ITraceClass clazz = null;
						if (proxy.isMessageElement()) {
							IMessage message = (IMessage) proxy.getElement();
							if (message instanceof ITargetMessage) {
								activation = message.getActivation();
							} else if (message instanceof IOriginMessage) {
								activation = ((IOriginMessage)message).getTarget().getActivation();
							}
						} else if (proxy.getKind() == ITraceModel.TRACE_CLASS_METHOD) {
							method = (ITraceClassMethod) proxy.getElement();
						} else if (proxy.getKind() == ITraceModel.TRACE_CLASS) {
							clazz = (ITraceClass) proxy.getElement();
						} else if (proxy.getKind() == ITraceModel.ACTIVATION) {
							activation = (IActivation) proxy.getElement();
						}
						if (activation != null) {
							if (activations == null) {
								activations = new TreeSet<Long>();
							}
							activations.add(((ActivationImpl)activation).getModelID());
							method = activation.getMethod();
						}
						if (method != null) {
							if (methods == null) {
								methods = new TreeSet<MethodKey>();
							}
							methods.add(new MethodKey(method.getTraceClass().getName(), method.getName(), method.getSignature()));
							clazz = method.getTraceClass();
						}
						if (clazz != null) {
							if (classes == null) {
								classes = new TreeSet<String>();
							}
							classes.add(clazz.getName());
						}
						
					}
				}
				
				if ((classes != null && !classes.isEmpty()) || (matches != null && !matches.isEmpty())) {
					//add a new search result
					searchResults.updateSearch(scope[i], classes, methods, activations, matches);
				}
			} catch (SQLException e) {
				s.merge(SketchPlugin.getDefault().createStatus(e));
			} catch (CoreException e) {
				s.merge(e.getStatus());
			}
			monitor.worked(100);
		}
		monitor.done();
		return s;
	}
	/**
	 * @param classes the class names to search for. This will be pruned of classes that
	 * don't contain any activations.
	 * @param methods the methods to search for. This will be pruned of methods
	 * that don't contain any activations.
	 * @param iProgramSketch
	 * @return 
	 * @throws CoreException 
	 * @throws SQLException 
	 */
	private TreeSet<Long> findActivations(SortedSet<String> classes,
			SortedSet<MethodKey> methods, IProgramSketch sketch) throws SQLException, CoreException {
		//first, remove all of the methods to search for if 
		//there are classes that already match.
		TreeSet<MethodKey> workingMethods = new TreeSet<MethodKey>();
		for (Iterator<MethodKey> it = methods.iterator(); it.hasNext();) {
			MethodKey key = it.next();
			if (!classes.contains(key.type)) {
				workingMethods.add(key);
			}
		}
		//now, find all of the activations that exist for matching classes.
		PreparedStatement ps = sketch.getPortal().prepareStatement("SELECT model_id FROM Activation WHERE type_name=?");
		TreeSet<Long> activations = new TreeSet<Long>();
		for (Iterator<String> it = classes.iterator(); it.hasNext();) {
			String c = it.next();
				ps.setString(1, c);
				ResultSet results = ps.executeQuery();
				if (results.next()) {
					do {
						activations.add(results.getLong(1));
					} while (results.next());
				} else {
					it.remove();
				}
			
		}
		ps = sketch.getPortal().prepareStatement("SELECT model_id FROM Activation WHERE type_name=? AND method_name=? AND method_signature=?");
		for (Iterator<MethodKey> it = methods.iterator(); it.hasNext();) {
			MethodKey mk = it.next();
			ps.setString(1, mk.type);
			ps.setString(2, mk.name);
			ps.setString(3, mk.signature);
			ResultSet results = ps.executeQuery();
			if (results.next()) {
				do {
					activations.add(results.getLong(1));
				} while (results.next());
			} else {
				it.remove();
			}
		}
		return activations;
	}
	/**
	 * @param methods2
	 * @param iProgramSketch
	 * @throws CoreException 
	 * @throws SQLException 
	 */
	private void searchForMethods(Collection<MethodKey> methods,
			IProgramSketch sketch) throws SQLException, CoreException {
		Statement s = sketch.getPortal().getDefaultConnection().createStatement();
		String likeString = getLikeString();
		ResultSet results = s.executeQuery("Select * from Method where " + 
			(((searchMask & CASE_SENSITIVE) == 0) ? "UCASE" : "") + 
			"(method_name) LIKE '" + likeString + "' ESCAPE '/'");
		while (results.next()) {
			String name = results.getString("method_name");
			String type = results.getString("type_name");
			String sig = results.getString("method_signature");
			methods.add(new MethodKey(type, name, sig));
		}	
	}
	
	private List<Match> searchForAnnotations(IProgramSketch sketch) {
		PresentationData data = PresentationData.connect(sketch);
		Pattern pattern = getRegex();
		LinkedList<Match> matches = new LinkedList<Match>();
		try {
			ITraceModelProxy[] proxies = data.getAnnotatedElements();
			for (ITraceModelProxy proxy : proxies) {
				String text = data.getAnnotation(proxy.getElementId());
				if (text != null && !text.isEmpty()) {
					text = text.replaceAll("\\s+", " ");
					Matcher matcher = pattern.matcher(text);
					while (matcher.find()) {
						Match match = new Match(proxy, matcher.start(), matcher.end() - matcher.start());
						if (match.getLength() > 0) {
							matches.add(match);
						}
					}
				}
			}
		} finally {
			data.disconnect();
		}
		return matches;
	}
	/**
	 * 
	 */
	private String getLikeString() {
		String likeString =  searchString.replace("/", "//");
		likeString = searchString.replace("'", "/'");
		likeString = searchString.replace("_", "/_");
		likeString = searchString.replace('?', '_');
		likeString = searchString.replace('*', '%');
		return likeString;
	}
	
	private Pattern getRegex() {
		StringBuffer buff = new StringBuffer();
		for (char c : searchString.toCharArray()) {
			switch (c) {
			case '?':
				buff.append('.');
				break;
			case '*':
				buff.append(".*");
				break;
			// characters that need to be escaped in the regex.
            case '(':
            case ')':
            case '{':
            case '}':
            case '.':
            case '[':
            case ']':
            case '$':
            case '^':
            case '+':
            case '|':
            	buff.append("\\\\");
            default:
            	buff.append(c);
			}
		}
		int flags =
			(((CASE_SENSITIVE & searchMask) == 0) ? Pattern.CASE_INSENSITIVE : 0) |
			//Pattern.MULTILINE |
			Pattern.UNICODE_CASE;
		return Pattern.compile(buff.toString(), flags);
	}
	/**
	 * @return
	 * @throws CoreException 
	 * @throws SQLException 
	 */
	private void searchForClasses(Collection<String> classes, IProgramSketch sketch) throws SQLException, CoreException {
		Statement s = sketch.getPortal().getDefaultConnection().createStatement();
		String likeString = getLikeString();
		ResultSet results = s.executeQuery("Select * from TraceClass where " + 
			(((searchMask & CASE_SENSITIVE) == 0) ? "UCASE" : "") + 
			"(type_name) LIKE '" + likeString + "' ESCAPE '/'");
		while (results.next()) {
			String type = results.getString("type_name");
			classes.add(type);
		}	
	}

}
