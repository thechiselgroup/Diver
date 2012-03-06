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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.draw2d.Animation;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.zest.custom.sequence.internal.AbstractSimpleProgressRunnable;
import org.eclipse.zest.custom.sequence.internal.SimpleProgressMonitor;
import org.eclipse.zest.custom.sequence.widgets.Activation;
import org.eclipse.zest.custom.sequence.widgets.Call;
import org.eclipse.zest.custom.sequence.widgets.Lifeline;
import org.eclipse.zest.custom.sequence.widgets.Message;
import org.eclipse.zest.custom.sequence.widgets.MessageGroup;
import org.eclipse.zest.custom.sequence.widgets.UMLItem;
import org.eclipse.zest.custom.sequence.widgets.UMLSequenceChart;
import org.eclipse.zest.custom.sequence.widgets.internal.IWidgetProperties;

/**
 * A runnable for laying out sequence diagrams made to be forkable so that the
 * main bulk of the layout work doesn't have to happen inside the UI thread.
 * That way, a progress indicator can be used and the users can continue to
 * work.
 * 
 * @author Del Myers
 * 
 */
class SequenceLayoutRunnable extends AbstractSimpleProgressRunnable {
	/**
	 * Message groups have their own "containment" heirarchy containing messages
	 * and other message groups. This makes it difficult to keep track of the
	 * size and location of the group until after the rest of the chart has been
	 * layed-out. Instead, we keep a tree of message groups which can be
	 * traversed at the end, and applied later.
	 * 
	 * @author Del Myers
	 * 
	 */
	private class MessageGroupLayoutNode {
		List<MessageGroupLayoutNode> childGroups;
		List<Message> containedMessages;
		Rectangle layout;
		private MessageGroup group;
		private MessageGroupLayoutNode parent;

		MessageGroupLayoutNode(MessageGroupLayoutNode parent, MessageGroup group) {
			childGroups = new LinkedList<MessageGroupLayoutNode>();
			containedMessages = new LinkedList<Message>();
			layout = new Rectangle();
			this.group = group;
			this.parent = parent;
			if (parent != null) {
				parent.childGroups.add(this);
			}
		}
	}
	/**
	 * Defines the family for a layout runnable running on a given chart.
	 * 
	 * @author Del Myers
	 * 
	 */
	private static final class RunnableFamily {
		private UMLSequenceChart chart;

		private RunnableFamily(UMLSequenceChart chart) {
			this.chart = chart;
		}

		public boolean equals(Object obj) {
			if (obj == null || !obj.getClass().equals(getClass())) {
				return false;
			}
			return ((RunnableFamily) obj).chart == this.chart;
		}
	}
	private class TempLayoutObject {
		/**
		 * Temporary variable that is used to track the number of activations
		 * being executed on a lifeline during they layout.
		 */
		int executions;
		/**
		 * An identifier for the widget. Used for consistency checks.
		 */
		String id;

		/**
		 * The actual layout object. For messages, the x and y represent the
		 * source and destination coordinates respectively. For all others, the
		 * rectangle represents the bounding box.
		 * 
		 * Activations will have their layout relative to the bounding box of
		 * the lifeline in which they appear. The same will be true for the
		 * endpoints of the messages (relative to the lifeline in which the
		 * endpoint is contained). Lifelines are relative to the screen.
		 */
		Rectangle layout;

		/**
		 * Field for when applying the layout for messages. Indicates that the
		 * opposite end of the message has already been layed-out.
		 */
		//private boolean oppositeVisited;

		TempLayoutObject(String id) {
			this.id = id;
			this.layout = new Rectangle();
			this.executions = 0;
			//this.oppositeVisited = false;
		}

	}
	private static final int ACTIVATION_WIDTH = 7;
	private static final int ACTIVATION_HALF_WIDTH = ACTIVATION_WIDTH / 2;
	
	public static final int TIME_UNIT_HEIGHT = 18;

	private UMLSequenceChart chart;
	private GC gc;

	/**
	 * Temporary data stored on the widgets to organize their layout.
	 */
	private final String LAYOUT_DECORATION = "lytdec";

