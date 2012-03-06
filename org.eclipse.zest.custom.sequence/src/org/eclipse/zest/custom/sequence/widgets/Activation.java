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
package org.eclipse.zest.custom.sequence.widgets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.zest.custom.sequence.ZestUMLColors;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * An activation represents a method execution within a sequence diagram.
 * @author Del Myers
 */
public class Activation extends UMLColoredItem implements IExpandableItem {
	
	
	/**
	 * This activation is expanded.
	 */
	private boolean expanded;
	
	private LinkedList<Message> messageList;
	
	/**
	 * List of messages that have this activation as a target.
	 */
	private LinkedList<Message> targetMessages;
	
	private Message[] messages;
	
	private MessageGroup[] messageGroups;
	
	private LinkedList<MessageGroup> messageGroupList;
	
	private Lifeline target;



	private Call call;
	


	/**
	 * @param parent
	 * @param style
	 */
	public Activation(UMLChart parent) {
		super(parent);
		expanded = false;
		setBackground(ZestUMLColors.ColorActivation.getColor());
		setForeground(parent.getForeground());
		this.messageList = null;
		messageGroupList = new LinkedList<MessageGroup>();
		messages = null;
		targetMessages = new LinkedList<Message>();
	}
	

	
	/* (non-Javadoc)
	 * @see org.eclipse.mylar.zest.custom.sequence.widgets.UMLItem#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
	 */
	@Override
	protected void widgetDisposed(DisposeEvent e) {
		setLifeline(null);
		if (messageList != null) {
			for (Message m : messageList.toArray(new Message[messageList.size()])) {
				if (!m.isDisposed()) {
					if (m.getSource() == this) {
						m.setSource(null);
					} else if (m.getTarget() == this) {
						m.setTarget(null);
					}
				}
			}
		}
		messageList = null;
		messages = null;
		for (MessageGroup g : messageGroupList.toArray(new MessageGroup[messageGroupList.size()])) {
			if (!g.isDisposed()) {
				g.setRange(null, 0, 0);
			}
		}
		messageGroupList = null;
		messageGroups = null;
		super.widgetDisposed(e);
	}
	
	
	/**
	 * @return the expanded state of this activation
	 */
	public boolean isExpanded() {
		return expanded;
	}
	
	public void setExpanded(boolean expanded) {
		if (this.expanded == expanded) return;
		this.expanded = expanded;
		getChart().markDirty();
		
		firePropertyChange(IWidgetProperties.EXPANDED, !expanded, expanded);
		if ((getSequenceChart().getStyle() & SWT.VIRTUAL) != 0 && messageList == null) {
			//make sure that after this call, the children state will be correct.
			messageList = new LinkedList<Message>();
			firePropertyChange(IWidgetProperties.MESSAGE, null, new Object());
		}
	}
	
