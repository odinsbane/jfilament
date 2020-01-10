package snakeprogram.util;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import snakeprogram.MultipleSnakesStore;
import snakeprogram.Snake;
import snakeprogram.SnakeIO;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CurveGeometry {
    List<double[]> points;
    double length;
    /**
     * distances[i] corresponds to the cumulative distance from the origin to the current index.
     */
    double[] distances;
    private Integer frame;

    public CurveGeometry(List<double[]> snake){
        this.points = snake;
        distances = new double[snake.size()-1];

        length = 0;
        for(int i = 0; i<snake.size()-1; i++){
            double[] a = snake.get(i);
            double[] b = snake.get(i+1);
            double dx = b[0] - a[0];
            double dy = b[1] - a[1];
            length += Math.sqrt(dx*dx + dy*dy);
            distances[i] = length;
        }
    }

    /**
     * Gets the normal by rotating 90 ccw.
     * @param tangent
     * @return
     */
    public double[] getNormal(double[] tangent){
        double c = 0;
        double s = 1;
        double nx = c*tangent[0] + s*tangent[1];
        double ny = -s * tangent[0] + c*tangent[1];
        return new double[]{nx, ny};
    }

    int getLowerIndex(double s){
        for(int i = 0; i<distances.length; i++){

            if(s<=distances[i]){
                return i;
            }

        }
        return distances.length-1;
    }

    public double getLength(){
        return length;
    }

    /**
     * Returns the cumulative distance to the point found at the provided index.
     *
     * @param index
     * @return
     */
    public double getDistance(int index){
        if(index == 0) return 0;

        return distances[index-1];
    }

    /**
     * Get the position parameterized by the distance from the origin.
     *
     * @param s distance from origin
     * @return
     */
    public double[] getPosition(double s){
        int dex = getLowerIndex(s);

        // if s is greater than or equal to the length of the curve, then the endpoint is returned.
        if(dex>=distances.length){

            return points.get(points.size()-1);
        }
        double lower, upper;

        lower = getDistance(dex);
        upper = getDistance(dex + 1);

        double ds = upper - lower;

        double f = ds>0? (s - lower) / ds : 0;
        double[] a = points.get(dex);
        double[] b = points.get(dex+1);

        double x = (b[0] - a[0])*f + a[0];
        double y = (b[1] - a[1])*f + a[1];
        //return a;
        return new double[]{x, y};
    }

    /**
     * Gets the tangent vector as a parameter along the length of a snake.
     *
     * @param s position along snake from the first point.
     *
     * @return {tx, ty} normalized.
     */
    public double[] getTangent(double s){
        if(s>=length){
            return getTangent(length-1);
        }
        if(points.size()<2){
            throw new RuntimeException("Curve contains less than two points.");
        }

        double[] A = getPosition(s);
        double[] B = getPosition(s+1);
        double dx = B[0] - A[0];
        double dy = B[1] - A[1];

        double mag = Math.sqrt(dx*dx + dy*dy);
        if(mag==0){
            return getTangent(s+0.5);
        }
        return new double[]{dx/mag, dy/mag};
    }


    public static List<double[]> createProfile(ImageProcessor proc, List<double[]> curve, int width){
        List<double[]> points = new ArrayList<>(curve);

        CurveGeometry g = new CurveGeometry(points);
        double length =  g.length;
        int imgWidth = (int)length;
        if(length - imgWidth > 0){
            imgWidth += 1;
        }

        ImageStack stack = new ImageStack(imgWidth, width);

        double li = g.length;
        int steps = (int)li;
        if(li - steps > 0){
            steps += 1;
        }

        double[] values = new double[steps];
        double[] distance = new double[steps];
        double[] maximums = new double[steps];
        for(int i = 0; i<steps; i++){
            double s = i;

            double[] tangent = g.getTangent(s);
            double[] normal = g.getNormal(tangent);
            double[] origin = g.getPosition(s);


            double offset = -width/2.0;

            double v = 0;
            double max = -Double.MAX_VALUE;

            for(int j = 0; j<width; j++){
                double lat = offset + j;
                double x = normal[0]*lat + origin[0];
                double y = normal[1]*lat + origin[1];
                double px = proc.getInterpolatedPixel(x,y);
                v  += px;
                max = px>max?px:max;
            }
            distance[i] = s;
            values[i] = v/width;
            maximums[i] = max;
        }

        return Arrays.asList(distance, values, maximums);
    }

    public static ImagePlus createKimograph(ImagePlus original, Snake snake, int width){
        //BufferedWriter w = Files.newBufferedWriter(Paths.get("log.txt"));
        ImagePlus kimo = original.createImagePlus();

        double length = 0;

        List<CurveGeometry> geom = new ArrayList<>();
        for(Integer i: snake){
            List<double[]> points = new ArrayList<>(snake.getCoordinates(i));
            if(snake.TYPE==Snake.CLOSED_SNAKE){
                points.add(points.get(0));
            }
            CurveGeometry g = new CurveGeometry(points);
            length =  g.length>length?g.length:length;
            g.setFrame(i);
            geom.add(g);
        }
        int imgWidth = (int)length;
        if(length - imgWidth > 0){
            imgWidth += 1;
        }

        ImageStack stack = new ImageStack(imgWidth, width);

        for(CurveGeometry geometry: geom){
            double li = geometry.length;
            int steps = (int)li;
            if(li - steps > 0){
                steps += 1;
            }

            ImageProcessor proc = original.getStack().getProcessor(geometry.frame);
            ImageProcessor kSlice = proc.createProcessor(imgWidth, width);
            for(int i = 0; i<steps; i++){
                double s = i;

                double[] tangent = geometry.getTangent(s);
                double[] normal = geometry.getNormal(tangent);
                double[] origin = geometry.getPosition(s);


                double offset = -width/2.0;
                for(int j = 0; j<width; j++){
                    double lat = offset + j;
                    double x = normal[0]*lat + origin[0];
                    double y = normal[1]*lat + origin[1];
                    double v = proc.getInterpolatedValue(x, y);

                    kSlice.set(i, j, (int)v);

                }
            }
            stack.addSlice(kSlice);
        }

        kimo.setStack(stack);
        return kimo;

    }

    public static void main(String[] args) throws IOException {
        new ImageJ();

        MultipleSnakesStore snakes = SnakeIO.loadSnakes(args[0], new HashMap<>());
        ImagePlus plus = IJ.openImage(new File(args[1]).getAbsolutePath());
        plus.show();
        for(Snake s: snakes){

            ImagePlus ck = createKimograph(plus, s, 100);

            ck.show();
        }

    }

    public void setFrame(Integer frame) {
        this.frame = frame;
    }

    public Integer getFrame() {
        return frame;
    }
}
