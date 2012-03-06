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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import ca.uvic.chisel.javasketch.IProgramSketch;
import ca.uvic.chisel.javasketch.SketchPlugin;
import ca.uvic.chisel.javasketch.data.model.ITraceModel;
import ca.uvic.chisel.javasketch.ui.internal.presentation.metadata.PresentationData;

/**
 * @author Del Myers
 *
 */
public class GeneralNotesPropertySection extends TraceModelPropertySection {

	private Group detailsGroup;
	private Text text;

	/**
	 * 
	 */
	public GeneralNotesPropertySection() {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		Composite form = getWidgetFactory().createFlatFormComposite(parent);
		form.setLayout(new GridLayout(1, false));
		detailsGroup = getWidgetFactory().createGroup(form, "Notes");
		detailsGroup.setText("Notes");
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		detailsGroup.setLayoutData(data);
		detailsGroup.setLayout(new GridLayout(1, false));
		createDetailsPages(detailsGroup);
        
	}

	/**
	 * @param detailsGroup2
	 */
	private void createDetailsPages(Composite parent) {
		text = getWidgetFactory().createText(parent, "", SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 150;
		text.setLayoutData(gd);
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Object o = getModelObject();
				if (o instanceof ITraceModel) {
					PresentationData pd = getPresentation(o);
					if (pd != null) {
						try {
							pd.setAnnotation((ITraceModel)o, text.getText());
						} finally {
							pd.disconnect();
						}
					}
				}
			}
		});
	}
	
		
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
	public void refresh() {
		super.refresh();
		Object o = getModelObject();
		String string = null;
		if (o instanceof ITraceModel) {
			PresentationData pd = getPresentation(o);
			if (pd != null) {
				try {
					string = pd.getAnnotation((ITraceModel)o);
				}
				finally {
					pd.disconnect();
				}
			}
		}
		if (string == null) {
			string = "";
		}
		text.setText(string);
	}
	
	PresentationData getPresentation(Object o) {
		if (o instanceof ITraceModel) {
			ITraceModel modelElement = (ITraceModel) o;
			IProgramSketch sketch = SketchPlugin.getDefault().getSketch(modelElement);
			if (sketch != null) {
				return PresentationData.connect(sketch);
			}
		}
		return null;
	}

}
