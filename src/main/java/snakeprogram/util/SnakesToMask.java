package snakeprogram.util;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import snakeprogram.MultipleSnakesStore;
import snakeprogram.Snake;

import java.awt.Polygon;
import java.util.List;

/**
 * Created by msmith on 07/06/19.
 */
public class SnakesToMask {

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
