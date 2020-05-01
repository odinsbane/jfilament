/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package snakeprogram;

import Jama.LUDecomposition;
import Jama.Matrix;
import snakeprogram.energies.ExternalEnergy;
import snakeprogram.energies.ImageEnergy;

import java.util.ArrayList;
import java.util.List;

/**
   *    This class deforms snakes.  You create an instance of this class either a TwoDContourDeformation or a TwoDCurveDevormation
   *    and set the relevant parameter, then you can deform the snake.  This modifies the X-Y data inplace.
   *    
   *    
   * @author Lisa,Matt
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
   **/
public abstract class TwoDDeformation {
    
    List<double[]> vertices;

    /** This is the spring constant for the stretch term*/
    double alpha;
    
    /** Bending term */
    double beta;
    
    /** The step size (bigger is smaller steps) */
    double gamma;
    
    /** The image weight */
    double weight;
    
    /** This is the magnitude of the for applied at the ends (when applicable) */
    double stretch;

    /** Modifies the stretch / balloon force.*/
    double foregroundIntensity;
    
    /** The background intensity */
    double backgroundIntensity;
    
    boolean fixedFront = false;
    boolean fixedBack = false;

    public static int squareSize = 6;
    
    ImageEnergy imageEnergy;
    
    double[][] matrixA;
    double meanInt;
    double MAX_SEGMENT_LENGTH = 1;

    List<ExternalEnergy> externalEnergies = new ArrayList<>();

    /**
     * Prepares a deformation routine for the points provided.
     *
     * @param points points that will be deformed.
     *
     * @param ie the image that the points will be deformed to. additional
     *           external energies can be added, but this will be used to determine whether
     *           a stretch force, or ballooning force will be applied.
     */
    TwoDDeformation(List<double[]> points, ImageEnergy ie){
        this.vertices = points;
        imageEnergy = ie;
            
    }
      
    /** Set this before using */
     public void setAlpha(double alpha){
        this.alpha=alpha;
}

   /** Needs to be set */
   public void setBeta(double beta){
       this.beta = beta;
    }

   /** Needs to be set */
   public void setGamma(double gamma){
       this.gamma = gamma;
   }

   /** This needs to be set*/
   public void setWeight(double weight){
       this.weight = weight;
   }

   /** This needs to be set */
   public void setForegroundIntensity(double foregroundIntensity){
       this.foregroundIntensity = foregroundIntensity;
       meanInt = (foregroundIntensity - backgroundIntensity)*0.5 + backgroundIntensity;
   }

   /** This needs to be set */
   public void setBackgroundIntensity(double backgroundIntensity){
       this.backgroundIntensity = backgroundIntensity;
      meanInt = (foregroundIntensity - backgroundIntensity)*0.5 + backgroundIntensity;

   }

   /** No default value needs to be set for TwoDCurveDeformations */
   public void setStretch(double stretch)
   {
       this.stretch = stretch;
   }

   public void addExternalEnergy(ExternalEnergy erg){
       externalEnergies.add(erg);
   }

   // This method solves a matrix equation to find new x and y coordinates for the points
   public void deformSnake() {
       int contourSize = vertices.size();

       //Vx and Vy represent the second term in the matrix equation
       double[] Vx = new double[contourSize];
       double[] Vy = new double[contourSize];


       energyWithGradient(Vx, Vy);

       for (ExternalEnergy energy : externalEnergies) {

           for (int i = 0; i < vertices.size(); i++) {
               double[] pos = vertices.get(i);
               double[] f = energy.getForce(pos[0], pos[1]);
               Vx[i] += f[0];
               Vy[i] += f[1];
           }
       }
       if (fixedFront) {

           matrixA[0][0] = matrixA[0][0] - gamma + 1e12;

           Vx[0] = 1e12 * vertices.get(0)[0];
           Vy[0] = 1e12 * vertices.get(0)[1];

       }

       if (fixedBack) {

           int last = matrixA.length - 1;
           matrixA[last][last] = matrixA[last][last] - gamma + 1e12;

           if (last == Vx.length) {
               System.out.println("how?");
           }

           Vx[last] = 1e12 * vertices.get(last)[0];
           Vy[last] = 1e12 * vertices.get(last)[1];
       }

       Matrix vectorX, vectorY, solutionX, solutionY;

       //converts Vx and Vy to matrices so they can be multiplied
       vectorX = new Matrix(Vx, contourSize);
       vectorY = new Matrix(Vy, contourSize);


       //converts the array initialized in the method initializeMatrix to a matrix and finds its inverse
       Matrix A = new Matrix(matrixA);

       LUDecomposition adecom = A.lu();

       solutionX = adecom.solve(vectorX);
       solutionY = adecom.solve(vectorY);

       //resets verticesX and verticesY to the new values found in the solutionX and solutionY matrices
       vertices.clear();
       for (int i = 0; i < contourSize; i++) {
           vertices.add(new double[]{solutionX.get(i, 0), solutionY.get(i, 0)});
       }

   }


    /**
     * Calculates the distance between two points.
     *
     * @param x1 first point
     * @param x2 second point
     * @return |x1 - x2|
     */
   public static double pointDistance(double[] x1, double[] x2 ){
        double distance = 0;
        for(int i = 0; i<x1.length; i++){
            double dsi = x1[i] - x2[i];
            distance += dsi*dsi;
        }
       return Math.sqrt(distance);
   }

    /**
     * One-dimensional linear interpolation.
     *
     * @param x1 first point
     * @param x2 second point
     * @param t fraction of position between two points. t=0 is x1 and t=1 is x2
     * @return t*(x2 - x1) + p1
     */
    public static double interpolate(double x1, double x2, double t){
       return (x1 + t*(x2 - x1));
   }

    /**
     * Multi-dimensional linear interpolation.
     *
     * @param p1 first point
     * @param p2 second point
     * @param t fraction of position between two points.
     * @return t*(p2 - p1) + p1
     */
   public static double[] interpolate(double[] p1, double[] p2, double t){
        double[] ret_value = new double[p1.length];
        for(int i = 0; i<p1.length; i++)
            ret_value[i] = interpolate(p1[i],p2[i],t);
        return ret_value;
   }
   
   /**
      * Given the coordinate points this will calculate the image energy and return
      * the value as a double[] Ex Ey
      **/
   double[] imageEnergy(double x, double y){
        
        return imageEnergy.getImageEnergy(x,y);
   
   }

   public void setFixedEndPoints(boolean v){
       fixedBack = v;
       fixedFront = v;
   }

   public void setFixedFront(boolean v){
       fixedFront = v;
   }

   public void setFixedBack(boolean v){
       fixedBack = v;
   }

     /**
        *
        *   This will use the class variables square size, and the blurred image to find
        *   the maximum pixel value in the square.  This is used for determining the head
        *   force on the filament.
        *   
        **/
    double getMaxPixel(double x, double y){
        
        return imageEnergy.getMaxPixel(x,y);
    
    }

    /**
     * For normalizing the distance between points. There are two implementations of this,
     * for open snakes and connected snakes.
     *
     * @param MAX_SEGMENT_LENGTH
     * @throws TooManyPointsException
     * @throws InsufficientPointsException
     */
    public abstract void addSnakePoints(double MAX_SEGMENT_LENGTH) throws TooManyPointsException, InsufficientPointsException;
    
    /**
     *  Accumulates forces due to image energy includes the 'stretching' term.
     *
     * @param fx
     * @param fy
     */
    public abstract void energyWithGradient(double[] fx, double[] fy);

    /**
       *    Initializes the double[][] array according to curve type
       **/
    public abstract void initializeMatrix() throws InsufficientPointsException;
}
