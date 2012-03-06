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

import java.util.TreeSet;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

import ca.uvic.chisel.javasketch.FilterSettings;

/**
 * Editing support that provides a text cell editor.
 * @author Del Myers
 *
 */
class JavaSketchFilterEditingSupport extends EditingSupport {
	
	private TextCellEditor editor;
		
	/**
	 * @param viewer
	 */
	public JavaSketchFilterEditingSupport(ColumnViewer viewer) {
		super(viewer);
		editor = new TextCellEditor((Composite)viewer.getControl());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.EditingSupport#canEdit(java.lang.Object)
	 */
	@Override
	protected boolean canEdit(Object element) {
		return (getViewer().getInput() instanceof String[]) &&
			(element instanceof String);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.EditingSupport#getCellEditor(java.lang.Object)
	 */
	@Override
	protected CellEditor getCellEditor(Object element) {
		if (element instanceof String) {
			return editor;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.EditingSupport#getValue(java.lang.Object)
	 */
	@Override
	protected Object getValue(Object element) {
		//the element will just be a string, return it.
		return element;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.EditingSupport#setValue(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void setValue(Object element, Object value) {
		if (FilterSettings.isValidFilterString(value.toString()) >= 0) {
			return;
		}
		if (element.equals(value)) {
			return;
		}
		if (getViewer().getInput() instanceof String[]) {
			String[] values = (String[]) getViewer().getInput();
			TreeSet<String> filtered = new TreeSet<String>();
			filtered.add(value.toString());
			for (String s : values) {
				filtered.add(s);
			}
			if (filtered.contains("")) {
				filtered.remove("");
			}
			String[] input = new String[filtered.size()+1];
			input = filtered.toArray(input);
			input[filtered.size()] = "";
			getViewer().setInput(input);
		}
	}
	
	

	public TextCellEditor getTextCellEditor() {
		return editor;
	}

}
