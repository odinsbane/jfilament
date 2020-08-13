package snakeprogram.util;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import snakeprogram.MultipleSnakesStore;
import snakeprogram.Snake;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility class for labelling snakes, either fills closed contours, or draws lines for open curves.
 *
 * Created by msmith on 07/06/19.
 */
public class SnakesToMask {

    static class SmallSet{
        final int[] labels = new int[9];
        final int[] bins = new int[9];
        int add(int label){
            if(label==0){
                return -1;
            }
            for(int j = 0; j<9; j++){
                if(bins[j] == 0){
                    labels[j] = label;
                    bins[j] = 1;
                    return 1;
                } else{
                    if(labels[j]==label){
                        bins[j] += 1;
                        return bins[j];
                    }
                }
            }
            return -1;
        }

        /**
         * Finds to max counted label, and if none are found then
         * it returns the first occuring of even value.
         *
         * @return the most represented label.
         */
        int getMaxCountLabel(){
            int j = 0;
            int max = bins[0];
            for(int k = 1; k<9; k++){
                int c = bins[k];
                if(c == 0){
                    return labels[j];
                } else if (c>max){
                    j = k;
                    max = c;
                }
            }
            return labels[j];
        }
        int getMaxLabel(){
            int mx = labels[0];
            for(int i = 1; i<9; i++){

                if(bins[i] == 0){
                    return mx;
                }

                if(labels[i]>mx){
                    mx = labels[i];
                }

            }
            return mx;
        }
    }


    static int getMaxNeighbor(int x, int y, short[] image, int w, int h){

        SmallSet neighbors = new SmallSet();
        for(int i = -1; i<2; i++){
            int xi = x + i;
            if( xi < 0 || xi >= w ) {
                continue;
            }
            for(int j = -1; j<2; j++){
                int yi = y+j;
                if(yi<0 || yi>=h){
                    continue;
                }
                neighbors.add(image[xi + yi*w]);
            }
        }
        return neighbors.getMaxLabel();
    }

    /**
     * A max value region growing algorithm. In a pixel is black, then it is replaced with it's
     * maximum value neighbor.
     *
     * @param labelledStack
     */
    static public ImageStack fillVoids(ImageStack labelledStack){
        final int w = labelledStack.getWidth();
        final int h = labelledStack.getHeight();
        ImageStack filled = new ImageStack(w, h);
        for(int i = 1; i<=labelledStack.size(); i++){
            ImageProcessor processor = labelledStack.getProcessor(i).duplicate();
            short[] labels = (short[])processor.getPixels();
            Map<Point, Integer> writing = new HashMap<>();
            List<Point> working = new ArrayList<>();
            for(int j = 0; j<w; j++){
                for(int k = 0; k<h; k++){
                    if(labels[j + k*w] == 0){
                        int l = getMaxNeighbor(j, k, labels, w, h);
                        if(l == 0){
                            working.add(new Point(j, k));
                        } else{
                            writing.put(new Point(j, k), l);
                        }
                    }
                }
            }
            for(Point p: writing.keySet()){
                short v = (short)writing.get(p).intValue();
                labels[p.x + w*p.y] = v;
            }
            writing.clear();

            while(working.size()>0){
                Iterator<Point> piter = working.iterator();
                while(piter.hasNext()){
                    Point p = piter.next();
                    int l = getMaxNeighbor(p.x, p.y, labels, w, h);
                    if(l != 0){
                        writing.put(p, l);
                        piter.remove();
                    }
                }

                for(Point p: writing.keySet()){
                    labels[p.x + w*p.y] = (short)writing.get(p).intValue();
                }
                writing.clear();
            }
            processor.setPixels(labels);
            filled.addSlice(processor);

        }
        return filled;
    }

    /**
     * Creates an image of either lines or masks depending on the type of snake,
     * Creates a stack of ShortProcessor equal to the size of the image stack.
     *
     * Just shows the result.
     *
     * @param image
     * @param snakes
     *
     * @return a new ImagePlus with short stack the same size as the original imageplus.
     */
    public static ImagePlus labelImage(ImagePlus image, MultipleSnakesStore snakes){
        ImageStack original = image.getStack();
        ImageStack masked = new ImageStack(original.getWidth(), original.getHeight());
        for(int i = 0; i<original.getSize(); i++){
            masked.addSlice(new ShortProcessor(original.getWidth(), original.getHeight()));
        }
        int label = 1;
        for(Snake snake: snakes){

            for(Integer frame: snake){

                ImageProcessor proc = masked.getProcessor(frame);
                List<double[]> points = snake.getCoordinates(frame);
                if(snake.TYPE==Snake.CLOSED_SNAKE) {
                    SnakesToMask.snakeToMask(proc, points, label);
                } else{
                    SnakesToMask.snakeToLine(proc, points, label);
                }
            }

            label++;
        }

        //long start = System.currentTimeMillis();
        //fillVoids(masked);
        //System.out.println( System.currentTimeMillis() - start);

        return new ImagePlus("Labelled Image", masked);
    }
    public static void createBinaryMask(ImagePlus image, MultipleSnakesStore snakes){
        ImageStack original = image.getStack();
        ImageStack masked = new ImageStack(original.getWidth(), original.getHeight());
        SnakesToMask loc = new SnakesToMask();
        for(int i = 0; i<original.getSize(); i++){
            masked.addSlice(new ByteProcessor(original.getWidth(), original.getHeight()));
        }

        for(Snake snake: snakes){
            for(Integer frame: snake){

                ImageProcessor proc = masked.getProcessor(frame);
                List<double[]> points = snake.getCoordinates(frame);
                loc.snakeToMask(proc, points, 255);
            }
        }
        new ImagePlus("Binary mask", masked).show();

    }

