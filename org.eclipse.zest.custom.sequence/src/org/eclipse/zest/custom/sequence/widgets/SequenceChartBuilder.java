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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;

/**
 * A helper class for quickly creating sequence charts without the need for a viewer
 * or label providers. Keeps an internal state of where in the chart the next message
 * will be placed. The user can question the builder for that state, and update it
 * as he or she pleases. The class will do its best to keep the chart consistent, but
 * some care must be taken when changing the state of the builder.
 * 
 * @author Del Myers
 *
 */
public class SequenceChartBuilder {
	/**
	 * The chart that we will build on.
	 */
	private UMLSequenceChart chart;
	
	/**
	 * The activation that we are running on.
	 */
	private Activation currentActivation;
	
	/**
	 * The index at which the next call/return will be made on the activation.
	 */
	private int index;
	
	/**
	 * A mapping of message groups that have not yet gone out of scope. The will need to
	 * be updated as the chart evolves.
	 */
	private Map<Activation, List<MessageGroup>> unscopedGroups;
	
	/**
	 * The names of the lifelines that are used.
	 */
	private Map<String, Lifeline> lifelineNames;

	private boolean isRedrawing;
	
	/**
	 * Creates a new sequence diagram on the given chart. The chart is cleared in order to 
	 * reset its state.
	 * @param chart the chart to use.
	 * @param start the name of the starting lifeline.
	 */
	public SequenceChartBuilder(UMLSequenceChart chart, String start) {
		lifelineNames = new HashMap<String, Lifeline>();
		unscopedGroups = new HashMap<Activation, List<MessageGroup>>();
		chart.clearChart();
		Lifeline startLine = new Lifeline(chart);
		startLine.setData(start);
		startLine.setText(start);
		Activation root = new Activation(chart);
		root.setData("Start");
		root.setText("Start");
		root.setLifeline(startLine);
		lifelineNames.put(start, startLine);
		currentActivation = root;
		index = 0;
		chart.setRootActivation(root);
		this.chart = chart;
		isRedrawing = true;
	}
	
	/**
	 * Turns off redrawing on the chart in order to avoid updates.
	 */
	public void turnOffRedraw() {
		if (isRedrawing) {
			chart.setRedraw(false);
			isRedrawing = false;
		}
	}
	
	/**
	 * Turns on redrawing on the chart so that the chart will be updated.
	 */
	public void turnOnRedraw() {
		if (!isRedrawing) {
			chart.setRedraw(true);
			isRedrawing = true;
		}
	}
	
	/**
	 * Returns the index at which the next call/return will be inserted.
	 * @return the index at which the next call/return will be inserted.
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * Attempts to set the index of insertion to the given index. If the index is out-of-range,
	 * it will be set to the nearest point that the chart will accept insertion. For convenience,
	 * this number will be returned.
	 * @param index the index to attempt to set the "cursor" to.
	 * @return the index that was actually set.
	 */
	public int setIndex(int index) {
		Message[] messages = currentActivation.getMessages();
		if (index > messages.length) {
			index = messages.length;
		} else if (index < 0) {
			index = 0;
		}
		this.index = index;
		return index;
	}
	
	/**
	 * Creates a call with the given name at the current insertion index, to the
	 * given life line. Does not follow the call to that lifeline, but rather increments
	 * the index to insert another call. Message groups that have been created and
	 * enclose the call have their sizes incremented. If there is no lifeline for the
	 * given name, one is created.
	 * @param callName the call to insert.
	 * @param lifeline the lifeline to call.
	 * @return the created call for convenience.
	 */
	public Call makeCall(String callName, String lifeline) {
		Lifeline line = lifelineNames.get(lifeline);
		if (line == null) {
			line = new Lifeline(chart);
			line.setText(lifeline);
			line.setData(lifeline);
			lifelineNames.put(lifeline, line);
		}
		Activation a = new Activation(chart);
		a.setText(callName);
		a.setData(callName);
		a.setLifeline(line);
		Call call = new Call(chart);
		call.setData(callName);
		call.setText(callName);
		currentActivation.addMessage(index, call, a);
		incrementGroups();
		index++;
		return call;
	}
	
	
	/**
	 * Sets the parent lifeline of the given lifeline to the new parent. 
	 * If the parent doesn't exist, then a new lifeline is created.
	 * @param lifeline the name of the lifeline for which a container will be made.
	 * The lifeline must already exist, or null will be returned.
	 * @param parent the name of the new parent. 
	 * @return the parent lifeline for convenience, or null if the life line could
	 * not be set.
	 */
	public Lifeline setContainer(String lifeline, String parent) {
		Lifeline line = lifelineNames.get(lifeline);
		if (line == null) {
			return null;
		}
		Lifeline parentLine = lifelineNames.get(parent);
		if (parentLine == null) {
			parentLine = new Lifeline(chart);
			parentLine.setText(parent);
			parentLine.setData(parent);
			lifelineNames.put(parent, parentLine);
		}
		parentLine.addChild(line);
		return parentLine;
	}
	
	private void incrementGroups() {
		MessageGroup[] groups = currentActivation.getMessageGroups();
		for (MessageGroup g : groups) {
			if (g.getOffset() <= index && (g.getOffset() + g.getLength() >= index)) {
				if (index == g.getOffset() + g.getLength()) {
					//only increase the length if the group hasn't been closed.
					List<MessageGroup> contextGroups = unscopedGroups.get(currentActivation);
					if (contextGroups != null && contextGroups.contains(g)) {
						g.setRange(currentActivation, g.getOffset(), g.getLength()+1);
					}
				} else {
					g.setRange(currentActivation, g.getOffset(), g.getLength()+1);
				}
			}
		}
	}
	
