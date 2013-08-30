package snakeprogram3d;

import Jama.LUDecomposition;
import Jama.Matrix;

import java.util.ArrayList;

/**
 *This class deforms a three-d snake, and re-positions points so that they are equally 
 * spaced.
 *
 * @author Lisa Vasko, Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class ThreeDCurveDeformation implements ThreeDDeformation{

    ArrayList<double[]> vertices;

    double[][] matrixA;
    SnakeImages images;
    double alpha;
    double beta;
    double gamma;
    double weight;
    double stretch;
    double forInt;
    double backInt;
    double MAX_SEGMENT_LENGTH;

    /**
     * Orders the image data, and the segment length, verts will be rearranged.
     *
     * @param verts List of points that will be deformed
     * @param images 3D image data
     * @param max_seg maximum segment length, when the points are re-initialized.
     */
    ThreeDCurveDeformation(ArrayList<double[]> verts, SnakeImages images, double max_seg){
        MAX_SEGMENT_LENGTH = max_seg;
        this.vertices = verts;
        this.images=images;
                
    }
      
    
   public void setAlpha(double alpha){
        this.alpha=alpha;
    }

   public void setBeta(double beta){
       this.beta = beta;
    }

   public void setGamma(double gamma){
       this.gamma = gamma;
   }

   public void setWeight(double weight){
       this.weight = weight;
   }

   public void setForInt(double forInt){
       this.forInt = forInt;
   }

   public void setBackInt(double backInt){
       this.backInt = backInt;

   }

   public void setStretch(double stretch)
   {
       this.stretch = stretch;
   }

   
   /**
    * Solves the euler step.
    */
   public void deformSnake(){

        int contourSize = vertices.size();

        //Vx and Vy represent the second term in the matrix equation
        double[] Vx = new double [contourSize];
        double[] Vy = new double [contourSize];
        double[] Vz = new double [contourSize];

        energyWithGradient(Vx,Vy,Vz);

        Matrix vectorX, vectorY, vectorZ, solutionX, solutionY, solutionZ;

        //converts Vx and Vy to matrices so they can be multiplied
        vectorX = new Matrix(Vx, contourSize);
        vectorY = new Matrix(Vy, contourSize);
        vectorZ = new Matrix(Vz, contourSize);


        //converts the array initialized in the method initializeMatrix to a matrix and finds its inverse
        Matrix A = new Matrix(matrixA);

        LUDecomposition adecom = A.lu();

        solutionX = adecom.solve(vectorX);
        solutionY = adecom.solve(vectorY);
        solutionZ = adecom.solve(vectorZ);

        //resets verticesX and verticesY to the new values found in the solutionX and solutionY matrices
        for(int i = 0; i < contourSize; i++){
           vertices.set(i, new double[]{ solutionX.get(i, 0), solutionY.get(i, 0), solutionZ.get(i, 0)});
        }
   
   }
   
   
   /**
    * Finds the distance between two 3D points
    * @param p1 point one
    * @param p2 point two
    * @return
    */
   static double pointDistance(double[] p1, double[] p2){
       double distance = 0;
       double d;
       for(int i = 0; i<3; i++){
            d = p1[i]-p2[i];
            distance += d*d;
       }
        
       return Math.sqrt(distance);
   }

   /**
    * Linear Interpolation between two 1D points.
    *
    * @param x1
    * @param x2
    * @param t
    * @return the value t between x1 and x2
    */
   static double interpolate(double x1, double x2, double t){
       return (x1 + t*(x2 - x1));
   }

   /**
    * Arbitrarily dimensioned linear interpolation.
    * @param x1 first point
    * @param x2 second point
    * @param t weight
    * @return the interpolated values
    */
    static double[] interpolate(double[] x1, double[] x2, double t){
        double[] ret_val = new double[x1.length];
        for(int i = 0; i<x1.length; i++){
            ret_val[i] = interpolate(x1[i],x2[i],t);
        }
        
        return ret_val;
    }

   
   /**
      * Given the coordinate points this will calculate the image energy and return
      * the value as a double[] Ex Ey Ez.  It takes the gradient.
      **/
   double[] imageEnergy(double[] pt){
        
        double[] ret_value = new double[3];
        double x = pt[0];
        double y = pt[1];
        double z = pt[2];
        
     
        //gradient in x
        ret_value[0] = images.getPixel(x+0.5, y,z) - images.getPixel(x-0.5,y,z);
    
        //gradient in y
        ret_value[1] = images.getPixel(x,y+0.5,z) - images.getPixel(x,y-0.5,z);
    
        //gradient in z
        ret_value[2] = images.getPixel(x,y,z+0.5) - images.getPixel(x,y,z-0.5);
        
        return ret_value;
   
    }

   /*
    * Before solving the matrix this creates it using the alpha and betas.
    */
   public void initializeMatrix(){

       //finds the number of points currently on the snake
       int contourSize = vertices.size();
       double msl_2 = MAX_SEGMENT_LENGTH*MAX_SEGMENT_LENGTH;
       double alpha = this.alpha/ msl_2;
       double msl_4 = msl_2*msl_2;
       double beta = this.beta/ msl_4;
       
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
    * Replaces too many or two few points with equal distance points.
    *
    * @throws IllegalAccessException when the snake is too long.
    */
    public void addSnakePoints() throws IllegalAccessException{
        int pointListSize = vertices.size();
        
        
        
        double cumulativeDistance = 0;
        ArrayList<Double> cumulativeDistances = new ArrayList<Double>(pointListSize-1);
        double distanceValue;

        for(int i = 0; i < pointListSize-1; i++){
            cumulativeDistances.add(i, cumulativeDistance);
            distanceValue = pointDistance(vertices.get(i), vertices.get(i+1) );
            cumulativeDistance += distanceValue;
        }

        cumulativeDistances.add(cumulativeDistance);

        int newPointListSize = (int)(cumulativeDistance/MAX_SEGMENT_LENGTH);
        
        if(newPointListSize>SnakeApplication.MAXLENGTH)
            throw new IllegalAccessException("" + newPointListSize);
            
        double segmentLength = cumulativeDistance/(double)newPointListSize;

        int i = 0;
        int i_last = pointListSize - 1;

        ArrayList<double[]> newVertices = new ArrayList<double[]>(newPointListSize);

        newVertices.add(i, vertices.get(i));


        for(int s = 1; s < newPointListSize; s++){
            while(cumulativeDistances.get(i) < s*segmentLength){
                i_last = i;
                //i = (i+1)%(pointListSize+1);
                i++;
            }
            double t = (s*segmentLength - cumulativeDistances.get(i_last))/(cumulativeDistances.get(i) - cumulativeDistances.get(i_last));
            
            newVertices.add(s, interpolate(vertices.get(i_last), vertices.get(i), t));

        }

        newVertices.add(vertices.get(pointListSize-1));
        
        
        vertices.clear();
        vertices.addAll(newVertices);
        
        if(vertices.size()>2)
            initializeMatrix();
    }
    /**
     *  Modifies Vx,Vy in place to find the forces it uses the image term and 
     *  the 'stretching' term.
     **/
     
    public void energyWithGradient(double[] Vx, double[] Vy, double[] Vz){

        double[] current;
        int contourSize = Vx.length;
         
       for(int i = 0; i < contourSize; i++){
           current = vertices.get(i);
           
           double[] energies = imageEnergy(current);



           Vx[i] = current[0]*gamma + weight*energies[0];
           Vy[i] = current[1]*gamma + weight*energies[1];
           Vz[i] = current[2]*gamma + weight*energies[2];
       }


        double[] pt = vertices.get(0);
        
        double[] head_forces = getHeadForce();
        
        double factor = stretch*getStretchFactor(pt, head_forces);

        Vx[0] = Vx[0] + factor*head_forces[0];
        Vy[0] = Vy[0] + factor*head_forces[1];
        Vz[0] = Vz[0] + factor*head_forces[2];
        

        double[] tail_forces = getTailForce();
        
        pt = vertices.get(contourSize-1);
        
        factor = stretch*getStretchFactor(pt, tail_forces);
        
        Vx[contourSize - 1] = Vx[contourSize - 1] + factor*tail_forces[0];
        Vy[contourSize - 1] = Vy[contourSize - 1] + factor*tail_forces[1];
        Vz[contourSize - 1] = Vz[contourSize - 1] + factor*tail_forces[2];
       
   }
   
   /**
      * Gets a 3d vector that represents the force, without modification due to intensity.  
      * returns a normalized vector
      *@param ptA the point the force is too (length 3 double[])
      *@param ptB the point the force is from
      **/
   double[] getStretchForce(double[] ptA, double[] ptB){
        double[] force = new double[ptA.length];
        double mag = 0.;
        for(int i = 0; i<ptA.length; i++){
            force[i] = ptA[i] - ptB[i];
            mag += force[i]*force[i];
        }
        
        double f = 1./Math.sqrt(mag);
        
        for(int i = 0; i<ptA.length; i++){
            force[i] = force[i]*f;
        }

        return force;
   
   }

   /**
    * gets a normalized head force for the first and 10th point.
    * or the mid point if the snake if it is less than 10 elements long.
    *
    * @return a normalized vector.
    */
   double[] getHeadForce(){
        double[] force;
        int contourSize = vertices.size();
        if(vertices.size()>10){
       
           force = getStretchForce(vertices.get(0), vertices.get(10));
       
        } else{
           
           force = getStretchForce(vertices.get(0),vertices.get(contourSize/2));

           }

           return force;
   
   }

   /**
    * gets a normalized tail force for the last and 10th point from the last,
    * or the mid point if the snake if it is less than 10 elements long.
    *
    * @return a normalized vector.
    */
   double[] getTailForce(){
        double[] force;
        int contourSize = vertices.size();

        if(vertices.size()>10){
       
          force = getStretchForce(vertices.get(contourSize-1),vertices.get(contourSize-9));
           
        } else{
           
           force = getStretchForce(vertices.get(contourSize-1), vertices.get(contourSize/2));
           
        }
       
        return force;
   }

        
    /**
     * Given the factor and the direction this calculates the stretch for based on
     * where the snake is expected to go.
     *
     * @param pt the position of the tip.
     * @param direction the direction the snake is pointing.
     * @return a factor for the force.
     */
    double getStretchFactor(double[] pt, double[] direction){
        double factor;
        //if(pt[0]>0&&pt[1]<images.getWidth()&&pt[1]>0&&pt[1]<images.getHeight()&&pt[2]>0&&pt[2]<images.getDepth())
        if(pt[0]>0&&pt[0]<images.getWidth()&&pt[1]>0&&pt[1]<images.getHeight()){
            double mean = (forInt + backInt)/2.;
            factor = (images.getMaxPixel(pt, direction) - mean)/(forInt - mean);
        }else {
            factor = 0.;
        }
        return factor;
    }
   
}


