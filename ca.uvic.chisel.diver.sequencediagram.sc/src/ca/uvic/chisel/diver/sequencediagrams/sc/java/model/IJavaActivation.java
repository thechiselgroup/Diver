/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: the CHISEL group - initial API and implementation
 *******************************************************************************/
package ca.uvic.chisel.diver.sequencediagrams.sc.java.model;

import java.util.List;

/**
 * @author Del
 *
 */
public interface IJavaActivation extends IJavaCallModel {

	public abstract List<JavaMessage> getMessages();
	
	
	
	public JavaMessage getCallingMessage();

	
	public JavaObject getLifeLine();
}