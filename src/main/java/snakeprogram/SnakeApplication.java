package snakeprogram;

/**
   *    This is the main program for the snake program this will be where all of the data is stored
   *    it will also off all of the entry points either via main, imagej or via an applet.
   *    Finally it will delegate the actions as called by the SnakeFrame UI
   *
 * @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
   **/


import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.time.LocalDateTime;

public class SnakeApplication implements PlugInFilter{
    //it's back.
    final public static String VERSION = "1.1.9";

    ImagePlus implus;

    
   public static void main(String args[]) {
        new ImageJ();

        final SnakeModel sm = new SnakeModel();
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                sm.getFrame().setVisible(true);
            }
        });
    }
    
    public int setup(String arg, ImagePlus imp) {
		implus = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
        final SnakeModel sm = new SnakeModel();
        sm.getFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        sm.loadImage(implus);
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                sm.getFrame().setVisible(true);
            }
        });
	}

   }