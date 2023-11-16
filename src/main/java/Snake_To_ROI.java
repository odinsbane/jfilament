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
