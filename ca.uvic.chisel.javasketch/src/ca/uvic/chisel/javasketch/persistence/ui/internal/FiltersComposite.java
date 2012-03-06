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
package ca.uvic.chisel.javasketch.persistence.ui.internal;


import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import ca.uvic.chisel.javasketch.FilterSettings;
import ca.uvic.chisel.javasketch.ui.IFilterContext;
import ca.uvic.chisel.javasketch.utils.LaunchConfigurationUtilities;

/**
 * A composite that can be used for setting up filters for java traces
 * @author Del Myers
 *
 */
public class FiltersComposite extends Composite {
	
	public static final int SHORTCUT_PROJECT_CLASSES = 0;
	public static final int SHORTCUT_PREVIOUS_SKETCH = 1;
	public static final int SHORTCUT_NONE = 2;
	
	private final class SimpleJavaContentProposalProvider implements IContentProposalProvider {

		private IJavaElement[] packagesAndClasses;

		/* (non-Javadoc)
		 * @see org.eclipse.jface.fieldassist.IContentProposalProvider#getProposals(java.lang.String, int)
		 */
		@Override
		public IContentProposal[] getProposals(String contents, int position) {
			String subString = contents.substring(0, position);
			IJavaElement[] elements = getPackagesAndClasses();
			LinkedList<IContentProposal> proposals = new LinkedList<IContentProposal>();
			for (IJavaElement element : elements) {
				String name = element.getElementName();
				if (element instanceof IType) {
					name = ((IType)element).getFullyQualifiedName();
				}
				if (select(name, subString)) {
					proposals.add(makeContentProposal(element, position));
				}
			}
			return proposals.toArray(new IContentProposal[proposals.size()]);
		}

		/**
		 * @param elementName
		 * @return
		 */
		private boolean select(String elementName, String filter) {
			if (filter.startsWith("*")) {
				filter = filter.substring(1);
				if (filter.endsWith("*")) {
					filter = filter.substring(0, filter.length()-1);
				}
				return elementName.contains(filter);
			} 
			return elementName.startsWith(filter);
		}

		/**
		 * @param element
		 * @return
		 */
		private IContentProposal makeContentProposal(final IJavaElement element, final int cursor) {
			return new IContentProposal() {

				@Override
				public String getContent() {
					String s = ((element instanceof IType) ? ((IType)element).getFullyQualifiedName() : element.getElementName());
					s += ".*";
					return s.substring(cursor);
				}

				@Override
				public int getCursorPosition() {
					String s = ((element instanceof IType) ? ((IType)element).getFullyQualifiedName() : element.getElementName());
					s += ".*";
					return s.length();
				}

				@Override
				public String getDescription() {
					return (
						(element instanceof IType) ? 
								((IType)element).getFullyQualifiedName() + ": Java Type" : 
								element.getElementName() + ": Java Package");
				}

				@Override
				public String getLabel() {
					String s = element.getElementName();
					if (element instanceof IType) {
						s = " " + s;
					}
					return s;
				}
				
			};
		}

		/**
		 * @return
		 */
		private IJavaElement[] getPackagesAndClasses() {
			if (javaContext != null) {
				if (packagesAndClasses == null) {
					LinkedList<IJavaElement> list = new LinkedList<IJavaElement>();
					try {
						Collection<IPackageFragmentRoot> roots = getFragmentRoots();
						for (IPackageFragmentRoot root : roots) {
							for (IJavaElement element : root.getChildren()) {
								if (element instanceof IPackageFragment) {
									list.add(element);
									IPackageFragment fragment = (IPackageFragment) element;
									for (IJavaElement child : fragment.getChildren()) {
										if (child instanceof IClassFile) {
											list.add(((IClassFile)child).getType());
										} else if (child instanceof ICompilationUnit) {
											ICompilationUnit cu = (ICompilationUnit) child;
											list.addAll(Arrays.asList(cu.getAllTypes()));
										}
									}
								}
							}
						}
						
					} catch (JavaModelException e) {
					}
					packagesAndClasses = list.toArray(new IJavaElement[list.size()]);
				}
				return packagesAndClasses;
			}
			return new IJavaElement[0];
		}

		/**
		 * @return
		 */
		private Collection<IPackageFragmentRoot> getFragmentRoots() {
			Comparator<IPackageFragmentRoot> fragmentComparator = new Comparator<IPackageFragmentRoot>() {

				@Override
				public int compare(IPackageFragmentRoot o1,
						IPackageFragmentRoot o2) {
					return o1.getElementName().compareTo(o2.getElementName());
				}
				
			};
			TreeSet<IPackageFragmentRoot> roots = new TreeSet<IPackageFragmentRoot>(fragmentComparator);
			for (IJavaProject jp : javaContext) {
				try {
					roots.addAll(Arrays.asList(jp.getPackageFragmentRoots()));
				} catch (JavaModelException e) {}
			}
			return roots;
		}
		
	}


