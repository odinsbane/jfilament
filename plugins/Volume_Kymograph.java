import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import snakeprogram3d.MultipleSnakesStore;
import snakeprogram3d.Snake;
import snakeprogram3d.SnakeIO;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: mbs207
 * Date: Jul 8, 2010
 * Time: 7:06:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class Volume_Kymograph implements PlugInFilter {
    ImagePlus implus;
    double Z;
    int N;
    public int setup(String s, ImagePlus imagePlus) {
        implus = imagePlus;
        N = implus.getNSlices();
        return DOES_ALL;
    }

    public void run(ImageProcessor imageProcessor) {
        HashMap<String, Double> constants = new HashMap<String, Double>();
        MultipleSnakesStore store = SnakeIO.loadSnakes((Frame)IJ.getInstance(), constants);
        Z = 1;
        try{
            Z = constants.get("zresolution");
        } catch(Exception e){
            GenericDialog gd = new GenericDialog("Constants");
            gd.addNumericField("Z resolution( px per slice ): ", Z, 2);
            gd.showDialog();
            Z = gd.getNextNumber();
        }
        for(Snake s: store){
            
            threeDSnakeKymograph(s);

        }
    }

    public void threeDSnakeKymograph(Snake s){


        Plot showing = null;

        for(Integer frame: s){

            ArrayList<double[]> cnets = s.getCoordinates(frame);
            int pts = cnets.size();
            double[] x = new double[pts];
            double[] y = new double[pts];

            for(int i = 0; i<pts; i++){

                x[i] = i;
                y[i] = getPixel(cnets.get(i), frame);
            }








            if(showing==null){
                showing = new Plot("values","x","y",x, y);
            } else{
                showing.addPoints(x, y, Plot.LINE);
            }
            




        }

        if(showing!=null)
            showing.show();




    }

    public double getPixel(double[] xyz, int frame){

        double x = xyz[0];
        double y = xyz[1];
        double z = xyz[2];

        double slice = z/Z + 1;
        if(slice<=1){
            return getPixel(x,y,1 + frame*N);


        } else if(slice>=N){

            return getPixel(x,y,N + frame*N);

        } else{
            int low = (int)slice + frame*N;
            int high = low + 1;
            double a = getPixel(x,y,low);
            double b = getPixel(x,y,high);
            double t = frame*N + slice - low;


            return interpolate(a,b,t);


        }
    }

    public double getPixel(double x, double y, int slice){

        return implus.getStack().getProcessor(slice).getInterpolatedValue(x,y);


    }
    static double interpolate(double x1, double x2, double t){
       return (x1 + t*(x2 - x1));
   }
}
