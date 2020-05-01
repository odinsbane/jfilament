package snakeprogram.energies;

import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import snakeprogram.TwoDDeformation;

import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses the gradient of the image intensity to find edges. This includes a force outwards.
 * to make the cell expand to fill regions.
 *
 * User: msmith
 * Date: 3/3/13
 * Time: 7:44 PM
 *
 */
public class BalloonGradientEnergy{
    final ImageProcessor blurred_image;
    final ImageProcessor image;
    final ImageProcessor intensity_image;
    double[] center_of_mass = new double[2];
    int winding;
    int index;
    public double FOREGROUND;
    public double MAGNITUDE;
    public double BACKGROUND;
    List<double[]> points;
    double[] last_point;

    public BalloonGradientEnergy(ImageProcessor img, double blur_sigma, List<double[]> points){
        image = img.convertToFloat();
        blurred_image = image.duplicate();
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(blurred_image, blur_sigma, blur_sigma, .01);
        intensity_image = blurred_image.duplicate();
        blurred_image.filter(ImageProcessor.FIND_EDGES);
        double area = calculateCentroid(points, center_of_mass);
        if(area>0){
            winding = -1;
        }
        this.points = points;
        index = 0;
    }

   public double[] getImageEnergy(double x, double y){
       double[] ret_value = balloonValues(x,y);

        //offset to place center point at center of pixel
        x = x - 0.5;
        y = y - 0.5;


        //gradient in x
        ret_value[0] += blurred_image.getInterpolatedPixel(x+0.5, y) - blurred_image.getInterpolatedPixel(x-0.5,y);

        //gradient in y
        ret_value[1] += blurred_image.getInterpolatedPixel(x ,y+0.5) - blurred_image.getInterpolatedPixel(x,y-0.5);

       return ret_value;

   }

    /**
     * In progress.
     *
     * @param x
     * @param y
     * @return should be a local outward direction of the curve.
     */
    public double[] getOutwardDirection(double x, double y){
        double[] pt = points.get(index);
        double delta = pt[0] - x + pt[1] - y;

        //ideally index should be where it left off.
        int start_index = index;

        while(delta!=0){

            index++;
            if(index==start_index){
                throw new RuntimeException("Round and Round");
            }
            if(index>=points.size()){
                index=0;
            }
            pt = points.get(index);
            delta = pt[0] - x + pt[1] - y;
        }
        int next_index = index+1;
        next_index = next_index>=points.size()?next_index=0:next_index;
        int previous_index = index-1;
        previous_index = previous_index>0?previous_index:points.size()-1;

        double[] next = points.get(next_index);
        double[] previous = points.get(previous_index);
        //90 degree rotation.
        // -dy
        double dx = (previous[1] - next[1])*winding;
        // +dx
        double dy = (next[0] - previous[0])*winding;

        index = next_index;
        return new double[]{dx,dy};
    }

    /**
     * Ballon values based on the maximum intensity, and pushes the shape out from the center of mass.
     *
     * @param x point ordinate
     * @param y point oridinate
     * @return two-d force at x corresponding to a balloon force.
     */
    public double[] balloonValues(double x, double y){
        double[] v = getExtremePixels(x, y);
        if(v[0]>=FOREGROUND||v[1]<BACKGROUND){

            //calculation based on center of mass

            double dx = x - center_of_mass[0];
            double dy = y - center_of_mass[1];

            /*
            double[] d = getOutwardDirection(x,y);
            double dx = d[0];
            double dy = d[1];
            */

            double mag = Math.sqrt(dx*dx + dy*dy);
            double factor = mag==0?0:MAGNITUDE/mag;

            if(v[1]<BACKGROUND){
                factor = -1*factor;
            }

            return new double[]{
                    factor*dx,
                    factor*dy
                };
        }
        return new double[]{0,0};
    }

    /**
     * Legacy code for the Intensity Energy method.
     *
     * @param x
     * @param y
     * @return max pixel in a region about x and y.
     */
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

    /**
     * Returns both the max and min values at the point (x,y)
     * @param x position ordinate
     * @param y position ordinate
     * @return {max_pixel, min_pixel}
     */
    public double[] getExtremePixels(double x, double y){
        //offset to place center point at center of pixel
        x = x - 0.5;
        y = y - 0.5;

        double max_pixel = 0;
        double min_pixel = Double.MAX_VALUE;
        int half = TwoDDeformation.squareSize/2;
        for(int i = -half; i<=half; i++){
            for(int j = -half; j<=half; j++){
                double d = intensity_image.getInterpolatedValue(x, y);
                max_pixel = (max_pixel>d)?max_pixel:d;
                min_pixel = (min_pixel<d)?min_pixel:d;
            }
        }


        return new double[]{ max_pixel, min_pixel};
    }

    /**
     * Legacy
     *
     * @return the unprocessed image.
     */
    public ImageProcessor getProcessor(){
        return image;
    }

    /**
     *
     * Calculating the centroid of a polygon simple polygon courtesy of wikipedia,
     * ref Bourke, Paul (July 1988). "Calculating The Area And Centroid Of A Polygon". Retrieved 6-Feb-2013.
     *
     * @param points the closed curve defined by the points.
     * @param com where the com will be placed.
     * @return x,y coordinates of the centroid
     */
    public static double calculateCentroid(List<double[]> points, double[] com){
        double sumx = 0;
        double sumy = 0;
        double area = 0;
        int n = points.size();
        for(int i =0; i<points.size(); i++){
            double[] a = points.get(i);
            double[] b = points.get((i+1)%n);

            double chunk = a[0]*b[1] - a[1]*b[0];
            sumx += (a[0] + b[0])*chunk;
            sumy += (a[1] + b[1])*chunk;

            area += chunk;

        }
        area=0.5*area;

        com[0] = sumx/(6*area);
        com[1] = sumy/(6*area);

        return area;
    }

    /**
     * for the given points that represent a closed contour, this finds the average values according to the
     * imageprocessor.
     *
     * @param pts points to represent closed contour.
     * @param ip image data
     * @return {inner average, outer average}
     */
    static public double[] getAverageIntensity(List<double[]> pts, ImageProcessor ip){
        Path2D path = new Path2D.Double();
        double[] pt = pts.get(0);
        path.moveTo(pt[0], pt[1]);
        for(int i = 1; i<pts.size(); i++){
            pt = pts.get(i);
            path.lineTo(pt[0], pt[1]);

        }
        path.closePath();

        double inner_sum = 0;
        double inner_n = 0;

        double outer_sum = 0;
        double outer_n = 0;
        Rectangle2D box = path.getBounds();
        int offset_x = (int)box.getX();
        int offset_y = (int)box.getY();
        for(int i = 0; i<box.getWidth(); i++){
            for(int j=0; j<box.getHeight(); j++){
                int img_x = i+offset_x;
                int img_y = j+offset_y;
                if(path.contains(img_x,img_y)){
                    inner_sum+= ip.getPixelValue(img_x,img_y);
                    inner_n++;
                } else{
                    outer_sum+= ip.getPixelValue(img_x,img_y);
                    outer_n++;
                }
            }
        }
        return new double[]{inner_sum/inner_n, outer_sum/outer_n};

    }
}
