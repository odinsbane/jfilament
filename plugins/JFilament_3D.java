/**
 * This is the JFilament 2D active contours plugin.
   *
   **/

   
import javax.swing.*;
import java.awt.Graphics;
import java.awt.Color;
import ij.plugin.filter.GaussianBlur;
import java.io.*;

import snakeprogram3d.SnakeApplication;



import ij.ImagePlus;
import ij.process.*;
import ij.ImageStack;
import ij.plugin.filter.*;

public class JFilament_3D implements PlugInFilter{
    
    public int setup(String arg, ImagePlus imp) {
       return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
        SnakeApplication.runAsPlugin();
    }
    
    
   }
