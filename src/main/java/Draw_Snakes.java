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
