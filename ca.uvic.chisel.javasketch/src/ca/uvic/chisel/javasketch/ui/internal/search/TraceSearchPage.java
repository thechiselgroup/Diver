package ca.uvic.chisel.javasketch.ui.internal.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.ui.internal.views.TraceNavigatorLabelProvider;

public class TraceSearchPage extends DialogPage implements ISearchPage {
	public static final String ID ="ca.uvic.chisel.javasketch.page.search";
	
	private static final String TRACE_SEARCH_SECTION = "trace_search";
	private static final String TRACE_SEARCH_STRINGS = "search_strings";
	private static final String TRACE_SEARCH_MASK = "search_mask";
	private static final String TRACE_SEARCH_SCOPE = "search_scope";
	private ISearchPageContainer container;
	private Combo searchCombo;
	private Button classesButton;
	private Button methodsButton;
	private Button activationsButton;
	private Button propertiesButton;
	private ContainerCheckedTreeViewer tracesViewer;
	private Button caseSensitive;

	public TraceSearchPage() {

	}

	public TraceSearchPage(String title) {
		super(title);
	}

	public TraceSearchPage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public boolean performAction() {
		int searchMask = getSearchMask();

		LinkedList<IProgramSketch> scope = new LinkedList<IProgramSketch>();
		for (Object o : tracesViewer.getCheckedElements()) {
			if (o instanceof IProgramSketch) {

				scope.add((IProgramSketch) o);
			}
		}
		NewSearchUI.runQueryInBackground(new TraceSearchQuery(scope
			.toArray(new IProgramSketch[scope.size()]), searchCombo.getText(),
			searchMask));
		return true;
	}

	/**
	 * @return
	 */
	private int getSearchMask() {
		int searchMask = 0;
		if (classesButton.getSelection()) {
			searchMask = TraceSearchQuery.CLASSES;
		}
		if (methodsButton.getSelection()) {
			searchMask = TraceSearchQuery.METHODS;
		}
		if (activationsButton.getSelection()) {
			searchMask |= TraceSearchQuery.ACTIVATIONS;
		}
		if (propertiesButton.getSelection()) {
			searchMask = TraceSearchQuery.PROPERTIES;
		}
		if (caseSensitive.getSelection()) {
			searchMask |= TraceSearchQuery.CASE_SENSITIVE;
		}
		return searchMask;
	}

	@Override
	public void setContainer(ISearchPageContainer container) {
		this.container = container;
		container.setPerformActionEnabled(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite page = new Composite(parent, SWT.BORDER);
		page.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		page.setLayout(new GridLayout());
		createSearchArea(page);
		createTypesArea(page);
		createTracesArea(page);
		setControl(page);
		reload();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			getControl().getParent().layout();
		}
	}

	/**
	 * @param page
	 */
	private void createTracesArea(Composite page) {
		Group tracesGroup = new Group(page, SWT.NONE);
		tracesGroup.setLayout(new GridLayout());
		tracesGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tracesGroup.setText("Search Scope");

		tracesViewer = new ContainerCheckedTreeViewer(tracesGroup, SWT.BORDER
				| SWT.CHECK | SWT.DOUBLE_BUFFERED);
		tracesViewer.setContentProvider(new SimpleTraceContentProvider());
		tracesViewer.setLabelProvider(new TraceNavigatorLabelProvider());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		// gd.heightHint = 300;
		tracesViewer.getTree().setLayoutData(gd);
		tracesViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		// treeCheckSelectionListener = new TreeCheckSelectionListener();
		// tracesViewer.getTree().addSelectionListener(treeCheckSelectionListener);
		tracesViewer.expandAll();

	}

	/**
	 * @param page
	 */
	private void createTypesArea(Composite page) {
		Group typesGroup = new Group(page, SWT.NONE);
		typesGroup.setText("Search For");

		typesGroup.setLayout(new GridLayout(3, true));
		GridDataFactory gdf = GridDataFactory.createFrom(new GridData(SWT.FILL,
			SWT.FILL, true, false));
		typesGroup.setLayoutData(gdf.create());

		classesButton = new Button(typesGroup, SWT.RADIO);
		classesButton.setText("Classes");
		classesButton.setLayoutData(gdf.create());

		methodsButton = new Button(typesGroup, SWT.RADIO);
		methodsButton.setText("Methods");
		methodsButton.setLayoutData(gdf.create());
		
		activationsButton = new Button(typesGroup, SWT.CHECK);
		activationsButton.setText("Include Activations");
		activationsButton.setLayoutData(gdf.create());
		
		SelectionListener enableListener = new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				activationsButton.setEnabled(true);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};
		
		methodsButton.addSelectionListener(enableListener);
		classesButton.addSelectionListener(enableListener);

		propertiesButton = new Button(typesGroup, SWT.RADIO);
		propertiesButton.setText("Notes");
		propertiesButton.setLayoutData(gdf.create());
		
		propertiesButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				activationsButton.setEnabled(false);
			}
			
		});
		
		
	}

	/**
	 * @param page
	 */
	private void createSearchArea(Composite page) {
		Composite searchArea = new Composite(page, SWT.NONE);
		searchArea.setLayout(new GridLayout(2, false));

		GridDataFactory gdf = GridDataFactory.createFrom(new GridData(SWT.FILL,
			SWT.FILL, true, false));
		GridData gd = gdf.create();
		// gd.widthHint = convertWidthInCharsToPixels(200);
		searchArea.setLayoutData(gd);

		Label instructions = new Label(searchArea, SWT.NONE);
		instructions
			.setText("Search String (* = any string, ? = any character)");
		gd = gdf.create();
		gd.horizontalSpan = 2;
		instructions.setLayoutData(gd);

		searchCombo = new Combo(searchArea, SWT.BORDER);
		gd = gdf.create();
		gd.widthHint = convertWidthInCharsToPixels(50);
		searchCombo.setLayoutData(gd);

		searchCombo.addVerifyListener(new VerifyListener() {

			@Override
			public void verifyText(VerifyEvent e) {
				if (searchCombo.getText() == null
						|| searchCombo.getText().isEmpty()) {
					setErrorMessage(null);
					container.setPerformActionEnabled(false);
				}
				// make sure that no illegal characters are present
				boolean ok = e.text.indexOf('%') < 0;
				if (!ok) {
					container.setPerformActionEnabled(false);
					setErrorMessage("Illegal character: %");
				} else {
					setErrorMessage(null);
					container.setPerformActionEnabled(true);
				}
			}
		});

		caseSensitive = new Button(searchArea, SWT.CHECK);
		caseSensitive.setText("Case sensitive");
		gd = gdf.create();
		gd.grabExcessHorizontalSpace = false;
		caseSensitive.setLayoutData(gd);

	}

	/**
	 * 
	 */
	private void reload() {
		IDialogSettings settings = SketchPlugin.getDefault()
			.getDialogSettings();
		IDialogSettings section = settings.getSection(TRACE_SEARCH_SECTION);
		if (section == null) {
			section = settings.addNewSection(TRACE_SEARCH_SECTION);
		}
		String[] searches = section.getArray(TRACE_SEARCH_STRINGS);
		if (searches != null) {
			if (searches.length > 0) {
				searchCombo.setItems(searches);
				searchCombo.setText(searches[0]);
			}
		}
		try {
			int mask = section.getInt(TRACE_SEARCH_MASK);
			activationsButton
				.setSelection((mask & TraceSearchQuery.ACTIVATIONS) != 0);
			if ((mask & TraceSearchQuery.CLASSES) != 0) {
				classesButton.setSelection(true);
				activationsButton.setEnabled(true);
			} else if ((mask & TraceSearchQuery.METHODS) != 0) {
				methodsButton.setSelection(true);
				activationsButton.setEnabled(true);
			} else if ((mask & TraceSearchQuery.PROPERTIES) != 0) {
				propertiesButton.setSelection(true);
				activationsButton.setEnabled(false);
			}
			caseSensitive
				.setSelection((mask & TraceSearchQuery.CASE_SENSITIVE) != 0);
		} catch (NumberFormatException e) {
			// there was no mask stored, set the activations button to
			// selected
			activationsButton.setSelection(true);
			caseSensitive.setSelection(true);

		}
		String[] checked = section.getArray(TRACE_SEARCH_SCOPE);
		if (checked != null) {
			HashSet<IProgramSketch> checkedSketches = new HashSet<IProgramSketch>();
			for (String id : checked) {
				IProgramSketch sketch = SketchPlugin.getDefault().getSketch(id);
				if (sketch != null) {
					checkedSketches.add(sketch);
				}
			}
			tracesViewer.setCheckedElements(checkedSketches.toArray());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	@Override
	public void dispose() {
		writeConfiguration();
		super.dispose();
	}

	/**
	 * 
	 */
	private void writeConfiguration() {
		IDialogSettings settings = SketchPlugin.getDefault()
			.getDialogSettings();
		IDialogSettings section = settings.getSection(TRACE_SEARCH_SECTION);
		if (section == null) {
			section = settings.addNewSection(TRACE_SEARCH_SECTION);
		}
		String[] searchStrings = section.getArray(TRACE_SEARCH_STRINGS);
		if (searchStrings == null) {
			searchStrings = new String[0];
		}
		LinkedList<String> searchList = new LinkedList<String>(Arrays
			.asList(searchStrings));
		for (Iterator<String> it = searchList.iterator(); it.hasNext();) {
			String s = it.next();
			if (s.equals(searchCombo.getText()) || s.isEmpty()) {
				it.remove();
			}
		}
		// remove the current search string from the list if it already
		// exists.
		LinkedList<String> newStrings = new LinkedList<String>();
		String s = searchCombo.getText();
		if (s != null && !s.isEmpty()) {
			newStrings.add(s);
		}
		newStrings.addAll(searchList);
		section.put(TRACE_SEARCH_STRINGS, newStrings
			.toArray(new String[newStrings.size()]));
		section.put(TRACE_SEARCH_MASK, getSearchMask());

		ArrayList<String> scope = new ArrayList<String>();
		for (Object o : tracesViewer.getCheckedElements()) {
			if (o instanceof IProgramSketch) {
				scope.add(((IProgramSketch) o).getID());
			}
		}
		section
			.put(TRACE_SEARCH_SCOPE, scope.toArray(new String[scope.size()]));

	}

}
