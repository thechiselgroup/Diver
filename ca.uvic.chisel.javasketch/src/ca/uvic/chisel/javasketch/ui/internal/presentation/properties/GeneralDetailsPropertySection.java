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

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import ca.uvic.chisel.javasketch.data.model.IActivation;
import ca.uvic.chisel.javasketch.data.model.IMessage;
import ca.uvic.chisel.javasketch.data.model.IOriginMessage;
import ca.uvic.chisel.javasketch.data.model.ITargetMessage;
import ca.uvic.chisel.javasketch.data.model.IThread;
import ca.uvic.chisel.javasketch.data.model.ITrace;
import ca.uvic.chisel.javasketch.data.model.ITraceClass;
import ca.uvic.chisel.javasketch.data.model.ITraceClassMethod;

/**
 * @author Del Myers
 *
 */
public class GeneralDetailsPropertySection extends TraceModelPropertySection {

	private Composite detailsGroup;
	private Control emptyDetails;
	private Control traceDetails;
	private Control activationDetails;
	private Control messageDetails;
	private Control objectDetails;
	private GridDataFactory labelFactory;
	private GridDataFactory labelTextFactory;
	private Text activeFromText;
	private Text activeTimeText;
	private Text originText;
	private Text targetText;
	private Text messageTimeText;
	private Text traceTime;
	private Text analysisTime;
	private Text threadCount;
	private Control threadDetails;
	private Text messageLineText;

	/**
	 * 
	 */
	public GeneralDetailsPropertySection() {
		labelFactory = GridDataFactory.createFrom(new GridData(SWT.FILL, SWT.FILL, false, false));
		labelTextFactory = GridDataFactory.createFrom(new GridData(SWT.FILL, SWT.FILL, true, false));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		Composite form = getWidgetFactory().createFlatFormComposite(parent);
		detailsGroup = getWidgetFactory().createComposite(form);
       detailsGroup.setLayout(new StackLayout());
       createDetailsPages(detailsGroup);
	}

	/**
	 * @param detailsGroup2
	 */
	private void createDetailsPages(Composite parent) {
		StackLayout stack = new StackLayout();
		parent.setLayout(stack);
		this.emptyDetails = createEmptyDetails(parent);
		this.traceDetails = createTraceDetails(parent);
		this.activationDetails = createActivationDetails(parent);
		this.messageDetails = createMessageDetails(parent);
		this.objectDetails = createObjectDetails(parent);
		this.threadDetails = createThreadDetails(parent);
		
	}
	
	/**
	 * @param parent
	 * @return
	 */
	private Control createThreadDetails(Composite parent) {
		return createEmptyDetails(parent);
	}

	private Control createTraceDetails(Composite parent) {
		Composite details = getWidgetFactory().createComposite(parent);
		details.setLayout(new GridLayout(2, false));
		CLabel timeLabel = getWidgetFactory().createCLabel(details, "Trace Time:");
		timeLabel.setLayoutData(labelFactory.create());
		traceTime = getWidgetFactory().createText(details, "", SWT.READ_ONLY);
		traceTime.setLayoutData(labelTextFactory.create());
		CLabel analysisLabel = getWidgetFactory().createCLabel(details, "Analysis Time:");
		analysisLabel.setLayoutData(labelFactory.create());
		analysisTime = getWidgetFactory().createText(details, "", SWT.READ_ONLY);
		analysisTime.setLayoutData(labelTextFactory.create());
		CLabel threadCountLabel = getWidgetFactory().createCLabel(details, "Threads:");
		threadCountLabel.setLayoutData(labelFactory.create());
		threadCount = getWidgetFactory().createText(details, "", SWT.READ_ONLY);
		threadCount.setLayoutData(labelTextFactory.create());
		return details;
	}
	
	
	private Control createActivationDetails(Composite parent) {
		Composite details = getWidgetFactory().createComposite(parent);
		details.setLayout(new GridLayout(2, false));
		CLabel activeFromLabel = getWidgetFactory().createCLabel(details, "Called From:");
		activeFromLabel.setLayoutData(labelFactory.create());
		activeFromText = getWidgetFactory().createText(details, "", SWT.READ_ONLY);
		activeFromText.setLayoutData(labelTextFactory.create());
		CLabel timeLabel = getWidgetFactory().createCLabel(details, "Time:");
		timeLabel.setLayoutData(labelFactory.create());
		activeTimeText = getWidgetFactory().createText(details, "", SWT.READ_ONLY);
		activeTimeText.setLayoutData(labelTextFactory.create());
		return details;
		
	}
	
	
	private Control createMessageDetails(Composite parent) {
		Composite details = getWidgetFactory().createComposite(parent);
		details.setLayout(new GridLayout(2, false));
		CLabel originLabel = getWidgetFactory().createCLabel(details, "Source:");
		originLabel.setLayoutData(labelFactory.create());
		originText = getWidgetFactory().createText(details, "", SWT.READ_ONLY);
		originText.setLayoutData(labelTextFactory.create());
		CLabel targetLabel = getWidgetFactory().createCLabel(details, "Target:");
		targetLabel.setLayoutData(labelFactory.create());
		targetText = getWidgetFactory().createText(details, "", SWT.READ_ONLY);
		targetText.setLayoutData(labelTextFactory.create());
		CLabel timeLabel = getWidgetFactory().createCLabel(details, "Time:");
		timeLabel.setLayoutData(labelFactory.create());
		messageTimeText = getWidgetFactory().createText(details, "", SWT.READ_ONLY);
		messageTimeText.setLayoutData(labelTextFactory.create());
		CLabel lineLabel = getWidgetFactory().createCLabel(details, "Line Number:");
		lineLabel.setLayoutData(labelFactory.create());
		messageLineText = getWidgetFactory().createText(details, "", SWT.READ_ONLY);
		messageLineText.setLayoutData(labelTextFactory.create());
		return details;
	}
	
