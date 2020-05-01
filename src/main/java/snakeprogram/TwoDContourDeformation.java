package snakeprogram;

import snakeprogram.energies.ImageEnergy;

import java.util.ArrayList;
import java.util.List;

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
    boolean clockWise;
    /**
       *
       */
     public TwoDContourDeformation(List<double[]> vertices, ImageEnergy ie){
         super(vertices, ie);
         clockWise = determineClockWise(vertices);
     }

    /**
     * Calculates the area and checks the sign to see if the curve is wound clockwise or counterclock wise.
     * ref Bourke, Paul (July 1988). "Calculating The Area And Centroid Of A Polygon". Retrieved 6-Feb-2013.
     *
     * @param points the closed curve defined by the points.
     * @return x,y area
     */
     public static boolean determineClockWise(List<double[]> points){
         double area = 0;
         int n = points.size();
         for(int i =0; i<points.size(); i++){
             double[] a = points.get(i);
             double[] b = points.get((i+1)%n);

             double chunk = a[0]*b[1] - a[1]*b[0];
             area += chunk;
         }
         return area > 0;
     }
     
   /**
      *    This method intializes an array which will become the A matrix using the values of alpha, beta and gamma
      *    as well as the number of points in verticesX and verticesY
      **/

   public void initializeMatrix() throws InsufficientPointsException{

        //finds the number of points currently on the snake
        int contourSize = vertices.size();
        if(contourSize <= 1){
            throw new InsufficientPointsException("cannot create matrix with contour size: " + contourSize);
        }
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
     * Calculates the force due to the image energy. Applies the gamma term,
     * Also calculates a 'balloon' force that causes the contour to expand when it is
     * above the foreground intensity, and shrink when it is below the background intensity.
     *
     * @param fx force in the x direction.
     * @param fy force in the y direction.
     */
  public void energyWithGradient(double[] fx, double[] fy){

        double[] current;
        int contourSize = fx.length;
         
       for(int i = 0; i < contourSize; i++){
           current = vertices.get(i);
           
           double[] energies = imageEnergy(current[0],current[1]);


           fx[i] = current[0]*gamma + weight*energies[0];
           fy[i] = current[1]*gamma + weight*energies[1];
       }

       if(stretch != 0){
           applyBalloonForce( fx, fy );
       }
       
   }

    double[] getNormal(int index){
        int li = index - 1;
        if(li<0) li = vertices.size() + li;
        int hi = (index + 1)%vertices.size();

        double[] lower = vertices.get(li);
        double[] higher = vertices.get(hi);
        double dx = higher[0] - lower[0];
        double dy = higher[1] - lower[1];

        double m = Math.sqrt(dx*dx + dy*dy);
        dx = dx/m;
        dy = dy/m;
        if(!clockWise){
            dx = -dx;
            dy = -dy;
        }
        return new double[]{ -dy, dx};
    }


    /**
     * Balloon values based on the maximum intensity, and pushes the shape out from the center of mass. The foreground
     * intensity is used as an expanding region, and the background is used as a compressing region. The region
     * between is non-interacting.
     *
     * @param index vertex index that balloon force will be checked at.
     * @return two-d force at x corresponding to a balloon force.
     */
    public double[] balloonForce(int index){
        double[] loc = vertices.get(index);
        double x = loc[0];
        double y = loc[1];

        double v = getMaxPixel(x, y);
        if( (v >= foregroundIntensity) ){

            double[] normal = getNormal(index);

            double factor = -stretch;

            return new double[]{
                    factor*normal[0],
                    factor*normal[1]
            };
        } else if (v <= backgroundIntensity){
            double[] normal = getNormal(index);
            double factor = stretch;

            return new double[]{
                    factor*normal[0],
                    factor*normal[1]
            };
        }
        return new double[]{0,0};
    }

   public void applyBalloonForce(double[] fx, double[] fy){
        for(int i = 0; i<vertices.size(); i++){
            double[] v = balloonForce(i);
            fx[i] += v[0];
            fy[i] += v[1];
        }
   }

    /**
       *    Interpolates the points that are too far apart, and removes
       *    points that are too close together.  Includes the connection
       *    between the two end points
       */
    public void addSnakePoints(double msl) throws TooManyPointsException, InsufficientPointsException{
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
            throw new TooManyPointsException("" + newPointListSize);
            
            
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
