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
package org.eclipse.zest.custom.uml.viewers;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Widget;


public class BreadCrumbViewer extends ContentViewer {
	
	private static class BreadCrumbItem  extends Composite implements Listener {
		private Label imageLabel;
		private Label textLabel;
		
		private Image image;
		private String text;

		/**
		 * @param parent
		 * @param style
		 */
		public BreadCrumbItem(Composite parent) {
			super(parent, SWT.NONE);
			GridLayout layout = new GridLayout(2, false);
			layout.horizontalSpacing=3;
			layout.verticalSpacing=0;
			layout.marginHeight=0;
			layout.marginWidth=3;
			setLayout(layout);
		}
		
		/**
		 * 
		 */
		private void hook() {
			if (imageLabel != null) {
				imageLabel.addListener(SWT.MouseDown, this);
				imageLabel.addListener(SWT.MouseUp, this);
				imageLabel.addListener(SWT.MouseDoubleClick, this);
				imageLabel.addListener(SWT.KeyDown, this);
				imageLabel.addListener(SWT.KeyUp, this);
			}
			textLabel.addListener(SWT.MouseDown, this);
			textLabel.addListener(SWT.MouseUp, this);
			textLabel.addListener(SWT.MouseDoubleClick, this);
			textLabel.addListener(SWT.KeyDown, this);
			textLabel.addListener(SWT.KeyUp, this);
		}

		public void setText(String text) {
			this.text = text;
			rebuild();
		}
		
		public void setImage(Image image) {
			this.image = image;
			rebuild();
		}
		
		private void rebuild() {
			if (imageLabel != null && !imageLabel.isDisposed()) {
				imageLabel.dispose();
			}
			if (textLabel != null && !textLabel.isDisposed()) {
				textLabel.dispose();
			}
			if (image != null) {
				imageLabel = new Label(this, SWT.NONE);
				imageLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
				imageLabel.setImage(image);
			}
			textLabel = new Label(this, SWT.NONE);
			GridData textLayoutData = new GridData(SWT.BEGINNING, SWT.BEGINNING, true, true);
			if (image == null) {
				textLayoutData.horizontalSpan=2;
			}
			textLabel.setText((text != null) ? text : "");
			textLabel.setLayoutData(textLayoutData);
			hook();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
		 */
		public void handleEvent(Event e) {
			Event e2 = new Event();
			e2.button=e.button;
			e2.character=e.character;
			e2.count=e.count;
			e2.data=getData();
			e2.detail=e.detail;
			e2.display=getDisplay();
			e2.doit=e.doit;
			e2.end=e.end;
			e2.gc=e.gc;
			e2.height=e.height;
			e2.index=0;
			e2.item=null;
			e2.keyCode=e.keyCode;
			e2.start=e.start;
			e2.stateMask=e.stateMask;
			e2.text=e.text;
			e2.time=e.time;
			e2.type=e.type;
			e2.widget=this;
			e2.width=e.width;
			if (e.widget instanceof Control) {
				Point p = new Point(e.x, e.y);
				p = ((Control)e.widget).toDisplay(p);
				p = toControl(p);
				e2.x = p.x;
				e2.y = p.y;
			} else {
				e2.x = 0;
				e2.y = 0;
			}
			notifyListeners(e2.type, e2);
						
		}
		
	}
	
	
	private class MenuSelectionAction extends Action {
		private Object element;
		public MenuSelectionAction(Object element) {
			ILabelProvider provider = (ILabelProvider) getLabelProvider();
			setText("-> " + provider.getText(element));
			Image i = provider.getImage(element);
			if (i != null) {
				setImageDescriptor(ImageDescriptor.createFromImage(i));
			}
			this.element = element;
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public void run() {
			setSelection(new StructuredSelection(element));
		}
	}
	
	private class InternalMenuListener implements IMenuListener {
		/* (non-Javadoc)
		 * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
		 */
		public void menuAboutToShow(IMenuManager manager) {
			for (BreadCrumbItem w : links) {
				manager.add(new MenuSelectionAction(w.getData()));
			}
		}
	}
	
	private Composite control;
	private Composite linksComposite;
	private ArrayList<BreadCrumbItem> links;
	private Listener labelSelectionListener;
	private ISelection selection;
	private Menu menu;

