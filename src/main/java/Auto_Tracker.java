/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

import java.util.HashMap;
import java.util.ArrayList;

import snakeprogram.*;
import snakeprogram.energies.ImageEnergy;
import snakeprogram.energies.IntensityEnergy;

import javax.swing.SwingUtilities;
/**
 *
 * @author mbs207
 */

public class Auto_Tracker implements PlugInFilter  {
    ImagePlus imp;


    public int setup(String arg, ImagePlus imp) {
                System.setProperty("max.length","3000");
		this.imp = imp;
		return DOES_ALL;
    }

    public void run(ImageProcessor ip) {
        HashMap<String,Double> constants = new HashMap<String, Double>();

        MultipleSnakesStore store = SnakeIO.loadSnakes((Frame)null, constants);
        Snake s = store.getLastSnake();

        GenericDialog gd = new GenericDialog("Constants");
        int ITERATIONS = 10;
        gd.addNumericField("Number of iterations per frame: ", ITERATIONS, 2);
        gd.showDialog();

        if (gd.wasCanceled())
            return;
        ITERATIONS = (int)gd.getNextNumber();


        int start = s.getLastFrame();
        ImageStack istack = imp.getStack();

        ArrayList<double[]> coordinates;

        final int N = imp.getNSlices();
        while(start<N){
            final int p = start;
            SwingUtilities.invokeLater(new Runnable(){
                public void run(){
                    IJ.showStatus("auto tracking snakes");
                    IJ.showProgress(p, N);
                }
            });
            coordinates = new ArrayList<double[]>(s.getCoordinates(start));

            start++;
            ImageEnergy ie = new IntensityEnergy(istack.getProcessor(start), constants.get("smoothing"));

            if(initializeDeformer(coordinates, constants, ie, ITERATIONS, s.TYPE))
                s.addCoordinates(start, coordinates);
            else
                break;


        }

        SnakeIO.writeSnakes((Frame)null, constants, store);
        
    }

    public boolean initializeDeformer(ArrayList<double[]> s, HashMap<String, Double> constants,ImageEnergy ie, int iters, int type){

        TwoDDeformation cd = type==Snake.CLOSED_SNAKE?new TwoDContourDeformation(s,ie):new TwoDCurveDeformation(s,ie);
        cd.setBeta(constants.get("beta"));
        cd.setGamma(constants.get("gamma"));
        cd.setWeight(constants.get("weight"));
        cd.setStretch(constants.get("stretch"));
        cd.setAlpha(constants.get("alpha"));
        cd.setForegroundIntensity(constants.get("foreground"));
        cd.setBackgroundIntensity(constants.get("background"));

        try{
            for(int i = 0; i<iters; i++){
                cd.addSnakePoints(constants.get("spacing"));
                cd.deformSnake();
            }
        } catch(Exception e){

            return false;
        }

        return true;
    }
}