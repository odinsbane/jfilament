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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
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
        int getMaxLabel(){
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

    static public void fillVoids(ImageStack labelledStack){
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
        new ImagePlus("labelled and filled", filled).show();
    }


    public static void labelImage(ImagePlus image, MultipleSnakesStore snakes){
        ImageStack original = image.getStack();
        ImageStack masked = new ImageStack(original.getWidth(), original.getHeight());
        SnakesToMask loc = new SnakesToMask();
        for(int i = 0; i<original.getSize(); i++){
            masked.addSlice(new ShortProcessor(original.getWidth(), original.getHeight()));
        }
        int label = 1;
        for(Snake snake: snakes){

            for(Integer frame: snake){

                ImageProcessor proc = masked.getProcessor(frame);
                List<double[]> points = snake.getCoordinates(frame);
                if(snake.TYPE==Snake.CLOSED_SNAKE) {
                    loc.snakeToMask(proc, points, label);
                } else{
                    loc.snakeToLine(proc, points, label);
                }
            }

            label++;
        }

        long start = System.currentTimeMillis();
        //fillVoids(masked);
        System.out.println( System.currentTimeMillis() - start);

        new ImagePlus("Labelled Image", masked).show();
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

    static public void snakeToMask(ImageProcessor proc, List<double[]> snakePoints, int color){

        proc.setColor(color);
        int[] xs = new int[snakePoints.size()];
        int[] ys = new int[snakePoints.size()];
        for(int i = 0; i<snakePoints.size(); i++){
            double[] pt = snakePoints.get(i);
            xs[i] = (int)pt[0];
            ys[i] = (int)pt[1];

        }
        Polygon p = new Polygon(xs, ys, xs.length);
        proc.fillPolygon(p);
    }



}
