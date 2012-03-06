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
package ca.gc.drdc.oasis.tracing.cjvmtracer.internal;


/* Java class to hold static methods which will be called in byte code
 *    injections of all class files.
 */

public class JVMTrace {
 
    /* Master switch that activates methods. */
    
    private static int engaged = 0; 
  
    /* At the very beginning of every method, a call to method_entry() 
     *     is injected.
     */
    
    private static native void _method_entry(Object thr, int cnum, int mnum);
    public static void method_entry(int cnum, int mnum)
    {
	if ( engaged != 0 ) {
	    _method_entry(Thread.currentThread(), cnum, mnum);
	}
    }
    
    /* Before any of the return bytecodes, a call to method_exit() 
     *     is injected.
     */
    
    private static native void _method_exit(Object thr, int cnum, int mnum);
    public static void method_exit(int cnum, int mnum)
    {
	if ( engaged != 0 ) {
	    _method_exit(Thread.currentThread(), cnum, mnum);
	}
    }
    
}