	/**
	 * @author Del Myers
	 *
	 */
	private final class RemoveItemListener implements
			SelectionListener {
		TableViewer viewer;

		/**
		 * @param inclusionTable
		 */
		public RemoveItemListener(TableViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			ISelection s = viewer.getSelection();
			TreeSet<String> input = new TreeSet<String>(Arrays.asList((String[])viewer.getInput()));
			if (s instanceof IStructuredSelection) {
				Iterator<?> i = ((IStructuredSelection)s).iterator();
				while (i.hasNext()) {
					input.remove(i.next());
				}
			}
			viewer.setInput(input.toArray(new String[input.size()]));
			if (parentContext != null) {
				parentContext.filterChanged();
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	}

	/**
	 * @author Del Myers
	 *
	 */
	private final class AddFilterListener extends KeyAdapter implements
			SelectionListener {
		private Text text;
		private TableViewer viewer;

		/**
		 * @param addIncludeText
		 * @param inclusionTable
		 */
		public AddFilterListener(Text text, TableViewer viewer) {
			this.text = text;
			this.viewer = viewer;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (FilterSettings.isValidFilterString(text.getText()) < 0) {
				String[] input = (String[]) viewer.getInput();
				TreeSet<String> newInput = new TreeSet<String>(Arrays.asList(input));
				newInput.add(text.getText());
				viewer.setInput(newInput.toArray(new String[newInput.size()]));
				if (parentContext != null) {
					parentContext.filterChanged();
				}
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.swt.events.KeyAdapter#keyReleased(org.eclipse.swt.events.KeyEvent)
		 */
		@Override
		public void keyReleased(KeyEvent e) {
			if (e.character=='\n' && (FilterSettings.isValidFilterString(text.getText()) < 0)) {
				String[] input = (String[]) viewer.getInput();
				TreeSet<String> newInput = new TreeSet<String>(Arrays.asList(input));
				newInput.add(text.getText());
				viewer.setInput(newInput.toArray(new String[newInput.size()]));
			}
		}
	}

	/**
	 * @author Del Myers
	 *
	 */
	private final class AddFilterAssistListener implements ModifyListener {
		ControlDecoration decoration;
		private Button addButton;

		/**
		 * @param addButton 
		 * @param controlDecoration
		 */
		public AddFilterAssistListener(Button addButton, ControlDecoration controlDecoration) {
			this.decoration = controlDecoration;
			this.addButton = addButton;
		}

		@Override
		public void modifyText(ModifyEvent e) {
			Text text = (Text) e.widget;
			String s = text.getText();
			int problem = FilterSettings.isValidFilterString(s);
			
			if (decoration != null) {
				String decorationImage = null;
				String decorationText = null;
				if (javaContext != null) {
					decorationImage = FieldDecorationRegistry.DEC_CONTENT_PROPOSAL;
					decorationText = "Content assist available";
				}
				if (problem >= 0) {
					decorationImage = FieldDecorationRegistry.DEC_ERROR;
					decorationText = "Error: unexpected character '" + s.charAt(problem) + "'";
					if (addButton != null) {
						addButton.setEnabled(false);
					}
				} else {
					if (addButton != null) {
						addButton.setEnabled(true);
					}
				}
				if (decorationImage != null) {
					FieldDecoration dec = 
						FieldDecorationRegistry.getDefault().getFieldDecoration(decorationImage);
					decoration.setImage(dec.getImage());
					decoration.setDescriptionText(decorationText);
					decoration.show();
				} else {
					decoration.hide();
				}
			}

		}
	}

	/**
	 * @author Del Myers
	 *
	 */
	private final class SimpleCellLabelProvider extends CellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			if (cell.getColumnIndex() == 0) {
				if (cell.getElement() == null) {
					cell.setText("");
				}
				else {
					String s = cell.getElement().toString();
					//a simple method of telling whether or not
					//the item is a class or a package
					boolean capital = false;
					for (int i = 0; i < s.length(); i++) {
						if (Character.isUpperCase(s.charAt(i))) {
							capital = true;
							break;
						}
					}
					if (capital) {
						cell.setImage(JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS));
					} else {
						cell.setImage(JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE));
					}
					cell.setText(s);
				}
			}
		}
	}

	private Button quickFilterButton;


	private TableViewer inclusionTable;


	private TableViewer exclusionTable;


	private IJavaProject[] javaContext;


	private Group inclusionGroup;


	private Group exclusionGroup;


	private IFilterContext parentContext;

//removing the following buttons because they can be confusing.
	
	//private Button selectSketchButton;


	//private Combo sketchCombo;
	private Button noShortcutButton;