	/**
	 * Creates the given call and follows it to its destination. The insertion index is
	 * reset to 0, and will begin on the target lifeline.
	 * @param callName the call to insert.
	 * @param lifeline the lifeline to call.
	 * @return the created call for convenience.
	 */
	public Call followCall(String callName, String lifeline) {
		return followCall(makeCall(callName, lifeline));
	}

	/**
	 * Follows the given call to its destination, and sets the insertion index to 0. Returns
	 * the call for convenience.
	 * @param call the call to follow.
	 * @return the call followed.
	 */
	public Call followCall(Call call) {
		currentActivation = call.getTarget();
		index = 0;
		return call;
	}
	
	/**
	 * Makes a return to the given activation if it is reachable from the current point.
	 * This is used for fine-tuned control, and most often isn't necessary. It may be useful
	 * when returning because of an exceptional case, however. Will fail and return null
	 * if the activation isn't reachable.
	 * @param to the activation to return to.
	 * @return the created return, or null if the activation isn't reachable.
	 */
	public Return makeReturn(String returnName, Activation to) {
		Activation pointer = currentActivation;
		while (pointer != null) {
			if (pointer.getSourceCall() == null) {
				return null;
			}
			pointer = pointer.getSourceCall().getSource();
			if (pointer == to) {
				Return r = new Return(chart);
				r.setLineStyle(SWT.LINE_DASH);
				r.setTargetStyle(Return.OPEN_ARROW);
				r.setText(returnName);
				r.setData(returnName);
				currentActivation.addMessage(index, r, pointer);
				incrementGroups();
				index++;
				return r;
			}
		}
		return null;
	}
	
	/**
	 * Makes a return from the current lifeline to the nearest point on the given lifeline.
	 * @param returnName the name of the 
	 * @param lifeline
	 * @return
	 */
	public Return makeReturn(String returnName, String lifeline) {
		Lifeline line = lifelineNames.get(lifeline);
		if (line == null) {
			return null;
		}
		Activation pointer = currentActivation;
		while (pointer != null) {
			if (pointer.getSourceCall() == null) {
				return null;
			}
			pointer = pointer.getSourceCall().getSource();
			if (pointer.getLifeline() == line) {
				Return r = new Return(chart);
				r.setLineStyle(SWT.LINE_DASH);
				r.setTargetStyle(Return.OPEN_ARROW);
				r.setText(returnName);
				r.setData(returnName);
				currentActivation.addMessage(index, r, pointer);
				incrementGroups();
				index++;
				return r;
			}
		}
		return null;
	}
	
	/**
	 * Makes a return from the current location to the previous. 
	 * @param returnName the name to be displayed with the return.
	 * @return the created return, if it could be created. null otherwise.
	 */
	public Return makeReturn(String returnName) {
		if (currentActivation.getSourceCall() == null) {
			return null;
		}
		if (currentActivation.getSourceCall().getSource() != null) {
			Activation pointer = currentActivation.getSourceCall().getSource();
			Return r = new Return(chart);
			r.setLineStyle(SWT.LINE_DASH);
			r.setTargetStyle(Return.OPEN_ARROW);
			r.setText(returnName);
			r.setData(returnName);
			currentActivation.addMessage(index, r, pointer);
			incrementGroups();
			index++;
			return r;
		}
		return null;
	}
	
	/**
	 * Follows the given return to its destination, and resets the insertion index to immediately
	 * after its return. The current position in the chart is irrelevent. The return will
	 * be followed anyway.
	 * @param r the return to follow.
	 * @return the return followed.
	 */
	public Return followReturn(Return r) {
		if (r == null) {
			return null;
		}
		Activation pointer = r.getSource();
		Activation target = r.getTarget();
		Call m = pointer.getSourceCall();
		while (m != null && m.getSource() != null && m.getSource() != target) {
			m = m.getSource().getSourceCall();
		}
		if (m != null) {
			int i = 0;
			Message[] messages = target.getMessages();
			while (i < messages.length) {
				if (messages[i]== m) {
					i++;
					break;
				}
				i++;
			}
			this.index = i;
			currentActivation = target;
			return r;
		}
		return null;
	}
	
	/**
	 * Opens up a new group in the context of the current state of this builder. All subsequent
	 * messages will be added inside this group until closeGroup() is called in the same context.
	 * @param groupName the name of the group to open.
	 * @return the group made.
	 */
	public MessageGroup openGroup(String groupName) {
		List<MessageGroup> groups = unscopedGroups.get(currentActivation);
		if (groups == null) {
			groups = new LinkedList<MessageGroup>();
			unscopedGroups.put(currentActivation, groups);
		}
		MessageGroup group = new MessageGroup(chart);
		group.setText(groupName);
		group.setData(groupName);
		group.setRange(currentActivation, index, 0);
		groups.add(group);
		return group;
	}
	
	/**
	 * Closes the last group opened in the context of the current location in the builder.
	 * Subsequent messages will not be placed inside this group. 
	 * @return the group that was closed, or null if none.
	 */
	public MessageGroup closeGroup() {
		List<MessageGroup> groups = unscopedGroups.get(currentActivation);
		if (groups != null && groups.size() > 0) {
			return ((LinkedList<MessageGroup>)groups).removeLast();
		}
		return null;
	}
	
	/**
	 * Returns the activations that messages will be inserted on.
	 * @return the the current activation that methods will be added to.
	 */
	public Activation getCurrentActivation() {
		return currentActivation;
	}
	
	/**
	 * Resets the context to be on the given activation. The insertion index will be reset to the
	 * end of that activation.
	 * @param activation the activation to begin on.
	 */
	public void setContext(Activation activation) {
		this.currentActivation = activation;
		this.index = activation.getMessages().length;
	}
 
}
