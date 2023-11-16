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
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;

import java.awt.Frame;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import snakeprogram.Snake;
import snakeprogram.SnakeIO;
import snakeprogram.MultipleSnakesStore;

public class Boundary_Kymograph implements PlugInFilter {
    ImagePlus imp;
    double LINE_WIDTH;
    ArrayList<ImageProcessor> out_stack = new ArrayList<ImageProcessor>();
    ImageProcessor new_processor;
    ArrayList<double[]>kymo_pixels = new ArrayList<double[]>();
    public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

    public void run(ImageProcessor ip) {
        GenericDialog gd = new GenericDialog("Constants");
        LINE_WIDTH=4;
        gd.addNumericField("Width of Line(px): ", LINE_WIDTH, 2);
        gd.showDialog();
        
        if (gd.wasCanceled())
            return;
        LINE_WIDTH = (int)gd.getNextNumber();
        
        MultipleSnakesStore store = SnakeIO.loadSnakes((Frame)null, new HashMap<String, Double>());
        Snake s = store.getLastSnake();
        

            
        ImageProcessor improc;
        List<double[]> cnets;
        Plot showing = null;
        ImageStack istack = imp.getStack();

        int MAX_WIDTH = 0;
        for(Integer frame: s){
            improc = istack.getProcessor(frame).convertToFloat();
            cnets = s.getCoordinates(frame);


            double l = s.TYPE==Snake.CLOSED_SNAKE?
                s.findLength(frame) + calculateDistance(cnets.get(0), cnets.get(cnets.size()-1)):
                s.findLength(frame);

            kymo_pixels.clear();
            double[][] xyvalues = createKimograph(cnets, improc, l, s.TYPE);

            new_processor = new FloatProcessor(kymo_pixels.size(), kymo_pixels.get(0).length);
            MAX_WIDTH=kymo_pixels.size()>MAX_WIDTH?kymo_pixels.size():MAX_WIDTH;
            for(int i = 0; i<kymo_pixels.size(); i++){
                double[] column = kymo_pixels.get(i);
                for(int j = 0; j<column.length; j++)
                    new_processor.putPixelValue(i,j,column[j]);
            }
            out_stack.add(new_processor);
            if(showing==null){
                showing = new Plot("values","x","y",xyvalues[0], xyvalues[1]);
            } else{
                showing.addPoints(xyvalues[0], xyvalues[1], Plot.LINE);
            }
            
            System.out.println("frame");
        }

        ImageStack new_stack = new ImageStack(MAX_WIDTH, (int)LINE_WIDTH+2);
        for(int i = 0; i<out_stack.size(); i++){

            ImageProcessor nip = new FloatProcessor(MAX_WIDTH, (int)LINE_WIDTH+2);
            nip.copyBits(out_stack.get(i),0,0,Blitter.COPY);
            new_stack.addSlice("frame: " + (i+1),nip);

        }
        new ImagePlus("kymograph", new_stack).show();
        
        showing.show();
        
        
        
	}
    
    public double[][] createKimograph(List<double[]> pts,ImageProcessor ip, double l, int type){
        int N = pts.size();

        int n = type==Snake.OPEN_SNAKE?N-1:N;
        
        double[] xvalues = new double[n];
        double[] yvalues = new double[n];
        
        double ds;
        int j;
        
        double[] last = pts.get(0);
        double[] next;
        double cummulative = 0;
        

        for(int i = 0; i<n ; i++){
            
            j = i+1==N?0:i+1;
            next = pts.get(j);
            
            ds = calculateDistance(last, next)/l;
            
            xvalues[i] = (ds*0.5 + cummulative);
            yvalues[i] = collectBox(last, next, ip);
            cummulative += ds;
            last = next;
        }
        
        return new double[][]{xvalues, yvalues};
        
        
    }
    
    public double calculateDistance(double[] a, double[] b){
        double r = 0;
        for(int i = 0; i<a.length; i++)
            r += Math.pow(a[i] - b[i], 2);
        
        return Math.sqrt(r);
        
    }
    
    public double collectBox(double[] p1, double[] p2, ImageProcessor ip){
    
        //principle axis
        double lx = p2[0] - p1[0];
        double ly = p2[1] - p1[1];
        
        double lx_sq = Math.pow(lx,2);
        double ly_sq = Math.pow(ly,2);
        
        double length = Math.sqrt(lx_sq+ly_sq);
        
        
        //angle that the principle axis makes with the horizontal positive is ccw trig functions only
        
        double sintheta = ly/length;
        double costheta = lx/length;
        
        double startx = p1[0] - sintheta*LINE_WIDTH/2;
        double starty = p1[1] + costheta*LINE_WIDTH/2;
                
        double iCHANGE = LINE_WIDTH/((int)LINE_WIDTH + 1);
        double jCHANGE = length/((int)length + 1);
        double value = 0;
        double count = 0;
        double cx, cy;
        double t;
        double[] slicer;
        int Ni = (int)((LINE_WIDTH+iCHANGE)/iCHANGE);
        for(double j = 0; j<length+jCHANGE; j+=jCHANGE){

            slicer = new double[Ni];
            for(int k = 0; k<Ni; k++){
                double w = k*iCHANGE;
                //creates a map
                cx = startx + j*costheta + w*sintheta;
                cy = starty + j*sintheta - w*costheta;

                t = ip.getInterpolatedValue(cx, cy);
                value += t;
                slicer[k] = t;
                count++;
                
            }

            kymo_pixels.add(slicer);

        }
        
        return value/count;
        
        
    
        
    }

}
