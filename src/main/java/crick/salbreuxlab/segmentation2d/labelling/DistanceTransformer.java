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
package crick.salbreuxlab.segmentation2d.labelling;

import crick.salbreuxlab.segmentation2d.TwoDLabeledRegion;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DistanceTransformer{
    int levels;
    int max;
    int shift;

    public DistanceTransformer(int levels, int max, int shift){

        this.levels = levels;
        this.max = max;
        this.shift = shift;

    }

    /**
     * Levels are incremented by powers of two and can be separated by a bitwise masking.
     *
     * @param region
     * @param proc
     */
    public void categoricalLabelRegion(TwoDLabeledRegion region, ImageProcessor proc){
        DistanceTransformer.BoxedRegion box = new DistanceTransformer.BoxedRegion(region, new Rectangle(proc.getWidth(), proc.getHeight()));
        int step = max/levels;

        for(Integer i: box.cascades.keySet()){
            int level = i/step + 1;
            if(level>levels){
                level = levels;
            }

            int value = 1<<(shift + level - 1);
            for(int[] pt: box.cascades.get(i)){
                proc.set(pt[0], pt[1], value + proc.get(pt[0], pt[1]));
            }
        }

    }

    /**
     * Levels are incremented by one and will *not* be separable by bitwise categories. If the levels are
     * equal to the number of steps this is an actual distance transform.
     *
     * @param region
     * @param proc
     */
    public void distanceLabelRegion(TwoDLabeledRegion region, ImageProcessor proc){

        DistanceTransformer.BoxedRegion box = new DistanceTransformer.BoxedRegion(region, new Rectangle(proc.getWidth(), proc.getHeight()));
        int step = max/levels;

        for(Integer i: box.cascades.keySet()){
            int level = i/step;
            if(level>levels){
                level = levels;
            }

            int value = level<<shift;
            for(int[] pt: box.cascades.get(i)){
                proc.set(pt[0], pt[1], value + proc.get(pt[0], pt[1]));
            }
        }

    }

    /**
     * A Bound region of pixels for creating a distance transform.
     */
    class BoxedRegion{
        int originX;
        int originY;
        int width;
        int height;
        short[] pixels;
        HashMap<Integer, java.util.List<int[]>> cascades;
        BoxedRegion(TwoDLabeledRegion region, Rectangle bounds){

            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = -minX;
            int maxY = -minY;
            List<int[]> points = region.getPx();
            for(int[] pt: points){
                if(pt[0]<minX){
                    minX = pt[0];
                }
                if(pt[0]>maxX){
                    maxX = pt[0];
                }
                if(pt[1]<minY){
                    minY = pt[1];
                }
                if(pt[1]>maxY){
                    maxY = pt[1];
                }


            }
            //pad the image 1 px on any edge that is the edge of the image.
            short topPad = minY == 0 ? (short)1 : (short)0;
            short bottomPad = bounds.height - 1 == maxY ? (short)1 : (short) 0;
            short leftPad = minX == 0 ? (short)1 : 0;
            short rightPad = bounds.width - 1 == maxX ? (short)1 : 0;

            originX = minX - 1;
            width = maxX - minX + 1 + 2;
            originY = minY - 1;
            height = maxY - minY + 1 + 2;
            pixels = new short[width*height];

            for(int[] pt: points){
                int x = pt[0] - originX;
                int y = pt[1] - originY;

                pixels[x + y*width] = 1;
            }


            for(int i = 0; i<width; i++){

                pixels[i] = topPad;
                pixels[i + (height-1)*width] = bottomPad;

            }

            for(int j = topPad; j<height - bottomPad; j++){
                pixels[j*width] = leftPad;
                pixels[width-1 + j*width] = rightPad;
            }



            java.util.List<int[]> working = new ArrayList<>(points);
            int current = 1;
            cascades = new HashMap<>();
            while(working.size()>0){
                List<int[]> cascade = new ArrayList<>();
                for(int[] pt: working){

                    if(border(pt[0], pt[1])){
                        cascade.add(pt);
                    }

                }
                for(int[] pt: cascade){
                    working.remove(pt);
                    int x = pt[0] - originX;
                    int y = pt[1] - originY;
                    if(x <0 || y < 0 || x + y*width >= width*height){
                        System.out.println("borked!~");
                    }
                    pixels[x + y*width] = 0;
                }
                cascades.put(current, cascade);
                current++;
            }

        }
        int[][] span = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1,  0},          {1,  0},
            {-1,  1}, {0,  1}, {1,  1}
        };
        boolean border(int xg, int yg){

            int x = xg - originX;
            int y = yg - originY;

            if( x==0 || y==0 || x==width-1 || y == height - 1){
                //this says it is a border of the region. we want to
                //ignore it if a border of the image.
                throw new RuntimeException("Invalid point!");
            }

            for(int[] delta: span){
                int xi = x + delta[0];
                int yi = y + delta[1];

                if(pixels[xi + yi*width] == 0){
                    return true;
                }
            }

            return false;

        }
    }


}
