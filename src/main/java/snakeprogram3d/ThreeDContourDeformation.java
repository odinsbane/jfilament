package snakeprogram3d;

import Jama.LUDecomposition;
import Jama.Matrix;

import java.util.ArrayList;
import java.util.List;

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
public class ThreeDContourDeformation  implements ThreeDDeformation{

    List<double[]> vertices;

    double[][] matrixA;
    SnakeImages images;
    double alpha;
    double beta;
    double gamma;
    double weight;
    double MAX_SEGMENT_LENGTH;

    /**
     * Orders the image data, and the segment length, verts will be rearranged.
     *
     * @param verts points of the snake
     * @param images image data of the snake
     * @param max_seg maximum size of a segment, otherwise it will be subsampled.
     */
    ThreeDContourDeformation(List<double[]> verts, SnakeImages images, double max_seg){
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
   }

   public void setBackInt(double backInt){
   }

   public void setStretch(double stretch)
   {
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

               n = n<0? contourSize+n:n;
               n = n>=contourSize? n - contourSize : n;


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


        ArrayList<Double> cumulativeDistances = new ArrayList<Double>(pointListSize-1);

        double cumulativeDistance = 0;
        cumulativeDistances.add(cumulativeDistance);
        double distanceValue;

        for(int i = 0; i < pointListSize; i++){
            distanceValue = ThreeDCurveDeformation.pointDistance(vertices.get(i), vertices.get((i+1)%pointListSize));
            cumulativeDistance += distanceValue;
            cumulativeDistances.add(i, cumulativeDistance);
        }


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
                i = (i+1)%(pointListSize+1);
            }
            double t = (s*segmentLength - cumulativeDistances.get(i_last))/(cumulativeDistances.get(i) - cumulativeDistances.get(i_last));
            
            newVertices.add(s, ThreeDCurveDeformation.interpolate(vertices.get(i_last), vertices.get(i%pointListSize), t));

        }
        /*
        for(int s = 1; s < newPointListSize; s++){
            while(cumulativeDistances.get(i) < s*segmentLength){
                i_last = i;
                i = (i+1)%(pointListSize+1);
            }
            double t = (s*segmentLength - cumulativeDistances.get(i_last))/(cumulativeDistances.get(i) - cumulativeDistances.get(i_last));
            newVertices.add(s, interpolate(vertices.get(i_last), vertices.get(i%pointListSize), t));
        }

         */
        newVertices.add(vertices.get(pointListSize-1));
        
        
        vertices.clear();
        vertices.addAll(newVertices);
        
        if(vertices.size()>2)
            initializeMatrix();
    }
    /**
     *  Modifies Vx,Vy in place to find the forces it uses the image term and 
     *  the 'stretching' term.
     *
     * @param Vx forces in x direction
     * @param Vy "" "" in y direction
     * @param Vz "" "" in z direction.
     */
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

   }
   



   
}


