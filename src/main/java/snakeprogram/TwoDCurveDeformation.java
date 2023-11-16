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

import ij.process.ImageProcessor;
import snakeprogram.energies.ImageEnergy;

import java.util.ArrayList;
import java.util.List;

/**
 *  This is an open snake with a stretching for a both ends.
 * @author Lisa,Matt
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class TwoDCurveDeformation extends TwoDDeformation{
    public TwoDCurveDeformation(List<double[]> vertex_X, ImageEnergy ie){
            super(vertex_X,ie);
    }

   public void initializeMatrix() throws InsufficientPointsException{

       //finds the number of points currently on the snake
       int contourSize = vertices.size();
       if(contourSize<=3){
           throw new InsufficientPointsException("not enough points to create matrix: " + contourSize);
       }
       double alpha = this.alpha/ Math.pow(MAX_SEGMENT_LENGTH,2);
       double beta = this.beta/ Math.pow(MAX_SEGMENT_LENGTH,4);

       double[] pentadiagonal_coefficients = new double[]{
               beta,
               -(alpha + 4*beta),
               2*(alpha + 3*beta) + gamma,
               -(alpha + 4*beta),
               beta
           };

       double[][] array = new double[contourSize][contourSize];

       //initializes the array
       for(int i = 0; i < contourSize; i++){

           for(int k = 0; k<5; k++){
               int n = i - 2 + k;

               n = n<0? -n:n;
               n = n>=contourSize?2*contourSize - n - 2:n;

               array[i][n] = array[i][n] + pentadiagonal_coefficients[k];
           }

       }

        
        matrixA = array;
   }


 
    /**
       *    Interpolates the points that are too far apart, and removes
       *    points that are too close together.  Includes the connection
       *    between the two end points
       */
    public void addSnakePoints(double msl) throws TooManyPointsException, InsufficientPointsException{
        MAX_SEGMENT_LENGTH = msl;
        int pointListSize = vertices.size();
        
        
        
        double cumulativeDistance = 0;
        ArrayList<Double> cumulativeDistances = new ArrayList<Double>(pointListSize-1);
        double distanceValue;

        for(int i = 0; i < pointListSize-1; i++){
            cumulativeDistances.add(i, cumulativeDistance);
            distanceValue = pointDistance( vertices.get(i), vertices.get((i+1)%pointListSize) );
            cumulativeDistance += distanceValue;
        }

        cumulativeDistances.add(cumulativeDistance);

        int newPointListSize = (int)(cumulativeDistance/MAX_SEGMENT_LENGTH);
        
        if(newPointListSize>SnakeModel.MAXLENGTH)
            throw new TooManyPointsException("Attempting to create snake with too many points: " + newPointListSize);
            
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

        newVertices.add(vertices.get(pointListSize-1));

        
        vertices.clear();
        vertices.addAll(newVertices);
        
        

        if(vertices.size()>2) {
            initializeMatrix();
        } else if(vertices.size()==2){
            matrixA = new double[][]{
                    { gamma , -alpha },
                    { -alpha, gamma  }
            };
        }
    }
  /**
     *  Adds forces based on the image energy. Includes a stretch force.
     **/
     
  public void energyWithGradient(double[] Vx, double[] Vy){

        double[] current;
        int contourSize = Vx.length;
         
       for(int i = 0; i < contourSize; i++){
           current = vertices.get(i);
           double[] energies = imageEnergy(current[0],current[1]);
           Vx[i] = current[0]*gamma + weight*energies[0];
           Vy[i] = current[1]*gamma + weight*energies[1];
       }

       if(stretch != 0) {
           double[] hp = vertices.get(0);

           double hfactor = getStretchFactor(hp, true);

           double[] head_forces = getHeadForce();

           Vx[0] = Vx[0] + hfactor * head_forces[0];
           Vy[0] = Vy[0] + hfactor * head_forces[1];

           double[] tail_forces = getTailForce();

           double[] tp = vertices.get(contourSize - 1);

           double tfactor = getStretchFactor(tp, false);

           Vx[contourSize - 1] = Vx[contourSize - 1] + tfactor * tail_forces[0];
           Vy[contourSize - 1] = Vy[contourSize - 1] + tfactor * tail_forces[1];
       }
       
   }
   
   /**
      * Gets a 3d vector that represents the force, without modification due to intensity.
      *@param ptA the point the force is too (length 3 double[])
      *@param ptB the point the force is from
      **/
   double[] getStretchForce(double[] ptA, double[] ptB){
        double[] force = new double[ptA.length];
        double mag = 0.;
        for(int i = 0; i<ptA.length; i++){
            force[i] = ptA[i] - ptB[i];
            mag += Math.pow(force[i],2);
        }
        
        double f = stretch/Math.sqrt(mag);
        
        for(int i = 0; i<ptA.length; i++){
            force[i] = force[i]*f;
        }

        return force;
   
   }
   
   double[] getHeadForce(){
        double[] force;
        int contourSize = vertices.size();
        if(vertices.size()>20){
       
           force = getStretchForce(vertices.get(0), vertices.get(20));
       
        } else{
           
           force = getStretchForce(vertices.get(0),vertices.get(contourSize/2));

           }

           return force;
   
   }
   
   double[] getTailForce(){
        double[] force;
        int contourSize = vertices.size();

        if(vertices.size()>20){
       
          force = getStretchForce(vertices.get(contourSize-1),vertices.get(contourSize-19));
           
        } else{
           
           force = getStretchForce(vertices.get(contourSize-1), vertices.get(contourSize/2));
           
        }
       
        return force;
   }

    /**
     * gets the stretch factor by assuming the point is infront of the 
     * current point, this prevents having the stretch overshoot the 
     * snake.
     **/
    double getStretchFactor(double[] ptA,boolean head){
        
        double[] pt = new double[2];
        double[] force;
        int contourSize = vertices.size();
        if(head){
            force = getStretchForce(vertices.get(0),vertices.get(1));
            pt[0] = ptA[0] + force[0]*squareSize/(2*stretch);
            pt[1] = ptA[1] + force[1]*squareSize/(stretch*2);
        }else{
            force = getStretchForce(vertices.get(contourSize-1), vertices.get(contourSize-2));
            pt[0] = ptA[0] + force[0]*squareSize/(2*stretch);
            pt[1] = ptA[1] + force[1]*squareSize/(stretch*2);
        }
        
        double factor;
        ImageProcessor image = imageEnergy.getProcessor();
        if(pt[0]>0&&pt[0]<image.getWidth()&&pt[1]>0&&pt[1]<image.getHeight())
            factor = (getMaxPixel(pt[0],pt[1]) - meanInt)/(foregroundIntensity - backgroundIntensity);
        else {
            factor = 0.;
        }
        return factor;
    }

}


