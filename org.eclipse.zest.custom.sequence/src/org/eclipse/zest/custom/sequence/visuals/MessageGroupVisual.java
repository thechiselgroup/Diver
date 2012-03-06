/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Del Myers -- initial API and implementation
 *******************************************************************************/
package org.eclipse.zest.custom.sequence.visuals;

import java.util.List;

import org.eclipse.draw2d.ActionEvent;
import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.zest.custom.sequence.figures.ActivationGroupFigure;
import org.eclipse.zest.custom.sequence.widgets.Activation;
import org.eclipse.zest.custom.sequence.widgets.MessageGroup;
import org.eclipse.zest.custom.sequence.widgets.PropertyChangeListener;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * Visual part for activation groups.
 * @author Del Myers
 */

public class MessageGroupVisual extends WidgetVisualPart implements PropertyChangeListener {

	private class GroupExpandListener implements ActionListener {

		public void actionPerformed(ActionEvent event) {
			((MessageGroup)getWidget()).setExpanded(((ActivationGroupFigure)getFigure()).isExpanded());			
		}
		
	}
	
	//a figure that is used to fill the background area, if available.
	private RectangleFigure backgroundFigure;

	private GroupExpandListener expandListener;
	//Color used for highlighting the background.
	private Color backgroundBackupColor;
	
	/**
	 * @param item
	 * @param key
	 * @param parentFigure
	 */
	public MessageGroupVisual(UMLItem item, String key) {
		super(item, key);
		this.expandListener = new GroupExpandListener();
	}

	@Override
	public IFigure createFigures() {
		this.backgroundFigure = new RectangleFigure();
		backgroundFigure.setToolTip(new Label());
		backgroundFigure.addMouseListener(new MouseListener(){
			public void mouseDoubleClicked(MouseEvent me) {
				//me.consume();
			}

			public void mousePressed(MouseEvent me) {
				//me.consume();
			}

			public void mouseReleased(MouseEvent me) {
				//me.consume();
			}
			
		});
		IFigure figure = new ActivationGroupFigure();
		figure.setToolTip(new Label());
		return figure;
	}

	@Override
	public void activate() {
		super.activate();
		getWidget().addPropertyChangeListener(this);
		((ActivationGroupFigure)getFigure()).addActionListener(expandListener);
	}
	
	@Override
	public void deactivate() {
		getWidget().removePropertyChangeListener(this);
		((ActivationGroupFigure)getFigure()).removeActionListener(expandListener);
		if (!getWidget().isDisposed())
			getWidget().setData(IWidgetProperties.LAYOUT, null);
		super.deactivate();
	}
	
	
	@Override
	public void refreshVisuals() {
		if (getWidget().isDisposed()) return;
		((ActivationGroupFigure)getFigure()).setText(getWidget().getText());
		((ActivationGroupFigure)getFigure()).setForegroundColor(getWidget().getForeground()); 
		((ActivationGroupFigure)getFigure()).setArrowDown(((MessageGroup)getWidget()).isExpanded());
		Label tooltipFigure = (Label) getFigure().getToolTip();
		String tooltipText = getWidget().getTooltipText();
		if (tooltipText == null) {
			tooltipText = getWidget().getText();
		}
		tooltipFigure.setText(tooltipText);
		Rectangle bounds = getFigure().getBounds().getCopy();
		//boolean updateBounds = false;
		if (bounds == null || bounds.isEmpty()) {
			//updateBounds = true;
			MessageGroup group = (MessageGroup) getWidget();
			Activation a = group.getActivation();
			if (a != null) {
				WidgetVisualPart activationVisuals = (WidgetVisualPart) a.getData(MessageBasedSequenceVisuals.VISUAL_KEY);
				if (activationVisuals != null) {
					bounds = activationVisuals.getFigure().getBounds().getCopy();
				}
			}
			if (bounds == null) {
				//still null, make sure it is set.
				bounds = new Rectangle();
			}
			getFigure().setBounds(bounds);
			//getParentFigure().setConstraint(getFigure(), bounds);
		}
		//update the background
		if (backgroundFigure.getParent() != null) {
			Color backgroundColor = getWidget().getBackground();
			if (backgroundColor == null) {
				//remove from the background.
				backgroundFigure.setVisible(false);
			} else {
				//check to see if the widget is "pinned"
				if (getWidget().getData("pin") != null) {
					refreshPinnedColor();
					backgroundColor = backgroundBackupColor;
				} else {
					clearPinnedColor();
				}
				backgroundFigure.setBackgroundColor(backgroundColor);
				backgroundFigure.setBounds(bounds.getCopy());
				((Label)backgroundFigure.getToolTip()).setText(tooltipText);
			}
		}
	}

