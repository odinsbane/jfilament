package snakeprogram.util;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import snakeprogram.MultipleSnakesStore;
import snakeprogram.Snake;

import java.awt.Polygon;
import java.util.List;

/**
 * Created by msmith on 07/06/19.
 */
public class SnakesToMask {

    public static void createBinaryMask(ImagePlus image, MultipleSnakesStore snakes){
        System.out.println("creating binary mask");
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

    public void snakeToMask(ImageProcessor proc, List<double[]> snakePoints, int color){

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
