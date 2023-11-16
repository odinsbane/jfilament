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
package snakeprogram.util;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import snakeprogram.MultipleSnakesStore;
import snakeprogram.Snake;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnakesToDistanceTransform {
    int originX, originY, width, height;
    short[] backing;
    Map<Integer, List<int[]>> cascades;

    public SnakesToDistanceTransform(List<int[]> pixels, Rectangle bounds){
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -minX;
        int maxY = -minY;

        for(int[] pt: pixels){
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
        backing = new short[width*height];

        for(int[] pt: pixels){
            int x = pt[0] - originX;
            int y = pt[1] - originY;

            backing[x + y*width] = 1;
        }


        for(int i = 0; i<width; i++){

            backing[i] = topPad;
            backing[i + (height-1)*width] = bottomPad;

        }

        for(int j = topPad; j<height - bottomPad; j++){
            backing[j*width] = leftPad;
            backing[width-1 + j*width] = rightPad;
        }



        List<int[]> working = new ArrayList<>(pixels);
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
                    backing[x + y*width] = 0;
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

            if(backing[xi + yi*width] == 0){
                return true;
            }
        }

        return false;

    }

    /**
     * Uses the snake to create a closed Path2D, then checks each pixel in the bounding
     * region to see if it is contained within the path. The resulting pixels are
     * then used to create a distance transform by eroding the region.
     *
     * @param snake closed contour of 2d points.
     * @param proc destination for distance transform.
     */
    static public void snakeToDistanceTransform(List<double[]> snake, ImageProcessor proc){
        Path2D s = new Path2D.Double();
        if(snake.size()<3){
            return;
        }
        double[] pt = snake.get(0);
        s.moveTo(pt[0], pt[1]);
        for(int i = 1; i<snake.size(); i++){
            pt = snake.get(i);
            s.lineTo(pt[0], pt[1]);

        }
        s.closePath();
        Rectangle rect = s.getBounds();
        List<int[]> pixels = new ArrayList<>();
        int minx = rect.x<0? 0 : rect.x;
        int miny = rect.y<0? 0 : rect.y;
        int maxx = rect.x + rect.width;
        maxx = maxx >= proc.getWidth() ? proc.getWidth() : maxx;
        int maxy = rect.y + rect.height;
        maxy = maxy >= proc.getHeight() ? proc.getHeight() : maxy;
        for(int i = minx; i<maxx; i++){
            for(int j = miny; j<maxy; j++){
                if(s.contains(i, j)){
                    proc.set(i, j, 1);
                    pixels.add(new int[]{i, j});
                }
            }
        }
        SnakesToDistanceTransform stdt = new SnakesToDistanceTransform(
                pixels,
                new Rectangle(0, 0, proc.getWidth(), proc.getHeight() )
            );

        for(Integer l: stdt.cascades.keySet()){
            List<int[]> points = stdt.cascades.get(l);
            for(int[] ipt: points){
                proc.set(ipt[0], ipt[1], l - 1);
            }
        }
    }

    /**
     * Creates and shows distance transform from the provided snakes, if they are closed contours.
     *
     * @param original
     * @param snakes
     */
    static public void showDistanceTransform(ImagePlus original, MultipleSnakesStore snakes){
        int N = original.getStack().size();
        ImageStack stack = new ImageStack(original.getWidth(), original.getHeight());
        for(int i = 1; i<=N; i++){
            ShortProcessor transformed = new ShortProcessor(original.getWidth(), original.getHeight());
            for(Snake snake: snakes){
                if(snake.TYPE == Snake.CLOSED_SNAKE && snake.exists(i)){
                    snakeToDistanceTransform(snake.getCoordinates(i), transformed);
                }
            }
            stack.addSlice(transformed);
        }

        new ImagePlus("distance transformed", stack).show();
    }

}