	public UMLSequenceChart getSequenceChart() {
		return (UMLSequenceChart) getChart();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#toString()
	 */
	@Override
	public String toString() {
		return "Activation " + getText() + " on " + ((target == null) ? "null" : target.toString());
	}
	
	

	
	public boolean hasChildren() {
		if ((getSequenceChart().getStyle() & SWT.VIRTUAL) != 0 && messageList == null) return true;
		if (messageList == null) {
			return false;
		}
		for (Message m : getMessages()) {
			if (m instanceof Call) {
				return true;
			}
		}
		return messageGroupList.size() > 0;
		//return false;
	}
	
	/**
	 * Clears all the messages from this activation, and sets the item to have no children.
	 */
	public void clearAllMessages() {
		for (Message m : getMessages()) {
			removeMessage(m);
		}
	}


	/**
	 * Adds the given message as a message originating from this activation, at the given index.
	 * If the message is a Call, then the target is reparented to this activation. An Activation
	 * be targeted by only one call, and the old call will be removed completely from the
	 * call stack (though it will not be disposed). If it is a return, then the message is simply connected
	 * to the given target activation. That is, an activation can be targeted by multiple returns.
	 * Note that having a message return to an activation that isn't on the call stack will
	 * result in an exception being thrown when the chart is refreshed.
	 * @param index the index at which to add the message
	 * @param message the message to add.
	 * @param target the activation that should be targeted.
	 */
	public void addMessage(int index, Message message, Activation target) {
		checkWidget();
		if (message == null || message.isDisposed()) {
			return;
		}
		if (target == this) {
			throw new IllegalArgumentException("Activation cannot target itself");
		}
		if (messageList == null) {
			messageList = new LinkedList<Message>();
		}
		if (index > messageList.size() || index < -1) {
			SWT.error(SWT.ERROR_INVALID_RANGE, null, "index is out of range");
		}
		if (this.equals(message.getSource())) {
			//make sure that it is in the right order
			boolean changed = false;
			if (messageList.get(index) != message) {
				int i = messageList.indexOf(message);
				messageList.remove(i);
				messageList.add(index, message);
				changed = true;
			}
			if (message.getTarget() != target) {
				if (message instanceof Call) {
					target.setCall((Call)message);
				}
				message.setTarget(target);
				changed = true;
			}
			if (changed) {
				messages = null;
				getChart().markDirty();
				firePropertyChange(IWidgetProperties.MESSAGE, null, message);
			}
		} else {
			message.setSource(this);
			if (message instanceof Call) {
				target.setCall((Call)message);
				message.setTarget(target);
			} else {
				message.setTarget(target);
			}
			if (index == -1 || index == messageList.size()) {
				messageList.add(message);
			} else {
				messageList.add(index, message);
			}
			messages = null;
			getChart().markDirty();
			firePropertyChange(IWidgetProperties.MESSAGE, null, message);
		}
	}
	
	private void setCall(Call call) {
		if (this.call != null) {
			call.getSource().removeMessage(call);
			call.setTarget(null);
		}
		this.call = call;
	}
		
	/**
	 * Removes the given message from the list of messages extending to/from this
	 * activation. 
	 * @param message
	 */
	public void removeMessage(Message message) {
		checkWidget();
		if (messageList == null) {
			return;
		}
		if (!messageList.remove(message)) {
			//don't continue processing if the message didn't exist in the list.
			return;
		}
		messages = null;
		if (!message.isDisposed()) {
			if (message.getSource() == this) {
				message.setSource(null);
			} else if (message.getTarget() == this) {
				message.setTarget(null);
			} else {
				return;
			}
		}
		if (messageList.size() == 0) {
			messageList = null;
		}
		getChart().markDirty();
		firePropertyChange(IWidgetProperties.MESSAGE, message, null);
	}
	
	/**
	 * Gets the call that was used to reach this activation.
	 * @return the call that was used to reach this activation.
	 */
	public Call getSourceCall() {
		return call;
	}
	
	/**
	 * Returns all messages from this activation. Clients should not change the ordering
	 * of the messages in the returned array, as this causes undefined results.
	 * @return references to the messages in this activation.
	 */
	public Message[] getMessages() {
		checkWidget();
		if (messageList == null) {
			return new Message[0];
		}
		if (messages == null) {
			messages = new Message[messageList.size()];
			messages = messageList.toArray(messages);
			for (int i = 0; i < messages.length; i++) {
				messages[i].setIndexInActivation(i);
			}
		}
		return messages;
	}
	
	public MessageGroup[] getMessageGroups() {
		checkWidget();
		if (messageGroupList == null) {
			return new MessageGroup[0];
		}
		if (messageGroups == null) {
			messageGroups = messageGroupList.toArray(new MessageGroup[messageGroupList.size()]);
			Arrays.sort(messageGroups, new Comparator<MessageGroup>(){
				public int compare(MessageGroup o1, MessageGroup o2) {
					int diff = o1.getOffset() - o2.getOffset();
					if (diff == 0) {
						diff = o2.getLength() - o1.getLength();
					}
					return diff;
				}
			});
		}
		return messageGroups;
	}


	/**
	 * Returns the index for the given message in this activation, or -1 if it is not in this
	 * activation.
	 * @param message the message to check.
	 * @return the index of the message.
	 */
	public int getMessageIndex(Message message) {
		if (message.getSource() != this) {
			return -1;
		}
		//refresh the indexes.
		getMessages();
		return message.getIndexInActivation();
	}

	/**
	 * Returns the lifeline that this activation is on.
	 * @return
	 */
	public Lifeline getLifeline() {
		checkWidget();
		return target;
	}
	
	/**
	 * Returns the nearest lifeline in the lifeline hierarchy that is collapsed and
	 * visible.
	 * @return the nearest lifeline in the lifeline hierarchy that is collapsed and
	 * visible.
	 */
	public Lifeline getVisibleLifeline() {
		Lifeline top = getLifeline();
		Lifeline current = top;
		while (current != null) {
			if (!current.isExpanded()) {
				top = current;
			}
			current = current.getParent();
		}
		return top;
	}
	
	/**
	 * Resets the lifeline for this activation to the given lifeline.
	 * @param l the lifeline.
	 */
	public void setLifeline(Lifeline l) {
		if (l == getLifeline()) {
			return;
		}
		Lifeline oldTarget = target;
		
		try {
			if (target != null && !target.isDisposed()) {
				target.removeActivation(this);
			}
		} catch (SWTError e) {
			//unrecoverable error ignore. Most likely, the old target is disposed.
		} catch (SWTException e) {
			//unrecoverable error ignore. Most likely, the old target is disposed.
		}
		if (l != null) {
			l.addActivation(this);
		}
		target = l;
		getChart().markDirty();
		firePropertyChange(IWidgetProperties.OWNER, oldTarget, target);
	}



	/**
	 * Adds the given message group to this activation. The messages that are contained within
	 * the given range of the group will be enclosed inside the group.
	 * @param messageGroup
	 */
	void addGroup(MessageGroup messageGroup) {
		checkWidget();
		if (messageGroup.getActivation() != this) {
			if (!messageGroupList.contains(messageGroup)) {
				messageGroupList.add(messageGroup);
				messageGroups = null;
			}
		}
	}



	/**
	 * Removes the given group from the list of groups in this activation.
	 * @param messageGroup
	 */
	void removeGroup(MessageGroup messageGroup) {
		checkWidget();
		if (messageGroupList.remove(messageGroup)) {
			messageGroups = null;
		}
	}



	/**
	 * @param message
	 */
	void removeTargetMessage(Message message) {
		checkWidget();
		if (message.getTarget() != this) return;
		targetMessages.remove(message);
	}



	/**
	 * @param message
	 */
	void addTargetMessage(Message message) {
		checkWidget();
		if (message.getTarget() != this) {
			targetMessages.add(message);
		}
	}
	
	/**
	 * Returns all messages that are attached to this activation (both as sources and targets)
	 * ordered by their execution.
	 */
	List<Message> getVisibleOrderedMessages() {
		List<Message> orderedList = new ArrayList<Message>();
		for (Message m : getMessages()) {
			if (m.isVisible()) {
				orderedList.add(m);
			}
		}
		for (Message m : targetMessages) {
			if (m.isVisible()) {
				orderedList.add(m);
			}
		}
		Collections.sort(orderedList, new Comparator<Message>(){
			public int compare(
					Message o1,
					Message o2) {
				Rectangle r1 = getSequenceChart().getItemBounds(o1);
				Rectangle r2 = getSequenceChart().getItemBounds(o2);
				if (r1 == null) {
					if (r2 == null) {
						return 0;
					}
					return -1;
				}
				if (r2 == null) {
					return 1;
				}
				return r1.y-r2.y;
			}
			
		});
		return orderedList;
	}
}
