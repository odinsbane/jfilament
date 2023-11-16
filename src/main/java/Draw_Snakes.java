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
import java.awt.*;
import java.awt.event.*;
import ij.plugin.filter.*;

import java.util.*;
import java.util.List;
import javax.swing.*;

import snakeprogram.*;

/**
 * This plugin draws already existing snakes onto an image.  It
 * converst the image to rgb.
 */
public class Draw_Snakes implements PlugInFilter{
    ImagePlus implus;
    
    public int setup(String cmd, ImagePlus implus){
        this.implus = implus;
        return DOES_ALL;
    }
    
    public void run(ImageProcessor improc){
        
        
        HashMap<String, Double> constants = new HashMap<String,Double>();
        String fname = SnakeIO.getOpenFileName(null);
        
        MultipleSnakesStore SNAKES = SnakeIO.loadSnakes(fname,constants);
                
        ImageStack outstack = new ImageStack(implus.getWidth(), implus.getHeight());
        ImageStack istack = implus.getStack();
        
        for(int i = 1; i<=istack.getSize(); i++){
                            
            ImageProcessor ip = istack.getProcessor(i).convertToRGB().duplicate();
            outstack.addSlice(istack.getSliceLabel(i), ip);
            for(Snake s: SNAKES){
                Color c = Color.RED;
                if(s.exists(i))
                    drawSnake(s.getCoordinates(i), ip, c);
            }
            
        }
            ImagePlus x = new ImagePlus("snaked",outstack);
            x.show();
    }

    /**
     * Draws a snake on the image.
     * @param snake length 2 double[] coordinates that represent a snake.
     * @param ip image processor that the snake is drawn upon.
     * @param c the color the snake is drawn
     */
    public static void drawSnake(List<double[]> snake, ImageProcessor ip, Color c){
        ip.setColor(c);
        for(int i =1; i<snake.size(); i++){
            double[] current = snake.get(i);
            double[] last = snake.get(i-1);
            ip.drawLine((int)current[0], (int)current[1], (int)last[0], (int)last[1]);
            
        }
        
    }
    
    
   

}