	/**
	 * @param parent
	 * @param style
	 */
	public FiltersComposite(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new GridLayout(2, true));
		
		//create a button for quickly selecting only classes in the project
		Composite buttonArea = new Composite(this, SWT.NONE);
		buttonArea.setLayout(new GridLayout(2, false));
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace=true;
		gd.grabExcessVerticalSpace=false;
		gd.horizontalAlignment = SWT.FILL;
		gd.horizontalSpan=2;
		buttonArea.setLayoutData(gd);
		quickFilterButton = new Button(buttonArea, SWT.RADIO);
		quickFilterButton.setText("Only index classes defined in this project");
		gd = new GridData();
		gd.grabExcessHorizontalSpace=true;
		gd.grabExcessVerticalSpace=false;
		gd.horizontalSpan=2;
		gd.horizontalAlignment=GridData.BEGINNING;
		quickFilterButton.setLayoutData(gd);
		quickFilterButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnabledState();
				if (parentContext != null) {
					parentContext.filterChanged();
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		
//		selectSketchButton = new Button(buttonArea, SWT.RADIO);
//		selectSketchButton.setText("Filter Based on Previous Trace");
//		selectSketchButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
//		selectSketchButton.addSelectionListener(new SelectionListener() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				updateEnabledState();
//				parentContext.filterChanged(FiltersComposite.this);
//				
//			}
//			
//			@Override
//			public void widgetDefaultSelected(SelectionEvent e) {
//				widgetSelected(e);
//			}
//		});
//		
//		sketchCombo = new Combo(buttonArea, SWT.DROP_DOWN | SWT.READ_ONLY);
//		sketchCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		sketchCombo.addSelectionListener(new SelectionListener() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				parentContext.filterChanged(FiltersComposite.this);
//				
//			}
//			
//			@Override
//			public void widgetDefaultSelected(SelectionEvent e) {
//				widgetSelected(e);
//			}
//		});
		
//		if (javaContext != null) {
//			populateCombo();
//		}
		
		noShortcutButton = new Button(buttonArea, SWT.RADIO);
		noShortcutButton.setText("Set Filter Manually");
		gd = new GridData();
		gd.grabExcessHorizontalSpace=true;
		gd.grabExcessVerticalSpace=false;
		gd.horizontalSpan=2;
		gd.horizontalAlignment=GridData.BEGINNING;
		noShortcutButton.setLayoutData(gd);
		
		inclusionGroup = new Group(this, SWT.FULL_SELECTION);
		inclusionGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		inclusionTable = createTableArea(inclusionGroup);
		inclusionGroup.setText("Inclusion Filters");
		
		
		exclusionGroup = new Group(this, SWT.FULL_SELECTION);
		exclusionGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		exclusionGroup.setText("Exclusion Filters");
		exclusionTable = createTableArea(exclusionGroup);
	}
	
	/**
	 * 
	 */
//	private void populateCombo() {
//		IProgramSketch[] sketches = SketchPlugin.getDefault().getStoredSketches();
//		if (sketches != null) {
//			Arrays.sort(sketches, new Comparator<IProgramSketch>() {
//
//				@Override
//				public int compare(IProgramSketch o1, IProgramSketch o2) {
//					int diff = o1.getProcessTime().compareTo(o2.getProcessTime());
//					if (diff == 0) {
//						diff = o1.getID().compareTo(o2.getID());
//					}
//					return diff;
//				}
//			});
//			sketchCombo.setData(sketches);
//			DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
//			for (IProgramSketch sketch : sketches) {
//				sketchCombo.add(sketch.getLabel() + " at " + dateFormat.format(sketch.getProcessTime()));
//			}
//		}
//		
//	}

