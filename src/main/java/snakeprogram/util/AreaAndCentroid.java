package snakeprogram.util;

import ij.ImagePlus;
import ij.process.ImageProcessor;

//import lightgraph.Graph;
//import lightgraph.DataSet;

import snakeprogram.MultipleSnakesStore;
import snakeprogram.Snake;
import snakeprogram.SnakeIO;

import javax.swing.*;
import java.awt.geom.Path2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;

/**
 *
 *
 * User:
 * Date: 2/25/13
 * Time: 12:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class AreaAndCentroid {
    static int LINE_WIDTH=5; //for calculating value along perimeter.
    public final static String LEFT = "left";
    public final static String RIGHT = "right";

    /**
     * Splits a single snake into two snakes, a left and right side snake, based on the furrow.
     *
     * @param points that the furrow was created from.
     * @param furrow two points to define the furrow {x1,y1,x2,y2}.
     * @return Two arraylists, left and right, cnets.
     */
    static public HashMap<String, List<double[]>> furrowSplit(List<double[]> points, double[] furrow){
        ArrayList<double[]> left = new ArrayList<double[]>();
        ArrayList<double[]> right = new ArrayList<double[]>();

        HashMap<String, List<double[]>> left_right = new HashMap<String,List<double[]>>();
        left_right.put(LEFT,left);
        left_right.put(RIGHT,right);

        //direction furrow points.
        double[] dir = new double[]{
            furrow[2]-furrow[0],
            furrow[3]-furrow[1]
        };

        //starting of furrow.
        double[] origin = new double[]{
            furrow[0],
            furrow[1]
        };

        //end of furrow.
        double[] target = new double[]{
            furrow[2],
            furrow[3]
        };

        //place holders.
        double[] pointing = new double[2];
        double cross;


        //the cross product along the -x axis is less than zero
        boolean left_cross_product = (-dir[1]<0);
        int offset=0;

        boolean searching=true;
        int side = -1;

        //find all of the crossings.
        while(searching ){

            double[] pt = points.get(offset);
            pointing[0] = pt[0] - origin[0];
            pointing[1] = pt[1] - origin[1];

            cross = pointing[0]*dir[1] - dir[0]*pointing[1];

            boolean test = cross<0;
            if(cross==0){
                //could be improved.
            }else if(test==left_cross_product){
                //crossed add furrow point to approriate side.
                if(side==1){
                    searching=false;
                }
                //left side
                side=0;

            } else{
                if(side==0){
                    searching = false;
                }
                //right side
                side=1;
            }
            if(searching){
                offset++;
            }
        }

        //add all of the points.
        for(int i = 0; i<points.size(); i++){
            double[] pt = points.get(((i+offset)%points.size()));
            pointing[0] = pt[0] - origin[0];
            pointing[1] = pt[1] - origin[1];

            cross = pointing[0]*dir[1] - dir[0]*pointing[1];

            boolean test = cross<0;

            if(cross==0){

                //skip this point. It will be added to the ends after finished.

            } else if(test==left_cross_product){
                //on left side
                left.add( new double[]{pt[0], pt[1]});

            } else{
                right.add( new double[]{pt[0], pt[1]});
            }
            i++;
        }

        addFurrowPoint(target, left);
        addFurrowPoint(origin, left);

        addFurrowPoint(origin, right);
        addFurrowPoint(target, right);


        return left_right;
    }

    private static void addFurrowPoint(double[] pt, ArrayList<double[]> points){
        //top or bottom
        if(pt[1]>0){
            //top
            if(points.get(0)[1]>0){
                //first point is top.
                points.add(0,pt);
            } else{
                //last point is top.
                points.add(pt);
            }
        } else{
            //bottom
            if(points.get(0)[1]<0){
                //first is bottom
                points.add(0,pt);
            } else{
                //last point is bottom
                points.add(pt);
            }

        }

    }

    /**
     * Finds a cleavage furrow for a give snake
     * @param s contains coordinates of a closed polygon defining a cell.
     * @return the best guess for a cleavage furrow in each frame. {x1,y1,x2,y2}
     */
    public static ArrayList<double[]> getCleavageFurrow(Snake s){

        ArrayList<double[]> furrow = new ArrayList<double[]>();

        for(int i: s){
            List<double[]> points = s.getCoordinates(i);

            double min_top = Double.MAX_VALUE;
            double min_bottom = Double.MAX_VALUE;
            double[] top=null;
            double[] bottom=null;

            for(double[] pt: points){
                double d = pt[0]*pt[0] + pt[1]*pt[1];
                if(pt[1]>0){
                   //top
                    if(d<min_top){
                        min_top =  d;
                        top = pt;
                    }
                } else{
                    if(d<min_bottom){
                        min_bottom = d;
                        bottom = pt;
                    }
                }


            }
            if(top!=null&&bottom!=null){
                furrow.add(new double[]{
                        top[0], top[1],
                        bottom[0], bottom[1]
                });
            }
        }

        return furrow;

    }

    /**
     * Smooths each element in the furrow.
     *
     * @param furrow a smoothed furrow.
     */
    static public List<double[]> smoothFurrow(ArrayList<double[]> furrow){
        int width = 5;
        List<double[]> smoothed = new ArrayList<double[]>();
        for(int i = 0; i<furrow.size(); i++){

            double n = 0;

            double[] sum=new double[4];

            for(int j = -width; j<=width; j++){

                int index = i + j;
                if(index>=0&&index<furrow.size()){
                    double[] f = furrow.get(index);
                    n++;
                    for(int k = 0; k<4; k++){
                        sum[k] += f[k];
                    }

                }

            }

            double[] f = new double[4];

            for(int k = 0; k<4; k++){
                f[k] = sum[k]/n;
            }

            smoothed.add(f);

        }
        return smoothed;

    }


    /**
     * Finds the center of mass of the snake.
     *
     * @param snake contains coordinates per frame.
     * @return the location of the cm wrt to original coordinates.
     */
    public static ArrayList<double[]> moveToCOM(Snake snake){
        ArrayList<double[]> centers = new ArrayList<double[]>();
        for(Integer i: snake){

            List<double[]> points = snake.getCoordinates(i);
            double area = calculateArea(points);
            double[] c = calculateCentroid(area, points);
            centers.add(c);
            List<double[]> translated_points = translate(new double[]{-c[0], -c[1]},points);
            points.clear();
            points.addAll(translated_points);

        }

        return centers;

    }

    /**
     * Assumes the snake is at the center of mass
     * @param snake
     * @return The angles rotated.
     */
    static public ArrayList<Double> rotateToPrincipleAxis(Snake snake){
        ArrayList<Double> angles = new ArrayList<Double>();

        for(Integer i: snake){

            List<double[]> points = snake.getCoordinates(i);



            double ixx = Ixx(points);
            double ixy = Ixy(points);
            double iyy = Iyy(points);
            double alpha = Math.atan(ixy/(ixx -iyy))/2;


            List<double[]> rot = rotate(alpha, points);
            points.clear();
            points.addAll(rot);

            angles.add(alpha);
        }

        return angles;



    }

    /**
     * Return the coordinates in a rotated frame of reference.
     *
     * @param angle between x and x' axis ccw
     * @param points that will be expressed in a new coordinate system.
     * @return
     */
    public static List<double[]> rotate(double angle, List<double[]> points){
        List<double[]> rotated_points = new ArrayList<double[]>();

        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        for(double[] pt: points){
            rotated_points.add(new double[]{
                    cos*pt[0] + sin*pt[1],
                    -sin*pt[0] + cos*pt[1]
            });
        }


        return rotated_points;

    }

    public static void test(){

        List<double[]> points = new ArrayList<double[]>();
        points.add(new double[]{0.,0.});
        points.add(new double[]{1.,0.});
        points.add(new double[]{1.,1.});
        points.add(new double[]{0.,1.});

        check(points);
        points = translate(new double[]{-0.5,0}, points);
        check(points);
        points = translate(new double[]{0,-0.5}, points);
        check(points);

        points.clear();
        points.add(new double[]{1,0});
        points.add(new double[]{0,-1});
        points.add(new double[]{-1,0});
        points.add(new double[]{0,1});

        check(points);

    }

    public static void check(List<double[]> points){
        double area = calculateArea(points);
        double[] c = calculateCentroid(area, points);
        System.out.println(area + ", " + c[0] + ", " + c[1]);

        double ixx = Ixx(points);
        double ixy = Ixy(points);
        double iyy = Iyy(points);

        System.out.println(ixx + "," + ixy + "," + iyy);

    }



    /**
     * Calculating the area of a polygon simple polygon courtesy of wikipedia,
     * ref Bourke, Paul (July 1988). "Calculating The Area And Centroid Of A Polygon". Retrieved 6-Feb-2013.
     *
     * @param points list of points that define a simple closed polygon.
     * @return the area, positive or negative depend on the winding
     */
    public static double calculateArea(List<double[]> points) {
        double sum = 0;
        int n = points.size();
        for(int i =0; i<points.size(); i++){
            double[] a = points.get(i);
            double[] b = points.get((i+1)%n);

            //x_i y_(i+1) - x_(i+1) y_i
            sum += a[0] * b[1] - b[0]*a[1];


        }

        return 0.5*sum;

    }

    /**
     *
     * Calculating the centroid of a polygon simple polygon courtesy of wikipedia,
     * ref Bourke, Paul (July 1988). "Calculating The Area And Centroid Of A Polygon". Retrieved 6-Feb-2013.
     *
     * @param area the calculated area
     * @param points the closed curve defined by the points.
     *
     * @return x,y coordinates of the centroid
     */
    public static double[] calculateCentroid(double area, List<double[]> points){
        double sumx = 0;
        double sumy = 0;
        int n = points.size();
        for(int i =0; i<points.size(); i++){
            double[] a = points.get(i);
            double[] b = points.get((i+1)%n);

            double chunk = a[0]*b[1] - a[1]*b[0];
            sumx += (a[0] + b[0])*chunk;
            sumy += (a[1] + b[1])*chunk;

        }

        return new double[] {sumx/(6*area), sumy/(6*area)};
    }

    public static double Ixx(List<double[]> points){
        double sum = 0;
        int n = points.size();
        for(int i =0; i<points.size();i++){
            sum +=ixx(points.get(i),points.get((i+1)%n) );
        }
        return sum;
    }

     private static double ixx(double[] r1, double[] r2){

        double x1 = r1[0];
        double x2 = r2[0];
        double y1 = r1[1];
        double y2 = r2[1];
        return (1./12.) * (x1 - x2)*(y1 + y2)*(y1*y1 + y2*y2);

    }

    public static double Ixy(List<double[]> points){
        double sum = 0;
        int n = points.size();
        for(int i =0; i<points.size();i++){
            sum +=ixy(points.get(i),points.get((i+1)%n) );
        }
        return sum;
    }

    private static double ixy(double[] r1, double[] r2){

        double x1 = r1[0];
        double x2 = r2[0];
        double y1 = r1[1];
        double y2 = r2[1];
        return -(1./24.)*(x1 - x2)*((3*x1 + x2)*y1*y1 + 2*(x1 + x2)*y1*y2 + (x1 + 3*x2)*y2*y2);

    }

    public static double Iyy(List<double[]> points){
        double sum = 0;
        int n = points.size();
        for(int i =0; i<points.size();i++){
            sum +=iyy(points.get(i),points.get((i+1)%n) );
        }
        return sum;
    }

    private static double iyy(double[] r1, double[] r2){

        double x1 = r1[0];
        double x2 = r2[0];
        double y1 = r1[1];
        double y2 = r2[1];
        return (1./12.)*(x1 - x2)*(2*x1*x2*(y1 + y2) + x1*x1*(3*y1 + y2) + x2*x2*(y1 + 3*y2));
    }

    static public List<double[]> translate(double[] t, List<double[]> points){
        ArrayList<double[]> new_points = new ArrayList<double[]>(points.size());

        for(double[] p: points){
            new_points.add(new double[]{p[0]+t[0], p[1] + t[1]});
        }
        return new_points;

    }

    public static double estimateLobeVolume(List<double[]> lobe){
        List<double[]> top = new ArrayList<double[]>();
        List<double[]> bottom = new ArrayList<double[]>();

        for(double[] pt: lobe){
            if(pt[1]>0){
                top.add(pt);
            } else if(pt[1]<0){
                bottom.add(pt);
            } else{
                top.add(pt);
                bottom.add(pt);
            }
        }

        double top_volume = estimateVolume(top);
        double bottom_volume = estimateVolume(bottom);

        System.out.println(top_volume + "\t" + bottom_volume);

        return (-top_volume + bottom_volume)/2;
    }

    /**
     * Estimates the volume of a surface that has been revolved about the x-axis.
     *
     *
     * @param surface
     * @return
     */
    public static double estimateVolume(List<double[]> surface){
        double sum = 0;
        for(int i = 0;i<surface.size()-1;i++){

            double[] r1 = surface.get(i);
            double[] r2 = surface.get(i+1);


            sum+= (r1[0] - r2[0])*(r1[1]*r1[1] + r1[1]*r2[1] + r2[1]*r2[1]);
        }
        return -(1./3.)*sum*Math.PI;
    }

    static public double estimateCircumferenceIntensity(List<double[]> points, ImageProcessor ip){

        return 0;

    }

    /**
     * Finds the average intensity along the provided curve. The width is determined by the
     * static value LINE_WIDTH.
     *
     * @param curve polygon representation of a curved surface. Open curve.
     * @param ip image processor that the data should be extracted from.
     * @return intensity per unit length.
     */
    static public double averageIntensityAlongCurve(List<double[]> curve,ImageProcessor ip){

        double l = 0;

        int N = curve.size();
        int n = N-1;

        for(int i = 0; i<n; i++){
            l += calculateDistance(curve.get(i), curve.get(i+1));
        }


        double ds;
        int j;

        double[] last = curve.get(0);
        double[] next;

        double intensity_sum = 0;

        for(int i = 0; i<n ; i++){

            j = i+1==N?0:i+1;
            next = curve.get(j);

            ds = calculateDistance(last, next)/l;

            intensity_sum += collectBox(last, next, ip)*ds;
            last = next;
        }

        return intensity_sum/l;


    }

    /**
     *  distance between two points. arbitrary dimension.
     *
     * @param a x,y
     * @param b x,y
     * @return |a-b|
     */
    static public double calculateDistance(double[] a, double[] b){
        double r = 0;
        for(int i = 0; i<a.length; i++){
            r += square(a[i] - b[i]);
        }
        return Math.sqrt(r);

    }

    /**
     *
     * @param x arg
     * @return x*x
     */
    static public double square(double x){
        return x*x;
    }


    static public double collectBox(double[] p1, double[] p2, ImageProcessor ip){

        //principle axis
        double lx = p2[0] - p1[0];
        double ly = p2[1] - p1[1];

        double lx_sq = lx*lx;
        double ly_sq = ly*ly;

        double length = Math.sqrt(lx_sq+ly_sq);


        //angle that the principle axis makes with the horizontal positive is ccw trig functions only

        double sintheta = ly/length;
        double costheta = lx/length;

        double startx = p1[0] - sintheta*LINE_WIDTH/2;
        double starty = p1[1] + costheta*LINE_WIDTH/2;

        double iCHANGE = LINE_WIDTH*1./((int)LINE_WIDTH + 1);
        double jCHANGE = length/((int)length + 1);
        double value = 0;
        double count = 0;
        double cx, cy;
        double t;
        int Ni = (int)((LINE_WIDTH+iCHANGE)/iCHANGE);
        for(double j = 0; j<length+jCHANGE; j+=jCHANGE){

            for(int k = 0; k<Ni; k++){
                double w = k*iCHANGE;
                //creates a map
                cx = startx + j*costheta + w*sintheta;
                cy = starty + j*sintheta - w*costheta;

                t = ip.getInterpolatedValue(cx, cy);
                value += t;
                count++;

            }

            //kymo_pixels.add(slicer);

        }

        return value/count;




    }

    /**
     * Finds the average intensity
     * @param pts
     * @param ip
     * @return
     */
    static public double getAverageIntensity(List<double[]> pts, ImageProcessor ip){
        Path2D path = new Path2D.Double();
        double[] pt = pts.get(0);
        path.moveTo(pt[0], pt[1]);
        for(int i = 1; i<pts.size(); i++){
            pt = pts.get(i);
            path.lineTo(pt[0], pt[1]);

        }
        path.closePath();

        double sum = 0;
        double n = 0;
        for(int i = 0; i<ip.getWidth(); i++){
            for(int j=0; j<ip.getWidth(); j++){
                if(path.contains(i,j)){
                    sum+= ip.getPixelValue(i,j);
                    n++;
                }
            }
        }
        return sum/n;

    }

}