/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package snakeprogram;

import Jama.LUDecomposition;
import Jama.Matrix;
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
    
    /** The forgrount intensity will modify the resulting stretch force */
    double forInt;
    
    /** The background intensity */
    double backInt;
    
    
    public static int squareSize = 6;
    
    ImageEnergy IMAGE_ENERGY;
    
    double[][] matrixA;
    double meanInt;
    double MAX_SEGMENT_LENGTH = 1;
    TwoDDeformation(){
    
    }
    
    /**
       *    Creates a new 2-d deformation of this image.  
       **/
    TwoDDeformation(List<double[]> vertex_X, ImageEnergy ie){
        this.vertices = vertex_X;
        IMAGE_ENERGY = ie;
            
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
   public void setForInt(double forInt){
       this.forInt = forInt;
       meanInt = (forInt-backInt)*0.5 + backInt;
   }

   /** This needs to be set */
   public void setBackInt(double backInt){
       this.backInt = backInt;
      meanInt = (forInt-backInt)*0.5 + backInt;

   }

   /** No default value needs to be set for TwoDCurveDeformations */
   public void setStretch(double stretch)
   {
       this.stretch = stretch;
   }


   // This method solves a matrix equation to find new x and y coordinates for the points
   public void deformSnake(){

        int contourSize = vertices.size();
    
        //Vx and Vy represent the second term in the matrix equation
        double[] Vx = new double [contourSize];
        double[] Vy = new double [contourSize];
       

       energyWithGradient(Vx,Vy);
       
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
       for(int i = 0; i < contourSize; i++){
           vertices.add(new double[] { solutionX.get(i, 0), solutionY.get(i, 0)});
       }
   
   }
   
   
   //This method finds the distance between two points
   public static double pointDistance(double[] x1, double[] x2 ){
        double distance = 0;
        for(int i = 0; i<x1.length; i++)
            distance += Math.pow((x1[i] - x2[i]),2);
       return Math.sqrt(distance);
   }

   //This method interpolates to find the value of the point in between those two points
   public static double interpolate(double x1, double x2, double t){
       return (x1 + t*(x2 - x1));
   }

   /** Interpolates arrays of values using the above interpolation function */
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
        
        return IMAGE_ENERGY.getImageEnergy(x,y);
   
   }

     /**
        *
        *   This will use the class variables square size, and the blurred image to find
        *   the maximum pixel value in the square.  This is used for determining the head
        *   force on the filament.
        *   
        **/
    double getMaxPixel(double x, double y){
        
        return IMAGE_ENERGY.getMaxPixel(x,y);
    
    }
    
    /**
       *    Modifies the current dataset to interpolate points.  The different deformations have a different
       *    way to modify the points
       **/
    public abstract void addSnakePoints(double MAX_SEGMENT_LENGTH) throws java.lang.IllegalAccessException;
    
    /**
       *  Modifies Vx,Vy in place to find the forces it uses the image term and 
       *  the 'stretching' term.
       **/     
    public abstract void energyWithGradient(double[] Vx, double[] Vy);

    /**
       *    Initializes the double[][] array according to curve type
       **/
    public abstract void initializeMatrix();
}