	/**
	 * 
	 */
	private void refreshPinnedColor() {
		clearPinnedColor();
		RGB selectionRGB = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB();
		RGB widgetRGB = getWidget().getBackground().getRGB();
		int newR = (selectionRGB.red+widgetRGB.red*4)/5;
		int newG = (selectionRGB.green+widgetRGB.green*4)/5;
		int newB = (selectionRGB.blue+widgetRGB.blue*4)/5;
		RGB newRGB = new RGB(
			(newR <= 255) ? newR : 255,
			(newG <= 255) ? newG : 255,
			(newB <= 255) ? newB : 255
		);
		this.backgroundBackupColor = new Color(Display.getCurrent(), newRGB);
		backgroundFigure.setBackgroundColor(this.backgroundBackupColor);
	}

	/**
	 * 
	 */
	private void clearPinnedColor() {
		if (backgroundFigure != null && backgroundFigure.getBackgroundColor() == backgroundBackupColor) {
			backgroundFigure.setBackgroundColor(getWidget().getBackground());
		}
		if (backgroundBackupColor != null && !backgroundBackupColor.isDisposed()){
			backgroundBackupColor.dispose();
			backgroundBackupColor = null;
		}
	}

	public void propertyChanged(Object source, String property,
			Object oldValue, Object newValue) {
		if (getChartVisuals().refreshing) return; //early return
		if (IWidgetProperties.LAYOUT.equals(property) || IWidgetProperties.TEXT.equals(property)) {
			MessageGroup mg = (MessageGroup) getWidget();
			Rectangle bounds = (Rectangle) ((MessageGroup)getWidget()).getData(IWidgetProperties.LAYOUT);
			if (bounds == null) {
				bounds = getFigure().getBounds().getCopy();
			}
			getFigure().getParent().setConstraint(getFigure(), bounds);
			refreshVisuals();
			
			
			if (backgroundFigure.getParent() != null) {
				backgroundFigure.getParent().setConstraint(backgroundFigure, bounds);
			}
			Activation a = mg.getActivation();
			ActivationGroupFigure fig = (ActivationGroupFigure) getFigure();
			fig.setLabelOffset(0);
			if (a != null && !a.isDisposed()) {
				Rectangle aBounds = (Rectangle) a.getData(IWidgetProperties.LAYOUT);
				if (aBounds != null) {
					if (mg.isExpanded()) {
						fig.setLabelOffset(Math.max(aBounds.x - bounds.x, 0));
					}
				}
			}
		} else if ("pin".equals(property)) {
			if ((newValue != null)) {
				refreshPinnedColor();
				((ActivationGroupFigure)getFigure()).setLineWidth(3);
			} else {
				((ActivationGroupFigure)getFigure()).setLineWidth(1);
				clearPinnedColor();
			}
		} else if (IWidgetProperties.BACKGROUND_COLOR.equals(property)) {
			if (newValue == null) {
				backgroundFigure.setVisible(false);
			} else {
				backgroundFigure.setVisible(true);
			}
		}
		refreshVisuals();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#installFigures()
	 */
	@Override
	protected void installFigures() {
		super.installFigures();
		if (backgroundFigure.getParent() == null) {
			IFigure layer = getLayer(LayerConstants.BACKGROUND_LAYER);
			if (layer != null) {
				layer.add(backgroundFigure);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#uninstallFigures()
	 */
	@Override
	protected void uninstallFigures() {
		super.uninstallFigures();
		if (backgroundFigure.getParent() != null) {
			backgroundFigure.getParent().remove(backgroundFigure);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.zest.custom.sequence.visuals.WidgetVisualPart#getFigure()
	 */
	@Override
	public List<IFigure> getFigures() {
		List<IFigure> figures = super.getFigures();
		figures.add(backgroundFigure);
		return figures;
	}

}
