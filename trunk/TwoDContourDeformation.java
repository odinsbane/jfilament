package snakeprogram;

import java.util.ArrayList;

/**
 *  This type of curve is attached at the two end points and forms a closed loop
 *  
 * @author Lisa Vasko, Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class TwoDContourDeformation extends TwoDDeformation{
    
    /**
       *
       */
     public TwoDContourDeformation(ArrayList<double[]> vertex_X, ImageEnergy ie){
         super(vertex_X, ie);
     }
     
   /**
      *    This method intializes an array which will become the A matrix using the values of alpha, beta and gamma
      *    as well as the number of points in verticesX and verticesY
      **/

   public void initializeMatrix(){

        //finds the number of points currently on the snake
        int contourSize = vertices.size();
        double[][] array = new double[contourSize][contourSize];
       
        double alpha = this.alpha/ Math.pow(MAX_SEGMENT_LENGTH,2);
        double beta = this.beta/ Math.pow(MAX_SEGMENT_LENGTH,4);

        double[] pentadiagonal_coefficients = new double[]{
               beta,
               -(alpha + 4*beta),
               2*(alpha + 3*beta) + gamma,
               -(alpha + 4*beta),
               beta
           };

        //initializes the array
        for(int i = 0; i < contourSize; i++){

           for(int k = 0; k<5; k++){
               int n = i - 2 + k;

               n = n<0? contourSize+n:n;
               n = n>=contourSize? n - contourSize : n;

               array[i][n] = array[i][n] + pentadiagonal_coefficients[k];
           }

        }

        matrixA = array;
       
   }

    /**
       *    Calculates only the image energy the closed contour
       *    does not have any other terms
       */
  public void energyWithGradient(double[] Vx, double[] Vy){

        double[] current;
        int contourSize = Vx.length;
         
       for(int i = 0; i < contourSize; i++){
           current = vertices.get(i);
           
           double[] energies = imageEnergy(current[0],current[1]);


           Vx[i] = current[0]*gamma + weight*energies[0];
           Vy[i] = current[1]*gamma + weight*energies[1];
       }
       
   }
    /**
       *    Interpolates the points that are too far apart, and removes
       *    points that are too close together.  Includes the connection
       *    between the two end points
       */
    public void addSnakePoints(double msl) throws IllegalAccessException{
        this.MAX_SEGMENT_LENGTH = msl;
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

        int newPointListSize = (int)(cumulativeDistance/MAX_SEGMENT_LENGTH);
        
        if(newPointListSize>SnakeModel.MAXLENGTH)
            throw new IllegalAccessException("" + pointListSize);
            
            
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
        
        initializeMatrix();
    }


}
