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
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import snakeprogram.MultipleSnakesStore;
import snakeprogram.SnakeIO;
import snakeprogram.Snake;
import snakeprogram.util.CurveGeometry;

import java.awt.Frame;
import java.util.HashMap;

public class Kymograph_2D_II implements PlugInFilter {
    /**
     * This is a re-write of the boundary kymograph plugin
     */
    int LINE_WIDTH = 5;
    ImagePlus original;
    @Override
    public int setup(String s, ImagePlus imagePlus) {
        original = imagePlus;
        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        GenericDialog gd = new GenericDialog("Constants");
        gd.addNumericField("Width of Line(px): ", LINE_WIDTH, 2);
        gd.showDialog();

        if (gd.wasCanceled())
            return;
        LINE_WIDTH = (int)gd.getNextNumber();

        MultipleSnakesStore store = SnakeIO.loadSnakes(IJ.getInstance(), new HashMap<>());
        if (store==null){
            return;
        }
        int s = 1;
        for(Snake snake: store){
            ImagePlus kimo = CurveGeometry.createKimograph(original, snake, LINE_WIDTH);
            kimo.setTitle("Snake_" + s + "_kimograph_" + original.getShortTitle() + "");
            kimo.show();
            s++;
        }
    }
}
