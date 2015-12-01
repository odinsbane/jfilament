/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package snakeprogram.energies;

import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

/**
 * An interface so that snake can adapt to different energy functions.
 * The snake is always being pushed to the lowest energy.
 *
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public interface ImageEnergy{
    public final static int INTENSITY = 0;
    public final static int GRADIENT = 1;
    public final static int BALLOON = 2;
    /**
        *
        *   This will use the squaresize defined in TwoDDeformation, and the original image to find
        *   the maximum pixel value in the square.  This is used for determining the head
        *   force on the filament.
        *   
        **/
    public double getMaxPixel(double x, double y);
    
    /**
      * Given the coordinate points this will calculate the image energy and return
      * the value as a double[] Ex Ey
      **/
    public double[] getImageEnergy(double x, double y);
    
    /**
     * returns the original imageprocessor, that has been converted to a float
     * */
    public ImageProcessor getProcessor();
    
}


