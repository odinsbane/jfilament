package snakeprogram3d;

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
public interface ThreeDDeformation {

   public void setAlpha(double alpha);

   public void setBeta(double beta);

   public void setGamma(double gamma);

   public void setWeight(double weight);

   public void setForInt(double forInt);

   public void setBackInt(double backInt);

   public void setStretch(double stretch);


   /**
    * Solves the euler step.
    */
   public void deformSnake();

   public void initializeMatrix();
 
   /**
    * Replaces too many or two few points with equal distance points.
    *
    * @throws IllegalAccessException when the snake is too long.
    */
    public void addSnakePoints() throws IllegalAccessException;

}


