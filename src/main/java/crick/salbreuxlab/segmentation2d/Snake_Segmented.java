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
package crick.salbreuxlab.segmentation2d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import snakeprogram.Snake;
import snakeprogram.SnakeModel;

import java.util.*;

public class Snake_Segmented {
    ImagePlus plus;

    List<LabelledSnake> collection = new ArrayList<>();

    public Snake_Segmented(ImagePlus plus){
        this.plus = plus;
    }
    public List<Snake> getSnakes(){
        int n = collection.size();
        List<Snake> ret = new ArrayList<>(n);
        for(int i = 0; i<n; i++){
            ret.add(collection.get(i).snake);
        }
        return ret;
    }

    public List<LabelledSnake> getLabelledSnakes(){
        return collection;
    }
    public void process(){
        ImageStack stack = plus.getStack();
        int count = stack.getSize();
        for(int i = 0; i< count; i++){
            List<LabelledSnake> snakes = initializeSnakes(stack.getProcessor(i+1), i+1);
            collection.addAll(snakes);
        }
    }

    public void startJFilament(){
        SnakeModel model = new SnakeModel();
        model.loadImage(plus);
        collection.stream().map(ls->ls.snake).forEach(model::addNewSnake);
        model.getFrame().setVisible(true);
    }



    public List<LabelledSnake> initializeSnakes(ImageProcessor proc, int frame){
        int w = proc.getWidth();
        int h = proc.getHeight();
        Map<Integer, List<int[]>> regions = getRegions(proc);
        Set<Integer> toRemove = new HashSet<>();

        Integer maxKey = -1;
        int maxSize = -1;
        List<LabelledSnake> snakes = new ArrayList<>();

        for(Integer key: regions.keySet()){
            List<int[]> pxls = regions.get(key);
            for(int[] px: pxls) {
                if (px[0] == 0 || px[0] == w - 1 || px[1] == 0 || px[1] == h - 1) {
                    //touches.
                    toRemove.add(key);
                }
                break;
            }

            if(pxls.size()>maxSize){
                maxKey = key;
                maxSize = pxls.size();
            }
        }
        toRemove.add(maxKey);
        toRemove.forEach(regions::remove);
        for(Integer key: regions.keySet()){
            List<double[]> points = getBoundary(regions.get(key));
            if(points.size()<2) continue;

            Snake snake = new Snake( Snake.CLOSED_SNAKE);
            snake.addCoordinates(frame, points);

            snakes.add(new LabelledSnake(key, snake));
        }
        return snakes;
    }
    /**
     * Detects the boundary of the mask defined by px
     *
     * @param px mask as a collection of points.
     * @return points at the edge of the mask shifted 0.5 px to be "centered"
     */
    public List<double[]> getBoundary(List<int[]> px){
        List<double[]> curve = new ArrayList<>();
        int lastY = -1;
        int minX = plus.getWidth();
        int maxX = 0;
        int minY = plus.getHeight();
        int maxY = 0;
        for(int i = 0; i<px.size(); i++){
            int[] pt = px.get(i);
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

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;


        int[] filled = new int[height*width];

        List<double[]> all = new ArrayList<>(px.size());
        for(int[] pt: px){
            int x = pt[0] - minX;
            int y = pt[1] - minY;
            filled[x + y*width] = 255;
        }
        for(int[] pt: px){
            int x = pt[0] - minX;
            int y = pt[1] - minY;

            if(
                    x==0 ||
                    y == 0 ||
                    x==width-1 ||
                    y == height-1 ||
                    filled[y*width + x + 1]==0 ||
                    filled[y*width + x - 1]==0 ||
                    filled[(y+1)*width + x]==0 ||
                    filled[(y+1)*width + x - 1]==0 ||
                    filled[(y+1)*width + x + 1]==0 ||
                    filled[(y-1)*width + x + 1]==0 ||
                    filled[(y-1)*width + x]==0 ||
                    filled[(y-1)*width + x - 1]==0
            ){
                filled[x + y*width] = 255;
                all.add(new double[]{pt[0] + 0.5, pt[1] + 0.5});
            } else{
                filled[x + y*width] = 1;
            }
        }


        double[] latest = all.get(0);
        curve.add(latest);
        all.remove(0);
        while(all.size()>0){
            int next = getPseudoClosest(latest, all);
            latest = all.get(next);
            curve.add(latest);
            all.remove(next);
        }
        //remove ''wierd'' points
        double[] origin = curve.get(0);
        for(int i = curve.size()-1; i>0; i--){

            double[] next = curve.get(i);

            double dx = next[0] - origin[0];
            double dy = next[1] - origin[1];

            double d = dx*dx + dy*dy;

            //TODO remove criteria
            if(d>5){
                curve.remove(i);
            } else{
                //should be able to break at this point.
                origin = next;
            }


        }


        return curve;
    }


    int getPseudoClosest(double[] pt, List<double[]> rest){
        int n = rest.size();
        double min = Double.MAX_VALUE;
        int best = -1;
        for(int i = 0; i<n; i++){
            double[] opt = rest.get(i);
            double dx = pt[0] - opt[0];
            double dy = pt[1] - opt[1];
            double m = dx*dx + dy*dy;
            if(m==1){
                return i;
            }
            if(m<min){
                min = m;
                best = i;
            }

        }
        return best;

    }

    public Map<Integer, List<int[]>> getRegions(ImageProcessor processor){
        Map<Integer, List<int[]>> result = new HashMap<>();
        int w = processor.getWidth();
        int h = processor.getHeight();
        int n = w*h;
        for(int i = 0; i<n; i++){
            Integer b = processor.get(i);
            List<int[]> px = result.computeIfAbsent(b, ArrayList::new);
            px.add(new int[]{i%w, i/w});
        }
        return result;
    }
    //This method finds the distance between two points
    public static double pointDistance(double[] x1, double[] x2 ){
        double dx = x1[0] - x2[0];
        double dy = x1[1] - x2[1];

        return Math.sqrt(dx*dx + dy*dy);
    }

    //This method interpolates to find the value of the point in between those two points

    /**
     *
     * @param x1 first value
     * @param x2 second value
     * @param t fraction of distance between
     * @return x1 + (x2 - x1)*t
     */
    public static double interpolate(double x1, double x2, double t){
        return (x1 + t*(x2 - x1));
    }


    /**
     * Interpolates arrays of values using the above interpolation function
     * @param p1 first point
     * @param p2 second point
     * @param t fraction of distance between
     * @return p1 + (p2 - p1)*t
     */
    public static double[] interpolate(double[] p1, double[] p2, double t){
        double[] ret_value = new double[p1.length];
        for(int i = 0; i<p1.length; i++)
            ret_value[i] = interpolate(p1[i],p2[i],t);
        return ret_value;
    }

    public void addSnakePoints(List<double[]> vertices, double msl){
        int pointListSize = vertices.size();

        double cumulativeDistance = 0;
        ArrayList<Double> cumulativeDistances = new ArrayList<Double>();
        double distanceValue;

        for(int i = 0; i < pointListSize; i++){
            cumulativeDistances.add(i, cumulativeDistance);
            distanceValue = pointDistance(vertices.get(i), vertices.get((i+1)%pointListSize));
            cumulativeDistance += distanceValue;
        }

        cumulativeDistances.add(cumulativeDistance);

        int newPointListSize = (int)(cumulativeDistance/msl);



        double segmentLength = cumulativeDistance/(double)newPointListSize;

        int i = 0;
        int i_last = pointListSize - 1;

        ArrayList<double[]> newVertices = new ArrayList<double[]>(newPointListSize);

        newVertices.add(i, vertices.get(i));

        for(int s = 1; s < newPointListSize; s++){
            while(cumulativeDistances.get(i) < s*segmentLength){
                i_last = i;
                i = (i+1)%(pointListSize+1);
            }
            double t = (s*segmentLength - cumulativeDistances.get(i_last))/(cumulativeDistances.get(i) - cumulativeDistances.get(i_last));
            newVertices.add(s, interpolate(vertices.get(i_last), vertices.get(i%pointListSize), t));
        }



        vertices.clear();
        vertices.addAll(newVertices);

    }

    /**
     * Testing by creating a binary image
     * @param args not used.
     */
    public static void main(String[] args){
        int l = 256;
        ImageProcessor processor = new ColorProcessor(l, l);
        int a = 228;
        int b = 228;
        int borderX = (l - a)/2;
        int borderY = (l - b)/2;
        for(int i =borderX ; i < l - borderX; i++){
            for(int j = borderY; j < l - borderY; j++){
                double dx = i - l/2;
                double dy = j - l/2;
                double f = dx*dx*4/a/a + dy*dy*4/b/b;
                if(f <= 1){
                    processor.set(i, j, 1);
                }

            }
        }

        ImagePlus plus = new ImagePlus("elispe", processor);
        Snake_Segmented ss = null;
        long start = System.currentTimeMillis();
        for(int i = 0; i<1000; i++){
            ss = new Snake_Segmented(plus);
            ss.process();
        }
        System.out.println( "elapsed time: " + (System.currentTimeMillis() - start));
        ss.startJFilament();
    }

}