	/**
	 * The lifelines as they appear in the layout.
	 */
	private ArrayList<Lifeline> lifelines;

	/**
	 * Used to count the amount of work that must be done.
	 */
	private int messageCount;
	
	/**
	 * Used for building the layout for the message groups.
	 */
	private MessageGroupLayoutNode messageGroupLayout;
	private MessageGroupLayoutNode messageGroupLayoutPointer;

	public SequenceLayoutRunnable(UMLSequenceChart chart) {
		this.chart = chart;
	}

	@Override
	public Object getFamily() {
		return getFamily(chart);
	}

	public static Object getFamily(UMLSequenceChart chart) {
		return new RunnableFamily(chart);
	}

	@Override
	protected void doRunInUIThread(SimpleProgressMonitor monitor)
			throws InvocationTargetException {
		readAndDispatch();
		if (chart.isDisposed())
			return;
		lifelines = new ArrayList<Lifeline>();
		this.gc = new GC(chart);
		//needed to make sure that there is enough room when calculating
		//sizes.
		gc.setAntialias(SWT.ON);
		messageGroupLayout = new MessageGroupLayoutNode(null, null);
		messageGroupLayoutPointer = messageGroupLayout;
		Animation.markBegin();
		monitor.beginTask("Laying out diagram", 4000);
		try {
			if (!monitor.isCancelled()) {
				initializeLayout(monitor.createSubMonitor(
						"Initializing layout", 1000));
			}
			readAndDispatch();
			if (!monitor.isCancelled()) {
				calculateLayout(monitor.createSubMonitor("Calculating Layout",
						1000));
			}
			readAndDispatch();
			if (!monitor.isCancelled()) {
				// can't read and dispatch after this because that would mess up
				// the animation.
				applyLayout();
				monitor.worked(1000);
			}
		} finally {
			// clear the memory used.
			lifelines = null;
			gc.dispose();
			cleanWidgets();
			monitor.done();
		}
		Animation.run(250);
	
		// readAndDispatch();
	}

	/**
	 * @param createSubMonitor
	 * @return
	 */
	private void initializeLayout(SimpleProgressMonitor monitor) {
		Activation root = chart.getRootActivation();
		if (root == null || root.isDisposed() || root.isHidden()) {
			monitor.cancel();
			return;
		}
		Lifeline line = root.getVisibleLifeline();
		// store the index of the array with the lifeline layout, so that they
		// can be easily found in the list.
		TempLayoutObject lifelineLayout = new TempLayoutObject(lifelines.size()
				+ "");
		line.setData(LAYOUT_DECORATION, lifelineLayout);
		lifelines.add(line);
		Message[] messages = root.getMessages();
		TempLayoutObject rootLayout = new TempLayoutObject("1");
		monitor.beginTask("Initializing layout", messages.length);
		root.setData(LAYOUT_DECORATION, rootLayout);
		int sequence = 1;
		for (Message m : messages) {
			if (!m.isDisposed() && m.isVisible()) {
				initializeMessage(m, root, sequence);
				sequence++;
			}
			monitor.worked(1);
			if (monitor.isCancelled()) {
				monitor.done();
			}
		}
	
		monitor.done();
	}

	/**
	 * Initializes all the life line layouts in the order that they appear in
	 * the lifelines list.
	 * 
	 * @param lifelineLayout
	 */
	private void initializeLifeLineLayout(Lifeline line, int index) {
		TempLayoutObject lifelineLayout = (TempLayoutObject) line
				.getData(LAYOUT_DECORATION);
		if (lifelineLayout == null) {
			lifelineLayout = new TempLayoutObject(index + "");
			line.setData(LAYOUT_DECORATION, lifelineLayout);
		}
		int width = 25;
		if (line.getText() != null) {
			width = getExtents(line)+10;
		}
		int width2 = 0;
		if (line.getStereoType() != null) {
			width2 = getExtents(line.getStereoType())+10;
		}
		if (width2 > width) {
			width = width2;
		}
		width = width + 20; // a little padding.
		int x = 0;
		int y = 0;
		if (index > 0) {
			TempLayoutObject neighbor = (TempLayoutObject) lifelines.get(
					index - 1).getData(LAYOUT_DECORATION);
			x = neighbor.layout.x + neighbor.layout.width + neighbor.layout.y
					+ 10;
		}
		lifelineLayout.layout.setBounds(new Rectangle(x, y, width, 0));
	}

	

