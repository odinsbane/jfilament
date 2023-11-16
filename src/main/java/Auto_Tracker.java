/*-
 * #%L
 * JFilament 2D active contours.
 * %%
 * Copyright (C) 2010 - 2023 University College London
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the UCL LMCB nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
