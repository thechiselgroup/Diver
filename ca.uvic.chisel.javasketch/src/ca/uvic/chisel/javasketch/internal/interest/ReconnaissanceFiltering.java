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
package ca.uvic.chisel.javasketch.internal.interest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.internal.WriteDataUtils;
import ca.uvic.chisel.javasketch.data.model.ICall;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.imple.internal.ThreadImpl;
import ca.uvic.chisel.javasketch.internal.DBProgramSketch;
import ca.uvic.chisel.javasketch.utils.JavaSketchUtilities;

/**
 * A filtering class for software reconnaissance
 * @author Del
 *
 */
public class ReconnaissanceFiltering {
	
	private static final String METHOD_STATEMENT =
		"SELECT type_name, method_name, method_signature FROM Method";
	
	private static final String VALID_METHOD_STATEMENT =
		"select type_name, method_name, method_signature from Method except (" +
		" select m.type_name, m.method_name, m.method_signature " +
		" from Method m, HiddenMethods h where " +
		"  m.method_name = h.method_name AND " +
		"  m.type_name = h.type_name AND" +
		"  m.method_signature = h.method_signature)";
	
	
	private static final String FILTER_CALL_STATEMENT = 
		"select Messages.type_name, messages.method_name, messages.kind, messages.sequence from " +
		"(select Activation.type_name, Activation.method_name, Message.kind, " +
	    " Message.model_id, Message.sequence from Activation, " +
	    " (" + VALID_METHOD_STATEMENT + ") as Valid, Message where " +
	    " Activation.method_name = Valid.method_name AND " +
	    " Activation.type_name = Valid.type_name AND " +
	    " Activation.method_signature = Valid.method_signature AND " +
	    " Activation.thread_id = ? AND " +
	    " Activation.model_id=Message.activation_id) as Messages " +
	    "where messages.kind = 'ARRIVE'";
	
	private static final String FILTER_COUNT_STATEMENT =
		"select count (*) from (" + FILTER_CALL_STATEMENT + ")";
	
	
	public static class TreeNode<T> {
		private ArrayList<TreeNode<T>> children;
		private TreeNode<T> parent;
		private T element;
		public TreeNode(TreeNode<T> parent, T element) {
			children = new ArrayList<TreeNode<T>>();
			this.element = element;
			this.parent = parent;
			if (parent != null) {
				parent.addNode(this);
			}
		}
		
		private void addNode(TreeNode<T> child) {
			if (!children.contains(child)) {
				children.add(child);
			}
		}
		
		public boolean equals(Object o) {
			if (o == null || !o.getClass().equals(getClass())) {
				return false;
			}
			@SuppressWarnings("unchecked")
			TreeNode<T> that = (TreeNode<T>) o;
			if (this.getElement() == null) {
				if (that.getElement() == null) {
					return true;
				}
				return false;
			}
			return this.getElement().equals(that.getElement());
		}
		
		public T getElement() {
			return element;
		}
		
		public List<T> getChildElements() {
			ArrayList<T> elements = new ArrayList<T>(children.size());
			for (TreeNode<T> child : children) {
				elements.add(child.getElement());
			}
			return elements;
		}

		/**
		 * @return
		 */
		public List<? extends TreeNode<T>> getChildren() {
			return children;
		}
		
		/**
		 * @return the parent
		 */
		public TreeNode<T> getParent() {
			return parent;
		}
		
	}
	
	public static class FilteredThreadTreeNode implements Comparable<FilteredThreadTreeNode> {
		private ArrayList<FilteredThreadTreeNode> children;
		public final long order;
		public boolean isLeaf;
		private FilteredThreadTreeNode parent;
		public FilteredThreadTreeNode(FilteredThreadTreeNode parent, long order, boolean leaf) {
			children = new ArrayList<FilteredThreadTreeNode>();
			this.order = order;
			this.isLeaf = leaf;
			this.parent = parent;
		}
		
