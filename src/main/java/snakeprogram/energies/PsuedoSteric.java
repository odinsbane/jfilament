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
package snakeprogram.energies;

import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import snakeprogram.Snake;
import snakeprogram.util.SnakesToDistanceTransform;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PsuedoSteric implements ExternalEnergy {
    ImageProcessor distance_transformed;
    double weight;
    public PsuedoSteric(ImageProcessor imp, List<List<double[]> >neighbors, double weight){
        distance_transformed = new ShortProcessor(imp.getWidth(), imp.getHeight());
        this.weight = weight;
        for(List<double[]> neighbor: neighbors){
            SnakesToDistanceTransform.snakeToDistanceTransform(neighbor, distance_transformed);
        }
    }

    @Override
    public double[] getForce(double x, double y){
        //offset to place center point at center of pixel
        x = x - 0.5;
        y = y - 0.5;

        double[] ret_value = new double[2];

        //gradient in x
        ret_value[0] = -weight*(distance_transformed.getInterpolatedPixel(x+0.5, y) - distance_transformed.getInterpolatedPixel(x-0.5,y));

        //gradient in y
        ret_value[1] = -weight*(distance_transformed.getInterpolatedPixel(x ,y+0.5) - distance_transformed.getInterpolatedPixel(x,y-0.5));

        return ret_value;

    }


}