	public BreadCrumbViewer(Composite parent, int style) {
		control = new Composite(parent, SWT.FLAT | SWT.BORDER);
		control.setBackground(parent.getBackground());
		control.setLayout(new GridLayout(2, false));
		linksComposite = new Composite(control, SWT.FLAT);
		linksComposite.setBackground(parent.getBackground());
		GridData lcData = new GridData(SWT.LEFT, SWT.FILL, true, false);
		lcData.heightHint = control.getFont().getFontData()[0].getHeight() + 10;
		linksComposite.setLayoutData(lcData);
		this.links = new ArrayList<BreadCrumbItem>();
		RowLayout linksLayout = new RowLayout();
		linksLayout.fill = true;
		linksLayout.wrap = false;
		linksLayout.justify =false;
		linksLayout.pack = true;
		linksComposite.setLayout(linksLayout);
		Button menuButton = new Button(control, SWT.PUSH | SWT.RIGHT | SWT.FLAT);
		menuButton.addPaintListener(new PaintListener(){
			public void paintControl(PaintEvent e) {
				if (e.widget instanceof Control) {
					Control c = (Control) e.widget;
					Rectangle bounds = c.getBounds();
					e.gc.fillRectangle(new Rectangle(0,0,bounds.width, bounds.height));
					if (e.widget.equals(e.widget.getDisplay().getCursorControl())) {
						e.gc.setBackground(e.widget.getDisplay().getSystemColor(SWT.COLOR_WHITE));
						e.gc.fillRoundRectangle(0,0, bounds.width-1,bounds.height-1,3,3);
						e.gc.drawRoundRectangle(0,0, bounds.width-1,bounds.height-1,3,3);
					}
					int x = bounds.width/2 - 3;
					int y = bounds.height/2 - 3;
					e.gc.setAntialias(SWT.ON);
					e.gc.setLineWidth(1);
					e.gc.drawLine(x, y, x+3, y+3);
					e.gc.drawLine(x+3, y+3, x, y+6);
					e.gc.drawLine(x+3, y, x+6, y+3);
					e.gc.drawLine(x+6, y+3, x+3, y+6);
					
				}
			}
		});
		menuButton.addMouseTrackListener(new MouseTrackListener(){
			public void mouseEnter(MouseEvent e) {					
			}

			public void mouseExit(MouseEvent e) {
				((Button)e.widget).redraw();
			}

			public void mouseHover(MouseEvent e) {
			}
			
		});
		//menuButton.setEnabled(true);
		
		menuButton.setToolTipText("Show Trace To Root");
		MenuManager manager = new MenuManager("Show Trace To Root");
		this.menu = manager.createContextMenu(menuButton);
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new InternalMenuListener());
		menuButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				menu.setVisible(true);
			}
		});
		//for some reason, we have to manually dispose the menu. It isn't done automatically.
		menuButton.addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e) {
				if (menu != null && !menu.isDisposed()) {
					menu.dispose();
				}
			}
			
		});
		GridData buttonData = new GridData(SWT.BEGINNING, SWT.TOP, false, false);
		buttonData.widthHint=16;
		buttonData.heightHint=16;
		menuButton.setLayoutData(buttonData);
		labelSelectionListener = new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.KeyDown:
					if (event.character != ' ' && event.character != '\n') {
						return;
					}
				case SWT.MouseUp:
					if (event.button != 1) {
						return;
					}
				}
				ISelection selection = new StructuredSelection(event.widget.getData());
				setSelection(selection);
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	@Override
	public Control getControl() {
		return control;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#getSelection()
	 */
	@Override
	public ISelection getSelection() {
		return selection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#refresh()
	 */
	@Override
	public void refresh() {
		//always do a complete refresh.
		for (Widget child : linksComposite.getChildren()) {
			child.dispose();
		}
		links.clear();
		Object[] elements = ((IStructuredContentProvider)getContentProvider()).getElements(getInput());
		for (int i = 0; i < elements.length-1; i++) {
			BreadCrumbItem linkLabel = new BreadCrumbItem(linksComposite);
			linkLabel.setCursor(linkLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
			linkLabel.addListener(SWT.KeyDown, labelSelectionListener);
			linkLabel.addListener(SWT.MouseUp, labelSelectionListener);
			linkLabel.setBackground(linksComposite.getBackground());
			update(linkLabel, elements[i]);
			links.add(linkLabel);
			Label arrow = new Label(linksComposite, SWT.NONE);
			arrow.setText("->");
			arrow.setBackground(control.getBackground());
		}
		if (elements.length > 0) {
			BreadCrumbItem linkLabel = new BreadCrumbItem(linksComposite);
			linkLabel.setCursor(linkLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
			linkLabel.addListener(SWT.KeyDown, labelSelectionListener);
			linkLabel.addListener(SWT.MouseUp, labelSelectionListener);
			linkLabel.setBackground(control.getBackground());
			update(linkLabel, elements[elements.length-1]);
			links.add(linkLabel);
		}	
		linksComposite.layout(true, true);
		control.layout(true, true);
	}


	/**
	 * @param linkLabel
	 * @param object
	 */
	private void update(BreadCrumbItem widget, Object element) {
		ILabelProvider provider = (ILabelProvider) getLabelProvider();
		widget.setData(element);
		widget.setText(provider.getText(element));
		widget.setImage(provider.getImage(element));
		widget.setForeground(widget.getDisplay().getSystemColor(SWT.COLOR_BLUE));
		widget.setCursor(widget.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection, boolean)
	 */
	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		this.selection = selection;
		fireSelectionChanged(new SelectionChangedEvent(this, selection));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#inputChanged(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void inputChanged(Object input, Object oldInput) {
		refresh();
	}

	
	
}