		/**
		 * @return the parent
		 */
		public FilteredThreadTreeNode getParent() {
			return parent;
		}
		/**
		 * @param node
		 * @return
		 */
		public List<Long> getSequence() {
			FilteredThreadTreeNode node = this;
			if (node == null || node.getParent() == null) {
				return Collections.emptyList();
			}
			LinkedList<Long> sequence = new LinkedList<Long>();
			sequence.add(node.order);
			//don't want to put the number for the root
			node = node.getParent();
			while (node.getParent() != null) {
				sequence.addFirst(node.order);
				node = node.getParent();
			}
			return sequence;
		}
		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(FilteredThreadTreeNode that) {
			long diff = this.order - that.order;
			if (diff < 0) {
				return -1;
			} else if (diff > 0) {
				return 1;
			}
			return 0;
		}
		
		public boolean equals(Object o) {
			if (o == null || !getClass().equals(o.getClass())) {
				return false;
			}
			return this.order == ((FilteredThreadTreeNode)o).order;
		}
		
		public FilteredThreadTreeNode getChild(long order) {
			int location = indexOf(order);
			if (location >= 0) {
				return children.get(location);
			}
			return null;
		}
		
		/**
		 * @return the children
		 */
		public ArrayList<FilteredThreadTreeNode> getChildren() {
			return children;
		}
		
		/**
		 * Adds a new child for the given order and returns it. If a child
		 * already exists, it is returned.
		 * @param order
		 * @return
		 */
		public FilteredThreadTreeNode add(long order, boolean leaf) {
			int location = indexOf(order);
			if (location >= 0) {
				return children.get(location);
			} else {
				// location is (-(insertionpoint) - 1).
				FilteredThreadTreeNode child = new FilteredThreadTreeNode(this, order, leaf);
				int insertion = -(location + 1);
				if (insertion >= children.size()) {
					children.add(child);
				} else {
					children.add(insertion, child);
				}
				return child;
			}
			
		}
		
		protected int indexOf(long order) {
			return Collections.binarySearch(children, new FilteredThreadTreeNode(null, order, false));
		}
		
	}
	
	private static class JavaElementTree {
		private HashMap<IJavaElement, TreeNode<IJavaElement>> allChildren;
		private TreeNode<IJavaElement> root;
		public JavaElementTree() {
			allChildren = new HashMap<IJavaElement, ReconnaissanceFiltering.TreeNode<IJavaElement>>();
			root = new TreeNode<IJavaElement>(null, null);
		}
		
		public TreeNode<IJavaElement> getNode(IJavaElement element) {
			if (element == null) {
				return root;
			}
			return allChildren.get(element);
		}
		
		/**
		 * Adds the given java element and all of its parents to the tree.
		 * @param element the element to add.
		 * @return the new tree node created for the element.
		 */
		public TreeNode<IJavaElement> addElementInTree(IJavaElement element) {
			TreeNode<IJavaElement> node = getNode(element);
			if (node != null) {
				return node;
			}
			TreeNode<IJavaElement> parent = addElementInTree(element.getParent());
			node = new TreeNode<IJavaElement>(parent, element);
			allChildren.put(element, node);
			return node;
		}
		
		public boolean contains(IJavaElement element) {
			return allChildren.containsKey(element);
		}
		
	}
	
	public static class FilteredThreadTree {
		protected FilteredThreadTreeNode root;
		protected IThread thread;
		public FilteredThreadTree(IThread thread) {
			root = new FilteredThreadTreeNode(null, 0, false);
			this.thread = thread;
		}
		
		public boolean contains(String sequence) {
			List<Long> orders = WriteDataUtils.fromStoredSequence(sequence);
			FilteredThreadTreeNode node = root;
			for (long l : orders) {
				
					node = node.getChild(l);
					if (node == null) {
						return false;
					}
				
			}
			return true;
		}
		