	/**
	 * @param m
	 */
	private void initializeMessage(Message m, Activation source, int sequence) {
		Activation target = m.getTarget();
		if (!m.isVisible()) {
			return;
		}
		// don't process in-coming messages.
		if (target == null || target == source) {
			return;
		}
		TempLayoutObject sourceLayout = (TempLayoutObject) source
				.getData(LAYOUT_DECORATION);
		if (sourceLayout == null) {
			throw new IllegalStateException("Message " + m.toString()
					+ " cannot be initialized without a source.");
		}
		TempLayoutObject messageLayout = (TempLayoutObject) m
				.getData(LAYOUT_DECORATION);
		if (messageLayout != null) {
			throw new IllegalStateException("Message " + m.toString()
					+ " cannot be called more than once.");
		}
		messageLayout = new TempLayoutObject(sourceLayout.id + "." + sequence);
		m.setData(LAYOUT_DECORATION, messageLayout);
		// update the message count so that we know how much work to do.
		messageCount++;
	
		Lifeline lifeline = target.getVisibleLifeline();
		if (!lifeline.isVisible()) {
			return;
		}
		TempLayoutObject lifelineLayout = (TempLayoutObject) lifeline
				.getData(LAYOUT_DECORATION);
		if (lifelineLayout == null) {
			lifelineLayout = new TempLayoutObject("" + lifelines.size());
			lifeline.setData(LAYOUT_DECORATION, lifelineLayout);
			lifelines.add(target.getVisibleLifeline());
		}
		TempLayoutObject targetLayout = (TempLayoutObject) target
				.getData(LAYOUT_DECORATION);
		if (targetLayout == null) {
			if (source.isExpanded()) {
				// only work with targets that are expanded when the layout
				// is null.
				// other ones will be returns, and have to be processed
				// anyway.
				// make the creation path through this message. This will be
				// used to ensure
				// that messages don't return to a path that they didn't
				// originate from.
				targetLayout = new TempLayoutObject(messageLayout.id);
				target.setData(LAYOUT_DECORATION, targetLayout);
				int csequence = 1;
				for (Message cm : target.getMessages()) {
					if (!cm.isDisposed() && m.isVisible()) {
						initializeMessage(cm, m.getTarget(), csequence);
						csequence++;
					}
				}
			}
		} else {
			// make sure that this message was called from the same sequence
			// that created it
			// otherwise, we are sending a message to either a dead
			// activation, or one that
			// can't be reached yet.
			if (messageLayout.id.startsWith(sourceLayout.id)) {
				
			} else if (!messageLayout.id.startsWith(targetLayout.id)) {
				throw new IllegalStateException("Message " + m.toString()
						+ " cannot call activation " + target.toString()
						+ ": they are not on the same call path.");
			}
		}
		readAndDispatch();
	}

	/**
	 * @param createSubMonitor
	 */
	private void calculateLayout(SimpleProgressMonitor monitor) {
		Activation root = chart.getRootActivation();
		monitor.beginTask("Calculating Layout", messageCount);
		// initialize the lifeline layouts.
		int index = 0;
		for (Lifeline l : lifelines) {
			initializeLifeLineLayout(l, index);
			index++;
		}
		recursiveCalculateLayout(root,
				0, monitor);
		monitor.done();
	}

