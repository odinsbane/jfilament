/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package snakeprogram;

import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

/**
 * Calculates an energy based on the intensity of the image.  The snake points
 * are pushed towards the brightest regions.
 *
 * @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class IntensityEnergy implements ImageEnergy{
    final ImageProcessor blurred_image;
    final ImageProcessor image;
    public IntensityEnergy(ImageProcessor img, double blur_sigma){
        image = img.convertToFloat();
        blurred_image = image.duplicate();
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(blurred_image, blur_sigma, blur_sigma, .01);

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
