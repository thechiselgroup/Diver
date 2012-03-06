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
package ca.uvic.chisel.javasketch.ui.internal.views;

import java.util.Calendar;

import org.eclipse.core.runtime.IAdaptable;

/**
 * A silly hack that allows a calendar object that may have the same date as another
 * object to be distinguished based on its parent in a tree.
 * @author Del Myers
 *
 */
public class ParentedCalendar implements IAdaptable {

	private Calendar calendar;
	private Object parent;

	/**
	 * @param parentElement
	 * @param day
	 */
	public ParentedCalendar(Object parentElement, Calendar calendar) {
		this.parent = parentElement;
		this.calendar = calendar;
	}
	
	/**
	 * @return the parent
	 */
	public Object getParent() {
		return parent;
	}
	
	/**
	 * @return the calendar
	 */
	public Calendar getCalendar() {
		return calendar;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass().equals(getClass())) {
			ParentedCalendar that = (ParentedCalendar) obj;
			return that.calendar.equals(calendar) && that.parent.equals(parent);
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return calendar.hashCode() + parent.hashCode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.equals(Calendar.class)) {
			return calendar;
		}
		return null;
	}

}