	/**
	 * Lays out the given activation, and all of its out-going calls and
	 * lifelines.
	 * 
	 * @param activation
	 *            the activation to layout
	 * @param top
	 *            the top at which the activation should start
	 * @param monitor
	 *            the monitor to give updates on progress. Should increment for
	 *            every processed message
	 * @return the bottom coordinate of the layed-out activation.
	 */
	private int recursiveCalculateLayout(Activation a, int top,
			SimpleProgressMonitor monitor) {
		TempLayoutObject layout = (TempLayoutObject) a
				.getData(LAYOUT_DECORATION);
		TempLayoutObject lifelineLayout = (TempLayoutObject) a.getVisibleLifeline()
				.getData(LAYOUT_DECORATION);
		int centerx = lifelineLayout.layout.width / 2
				+ (ACTIVATION_HALF_WIDTH * lifelineLayout.executions);
		layout.layout.setBounds(new Rectangle(centerx - ACTIVATION_HALF_WIDTH,
				top, ACTIVATION_WIDTH, TIME_UNIT_HEIGHT));
		lifelineLayout.executions++;
		layout.layout.y = top;
		// if no messages are processed, then we have to pad the bottom.
		boolean addToBottom = true;
		Message[] messages = a.getMessages();
		MessageGroup[] groups = a.getMessageGroups();
		int groupIndex = 0;
		
		for (int i = 0; i < messages.length; i++) {
			//move up the group tree, getting rid of groups that 
			//are no longer in range.
			while (messageGroupLayoutPointer.group != null && messageGroupLayoutPointer.group.getActivation() == a && 
					(messageGroupLayoutPointer.group.getOffset() + messageGroupLayoutPointer.group.getLength() <= i)) {
				messageGroupLayoutPointer = messageGroupLayoutPointer.parent;
				//top += TIME_UNIT_HEIGHT;
				top += 3;
			}
			if (messageGroupLayoutPointer == null) {
				messageGroupLayoutPointer = messageGroupLayout;
			}
			Message m = messages[i];
			while (groupIndex < groups.length
					&& groups[groupIndex].getOffset() <= i) {
				MessageGroup group = groups[groupIndex];
				if (group.isVisible()) {
					MessageGroupLayoutNode node = new MessageGroupLayoutNode(messageGroupLayoutPointer, group);
					if (group.getLength() > 0) {
						messageGroupLayoutPointer = node;
					}
					//initialize the layout. The X position will be at the same position
					//as the activation.
					top += TIME_UNIT_HEIGHT;
					node.layout.y = top;
					node.layout.width = (group.isExpanded() ? 0 : 40);
					node.layout.height = TIME_UNIT_HEIGHT;
					node.layout.x = layout.layout.x;
					// add a little bit of time
					top += TIME_UNIT_HEIGHT;
				}
				groupIndex++;
			}
			
			
			if (!m.isVisible())
				continue;
			if (messageGroupLayoutPointer.group != null) {
				messageGroupLayoutPointer.containedMessages.add(m);
			}
			// calculate for out-going messages.
			boolean messagePointsRight = messagePointsRight(m);
			TempLayoutObject messageLayout = (TempLayoutObject) m
					.getData(LAYOUT_DECORATION);
			if (m.getSource() == a) {
				top += TIME_UNIT_HEIGHT;
				nudgeLifelines(m);
				// check the target to see if it is a return.
				Activation target = m.getTarget();
				
				// set-up the message layout
				messageLayout.layout.y = top;
				boolean isSelfCall = isSelfCall(m);
				if (isSelfCall(m) && m instanceof Call) {
					top += (TIME_UNIT_HEIGHT/3);
				}
				messageLayout.layout.height = top;
				if (m instanceof Call) {
					top = recursiveCalculateLayout(target, top, monitor);
					if (isSelfCall(m)) {
						top +=TIME_UNIT_HEIGHT/3;
					}
				} else {
					addToBottom = false;
					if (isSelfCall) {
						messageLayout.layout.height += (TIME_UNIT_HEIGHT/3);
					}
				}
				
				TempLayoutObject targetLayout = (TempLayoutObject) target
				.getData(LAYOUT_DECORATION);
				if (!target.isVisible() || targetLayout == null) {
					//account for targets that no longer exist.
					messageLayout.layout.x = centerx - ACTIVATION_HALF_WIDTH;
					messageLayout.layout.width = messageLayout.layout.x;
				} else {
					if (messagePointsRight) {
						messageLayout.layout.x = centerx + ACTIVATION_HALF_WIDTH;
						messageLayout.layout.width = targetLayout.layout.x + ((isSelfCall) ? targetLayout.layout.width : 0);
					} else {
						messageLayout.layout.x = centerx - ACTIVATION_HALF_WIDTH;
						messageLayout.layout.width = targetLayout.layout.x + ACTIVATION_WIDTH;
					}
				}
				monitor.worked(1);
				readAndDispatch();
			} 
		}
		
		if (addToBottom)
			top += TIME_UNIT_HEIGHT;
		layout.layout.height = top - layout.layout.y;
		//close all the groups.
		int groupPadding = 0;
		while (messageGroupLayoutPointer.group != null && messageGroupLayoutPointer.group.getActivation()==a) {
			messageGroupLayoutPointer = messageGroupLayoutPointer.parent;
			groupPadding += 80;
		}
		if (messageGroupLayoutPointer == null) {
			messageGroupLayoutPointer = messageGroupLayout;
		}
		if (layout.layout.y + layout.layout.height > lifelineLayout.layout.y + lifelineLayout.layout.height) {
			int bottom = layout.layout.y + layout.layout.height + 10;
			lifelineLayout.layout.height = bottom - lifelineLayout.layout.y;
		}
		lifelineLayout.executions--;
		return layout.layout.y + layout.layout.height;
	
	}

