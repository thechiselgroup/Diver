/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Del Myers - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.javasketch.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import ca.uvic.chisel.hsqldb.server.IDataPortal;
import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.data.model.imple.internal.ThreadImpl;
import ca.uvic.chisel.javasketch.utils.JavaSketchUtilities;

/**
 * Utility class to aid in relating trace model elements to java elements.
 * @author Del Myers
 *
 */
public class JavaSearchUtils {
	private static TreeMap<String, IType> cachedTypes = new TreeMap<String, IType>();
	
	private static final String METHOD_QUERY = "SELECT method_signature, arrival_id, model_id FROM Activation WHERE type_name=? AND method_name=? AND thread_id=?";
	private static final String CLASS_QUERY  = "SELECT arrival_id, model_id FROM Activation WHERE type_name=? and thread_id=?";
//	private static final String ARRIVAL_TIME_QUERY = "SELECT time, order_num FROM MESSAGE where model_id=?";
	/**
	 * @author Del Myers
	 *
	 */
	private static final class LocalSearchRequestor extends SearchRequestor {
		List<SearchMatch> matches = new ArrayList<SearchMatch>();
	
		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			if (match.isExact()) {
				matches.add(match);
			}
		}
	}

	public static SearchPattern createMethodDeclarationPattern(ITraceClassMethod method) {
		if (method == null) {
			return null;
		}
		ITraceClass callingClass = method.getTraceClass();
		if (callingClass == null) {
			return null;
		}
		String className = callingClass.getName();
		className = Signature.toString(className);
		boolean isConstructor = false;
		String methodName = method.getName();
		isConstructor = methodName.startsWith("<init");
		if (isConstructor) {
			int lastDotIndex = className.lastIndexOf('.');
			if (lastDotIndex >=0) {
				methodName = className.substring(lastDotIndex+1);
			}
		}
		
		String methodString = Signature.toString(method.getSignature(), methodName, null, false, true);
		String returnType = "";
		if (!isConstructor) {
			int returnIndex = methodString.indexOf(' ');
			if (returnIndex > 0) {
				returnType = " " + methodString.substring(0,returnIndex);
				methodString = methodString.substring(returnIndex+1, methodString.length());
			}
		}
		return SearchPattern.createPattern(
			className + '.' + methodString + returnType,
			IJavaSearchConstants.METHOD,
			IJavaSearchConstants.DECLARATIONS,
			SearchPattern.R_EXACT_MATCH);
	}

	/**
	 * Tries to find a java element corresponding to the given model element.
	 * @param traceElement
	 * @param monitor 
	 * @return
	 * @throws InterruptedException 
	 * @throws CoreException 

	 */
	public static IJavaElement findElement(ITraceModel traceElement, IProgressMonitor monitor) throws InterruptedException, CoreException {
		if (traceElement == null) {
			return null;
		}
		IProgramSketch sketch = SketchPlugin.getDefault().getSketch(traceElement);
		if (sketch == null) {
			return null;
		}
		IJavaSearchScope scope = JavaSketchUtilities.createJavaSearchScope(sketch);
		if (scope == null) {
			return null;
		}
		
		if (traceElement instanceof ITraceClass) {
			try {
				String qualifiedName = ((ITraceClass)traceElement).getName();
				return searchForType(qualifiedName, scope, monitor);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			ITraceClassMethod method = null;
			if (traceElement instanceof ITraceClassMethod) {
				method = (ITraceClassMethod) traceElement;
			} else if (traceElement instanceof IMessage) {
				IMessage call = (IMessage) traceElement;
				IActivation activation = call.getActivation();
				if (activation == null) {
					return null;
				}
				method = activation.getMethod();
				
			} else if (traceElement instanceof IActivation) {
				IActivation activation = (IActivation) traceElement;
				method = activation.getMethod();
			}
			if (method != null) {
				return searchForMethod(method, scope, monitor);
			}
			
		}
		 
		
		return null;
	}
	
	public static final boolean areMethodsSimilar(ITraceClassMethod method1, ITraceClassMethod method2) {
		if ((method1 == method2) && method1 == null) {
			return true;
		}
		if (method1 == null || method2 == null) {
			return false;
		}
		if (!method1.getName().equals(method2.getName())) {
			return false;
		}
		if (!method1.getSignature().equals(method2.getSignature())) {
			return false;
		}
		try {
			IJavaElement element1 = findElement(method1.getTraceClass(), new NullProgressMonitor());
			IJavaElement element2 = findElement(method2.getTraceClass(), new NullProgressMonitor());
			if (element1 instanceof IType && element2 instanceof IType) {
				IType type1 = (IType) element1;
				IType type2 = (IType) element2;
				if (type1.getFullyQualifiedName().equals(type2.getFullyQualifiedName())) {
					return true;
				}
				ITypeHierarchy hierarchy1 = type1.newTypeHierarchy(new NullProgressMonitor());
				ITypeHierarchy hierarchy2 = type2.newTypeHierarchy(new NullProgressMonitor());
				for (IType h1 : hierarchy1.getAllClasses()) {
					for (IType h2: hierarchy2.getAllClasses()) {
						if (h1.getFullyQualifiedName().equals(h2.getFullyQualifiedName())) {
							return true;
						}
					}
				}
			}
		} catch (InterruptedException e) {
			
		} catch (CoreException e) {
		}
		return false;
	}

	private static IJavaElement searchForMethod(ITraceClassMethod method,
			IJavaSearchScope scope, IProgressMonitor monitor) throws CoreException, InterruptedException {
		monitor.beginTask("Searching for method", 200);
		String classSignature = method.getTraceClass().getName();
		try {
			classSignature = Signature.toString(classSignature);
		} catch (IllegalArgumentException e) {}
		String methodName = method.getName();
		String signature = method.getSignature();
		return searchForMethod(scope, monitor, classSignature, methodName,
			signature);
	}

	/**
	 * @param scope
	 * @param monitor
	 * @param classSignature
	 * @param methodName
	 * @param signature
	 * @return
	 * @throws CoreException
	 * @throws InterruptedException
	 */
	public static IJavaElement searchForMethod(IJavaSearchScope scope,
			IProgressMonitor monitor, String classSignature, String methodName,
			String signature) throws CoreException, InterruptedException {
		
		SubProgressMonitor typeMonitor = new SubProgressMonitor(monitor, 100);
		SubProgressMonitor hierarchyMonitor = new SubProgressMonitor(monitor, 100);
		IType foundType = (IType) searchForType(classSignature, scope, typeMonitor);
		if (foundType == null) return null;
		boolean isConstructor = false;
		if (methodName.startsWith("<init")) {
			isConstructor = true;
			boolean i = isConstructor;
			boolean isAnonymous = false;
			if (!foundType.exists()) {
				//check anonymity using the dollar sign
				String elementName = foundType.getElementName();
				if (elementName.length() > 0 && Character.isDigit(elementName.charAt(0))) {
					isAnonymous = true;
				}
			} else {
				isAnonymous = foundType.isAnonymous();
			}
			if (isAnonymous) {
				methodName = "";
			} else {
				methodName = foundType.getElementName();
			}
		}
		String[] methodParamTypes = Signature.getParameterTypes(signature);
		IMethod searchMethod = foundType.getMethod(methodName, methodParamTypes);
		IMethod[] methods = foundType.getMethods();
		for (IMethod checkMethod : methods) {
			if (isSimilar(searchMethod, checkMethod)) {
				return checkMethod;
			}
		}
		return searchMethod;
	}

	public static boolean isSimilar(IMethod method1, IMethod method2) {
		String[] myParams = method1.getParameterTypes();
		String[] theirParams = method2.getParameterTypes();
		int myParamsLength = myParams.length;
		String[] simpleNames = new String[myParamsLength];
		for (int i = 0; i < myParamsLength; i++) {
			String erasure = Signature.getTypeErasure(myParams[i]);
			simpleNames[i] = Signature.getSimpleName(Signature.toString(erasure));
		}
		String name = method1.getElementName();
		if (name.equals(method2.getElementName())) {
				
				if (myParamsLength == theirParams.length) {
					for (int i = 0; i < myParamsLength; i++) {
						
						String mySimpleName = simpleNames[i];
						String simpleName2 = Signature.getSimpleName(Signature.toString(Signature.getTypeErasure(theirParams[i])));
						//first, check for generics. If my param is not generic,
						//but theirs is, it is a match.
						int myTypeKind = Signature.getTypeSignatureKind(myParams[i]);
						int theirTypeKind = Signature.getTypeSignatureKind(theirParams[i]);
						switch (theirTypeKind) {
						case Signature.TYPE_VARIABLE_SIGNATURE:
						case Signature.WILDCARD_TYPE_SIGNATURE:
						case Signature.CAPTURE_TYPE_SIGNATURE:
							switch (myTypeKind) {
							case Signature.CLASS_TYPE_SIGNATURE:
							case Signature.ARRAY_TYPE_SIGNATURE:
							continue;
							}
						}
						//otherwise, try and match exactly
						if (!mySimpleName.equals(simpleName2)) {
							return false;
						}
					}
					return true;
				}
			}
			return false;
	}
	

	private static IJavaElement searchForType(String classSignature, IJavaSearchScope scope, IProgressMonitor monitor) throws CoreException, InterruptedException {
		try {
			synchronized (cachedTypes) {
				IType found = cachedTypes.get(classSignature);
				if (found != null) {
					return found;
				}
				SearchEngine engine = new SearchEngine();
				SearchPattern pattern = null;
				SearchParticipant participant = SearchEngine.getDefaultSearchParticipant();
				LocalSearchRequestor requestor = new LocalSearchRequestor();
				pattern = SearchPattern.createPattern(
					classSignature.replace('$', '.'), 
					IJavaSearchConstants.CLASS,
					IJavaSearchConstants.DECLARATIONS, 
					SearchPattern.R_EXACT_MATCH);

				engine.search(pattern, new SearchParticipant[] {participant}, scope, requestor, monitor);
				if (requestor.matches.size() > 0) {
					found = (IType) requestor.matches.get(0).getElement();
					if (found.getFullyQualifiedName().equals(classSignature)) {
						cachedTypes.put(classSignature, found);
						return found;
					}

				}
				String[] className = classSignature.split("\\$");
				found = cachedTypes.get(className[0]);
				if (found == null) {


					//find the class
					pattern = SearchPattern.createPattern(
						className[0], 
						IJavaSearchConstants.CLASS,
						IJavaSearchConstants.DECLARATIONS, 
						SearchPattern.R_EXACT_MATCH);

					engine.search(pattern, new SearchParticipant[] {participant}, scope, requestor, monitor);
					if (monitor.isCanceled()) {
						throw new InterruptedException();
					}
					if (requestor.matches.size() > 0) {
						found = (IType) requestor.matches.get(0).getElement();
						if (found.getFullyQualifiedName().equals(classSignature)) {
							for (SearchMatch match : requestor.matches) {
								IType temp = (IType) match.getElement();
								if (temp.getTypeRoot() instanceof ICompilationUnit) {
									//prefer source types.
									found = temp;
									break;
								}
							}
							if (cachedTypes.size() > 100) {
								cachedTypes.clear();
							}
							cachedTypes.put(className[0], found);
						} else {
							found = null;
						}
					}
				}
				if (found == null)
					return null;
				StringBuilder childTypeName = new StringBuilder();
				childTypeName.append(className[0]);
				//check each of the indexes for the sub-types
				for (int i = 1; i < className.length; i++) {
					childTypeName.append('$');
					childTypeName.append(className[i]);
					IType parent = found;
					found = cachedTypes.get(childTypeName.toString());


					if (found == null) {
						boolean isAnonymous = false;
						Integer occurrenceCount = -1;
						try {
							occurrenceCount = Integer.parseInt(className[i]);
							isAnonymous = true;
						} catch (NumberFormatException e) {
							isAnonymous = false;
						}
						if (isAnonymous) {
							if (!parent.isBinary()) {
								found = parent.getType("", occurrenceCount);
								if (found != null) {
									if (found.exists()) {
										cachedTypes.put(childTypeName.toString(), found);
										continue;
									} else {
										found = null;
									}
								}
							} else {
								//if it is a binary type, there is no hope for 
								//finding an anonymous inner class. Use the number
								//as the type name, and cache the handle.
								found = parent.getType(className[i]);
								cachedTypes.put(childTypeName.toString(), found);
								continue;
							}
						}
						ArrayList<IType> childTypes = new ArrayList<IType>();
						LinkedList<IJavaElement> children = new LinkedList<IJavaElement>();
						children.addAll(Arrays.asList(parent.getChildren()));
						while (children.size() > 0) {
							IJavaElement child = children.removeFirst();
							if (child instanceof IType) {
								childTypes.add((IType) child);
							} else if (child instanceof IMember) {
								children.addAll(Arrays.asList(((IMember)child).getChildren()));
							}
						}
						int numIndex = 0;
						while (numIndex < className[i].length() && Character.isDigit(className[i].charAt(numIndex))) {
							numIndex++;
						}
						String name = className[i];
						try {
							//get a number at the beginning to find out if 
							//there is an occurrence count
							if (numIndex <= name.length()) {
								occurrenceCount = parseInt(name.substring(0, numIndex));
								if (occurrenceCount == null) {
									occurrenceCount = 1;
								}
								if (numIndex < name.length()-1) {
									name = name.substring(numIndex);
								} else {
									name = "";
								}
							}
							for (IType childType : childTypes) {
								if ("".equals(name)) {
									if (childType.getOccurrenceCount() == occurrenceCount) {
										found = childType;
										break;
									}
								} else {
									if (name.equals(childType.getElementName()) && childType.getOccurrenceCount() == occurrenceCount) {
										found = childType;
										break;
									}
								}
							}
							if (found == null) {
								if ("".equals(name)) {
									found = parent.getTypeRoot().getJavaProject().findType(classSignature);
									//found = parent.getType("" + occurrenceCount);
								} else {
									found = parent.getType(name, occurrenceCount);
								}
							}
							cachedTypes.put(childTypeName.toString(), found);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				}
				return found;
			}
		} catch (Exception e) {
			SketchPlugin.getDefault().log(e);
			return null;
		}
	}
	
	

	/**
	 * @param string
	 * @return the integer value of the string, or null if it cannot be parsed as an integer
	 */
	private static Integer parseInt(String string) {
		try {
			return Integer.valueOf(string);
		} catch (NumberFormatException e) {}
		return null;
	}

	/**
	 * Searches the given thread for activations that match the given java method.
	 * @param thread the thread to search.
	 * @param method the method to check.
	 * @param monitor this is a long-running process, and should be done inside a monitor.
	 * @return a list of found activations
	 * @throws CoreException if an error occurred during retrieval of the activations
	 */
	public static List<IActivation> findActivationsForMethod(IThread thread, IMethod element, IProgressMonitor monitor) throws CoreException {
		DBProgramSketch sketch = (DBProgramSketch) SketchPlugin.getDefault().getSketch(thread);
		ArrayList<IActivation> activations = new ArrayList<IActivation>();
		if (sketch == null) {
			return activations;
		}
		Exception ex = null;
		monitor.beginTask("Searching for matching activations", IProgressMonitor.UNKNOWN);
		try {
			PreparedStatement methodStatement = sketch.getPortal().prepareStatement(METHOD_QUERY);
			IType methodType = element.getDeclaringType();
			String qualifiedName = methodType.getFullyQualifiedName();
			String methodName = element.getElementName();
			methodStatement.setString(1, qualifiedName);
			methodStatement.setString(2, methodName);
			methodStatement.setLong(3, ((ThreadImpl)thread).getModelID());
			ResultSet results = methodStatement.executeQuery();
			
			while (results.next()) {
				if (monitor.isCanceled()) break;
				String signature = results.getString(1);
				//try to find a method that matches in the declaring type
				IMethod matchMethod = methodType.getMethod(methodName, Signature.getParameterTypes(signature));
				if (element.isSimilar(matchMethod)) {
					long model_id = results.getLong("model_id");
					IActivation a = ((ThreadImpl)thread).getByModelID(model_id);
					if (a != null) {
						activations.add(a);
					}
				}
			}
		} catch (IllegalArgumentException e) {
			ex=e;
		} catch (SQLException e) {
			ex=e;
		} 
		monitor.done();
		if (ex != null) {
			throw new CoreException(SketchPlugin.getDefault().createStatus(ex));
		}
		return activations;
	}
	
	public static List<IActivation> findActivationsForClass(IThread thread, IType element, IProgressMonitor monitor) throws CoreException {
		ArrayList<IActivation> activations = new ArrayList<IActivation>();
		DBProgramSketch sketch = (DBProgramSketch) SketchPlugin.getDefault().getSketch(thread);
		if (sketch == null) {
			return activations;
		}
		try {
			IDataPortal portal = sketch.getPortal();
			String qualifiedName = element.getFullyQualifiedName();
			PreparedStatement classStatement = portal.prepareStatement(CLASS_QUERY);
			classStatement.setString(1, qualifiedName);
			classStatement.setLong(2, ((ThreadImpl)thread).getModelID());
			ResultSet results = classStatement.executeQuery();
			while (results.next()) {
				if (monitor.isCanceled()) break;
				long model_id = results.getLong("model_id");
				IActivation activation = ((ThreadImpl)thread).getByModelID(model_id);
				if (activation != null) {
					activations.add(activation);
				}
			}
		} catch (SQLException e) {
			throw new CoreException(SketchPlugin.getDefault().createStatus(e));
		} finally {
			monitor.done();
		}
		return activations;
	}
	
	public static String getFullyQualifiedName(IType type, boolean includeOccurrenceCount) {
		IType declaring = type;
		String qualifiedName = "";
		try {
			while (declaring != null) {
				if (declaring.isAnonymous()) {
					qualifiedName = declaring.getOccurrenceCount() + "" + qualifiedName;
				} else {
					qualifiedName = declaring.getElementName() + qualifiedName;
					if (includeOccurrenceCount) {
						IJavaElement parent = declaring.getParent();
						if (parent instanceof IMember && !(parent instanceof IType)) {
							qualifiedName = type.getOccurrenceCount() + qualifiedName;
						}
					}
				}
				IJavaElement parent = declaring.getParent();
				while (parent != null) {
					if (parent instanceof IType) {
						declaring = (IType) parent;
						break;
					}
					parent = parent.getParent();
				}
				if (parent != null) {
					qualifiedName = "$" + qualifiedName;
				} else {
					declaring = null;
				}
			}
		} catch (JavaModelException e) {
			return type.getFullyQualifiedName();
		}
		String pack = type.getPackageFragment().getElementName();
		if (!"".equals(pack)) pack = pack + ".";
		qualifiedName = pack + qualifiedName;
		return qualifiedName;
	}

	/**
	 * @param traceData
	 * @param selection
	 * @return
	 */
	public static IThread[] findThreadsForElement(ITrace trace,
			IJavaElement element) {
		HashSet<IThread> threads = new HashSet<IThread>();
		IProgramSketch sketch = SketchPlugin.getDefault().getSketch(trace);
		if (sketch != null) {
			try {
				if (sketch.getPortal() != null) {
					PreparedStatement statement = null;
					if (element instanceof IType) {
						String typeName = getFullyQualifiedName((IType) element, true);
						statement = sketch.getPortal().prepareStatement("SELECT DISTINCT thread_id from Activation WHERE type_name=?");
						statement.setString(1, typeName);
					} else if (element instanceof IMethod) {
						IMethod method = (IMethod) element;
						ITraceClassMethod tcm = findSimilarMethod(trace, method);
						if (tcm != null) {
							statement = sketch.getPortal().prepareStatement("SELECT DISTINCT thread_id from Activation WHERE type_name=? AND method_name=? AND method_signature=?");
							statement.setString(1, tcm.getTraceClass().getName());
							statement.setString(2, tcm.getName());
							statement.setString(3, tcm.getSignature());
						}
					}
					if (statement != null) {
						ResultSet results = statement.executeQuery();
						TreeSet<Long> modelIDs = new TreeSet<Long>();
						while (results.next()) {
							modelIDs.add(results.getLong(1));
						}
						if (modelIDs.size() > 0) {
							for (IThread t : trace.getThreads()) {
								if (t instanceof ThreadImpl) {
									ThreadImpl thread = (ThreadImpl) t;
									if (modelIDs.contains(thread.getModelID())) {
										threads.add(thread);
									}
								}
							}
						}
					}
				}
			} catch (CoreException e) {
			} catch (SQLException e) {
			}
		}
		return threads.toArray(new IThread[threads.size()]);
	}
	
	public static ITraceClassMethod findSimilarMethod(ITrace trace, IMethod method) {
		IType declaringType = method.getDeclaringType();
		IProgramSketch sketch = SketchPlugin.getDefault().getSketch(trace);
		if (sketch != null) {
			ITraceClass tc = findSimilarType(trace, declaringType);
			if (tc != null) {
				try {
					PreparedStatement statement = sketch.getPortal().prepareStatement("SELECT method_signature FROM Method WHERE type_name=? AND method_name=?");
					statement.setString(1, tc.getName());
					statement.setString(2, method.getElementName());
					ResultSet results = statement.executeQuery();
					while (results.next()) {
						String signature = results.getString(1);
						IMethod compareMethod = declaringType.getMethod(method.getElementName(), Signature.getParameterTypes(signature));
						if (method.isSimilar(compareMethod)) {
							return tc.findMethod(method.getElementName(), signature);
						}
					}
				} catch (IllegalArgumentException e) {
				} catch (SQLException e) {
				} catch (CoreException e) {
				}
			}
		}
		return null;
	}
	
	public static ITraceClass findSimilarType(ITrace trace, IType type) {
		return trace.forName(getFullyQualifiedName(type, true));
	}

}
