package snakeprogram.energies;

import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import snakeprogram.TwoDDeformation;

import java.util.ArrayList;

/**
 * Uses the gradient of the image intensity to push snake towards edges.
 *
 * User: msmith
 * Date: 3/3/13
 * Time: 7:44 PM
 *
 */
public class GradientEnergy implements ImageEnergy {
    final ImageProcessor blurred_image;
    final ImageProcessor image;
    public GradientEnergy(ImageProcessor img, double blur_sigma){
        image = img.convertToFloat();
        blurred_image = image.duplicate();
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(blurred_image, blur_sigma, blur_sigma, .01);
        blurred_image.filter(ImageProcessor.FIND_EDGES);

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
            for(double d: image.getLine(x - half, y + i, x + half, y+i))
                max_pixel = (max_pixel>d)?max_pixel:d;

        }


        return max_pixel;
    }
    public ImageProcessor getProcessor(){
        return image;
    }

}
