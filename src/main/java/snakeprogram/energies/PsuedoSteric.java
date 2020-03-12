package snakeprogram.energies;

import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import snakeprogram.Snake;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PsuedoSteric implements ExternalEnergy {
    ImageProcessor distance_transformed;
    double weight;
    public PsuedoSteric(ImageProcessor imp, List<double[]> neighbors, double weight){
        distance_transformed = new ShortProcessor(imp.getWidth(), imp.getHeight());
        this.weight = weight;
    }


    public double[] getImageEnergy(double x, double y){

        //offset to place center point at center of pixel
        x = x - 0.5;
        y = y - 0.5;

        double[] ret_value = new double[2];

        //gradient in x
        ret_value[0] = weight*(distance_transformed.getInterpolatedPixel(x+0.5, y) - distance_transformed.getInterpolatedPixel(x-0.5,y));

        //gradient in y
        ret_value[1] = weight*(distance_transformed.getInterpolatedPixel(x ,y+0.5) - distance_transformed.getInterpolatedPixel(x,y-0.5));

        return ret_value;

    }

    @Override
    public double[] getForce(double x, double y) {
        return new double[0];
    }

}