		public boolean contains(ICall call) {
			return contains(call.getSequence());
		}
		
		
		public boolean add(List<Long> sequence) {
			
			FilteredThreadTreeNode node = root;
			int i = 0;
			for (Long order : sequence) {
				try {
					boolean leaf = i >= (sequence.size()-1);
					node = node.add(order, leaf);
					i++;
					if (node == null) {
						return false;
					}
				} catch (NumberFormatException e) {
					return false;
				}
			}
			return true;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj == null || !obj.getClass().equals(getClass())) {
				return false;
			}
			return this.thread.equals(((DatabaseThreadTree)obj).thread);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return thread.hashCode();
		}
		

		

		/**
		 * @return
		 */
		public FilteredThreadTreeNode getRoot() {
			return root;
		}
	}
	
	private static class DatabaseThreadTree extends FilteredThreadTree {
		private boolean committed;
		public DatabaseThreadTree(IThread thread) {
			super(thread);
		}
		
		public synchronized FilteredThreadTree copy() {
			FilteredThreadTree copy = new FilteredThreadTree(thread);
			if (!committed) {
				copy.root = this.root;
			} else {
				try {
					DBProgramSketch sketch = (DBProgramSketch)SketchPlugin.getDefault().getSketch(thread);
					PreparedStatement s =
						sketch.getPortal().prepareStatement("SELECT sequence, is_leaf FROM ValidCalls where thread_id = ? and is_leaf=TRUE ORDER BY sequence");
					s.setLong(1, ((ThreadImpl)thread).getModelID());
					ResultSet results = s.executeQuery();
					while (results.next()) {
						String sequence = results.getString(1);
						List<Long> nums = WriteDataUtils.fromStoredSequence(sequence);
						boolean is_leaf = results.getBoolean(2);
						copy.add(nums);
					}
				} catch (SQLException e) {
					return null;
				} catch (CoreException e) {
					return null;
				}
				
			}
			return copy;
		}
		
		public boolean contains(String sequence) {
			if (committed) {
				try {
					return dbContains(sequence);
				} catch (SQLException e) {
					
				} catch (CoreException e) {
					
				}
				return false;
			}
			return super.contains(sequence);
		}
		
		/**
		 * @param sequence
		 * @return
		 * @throws CoreException 
		 * @throws SQLException 
		 */
		private boolean dbContains(String sequence) throws SQLException, CoreException {
			DBProgramSketch sketch = (DBProgramSketch) SketchPlugin.getDefault().getSketch(thread);
			PreparedStatement statement = sketch.getPortal().prepareStatement(
				"Select count(*) from ValidCalls where thread_id = ? AND sequence=?");
			statement.setLong(1, ((ThreadImpl)thread).getModelID());
			statement.setString(2, sequence);
			ResultSet results = statement.executeQuery();
			if (results.next()) {
				int count = (results.getInt(1));
				return count > 0;
			}
			return false;
			
		}

		public boolean contains(ICall call) {
			if (!committed) {
				return super.contains(call);
			}
			try {
				return dbContains(call);
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				
			}
			return false;
		}
		
		/**
		 * @param call
		 * @return
		 * @throws CoreException 
		 * @throws SQLException 
		 */
		private boolean dbContains(ICall call) throws SQLException, CoreException {
			return dbContains(call.getSequence());
		}

		
		protected synchronized void commit() throws SQLException, CoreException {
			DBProgramSketch sketch = (DBProgramSketch) SketchPlugin.getDefault().getSketch(thread);
			PreparedStatement statement = sketch.getPortal().prepareStatement(
				"Delete from ValidCalls where thread_id=?");
			statement.setLong(1, ((ThreadImpl)thread).getModelID());
			statement.execute();
			LinkedList<FilteredThreadTreeNode> nodes = new LinkedList<FilteredThreadTreeNode>();
			nodes.add(root);
			Statement batch = sketch.getPortal().getDefaultConnection().createStatement();
				
			while (nodes.size() > 0) {
				FilteredThreadTreeNode node = nodes.remove();
				String sequence = getSequence(node);
				//escape single quotes
				sequence = sequence.replace("'", "''");
				if (!sequence.isEmpty()) {
					batch.addBatch("Insert INTO ValidCalls Values (" +
						((ThreadImpl)thread).getModelID() + "," +
						"'"+sequence+"'," +
						((node.isLeaf) ? "TRUE" : "FALSE")+")");
				}
				nodes.addAll(node.children);
			}
			
			batch.executeBatch();
			
			sketch.getPortal().getDefaultConnection().commit();
			root = null;
			committed = true;
		}

		/**
		 * @param node
		 * @return
		 */
		private String getSequence(FilteredThreadTreeNode node) {
			List<Long> sequence = node.getSequence();
			long[] array = new long[sequence.size()];
			int index = 0;
			for(Long s : sequence) {
				array[index] = s;
				index++;
			}
			return WriteDataUtils.toStoredSequence(array);
		}
	}
	
	private DBProgramSketch active;
	private Set<DBProgramSketch> hidden;
	private Map<IThread, DatabaseThreadTree> trees;


	private Set<MethodDescriptor> validMethods;
	private JavaElementTree validElements;
	private ReentrantReadWriteLock lock;

	
	/**
	 * 
	 */
	public ReconnaissanceFiltering() {
		lock = new ReentrantReadWriteLock();
		active = null;
		hidden = Collections.synchronizedSet(new HashSet<DBProgramSketch>());
		trees = Collections.synchronizedMap(new HashMap<IThread, DatabaseThreadTree>());
	}
	
	public void setHidden(IProgramSketch sketch, boolean hide, IProgressMonitor progress) {
		try {
			lock.writeLock().lock();
			if (!(sketch instanceof DBProgramSketch)) {
				return;
			}
			DBProgramSketch dbsketch = (DBProgramSketch) sketch;


			if (hide) {
				if (!hidden.contains(dbsketch)) {
					hidden.add(dbsketch);
					reset(progress);
				}
			} else {
				if (hidden.contains(dbsketch)) {
					hidden.remove(dbsketch);
					reset(progress);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void setActiveSketch(IProgramSketch active, IProgressMonitor progress) {
		try {
			lock.writeLock().lock();
			this.active = null;
			this.validMethods = null;
			trees.clear();
			if (!(active instanceof DBProgramSketch)) {
				return;
			}
			this.active = (DBProgramSketch) active;
			setup();

			reset(progress);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 
	 */
	private void setup() {
		if (active != null) {
			try {
				Connection connection = active.getPortal().getDefaultConnection(false);
				connection.createStatement().execute("DROP INDEX IDX_HiddenMethod_SIG IF EXISTS");
				connection.createStatement().execute("DROP INDEX IDX_HiddenMethod_TYPE IF EXISTS");
				connection.createStatement().execute("DROP INDEX IDX_HiddenMethod_NAME IF EXISTS");
				connection.createStatement().execute("DROP INDEX IDX_HiddenMethod_SIGNATURE IF EXISTS");
				connection.createStatement().execute("DROP VIEW ValidMethod IF EXISTS");
				connection.createStatement().execute("DROP TABLE HiddenMethods IF EXISTS");
				connection.createStatement().execute("CREATE TABLE HiddenMethods (" +
						"type_name VARCHAR(128), " +
						"method_name VARCHAR(128), " +
						"method_signature VARCHAR(512))");
				connection.createStatement().execute(
					"CREATE INDEX IDX_HiddenMethod_SIG ON" +
					" HiddenMethods(type_name, method_name, method_signature)");
				connection.createStatement().execute(
					"CREATE INDEX IDX_HiddenMethod_TYPE ON" +
					" HiddenMethods(type_name)");
				connection.createStatement().execute(
					"CREATE INDEX IDX_HiddenMethod_NAME ON" +
					" HiddenMethods(method_name)");
				connection.createStatement().execute(
					"CREATE INDEX IDX_HiddenMethod_SIGNATURE ON" +
					" HiddenMethods(method_signature)");
				connection.createStatement().execute("CREATE VIEW ValidMethod AS " + VALID_METHOD_STATEMENT);
				connection.createStatement().execute("DROP INDEX IDX_HiddenMethod_SEQUENCE IF EXISTS");
				connection.createStatement().execute("DROP TABLE ValidCalls IF EXISTS");
				connection.createStatement().execute("CREATE TABLE ValidCalls (" +
						"thread_id BIGINT," +
						"sequence VARCHAR(8000)," +
						"is_leaf BOOLEAN)");
				connection.createStatement().execute(
					"CREATE INDEX IDX_ValidCalls_sequence ON" +
					" ValidCalls(thread_id, sequence)");
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void reset(IProgressMonitor progress) throws SQLException {
		if (active == null) {
			return;
		}
		progress.beginTask("Calculating Used methods", 3);
		HashSet<MethodDescriptor> methods = new HashSet<MethodDescriptor>();
		this.validMethods = null;
		trees.clear();
		progress.subTask("Finding hidden methods");
		for (DBProgramSketch sketch : hidden) {
			if (!sketch.getProcessName().equals(active.getProcessName())) {
				continue;
			}
			if (sketch.getTraceData() == null) 
				continue;
			progress.subTask("Finding hidden methods: " + sketch.getLabel());
			if (sketch.equals(active)) {
				continue;
			}
			try {
				PreparedStatement methodStatement = sketch.getPortal().prepareStatement(METHOD_STATEMENT);
				ResultSet results = methodStatement.executeQuery();
				while (results.next()) {
					methods.add(new MethodDescriptor(results.getString(1), results.getString(2), results.getString(3)));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		progress.worked(1);
		progress.subTask("Inserting valid methods");
		try {
			Connection connection = active.getPortal().getDefaultConnection(false);
			Statement s = connection.createStatement();
			s.execute("DELETE FROM HiddenMethods");
			for (MethodDescriptor m : methods) {
				s.addBatch("INSERT INTO HiddenMethods VALUES ('" + m.type + "','"+m.name+"','"+m.signature+"')");
			}
			s.executeBatch();
			progress.worked(1);
			connection.commit();
			buildValidElements(new SubProgressMonitor(progress, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
			connection.commit();
		} catch (CoreException e) {
			e.printStackTrace();
		} finally {
			progress.done();
		}
		
	}
	
	public boolean filter(IThread thread, IProgressMonitor monitor) {
		try {
			lock.readLock().lock();
			synchronized(trees) {
				//first make sure that it is a thread in the active trace.
				if (active == null) return false;
				if (hidden == null || hidden.isEmpty()) return false;
				if (!active.equals(SketchPlugin.getDefault().getSketch(thread))) {
					return false;
				}
				if (!(thread instanceof ThreadImpl)) return false;
				if (trees.containsKey(thread)) {
					return false;
				}
				ThreadImpl impl = (ThreadImpl) thread;
				monitor.beginTask("Filtering Thread " + thread.getID(), 2);

				PreparedStatement activationsStatement  = active.getPortal().prepareStatement(FILTER_CALL_STATEMENT);
				PreparedStatement countStatement = active.getPortal().prepareStatement(FILTER_COUNT_STATEMENT);
				activationsStatement.setLong(1, impl.getModelID());
				countStatement.setLong(1, impl.getModelID());
				ResultSet results = countStatement.executeQuery();
				if (monitor.isCanceled()) {
					return false;
				}
				int size = 0;
				if (results.next()) {
					size = results.getInt(1);
				} else {
					return true;
				}
				results = activationsStatement.executeQuery();
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
				subMonitor.beginTask("Filtering calls", size);
				DatabaseThreadTree tree = new DatabaseThreadTree(thread);
				while (results.next()) {
					if (subMonitor.isCanceled()) {
						return false;
					} else if (!thread.isValid()){
						return false;
					}
					subMonitor.subTask(results.getString("type_name") + "." + results.getString("method_name"));
					String sequence = results.getString("sequence");
					if (sequence != null) {
						tree.add(WriteDataUtils.fromStoredSequence(sequence));
					}
					subMonitor.worked(1);
				}
				trees.put(thread, tree);
				subMonitor.subTask("Committing results");
				tree.commit();
				subMonitor.done();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally {
			monitor.done();
			lock.readLock().unlock();
		}
		return true; 
	}
	
	public FilteredThreadTree getFilteredThread(IThread thread) {
		DatabaseThreadTree tree =  trees.get(thread);
		if (tree != null) {
			return tree.copy();
		}
		return null;
	}

	/**
	 * @param thread
	 * @return
	 */
	public boolean isFiltered(IThread thread) {
		return trees.containsKey(thread);
	}

	/**
	 * @param call
	 */
	public boolean checkCall(ICall call) {
		try {
			lock.readLock().lock();

			synchronized (trees) {
				if (active == null) {
					return true;
				}
				DatabaseThreadTree tree = trees.get(call.getThread());
				if (tree != null) {
					return tree.contains(call);
				}
				return true;
			} 
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public Set<MethodDescriptor> getValidMethods() {
		try {
			lock.writeLock().lock();
		if (validMethods != null) {
			return validMethods;
		}
		HashSet<MethodDescriptor> methods = null;
		
			methods = new HashSet<MethodDescriptor>();
			PreparedStatement statement = active.getPortal().prepareStatement(
				"Select * from HiddenMethods"
			);
			ResultSet results = statement.executeQuery();
			HashSet<MethodDescriptor> hidden = new HashSet<MethodDescriptor>();
			while (results.next()) {
				MethodDescriptor md = new MethodDescriptor(results.getString("type_name"),  results.getString("method_name"),  results.getString("method_signature"));
				hidden.add(md);
			}
			
			statement = active.getPortal().prepareStatement(
				"Select * from Method");
			results = statement.executeQuery();
//			System.out.println("---------------------------------");
//			System.out.println("---------------------------------");
//			System.out.println("---------------------------------");
//			System.out.println("---------------------------------");
//			System.out.println("---------------------------------");
//			System.out.println("---------------------------------");
//			System.out.println("---------------------------------");
//			System.out.println("---------------------------------");
			
			while (results.next()) {
				MethodDescriptor md = new MethodDescriptor(results.getString("type_name"),  results.getString("method_name"),  results.getString("method_signature"));
				if (!hidden.contains(md)) {
//					System.out.println(md);
					methods.add(md);
				}
			}
			validMethods = Collections.unmodifiableSet(methods);
			return validMethods;
		} catch (SQLException e) {
			
			e.printStackTrace();
			return null;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		} finally {
			lock.writeLock().unlock();
		}
		
	  
		
	}
	
	private void buildValidElements(IProgressMonitor monitor) {
		validElements = new JavaElementTree();
		if (active == null) {
			return;
		}
		Set<MethodDescriptor> methods = getValidMethods();
		if (methods == null) {
			return;
		}
		monitor.beginTask("Indexing Java Elements", methods.size());
		IJavaSearchScope scope = JavaSketchUtilities.createJavaSearchScope(active);
		for (MethodDescriptor m : methods) {
			monitor.worked(1);
			IJavaElement je = m.getMethod(scope);
			if (je != null) {
				validElements.addElementInTree(je);
			}
		}
		monitor.done();
	}

	/**
	 * @param javaElement
	 * @return
	 */
	public boolean isValid(IJavaElement javaElement) {
		try { 
			lock.readLock().lock();
			if (active == null) return true;
			if (validElements == null) return true;
			return validElements.contains(javaElement);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public List<IJavaElement> getAllValidChildren(IJavaElement e) {
		LinkedList<IJavaElement> elements = new LinkedList<IJavaElement>();
		if (validElements == null || !validElements.contains(e)) {
			return elements;
		}
		TreeNode<IJavaElement> node = validElements.getNode(e);
		LinkedList<TreeNode<IJavaElement>> nodes = new LinkedList<TreeNode<IJavaElement>>();
		nodes.add(node);
		while (nodes.size() != 0) {
			node = nodes.removeFirst();
			elements.addAll(node.getChildElements());
			nodes.addAll(node.getChildren());
		}
		return elements;		
	}

	/**
	 * @return
	 */
	public IProgramSketch getActiveSketch() {
		return active;
	}

}
