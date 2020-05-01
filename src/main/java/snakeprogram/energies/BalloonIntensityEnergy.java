package snakeprogram.energies;

import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import snakeprogram.TwoDContourDeformation;
import snakeprogram.TwoDDeformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by smithm3 on 27/03/18.
 */
public class BalloonIntensityEnergy{


    final ImageProcessor blurred_image;
    final ImageProcessor image;
    double[] center_of_mass = new double[2];

    boolean CW;
    int index;
    public double FOREGROUND;
    public double MAGNITUDE;
    public double BACKGROUND;
    List<double[]> points;
    int last;
    public BalloonIntensityEnergy(ImageProcessor img, double blur_sigma, List<double[]> points){
        image = img.convertToFloat();
        blurred_image = image.duplicate();
        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(blurred_image, blur_sigma, blur_sigma, .01);
        double area = calculateCentroid(points, center_of_mass);
        if(area>0){
            CW=true;
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
     * Finds the point closest to the previous point.
     * @param x
     * @param y
     * @return
     */
    double[] getNormal(double x, double y){

        double min = Double.MAX_VALUE;
        int dex = -1;
        int n = points.size();
        for(int i = last; i<points.size() + last; i++){
            double[] pt = points.get(i%n);
            double dx = x - pt[0];
            double dy = y - pt[1];
            double m = dx*dx + dy*dy;
            if(m == 0){
                dex = i%n;
                break;
            } else if(m<min){
                min = m;
                dex = i%n;
            }
        }
        last = dex;
        if(dex == 0){
            System.out.println("index 0");
        }
        return getNormal(dex);
    }

    double[] getNormal(int index){
        int li = index - 1;
        if(li<0) li = points.size() + li;
        int hi = (index + 1)%points.size();

        double[] lower = points.get(li);
        double[] higher = points.get(hi);
        double dx = higher[0] - lower[0];
        double dy = higher[1] - lower[1];

        double m = Math.sqrt(dx*dx + dy*dy);
        dx = dx/m;
        dy = dy/m;
        if(!CW){
            dx = -dx;
            dy = -dy;
        }
        return new double[]{ -dy, dx};
    }

    /**
     * Balloon values based on the maximum intensity, and pushes the shape out from the center of mass. The foreground
     * intensity is used as an expanding region, and the background is used as a compressing region. The region
     * between is non-interacting.
     *
     * @param x point ordinate
     * @param y point oridinate
     * @return two-d force at x corresponding to a balloon force.
     */
    public double[] balloonValues(double x, double y){
        double[] v = getExtremePixels(x, y);
        if( (v[0]>=FOREGROUND) ^ (v[1]<BACKGROUND) ){

            double[] normal = getNormal(x, y);

            double factor = -MAGNITUDE*(v[0] - FOREGROUND);
            //shrink!
            if(v[1]<BACKGROUND){
                factor = -MAGNITUDE*(v[1] - BACKGROUND);
            }

            return new double[]{
                    factor*normal[0],
                    factor*normal[1]
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
                double d = blurred_image.getInterpolatedValue(x, y);
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
     * @return x,y area
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
    public static TwoDDeformation getDeformation(List<double[]> points, ImageEnergy ie) throws Exception{
        TwoDContourDeformation deformation = new TwoDContourDeformation(points, ie);
        deformation.setAlpha(1);
        deformation.setBeta(0.1);
        deformation.setGamma(5);
        deformation.setWeight(0);
        deformation.setStretch(1.7);

        deformation.setForegroundIntensity(255);
        deformation.setBackgroundIntensity(1);
        deformation.initializeMatrix();
        return deformation;
    }
    public static void main(String[] args) throws Exception{
        new ImageJ();
        ImageProcessor circle = new ShortProcessor(256, 256);
        circle.setColor(256);
        circle.fillOval(64, 64, 128, 128);


        for(int i = 0; i<5; i++){
            double theta = i*2*Math.PI/5;
            double cx = 128 + 64*Math.sin(theta);
            double cy = 128 + 64*Math.cos(theta);
            int r2 = 16;
            circle.fillOval((int)cx-r2, (int)cy - r2, 2*r2, 2*r2);
        }

        //circle.setColor(512);
        //circle.drawOval(64, 64, 128, 128);
        ImageProcessor display = circle.convertToRGB();
        double sigma = 1;
        List<double[]> points = new ArrayList<>();
        int n = 35;
        List<double[]> points2 = new ArrayList<>();
        for(int i = 0; i<n; i++){
            double theta = i*2*Math.PI/n;
            double x = 128 + 8 * Math.sin(theta);
            double y = 128 - 8 * Math.cos(theta);
            points.add(new double[]{x, y});
            double x2 = 128 + 128*Math.sin(theta);
            double y2 = 128 - 128*Math.cos(theta);
            points2.add(new double[]{x2, y2});
        }


        double ds = 64*Math.sin(Math.PI/n);
        IntensityEnergy ie = new IntensityEnergy(circle, sigma);
        TwoDDeformation deformation = getDeformation(points, ie);
        TwoDDeformation wrapper = getDeformation(points2, ie);
        wrapper.addSnakePoints(ds);
        display.setColor(0);
        for(int i = 0; i<n; i++){
            double[] start = points.get(i);
            double[] end = points.get((i+1)%n);
            display.drawLine((int)start[0], (int)start[1], (int)end[0], (int)end[1]);
        }
        for(int j = 0; j<255; j++) {
            deformation.deformSnake();
            deformation.addSnakePoints(ds);
            display.setColor(j);
            for(int i = 0; i<points.size(); i++){
                double[] start = points.get(i);
                double[] end = points.get((i+1)%points.size());
                display.drawLine((int)start[0], (int)start[1], (int)end[0], (int)end[1]);
            }
            wrapper.deformSnake();
            wrapper.addSnakePoints(ds);
            display.setColor((j<<8));
            for(int i = 0; i<points2.size(); i++){
                double[] start = points2.get(i);
                double[] end = points2.get((i+1)%points2.size());
                display.drawLine((int)start[0], (int)start[1], (int)end[0], (int)end[1]);
            }

        }
        display.setColor( 255<<16 );
        display.drawOval(64, 64, 128, 128);
        ImageProcessor proc = circle.duplicate();
        for(int i = 0; i<256; i++){
            for(int j = 0; j<256; j++){
                double p = ie.getMaxPixel(i, j);
                proc.set(i, j, (int) p);
            }
        }
        new ImagePlus("max", proc).show();
        new ImagePlus("snaked", display).show();
    }
}
