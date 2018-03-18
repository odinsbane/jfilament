import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import snakeprogram.MultipleSnakesStore;
import snakeprogram.Snake;
import snakeprogram.SnakeIO;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * .
 * User: mbs207
 */
public class Snake_To_ROI implements PlugInFilter {
    ImagePlus implus;
    public int setup(String args, ImagePlus imp){
        implus = imp;
        return DOES_ALL;
    }
    public void run(ImageProcessor ip){
        MultipleSnakesStore store = SnakeIO.loadSnakes(IJ.getInstance(), new HashMap<String, Double>());
        if(store==null){
            return;
        }
        Snake s = null;

        RoiManager manager = new RoiManager();
        PolygonRoi pr = null;
        int snakeNumber = 0;
        for(Snake snake: store){

            for(Integer i: snake){
                String name = String.format("s-%d-fr-%d", snakeNumber, i);
                PolygonRoi roi = createRoi(snake.getCoordinates(i), snake.TYPE);
                manager.add(implus, roi, i);
                int last = manager.getCount();
                manager.select(last-1);
                manager.runCommand("Rename", name);

                if(i==implus.getFrame()){
                    pr = roi;
                }
            }
        }


        if(pr!=null) {
            implus.setRoi(pr);
        }
    }

    PolygonRoi createRoi(List<double[]> pts, int type){
        int[] x = new int[pts.size()];
        int[] y = new int[pts.size()];
        for(int i = 0; i<pts.size(); i++){

            x[i] = (int)pts.get(i)[0];
            y[i] = (int)pts.get(i)[1];

        }
        PolygonRoi pr;

        if(type==Snake.CLOSED_SNAKE)
            pr = new PolygonRoi(x,y,y.length,Roi.POLYGON);
        else
            pr = new PolygonRoi(x,y,y.length, Roi.POLYLINE);

        return pr;
    }
}
