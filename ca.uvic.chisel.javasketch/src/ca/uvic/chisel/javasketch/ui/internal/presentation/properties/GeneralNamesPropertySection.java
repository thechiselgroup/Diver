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
package ca.uvic.chisel.javasketch.ui.internal.presentation.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.ui.internal.presentation.TraceThreadLabelProvider;

/**
 * @author Del Myers
 *
 */
public class GeneralNamesPropertySection extends TraceModelPropertySection {

	private Text typeText;
	private Text nameText;
	private TraceThreadLabelProvider threadLabelProvider;

	/**
	 * 
	 */
	public GeneralNamesPropertySection() {
		threadLabelProvider = new TraceThreadLabelProvider();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		Composite form = getWidgetFactory().createFlatFormComposite(parent);
		form.setLayout(new GridLayout(2, false));
		GridData data;
		
        
        CLabel labelLabel = getWidgetFactory()
        .createCLabel(form, "Type:"); //$NON-NLS-1$
        data = new GridData(SWT.FILL, SWT.FILL, false, false);
        labelLabel.setLayoutData(data);
        
        typeText = getWidgetFactory().createText(form, "", SWT.READ_ONLY); //$NON-NLS-1$
        data = new GridData(SWT.FILL, SWT.FILL, true, false);
        typeText.setLayoutData(data);
        
        CLabel nameLabel = getWidgetFactory()
        .createCLabel(form, "Name:"); //$NON-NLS-1$
        data = new GridData(SWT.FILL, SWT.FILL, false, false);
        nameLabel.setLayoutData(data);
        
		nameText = getWidgetFactory().createText(form, "", SWT.READ_ONLY); //$NON-NLS-1$
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
        nameText.setLayoutData(data);
        
	}
	
	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
	public void refresh() {
		String type = "";
		Object traceModel = getModelObject();
		String name = threadLabelProvider.getText(traceModel);
		
		if (traceModel != null) {
			if (traceModel instanceof ITrace) {
				type = "Trace";
				name = ((ITrace)traceModel).getLaunchID();
			} else if (traceModel instanceof IActivation) {
				type = "Activation";
			} else if (traceModel instanceof IMessage) {
				type = "Message";
			} else if (traceModel instanceof ITraceClass) {
				type = "Class";
			} else if (traceModel instanceof IThread) {
				type = "Thread";
				name = ((IThread)traceModel).getName();
			}
			
			if (name == null) {
				name = "";
			}
		}
		typeText.setText(type);
		nameText.setText(name);
		nameText.getParent().layout();
	}
	

}
