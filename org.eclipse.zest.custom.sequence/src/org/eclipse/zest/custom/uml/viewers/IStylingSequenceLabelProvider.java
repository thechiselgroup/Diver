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

import org.eclipse.swt.SWT;
import org.eclipse.zest.custom.sequence.widgets.Lifeline;
import org.eclipse.zest.custom.sequence.widgets.Message;

/**
 * A label provider that allows for styling of messages and lifelines.
 * 
 * 
 * @author Del Myers
 *
 */
public interface IStylingSequenceLabelProvider extends ISequenceLabelProvider {
	
	/**
	 * Returns an integer style value for the decoration at the source of a message.
	 * The returned value can be any of the styles in the Message class, or a one of
	 * these styles bitwise-ORred with the Message.FILL_MASK constant. May return
	 * -1 for the default style.
	 * 
	 * @see Message
	 * 
	 * @param messageElement
	 * @return the style for the source decoration on the given message. Or -1 for default.
	 */
	public int getMessageSourceStyle(Object messageElement);
	
	/**
	 * Returns an integer style value for the decoration at the target of a message.
	 * The returned value can be any of the styles in the {@link Message} class, or a one of
	 * these styles bitwise-ORred with the Message.FILL_MASK constant. May return -1 for
	 * the default style.
	 * 
	 * @see Message
	 * 
	 * @param messageElement
	 * @return the style for the target decoration on the given message. Or -1 for default.
	 */
	public int getMessageTargetStyle(Object messageElement);
	
	/**
	 * Return one of the {@link SWT} line style constants to be used to draw the given message,
	 * or -1 for the default style.
	 * @param messageElement the element to style.
	 * @return one of the {@link SWT} line style constants to be used to draw the given message,
	 * or -1 for the default style.
	 */
	public int getMessageLineStyle(Object messageElement);
	
	/**
	 * Returns and integer style value for the top of the given lifeline. May
	 * be any of the style constants defined on the {@link Lifeline} class. May
	 * also return -1 for the default style.
	 * 
	 * @see Lifeline
	 * 
	 * @param lifelineElement the lifeline to style.
	 * @return they style constant for the given lifeline. or -1 for default.
	 */
	public int getLifelineStyle(Object lifelineElement);

}