    /**
     * Drows the provided points as a line using the provided color.
     * @param proc destination of labels
     * @param snakePoints collection of points treated as an open curve.
     * @param color to be drawn.
     */
    static public void snakeToLine(ImageProcessor proc, List<double[]> snakePoints, int color){
        if(snakePoints.size() == 0){
            return;
        }
        double[] pt = snakePoints.get(0);
        proc.setColor(color);
        int x = (int)pt[0];
        int y = (int)pt[1];
        proc.moveTo(x, y);
        for(int i = 0; i<snakePoints.size(); i++){
            pt = snakePoints.get(i);
            x = (int)pt[0];
            y = (int)pt[1];

            proc.lineTo(x, y);
        }
    }

    /**
     * Fills the collection of points as a close contour using the 'color' provided.
     * @param proc
     * @param snakePoints
     * @param color
     */
    static public void fillSnake(ImageProcessor proc, List<double[]> snakePoints, int color){
        proc.setColor(color);
        int[] xs = new int[snakePoints.size()];
        int[] ys = new int[snakePoints.size()];
        for(int i = 0; i<snakePoints.size(); i++){
            double[] pt = snakePoints.get(i);
            xs[i] = (int)(pt[0]);
            ys[i] = (int)(pt[1]);

        }
        Polygon p = new Polygon(xs, ys, xs.length);
        proc.fillPolygon(p);
    }

    /**
     * Fills the collection of points as a close contour using the 'color' provided.
     * @param proc
     * @param snakePoints
     * @param color
     */
    static public void snakeToMask(ImageProcessor proc, List<double[]> snakePoints, int color){
        /*
        double xMax = 0;
        double yMax = 0;
        double xMin = proc.getWidth()-1;
        double yMin = proc.getHeight()-1;

        Path2D path = new Path2D.Double();
        double[] pt = snakePoints.get(0);
        path.moveTo(pt[0], pt[1]);
        for(int i = 1; i<snakePoints.size(); i++){
            pt = snakePoints.get(i);
            path.lineTo(pt[0], pt[1]);
        }
        path.closePath();

        for(int i = 0; i<snakePoints.size(); i++){
            pt = snakePoints.get(i);
            xMax = pt[0]>xMax? pt[0] : xMax;
            xMin = pt[0]<xMin? pt[0] : xMin;
            yMax = pt[1]>yMax ? pt[1] : yMax;
            yMin = pt[1]<yMin ? pt[1] : yMin;
        }
        for(int i = (int)xMin; i<xMax; i++){
            for(int j = (int)yMin; j<yMax; j++){
                if(path.contains(i, j)){
                    proc.set(i, j, color);
                };
            }
        }
        */
        fillSnake(proc, snakePoints, color);
        if(snakePoints.size() == 0){
            return;
        }
        double[] pt = snakePoints.get(0);
        proc.setColor(0);
        int x = (int)pt[0];
        int y = (int)pt[1];
        proc.moveTo(x, y);
        for(int i = 0; i<snakePoints.size(); i++){
            pt = snakePoints.get(i);
            x = (int)pt[0];
            y = (int)pt[1];

            proc.lineTo(x, y);
        }

    }

    static final int[][] faces = {
            {-1, 0},{0, -1}, {1, 0}, {0, 1}
    };
    static final int[][] corners = {
            {-1, -1}, {1, -1}, {1, 1}, {-1, 1}
    };

    /**
     * Checks a 3x3 region from mosaic and checks if the point is an edge, shares a face with a different
     * label, and if it is the not the minimum value in the region.
     *
     * This is to invert the creation of mosaic values by using a max expansion.
     *
     * @param mosaic
     * @param x
     * @param y
     * @return false for non-skeleton candidates. true if pixel is a skeleton candidate.
     */
    public static boolean nonMinEdgeRegion(ImageProcessor mosaic, int x, int y){
        int c = mosaic.get(x, y);
        int min = c;
        int xi, yi;
        int w = mosaic.getWidth();
        int h = mosaic.getHeight();
        boolean edge = false;
        for(int[] i: faces){
            xi = x + i[0];
            yi = y + i[1];
            if(xi>-1 && xi<w && yi>-1 && yi<h){
                int v = mosaic.get(xi, yi);
                if(v != c){
                    edge = true;
                }
                if(v < min){
                    min = v;
                }
            }
        }

        if(!edge) return false;

        for(int[] i: corners){
            xi = x + i[0];
            yi = y + i[1];
            if(xi>-1 && xi<w && yi>-1 && yi<h){
                int v = mosaic.get(xi, yi);
                if(v < min){
                    min = v;
                }
            }
        }
        return c != min;
    }

    /**
     * Converts the provided labelled image to a skeleton.
     *
     * @param mosaic A labelled image where edges will be drawn.
     * @return byte processor of 255 and 0's.
     */
    public static ImageProcessor skeletonizeMosaic(ImageProcessor mosaic){

        int w = mosaic.getWidth();
        int h = mosaic.getHeight();
        ImageProcessor skeleton = new ByteProcessor(w, h);
        for(int i = 0; i<w; i++){
            for(int j = 0; j<h; j++){
                if(nonMinEdgeRegion(mosaic, i, j)){
                    skeleton.set(i, j, 255);
                }
            }
        }
        return skeleton;
    }
}
