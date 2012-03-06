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
package ca.uvic.chisel.javasketch.ui.internal;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * An overlay icon that contains one image overlay.
 * @author Del Myers
 */
public class OverlayIcon extends CompositeImageDescriptor {
	public static final int TOP = 0;
	public static final int BOTTOM = 1;
	public static final int LEFT = 0;
	public static final int RIGHT = 1;

	private ImageDescriptor base;

    private ImageDescriptor overlay;
	private int horizontal;
	private int vertical;

    public OverlayIcon(ImageDescriptor base, ImageDescriptor overlay, int horizontal,
    		int vertical) {
        this.base = base;
        this.overlay = overlay;
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

 

    /**
     * @see CompositeImageDescriptor#drawCompositeImage(int, int)
     */
    protected void drawCompositeImage(int width, int height) {
        ImageData bg;
        if (base == null || (bg = base.getImageData()) == null) {
			bg = DEFAULT_IMAGE_DATA;
		}
        drawImage(bg, 0, 0);
        if (overlay.getImageData().width < width) {
        	width = overlay.getImageData().width;
        }
        if (overlay.getImageData().height < height) {
        	height = overlay.getImageData().height;
        }
        int x = 0;
        int y = 0;
        if (horizontal == RIGHT) {
        	//move the x value over
        	x = base.getImageData().width-width;
        	if (x < 0) {
        		x = 0;
        		width = base.getImageData().width;
        	}
        }
        if (vertical == BOTTOM) {
        	//move the y value over
        	y = base.getImageData().height-height;
        	if (y < 0) {
        		y = 0;
        		width = base.getImageData().height;
        	}
        }
        drawImage(overlay.getImageData(), x, y);
    }

     /**
     * @see CompositeImageDescriptor#getSize()
     */
    protected Point getSize() {
        return new Point(base.getImageData().width, base.getImageData().height);
    }
}