	/**
	 * Adjusts the distance between lifelines to make room for messages.
	 * 
	 * @param lifeline
	 * @param targetLifeline
	 * @param messageWidth
	 */
	private void nudgeLifelines(Message m) {
		Lifeline sourceLifeline = m.getSource().getVisibleLifeline();
		Lifeline targetLifeline = m.getTarget().getVisibleLifeline();
		if (!sourceLifeline.isVisible()) {
			return;
		} else if (!targetLifeline.isVisible()) {
			return;
		}
		int messageWidth = 0;
		if (m.getText() != null) {
			messageWidth = getExtents(m) + 10;
		}
		TempLayoutObject sourceLayout = (TempLayoutObject) sourceLifeline
				.getData(LAYOUT_DECORATION);
		TempLayoutObject targetLayout = (TempLayoutObject) targetLifeline
				.getData(LAYOUT_DECORATION);
		int sourceIndex = Integer.parseInt(sourceLayout.id);
		int targetIndex = -1;
		if (targetLayout != null) {
			// shouldn't happen right now, but may happen in the future that we
			// display returns to
			// parents that can't be seen (eg. in the case of a thrown
			// exception).
			targetIndex = Integer.parseInt(targetLayout.id);
		}
		boolean pointsRight = targetIndex >= sourceIndex;
	
		// check the distance between the source and its nearest neighbor, not
		// the source and target.
		TempLayoutObject first = sourceLayout;
		TempLayoutObject next = null;
		int firstIndex = sourceIndex;
		if (pointsRight) {
			if (sourceIndex < lifelines.size() - 1) {
				next = (TempLayoutObject) lifelines.get(sourceIndex + 1)
						.getData(LAYOUT_DECORATION);
			} else {
				// no need to do anything, just return.
				return;
			}
		} else {
			next = first;
			if (sourceIndex > 0) {
				first = (TempLayoutObject) lifelines.get(sourceIndex - 1)
						.getData(LAYOUT_DECORATION);
				firstIndex--;
			} else {
				firstIndex = 0;
				first = null;
			}
		}
		// make the right end-point to the center of the lifeline + the distance
		// needed for
		// embedded activations.
		int right = next.layout.getCenter().x
				+ (next.executions - 1)
				* ACTIVATION_WIDTH
				+ ((pointsRight) ? ACTIVATION_HALF_WIDTH
						: -ACTIVATION_HALF_WIDTH);
		int left = 0;
		if (first != null) {
			left = first.layout.getCenter().x + first.executions
					* ACTIVATION_WIDTH + ACTIVATION_HALF_WIDTH;
		}
		int distance = right - left;
		if (distance < messageWidth) {
			// adjust all of the lifelines to give room for the message.
			int nudge = messageWidth - distance;
			for (int i = firstIndex + 1; i < lifelines.size(); i++) {
				Lifeline line = lifelines.get(i);
				TempLayoutObject llayout = (TempLayoutObject) line
						.getData(LAYOUT_DECORATION);
				llayout.layout.x += nudge;
			}
	
		}
	}