	private Control createObjectDetails(Composite parent) {
		return createEmptyDetails(parent);
	}

	private Control createEmptyDetails(Composite parent) {
		Label label = getWidgetFactory().createLabel(parent, "No details");
		return label;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
	public void refresh() {
		Object traceModel = getModelObject();
		if (traceModel instanceof IActivation) {
			refreshActivation((IActivation)traceModel);
		} else if (traceModel instanceof IMessage) {
			refreshMessage((IMessage)traceModel);
		} else if (traceModel instanceof ITrace) {
			refreshTrace((ITrace)traceModel);
		} else if (traceModel instanceof ITraceClass) {
			refreshObject((ITraceClass)traceModel);
		} else if (traceModel instanceof IThread) {
			refreshThread((IThread)traceModel);
		} else {
			setTopControl(emptyDetails);
		}
	}

	/**
	 * @param traceModel
	 */
	private void refreshThread(IThread traceModel) {
		setTopControl(threadDetails);		
	}

	/**
	 * @param traceModel
	 */
	private void refreshObject(ITraceClass traceModel) {
		setTopControl(objectDetails);
	}

	/**
	 * @param traceModel
	 */
	private void refreshTrace(ITrace traceModel) {
		DateFormat format = DateFormat.getDateInstance(DateFormat.LONG);
		try {
			traceTime.setText(format.format(traceModel.getTraceTime()));
			analysisTime.setText(format.format(traceModel.getDataTime()));
			threadCount.setText(traceModel.getThreads().size() + "");
		} catch (NullPointerException e) {}
		setTopControl(traceDetails);
	}

	/**
	 * @param traceModel
	 */
	private void refreshMessage(IMessage traceModel) {
		String source = "";
		String target = "";
		String time = "";
		int line = -1;
		IActivation sourceActivation = null;
		IActivation targetActivation = null;
		try {
			if (traceModel instanceof IOriginMessage) {
				sourceActivation = traceModel.getActivation();
				targetActivation = ((IOriginMessage)traceModel).getTarget().getActivation();
				line = ((IOriginMessage)traceModel).codeLine();
			} else {
				targetActivation = traceModel.getActivation();
				sourceActivation = ((ITargetMessage)traceModel).getOrigin().getActivation();
				if (((ITargetMessage)traceModel).getOrigin() != null) {
					line = ((ITargetMessage)traceModel).getOrigin().codeLine();
				}
			}
			ITraceClassMethod sm = sourceActivation.getMethod();
			ITraceClassMethod tm = targetActivation.getMethod();
			source = sm.getTraceClass().getName() + "." + sm.getName();
			target = tm.getTraceClass().getName() + "." + tm.getName();
			long tl = sourceActivation.getTime();
			time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(tl));
		} catch (NullPointerException e) {}
		originText.setText(source);
		targetText.setText(target);
		messageTimeText.setText(time);
		messageLineText.setText(line + "");
		setTopControl(messageDetails);
	}

	/**
	 * @param traceModel
	 */
	private void refreshActivation(IActivation traceModel) {
		long time = traceModel.getTime();
		DateFormat format = DateFormat.getTimeInstance(DateFormat.MEDIUM);
		activeTimeText.setText(format.format(new Date(time)));
		String from = "";
		try {
			ITraceClassMethod method = 
				traceModel.getArrival().getOrigin().getActivation().getMethod();
			ITraceClass tc = method.getTraceClass();
			from = tc.getName() + "." + method.getName();
		} catch (NullPointerException e) {}
		activeFromText.setText(from);
		setTopControl(activationDetails);
		
		
	}

	/**
	 * @param activationDetails2
	 */
	private void setTopControl(Control details) {
		((StackLayout)detailsGroup.getLayout()).topControl = details;
		detailsGroup.layout();
	}

}
