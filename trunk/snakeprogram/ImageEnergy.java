/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package snakeprogram;

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


class GradientEnergy implements ImageEnergy{
    final ImageProcessor blurred_image;
    final ImageProcessor image;
    public GradientEnergy(ImageProcessor img, double blur_sigma){
        image = img.convertToFloat();
        blurred_image = image.duplicate();
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(blurred_image, blur_sigma, blur_sigma, .01);
        blurred_image.filter(1);
        
    }
    
   public double[] getImageEnergy(double x, double y){
        
        //offset to place center point at center of pixel
        x = x - 0.5;
        y = y - 0.5;
        
        double[] ret_value = new double[2];

        //gradient in x
        ret_value[0] = blurred_image.getInterpolatedPixel(x+0.5, y) - blurred_image.getInterpolatedPixel(x-0.5,y);
        
        //gradient in y
        ret_value[1] = blurred_image.getInterpolatedPixel(x ,y+0.5) - blurred_image.getInterpolatedPixel(x,y-0.5);
        
        return ret_value;
   
   }

     
    public double getMaxPixel(double x, double y){
        //offset to place center point at center of pixel
        x = x - 0.5;
        y = y - 0.5;
        
        double max_pixel = 0;
        int half = TwoDDeformation.squareSize/2;
        for(int i = -half; i<=half; i++){
            for(double d: blurred_image.getLine(x - half, y + i, x + half, y+i))
                max_pixel = (max_pixel>d)?max_pixel:d;
        
        }
        
        
        return max_pixel;
    }
    public ImageProcessor getProcessor(){
        return image;
    }
    
}