	/**
	 * Applies the layout that has been temporarily stored in the widgets.
	 */
	private void applyLayout() {
		// initialize the lifeline layouts.
		for (Lifeline l : lifelines) {
			TempLayoutObject lifelineLayout = (TempLayoutObject) l
					.getData(LAYOUT_DECORATION);
			l.setData(IWidgetProperties.LAYOUT, lifelineLayout.layout);
		}
		Activation root = chart.getRootActivation();
		applyLayout(root);
		layoutMessageGroups(messageGroupLayout);
	}
	/**
	 * Recursively visits the activations, applying their layouts and the
	 * layouts of their messages. Assumes that all lifelines
	 * have been previously layed out.
	 * @param root
	 */
	private void applyLayout(Activation a) {
		//apply the layout for all the called activations first.
		for (Message m : a.getMessages()) {
			if (m.isVisible() && m instanceof Call) {
				applyLayout(m.getTarget());
			}
		}
		TempLayoutObject activationLayout = (TempLayoutObject) a
		.getData(LAYOUT_DECORATION);
		Rectangle lifelineLayout = (Rectangle) a.getVisibleLifeline().getData(IWidgetProperties.LAYOUT);
		Rectangle activationBounds = new Rectangle(
				activationLayout.layout.x + lifelineLayout.x,
				activationLayout.layout.y + lifelineLayout.y,
				activationLayout.layout.width,
				activationLayout.layout.height
		);
		for (Message m : a.getMessages()) {
			if (m.isVisible()) {
				Rectangle targetLifelineBounds = (Rectangle)m.getTarget().getVisibleLifeline().getData(IWidgetProperties.LAYOUT);
				TempLayoutObject messageLayout = (TempLayoutObject) m.getData(LAYOUT_DECORATION);
				PointList points = new PointList();
				//add the start point.
				points.addPoint(messageLayout.layout.x + lifelineLayout.x, messageLayout.layout.y + lifelineLayout.y);
				//add end point
				Point end = null;
				if (targetLifelineBounds == null) {
					end = new Point(0, messageLayout.layout.height + lifelineLayout.y);
				} else {
					end = new Point(messageLayout.layout.width + targetLifelineBounds.x, messageLayout.layout.height + targetLifelineBounds.y);
				}
				if (m.getTarget().getVisibleLifeline() == m.getSource().getVisibleLifeline()) {
					//self-call, add two extra points.
					gc.setFont(m.getFont());
					int extents = getExtents(m) + 10;
					Point start = points.getFirstPoint();
					points.addPoint(start.x + extents, start.y);
					points.addPoint(start.x + extents, end.y);
				}
				points.addPoint(end);
				m.setData(IWidgetProperties.LAYOUT, points);
			}
		}
		a.setData(IWidgetProperties.LAYOUT, activationBounds);
	}