	/**
	 * @param group
	 * @return
	 */
	private TableViewer createTableArea(Group group) {
		group.setLayout(new GridLayout(3, false));
		final Text addText = new Text(group, SWT.SINGLE | SWT.BORDER);
		addText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		final Button addButton = new Button(group, SWT.PUSH);
		addButton.setText("Add");
		addButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
		Button removeButton = new Button(group, SWT.PUSH);
		removeButton.setText("Remove");
		addButton.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
		TableViewer tableViewer = new TableViewer(group);
		tableViewer.getTable().setLinesVisible(true);
		GridData tableData = new GridData(GridData.FILL, GridData.FILL, true, true);
		tableData.horizontalSpan = 3;
		tableViewer.getTable().setLayoutData(tableData);
//		TableLayout tl = new TableLayout();
//		tableViewer.getTable().setLayout(tl);
		final TableViewerColumn labelColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		labelColumn.setLabelProvider(new SimpleCellLabelProvider());
		tableViewer.setLabelProvider(new SimpleCellLabelProvider());
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setInput(new String[0]);
		addText.addModifyListener(new AddFilterAssistListener(addButton, new ControlDecoration(addText, SWT.TOP | SWT.LEFT)));
		AddFilterListener addInclusionListener = new AddFilterListener(addText, tableViewer);
		addButton.addSelectionListener(addInclusionListener);
		addButton.addKeyListener(addInclusionListener);
		addText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				getShell().setDefaultButton(addButton);
			}
		});
		removeButton.addSelectionListener(new RemoveItemListener(tableViewer));
		try {
			new ContentProposalAdapter(
					addText, 
					new TextContentAdapter(), 
					new SimpleJavaContentProposalProvider(), 
					KeyStroke.getInstance("Ctrl+Space"), 
					new char[] { '.' }
				);
		} catch (ParseException e1) {
		}
		tableViewer.getTable().addControlListener(new ControlAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.ControlAdapter#controlResized(org.eclipse.swt.events.ControlEvent)
			 */
			@Override
			public void controlResized(ControlEvent e) {
				labelColumn.getColumn().setWidth(((Table)e.widget).getClientArea().width);
			}
		});
		return tableViewer;
	}

	public void setInclusionFilters(String[] include) {
		TreeSet<String> filtered = new TreeSet<String>();
		for (String s : include) {
			filtered.add(s);
		}
		inclusionTable.setInput(filtered.toArray(new String[filtered.size()]));
	}
	
	public void setExclusionFilters(String[] exclude) {
		TreeSet<String> filtered = new TreeSet<String>();
		for (String s : exclude) {
			filtered.add(s);
		}
		exclusionTable.setInput(filtered.toArray(new String[filtered.size()]));
	}
	
	public String[] getInclusionFilters() {
		String[] input = (String[]) inclusionTable.getInput();
		TreeSet<String> filtered = new TreeSet<String>();
		for (String s : input) {
			filtered.add(s);
		}
		return filtered.toArray(new String[filtered.size()]);
	}
	
	public String[] getExclusionFilters() {
		String[] input = (String[]) exclusionTable.getInput();
		TreeSet<String> filtered = new TreeSet<String>();
		for (String s : input) {
			filtered.add(s);
		}
		filtered.remove("");
		return filtered.toArray(new String[filtered.size()]);
	}
	
	public boolean isUsingProjectClassesOnly() {
		return quickFilterButton.getSelection();
	}
	
	public void setFilterShortcut(int shortcut) {
		switch (shortcut) {
		case SHORTCUT_NONE:
			quickFilterButton.setSelection(false);
			noShortcutButton.setSelection(true);
			break;
//		case SHORTCUT_PREVIOUS_SKETCH:
//			selectSketchButton.setSelection(true);
//			break;
		case SHORTCUT_PROJECT_CLASSES:
			noShortcutButton.setSelection(false);
			quickFilterButton.setSelection(true);
			break;
		}
		updateEnabledState();
	}
	
//	public IProgramSketch getReferencedFilterSketch() {
//		int i = sketchCombo.getSelectionIndex();
//		if (i < 0) return null;
//		return ((IProgramSketch[])sketchCombo.getData())[i];
//	}
	
	protected void updateEnabledState() {
		boolean enabled = !quickFilterButton.getSelection();
//		sketchCombo.setEnabled(enabled);
		LinkedList<Control> controls = new LinkedList<Control>();
		if (inclusionGroup != null) {
			controls.add(inclusionGroup);
		}
		if (exclusionGroup != null) {
			controls.add(exclusionGroup);
		}
		while(controls.size() > 0) {
			Control c = controls.removeFirst();
			c.setEnabled(enabled);
			if (c instanceof Composite) {
				controls.addAll(Arrays.asList(((Composite)c).getChildren()));
			}
		}
	}

		
	public void setJavaContext(IJavaProject[] projects) {
		this.javaContext = projects;
	}
	
	public void setLaunchType(String launchType) {
		if (LaunchConfigurationUtilities.ECLIPSE_LAUNCH_TYPE.equals(launchType)) {
			quickFilterButton.setText("Only index classes defined in workspace plugins");
			layout();
		}
	}
	
	
	/**
	 * Set the context in which this composite is placed. The context is used to set dirty states of
	 * wizards or configurations. 
	 * @param context
	 */
	public void setParentContext(IFilterContext context) {
		this.parentContext = context;
	}

	/**
	 * @param referencedSketch
	 */
//	public void setReferencedFilterSketch(IProgramSketch referencedSketch) {
//		if (referencedSketch == null) return;
//		IProgramSketch[] sketches = (IProgramSketch[]) sketchCombo.getData();
//		if (sketches != null) {
//			for (int i = 0; i < sketches.length; i++) {
//				if (sketches[i].equals(sketches[i])) {
//					sketchCombo.select(i);
//					break;
//				}
//			}
//		}
//	}
}
