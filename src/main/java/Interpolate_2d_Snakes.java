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
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import snakeprogram.*;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Uses a snake to create a distance transform.  For the Chebyshev distance transform.
 * User: mbs207
 * Date: Sep 2, 2010
 * Time: 9:09:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class Interpolate_2d_Snakes implements PlugInFilter {
    static int STEPS = 20;
    public int setup(String s, ImagePlus imagePlus) {
        return DOES_ALL;
    }

    public void run(ImageProcessor ip){
        System.setProperty("max.length", "2500");
        HashMap<String,Double> constants = new HashMap<String, Double>();
        MultipleSnakesStore store = SnakeIO.loadSnakes((Frame)null, constants);
        Snake s = store.getLastSnake();
        int DEX = 1;

        ImageStack istack = new ImageStack(ip.getWidth(), ip.getHeight());

        Snake new_snake = new Snake(s.TYPE);
        MultipleSnakesStore new_store = new MultipleSnakesStore();
        new_store.addSnake(new_snake);

        while(DEX<s.getLastFrame()){
            int frame = DEX + 1;
            List<double[]> pts = s.getCoordinates(frame);
            ImageProcessor dt = new FloatProcessor(ip.getWidth(), ip.getHeight());
            for(int i = 0; i<ip.getWidth(); i++){
                for(int j = 0; j<ip.getHeight(); j++){

                    dt.setf(i,j, closest(pts, i,j));

                }

            }
            istack.addSlice("original",dt);
            
            ImageProcessor dx = dt.duplicate();
            ImageProcessor dy = dt.duplicate();
            dx.convolve(new float[]{1,0,-1,1,0,-1,1,0,-1},3,3);
            dy.convolve(new float[]{1,1,1,0,0,0,-1,-1,-1},3,3);

            int n = ip.getWidth()*ip.getHeight();
            float[] xpixels = (float[])dx.getPixels();
            float[] ypixels = (float[])dy.getPixels();

            for(int i = 0; i<n; i++){
                float x = xpixels[i];
                float y = ypixels[i];
                float mag = (float)Math.sqrt(x*x + y*y);
                xpixels[i] = x/mag;
                ypixels[i] = y/mag;
            }


            List<double[]> last_snake = s.getCoordinates(DEX);

            new_snake.addCoordinates(DEX, new ArrayList<double[]>(last_snake));

            double part;

            for(int k = 0; k<STEPS; k++){

                new_snake.addCoordinates(DEX+1+k,new ArrayList<double[]>());

                for(double[] pt: last_snake){
                    part = 1./(STEPS - k);
                    double[] npt = new double[] {pt[0] + dx.getInterpolatedValue(pt[0], pt[1])*dt.getInterpolatedValue(pt[0],pt[1])*part,
                        pt[1] + dy.getInterpolatedValue(pt[0], pt[1])*dt.getInterpolatedValue(pt[0],pt[1])*part };
                    new_snake.getCoordinates(DEX+1+k).add(npt);
                }

                TwoDDeformation tdd = new_snake.TYPE==Snake.CLOSED_SNAKE?
                        new TwoDContourDeformation(new_snake.getCoordinates(DEX+1+k), null):
                        new TwoDCurveDeformation(new_snake.getCoordinates(DEX+1+k), null);

                try {
                    tdd.addSnakePoints(0.5);
                } catch (SnakeDeformationException e) {
                    e.printStackTrace();
                }
                store.addSnake(new_snake);
                last_snake = new_snake.getCoordinates(DEX+1+k);
            }
            DEX++;
        }


        SnakeIO.writeSnakes((Frame)null,constants, new_store);

        new ImagePlus("transform", istack).show();
    }

    float closest(List<double[]> pts, int i, int j){
        double min =Double.MAX_VALUE;
        for(double[] d: pts){

            double v = Math.pow(d[0] - i, 2) + Math.pow(d[1] - j, 2);
            if(v<min)
                min = v;


        }

        return (float)Math.sqrt(min);
    }
}