	/**
	 * 
	 */
	private void layoutMessageGroups(MessageGroupLayoutNode node) {
		//run a depth-first search on the message group layout tree, making each group
		//surround the messages that are contained within it.
		//the root has no group, so we must check this
		int originX = 0;
		if (node.group != null) {
			//add all of the visible messages within the group.
			Rectangle activationLayout = (Rectangle) node.group.getActivation().getData(IWidgetProperties.LAYOUT);
			node.layout.x = originX = activationLayout.x;
		}
		for (MessageGroupLayoutNode child : node.childGroups) {
			layoutMessageGroups(child);
			//reset the x position to the final x position of the activation
			if (node.group != null) {
				//get the child's layout and expand it by a little, just to add some padding.
				Rectangle childLayout = child.layout.getCopy().expand(2, 2);
				node.layout.union(childLayout);
			}
		}
		//the root has no group, so we must check this
		if (node.group != null) {
			for (Message m : node.containedMessages) {
				PointList points = (PointList) m.getData(IWidgetProperties.LAYOUT);
				node.layout.union(points.getBounds().getCopy().expand(0, 5));
				if (m instanceof Call) {
					Activation target = m.getTarget();
					Rectangle tLayout = (Rectangle) target.getData(IWidgetProperties.LAYOUT);
					if (tLayout != null) {
						node.layout.union(tLayout.getCopy().expand(2, 2));
					}
				}
			}
			if (node.group.isExpanded()) {
				int textWidth = getExtents(node.group);
				int d = node.layout.width - (originX - node.layout.x);
				if (d < textWidth) {
					node.layout.width += (textWidth-d) + 14;
				}
			}
			node.group.setData(IWidgetProperties.LAYOUT, node.layout);
		}
	}

	/**
	 * 
	 */
	private void cleanWidgets() {
		LinkedList<UMLItem> items = new LinkedList<UMLItem>();
		items.add(chart.getRootActivation());
		while (items.size() != 0) {
			UMLItem item = items.removeFirst();
			// just a convenience check for when the root doesn't exist.
			if (item == null || item.isDisposed()) {
				continue;
			}
			item.setData(LAYOUT_DECORATION, null);
			if (item instanceof Message) {
				Message m = (Message) item;
				Activation target = m.getTarget();
				if (target != null && !target.isDisposed()
						&& target.getData(LAYOUT_DECORATION) != null) {
					items.add(target);
				}
			} else if (item instanceof Activation) {
				Activation a = (Activation) item;
				Lifeline l = a.getVisibleLifeline();
				if (l.getData(LAYOUT_DECORATION) != null) {
					l.setData(LAYOUT_DECORATION, null);
				}
				for (Message m : a.getMessages()) {
					if (m.getSource() == a) {
						if (!m.isDisposed()
								&& m.getData(LAYOUT_DECORATION) != null) {
							items.add(m);
						}
					}
				}
			}
		}
	}

	/**
	 * Calculates the bounding box of the given text for the diagram. No tab
	 * expansions or carriage returns are calculated.
	 * 
	 * @param text
	 *            the text to calculate.
	 * @return the width of the text.
	 */
	private int getExtents(UMLItem widget) {
		Label tempLabel = new Label();
		tempLabel.setText(widget.getText());
		tempLabel.setFont(widget.getFont());
		tempLabel.setIcon(widget.getImage());
		return tempLabel.getPreferredSize().width;
	}
	
	/**
	 * @param stereoType
	 * @return
	 */
	private int getExtents(String text) {
		Label tempLabel = new Label();
		tempLabel.setText(text);
		tempLabel.setFont(chart.getFont());
		return tempLabel.getPreferredSize().width;
	}

	/**
	 * Returns true if the given message returns has the same lifeline on its
	 * source and target.
	 * 
	 * @param m
	 *            the message to check.
	 * @return
	 */
	private boolean isSelfCall(Message m) {
		return m.getSource().getVisibleLifeline() == m.getTarget().getVisibleLifeline();
	}

	/**
	 * @return true if the given message points right in this layout.
	 */
	private boolean messagePointsRight(Message m) {
		if (!m.isVisible()) {
			return false;
		}
		Lifeline sourceLifeline = m.getSource().getVisibleLifeline();
		Lifeline targetLifeline = m.getTarget().getVisibleLifeline();
		TempLayoutObject sourceLayout = (TempLayoutObject) sourceLifeline
				.getData(LAYOUT_DECORATION);
		TempLayoutObject targetLayout = (TempLayoutObject) targetLifeline
				.getData(LAYOUT_DECORATION);
		if (sourceLayout == null || targetLayout == null) {
			return false;
		}
		int sourceIndex = Integer.parseInt(sourceLayout.id);
		int targetIndex = Integer.parseInt(targetLayout.id);
		return sourceIndex <= targetIndex;
	}
}
