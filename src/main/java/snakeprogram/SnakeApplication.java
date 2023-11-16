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
    final public static String VERSION = "1.2.1";

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
