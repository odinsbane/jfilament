package snakeprogram;
/**
   *    This class stores all of the state data and variables, it will make the decisions and final call.  It is designed to be the immediate
   *    executioner of ui implemented function calls.
 * @author Lisa Vasko, Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
   **/

import ij.ImagePlus;
import ij.gui.GenericDialog;
import snakeprogram.interactions.*;

import javax.swing.*;
import java.awt.Polygon;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
// Adri 07/03/2013  (in order to use Arrays class)
import java.util.*;

public class SnakeModel{

    public static int MAXLENGTH = 1500;

    private SnakeImages images;             //Contains all of the image data
    private SnakeFrame snake_panel;        //Contians the controls
    
    /**These contain the currently displayed snake data. x and y values*/
    private ArrayList<double[]> SnakeRaw;
    //ADRI 08/01/2013
    private ArrayList<int[]> xyCoord;
    private Snake CurrentSnake;
    /**The deformation for an open deformation*/
    private TwoDDeformation curveDeformation;

    
    //state variables.
    //private boolean zoomInInitialize = false;     //Zoom intialization during mouse clicks
    //private int zoomCounter = 0;                      //Zoom has multiple stages.
    //private boolean bZoomInBox = false;          //determines if a box should be drawn on the image while moving the mouse
    private boolean RUNNING = false;               //If the snakeDeformation is running.
    private boolean INTERRUPT = false;
    private boolean TRACK = false;

    //Deformation values.  These are maintained here for now, but should be moved in the near future
    private double alpha = 15;
    private double circleradius = 11;
    private double beta = 10;
    private double gamma = 40;
    private double weight = 0.5;
    private double stretch = 150;
    private double forIntMean = 255;
    private double backIntMean = 0;

    // Adri 21 dic 2012 
    // These two variables added in order to allow drawing of a snake linked to a snake in the previous frame (Continue Snake button)
    public Snake previousSnake;
    public boolean addToPrevious; 
 
    // Adri 26 dic 2012 
    // Variables for deformallsnakes method (we want to control order of execution)
    public boolean delflag;
    public int snakecycle;
    public int deformedsnakes = 0;
    
    // Adri 02 gen 2013
    public ArrayList<int[]> SnakeInts;
    
    
    //Model Values 
    public static int squareSize = 3;            //This is the size of the square that averaging is performed over for the 'getIntForMean' ops
    private int deformIterations = 100;          //Number of iterations when a 'deformSnake' is clicked
    private double IMAGE_SIGMA = 1.01;          //For bluring
    public double MAXIMUM_SPACING = 1.;

    
    
    //What to do with the tracking data!?
    private MultipleSnakesStore SnakeStore;
    private SnakeInteraction interactor;

    
    
    /**
       *    Starts the snakes application.
       **/
    SnakeModel(){
        SnakeStore = new MultipleSnakesStore();
        images = new SnakeImages(SnakeStore);
        snake_panel = new SnakeFrame(this);
    }

    
    /**  26 Dic 2012 Modified by Adri
     *   This method is called by the deformButton listener. The method
     *   initializes an object of type TwoDContourDeformation using the three-argument
     *   constructor. The object, contourDeformation, then calls the deformSnake() method.
     *   That method finds new x and y coordinates for the given points in order to make the
     *   snake fit the curve better. DeformSnakeButtonActionPerformed() then redraws the image
     *   in the panel given these new coordinates. 
     *
     * @throws IllegalAccessException when the snake has too many points.
     **/
 public void deformRunningMod(Snake snak) throws java.lang.IllegalAccessException{
     
     snake_panel.initializeProgressBar();

     SnakeRaw = snak.getCoordinates(images.getCounter());
     
     if(snak.TYPE==Snake.CLOSED_SNAKE)
         curveDeformation = new TwoDContourDeformation(SnakeRaw, energyFactory() );
     else
         curveDeformation = new TwoDCurveDeformation(SnakeRaw, energyFactory() );
     
     resetDeformation();
     //INTERRUPT=false;
     for(int j = 0; j<deformIterations; j++){
        
         //resets the value of the progress bar based on the iteration number
         int value = (int)(((j+1)*1.0/deformIterations)*100);
         snake_panel.updateProgressBar(value);
         
         curveDeformation.addSnakePoints(MAXIMUM_SPACING);
         curveDeformation.deformSnake();
         updateImagePanel();
         if(INTERRUPT)
             break;
     }

   RUNNING = false;
   enableUI();
 }
    
    
    
     /**    
        *   This method is called by the deformButton listener. The method
        *   initializes an object of type TwoDContourDeformation using the three-argument
        *   constructor. The object, contourDeformation, then calls the deformSnake() method.
        *   That method finds new x and y coordinates for the given points in order to make the
        *   snake fit the curve better. DeformSnakeButtonActionPerformed() then redraws the image
        *   in the panel given these new coordinates. 
        *
        * @throws IllegalAccessException when the snake has too many points.
        **/
    public void deformRunning() throws java.lang.IllegalAccessException{
        
    	if(!TRACK) snake_panel.initializeProgressBar();

        SnakeRaw = CurrentSnake.getCoordinates(images.getCounter());
        
        if(CurrentSnake.TYPE==Snake.CLOSED_SNAKE)
            curveDeformation = new TwoDContourDeformation(SnakeRaw, energyFactory() );
        else
            curveDeformation = new TwoDCurveDeformation(SnakeRaw, energyFactory() );
        
        resetDeformation();
        INTERRUPT=false;
        for(int j = 0; j<deformIterations; j++){
           
            //resets the value of the progress bar based on the iteration number
        	int value = (int)(((j+1)*1.0/deformIterations)*100);
            /**GD_Debug Temporary, just to show results in a GUI:
            GenericDialog gaedn22 = new GenericDialog("Workingframe");
            gaedn22.addNumericField("value: ", value, 0);
            gaedn22.showDialog();
            /**GD_Debug*/
            if(!TRACK) snake_panel.updateProgressBar(value);
            curveDeformation.addSnakePoints(MAXIMUM_SPACING);
            curveDeformation.deformSnake();
            //This is to update image and not progressbar
            if(!TRACK) updateImagePanel();
            if(INTERRUPT)
                break;
        }

      RUNNING = false;
      enableUI();
    }
    
    
    public ImageEnergy energyFactory(){
        
        if(snake_panel.getEnergyType())
            return new IntensityEnergy( images.getProcessor(),IMAGE_SIGMA );
        else
            return new GradientEnergy( images.getProcessor(),IMAGE_SIGMA );

    }
    

    

    
    
    /*****************************************************************/
    //                  PANEL ACTIONS
    /*****************************************************************/
    

    /**
     * For use with interacting with the image panel. Disables the panel and allows for clicking.
     *
     * @param si
     */
    public void registerSnakeInteractor(SnakeInteraction si){
        disableUI();
        snake_panel.image_panel.addMouseListener(si);
        snake_panel.image_panel.addMouseMotionListener(si);
        interactor = si;
    }

    /**
     * Presumes the interaction is over and enables the gui.
     *
     * @param si
     */
    public void unRegisterSnakeInteractor(SnakeInteraction si){
        if(interactor!=si) System.out.println("and error has occured");
        snake_panel.image_panel.removeMouseListener(si);
        snake_panel.image_panel.removeMouseMotionListener(si);
        interactor = null;
        enableUI();


    }

    
    /**
       *    When initializing a snake this will add points a right click will end
       *    the process and re-enable the UI.
       **/
    private void initializeSnakeClicked(MouseEvent evt){


    }
    
    /**
       *    Finds the location of the current mouse click and finds the 
       *    mean intensity about that point.  Then sets the value for 
       *    background intensity, and updates the UI.
       **/
    private void getBackgroundClicked(MouseEvent evt){

    }
    


    
    /**
       *    Chooses the nearest snake.  This method is called
       *    by another listener added by the snake_frame which is
       *    enabled or disable along with the rest of the GUI.
       *
       **/
    public void selectSnake(MouseEvent evt){
        double[] p = { images.fromZoomX(evt.getX()), images.fromZoomY(evt.getY())};
        
        double min = 1e6;
        
        int frame = images.getCounter();
        
        for(Snake s: SnakeStore){
            double distance = 1e6;
            if(s.exists(frame)){
                ArrayList<double[]> cx = s.getCoordinates(frame);
                int size = s.getSize(frame);
                for(int i = 0; i<size;i++){
                    double cd = TwoDDeformation.pointDistance(p,cx.get(i));
                    distance = distance>cd?cd:distance;
                }
            }
            if(distance<min){
                min = distance<min?distance:min;
                CurrentSnake = s;
            }
            if(min<1)
                break;
        }
        updateImagePanel();

    }
    
    /**
      *     Find the closest point on the current snake, this is for
      *     locating the point that will be deleted
      * 
      **/
    public double[] findClosestPoint(double x, double y){
        
        double[] pt = { x, y};
        double min = 1e6;
        double[] result = {0,0};
        for(double[] spt: CurrentSnake.getCoordinates(images.getCounter())){
            double d = pointDistance(pt,spt);
            if(d<min){
                result = spt;
                min = d;
            }
        }
        return result;
        
    }
    
    /**
      *     Find the closest end point on the snake to highlight
     *
     * @param x coordiante
     * @param y coordinate
     * @return
     */
    public double[] findClosestEnd(double x, double y){
        ArrayList<double[]> all = CurrentSnake.getCoordinates(images.getCounter());
        double[] hpt = all.get(0);
        double[] tpt = all.get(all.size()-1);
        double[] pt = { x, y};
        
        return (pointDistance(pt,hpt)>pointDistance(pt,tpt))?tpt:hpt;
        
    }
    
    /*****************************************************************/
    //                  BUTTON ACTIONS
    /*****************************************************************/
    
    /**
       *    Save all of the snakes elongation data
       *    
       **/
    public void saveElongationData(){
        disableUI();
        HashMap<String,Double> values = snake_panel.getConstants();
        SnakeIO.writeSnakeElongationData(getFrame(),values,SnakeStore,images.getStackSize());
        enableUI();
    }
    
    /**
       *    Save all of the snakes data, so that they may be reloaded
       *    
       **/
    public void saveSnake(){
        disableUI();
        HashMap<String,Double> values = snake_panel.getConstants();
        SnakeIO.writeSnakes(getFrame(),values,SnakeStore);
        enableUI();
    }
    
    /**
       *    Loads a snake from a file and sets the constant values in 
       *    the snake panel.  Uses the original snake_panel values
       *    so that any missing constants will be ignored.
       *    
       **/
    public void loadSnake(){
        disableUI();
        HashMap<String,Double> values = snake_panel.getConstants();
        MultipleSnakesStore ss = SnakeIO.loadSnakes(getFrame(),values);
        snake_panel.setConstants(values);
        if(ss != null){
            SnakeStore = ss;
            images.setSnakes(SnakeStore);
            CurrentSnake = SnakeStore.getLastSnake();
        }
        enableUI();
        updateImagePanel();
    }
    
    /**   
       *    Goes to the next frame and copies the current snake.  
       *    then deforms that snake.
       *
       **/
    public void trackSnakes(){
    
        if(checkForCurrentSnake()){
            int frame = images.getCounter();
            
            ArrayList<double[]> Xs = new ArrayList<double[]>(CurrentSnake.getCoordinates(frame));
                    
            nextImage();

            CurrentSnake.addCoordinates(images.getCounter(),Xs);
            
            updateImagePanel();
            deformSnake();
        }
    }
    
    /**   ADRI 09/01/2013 Modified Method
     *    Exactly as original trackSnakes() method
     *    Goes to the next frame and copies the current snake.  
     *    then deforms that snake.
     *    But Unlike the original, the frame is given as an argument.
     *    Obviously serial.
     *
     **/
  public void trackSnakesMod(int workingframe){
	  gotoImage(workingframe);
      /**GD_Debug Temporary, just to show results in a GUI:
      GenericDialog gaedn = new GenericDialog("Workingframe");
      gaedn.addNumericField("workingframe: ", workingframe, 0);
      gaedn.addNumericField("images.getCounter(): ", images.getCounter(), 0);
      gaedn.showDialog();
      //GD_Debug*/
      if(checkForCurrentSnake()){
          int frame = workingframe;
          ArrayList<double[]> Xs = new ArrayList<double[]>(CurrentSnake.getCoordinates(frame));
          nextImage();
 
    

          CurrentSnake.addCoordinates(images.getCounter(),Xs);
          if(!TRACK) updateImagePanel();
          //Thread y = new Thread(){
          //    public void run(){
                  deformSnakeNoThread();
         //     }
          //};
          //y.start();
          //try {
          //    y.join();    
          //} catch (InterruptedException e) {
          //    e.printStackTrace();
         // }
          //http://stackoverflow.com/questions/1908515/java-how-to-use-thread-join
          //http://stackoverflow.com/questions/4691533/java-wait-for-thread-to-finish
          }
  }
    
    /**   ADRI 09/01/2013 New Method
     *    Goes to the last frame of a snakes and performs tracking in next frame.
     *    Then goes on until last frame in which snake is trackable.
     **/
  public void trackSnakeAllFrames() {
          int frame = CurrentSnake.getLastFrame();          
          /**GD_Debug Temporary, just to show results in a GUI:
          GenericDialog gadn = new GenericDialog("flagTrackSnake");
          gadn.addNumericField("flagTrackSnake: ", flagTrackSnake.length, 0);
          gadn.showDialog();
          //GD_Debug*/
          // Adri 27 jun 2013 I add progressbar to the process.
          snake_panel.initializeProgressBar();
          // Adriano 28 jun 2013
          // I add the boolean condition because if we are having tracking then I don't calculate 
          // progressbar stuff in method deformrunning and things go faster.
          TRACK = true;
          double b = images.getStackSize();
          for (int i=frame;i<=images.getStackSize();i++){
       		int value = (int)(i/b*100); //b is a double so we have decimal division
       		/**GD_Debug Temporary, just to show results in a GUI:
            GenericDialog gaedn22 = new GenericDialog("Workingframe");
            gaedn22.addNumericField("value: ", value, 0);
            gaedn22.addNumericField("division: ", i/images.getStackSize(), 0);
            gaedn22.showDialog();
            /**GD_Debug*/
    	    
    	        	  //if(CurrentSnake.exists(i)){
            ////snake_panel.updateProgressBar(value); 
            ////updateImagePanel();
                		  trackSnakesMod(i);
//Note: still didn't understand why progress bar doesn't update.
//It is linked to the fact that i call a DeformSnakeNoThread method
//which is not parallel. But still i didn't get why
                		  
                		  //}

           
              //http://www.daniweb.com/software-development/java/threads/187194/create-start-threads-with-loop#
          } 
          TRACK = false;
  }
  
  /**   ADRI 10/01/2013 New Method
   *    Tracks all snakes of one frame, to the following frame.
   *    This method is still not implemented so i don't include the button.
   **/
//public void trackAllSnakesInOneFrame(){
//	   int numberofsnakes = SnakeStore.getNumberOfSnakes();
//        
//        /**GD_Debug Temporary, just to show results in a GUI:
//        GenericDialog gadn = new GenericDialog("flagTrackSnake");
//        gadn.addNumericField("flagTrackSnake: ", flagTrackSnake.length, 0);
//        gadn.showDialog();
//        //GD_Debug*/
//        for (int i=0;i<=numberofsnakes;i++){
//            CurrentSnake=SnakeStore.getSnake(i);
//              		  deformSnakeOneFrame();
//              	  }         
//            //http://www.daniweb.com/software-development/java/threads/187194/create-start-threads-with-loop#
//        }

  
  /**   ADRI 09/01/2013 New Method WARNING : IT IS SERIAL!!!! (VERY SLOW, MUST BE MADE PARALLEL)
   *    Track all snakes in all frames (PARTIALLY DONE, BUT IS STILL SERIAL AND NOT PARALLEL, WHICH IS BAD BECAUSE IS IS SLOW.)
   **/
  public void trackAllSnakesAllFrames(){
	   int numberofsnakes = SnakeStore.getNumberOfSnakes();
       for (int i=0;i<=numberofsnakes;i++){
           CurrentSnake=SnakeStore.getSnake(i);
           trackSnakeAllFrames();
           /**GD_Debug Temporary, just to show results in a GUI:
           GenericDialog gardn = new GenericDialog("flagTrackSnake");
           gardn.addNumericField("ith snake number: ", i, 0);
           gardn.addNumericField("numberofsnakes: ", numberofsnakes, 0);
           gardn.showDialog();
           //GD_Debug*/
       }
  }

    /**   ADRI 22/12/2012 New Method
     *    Deforms all snakes in a frame
     **/
    
    public void deformAllSnakesOld(){
        	snakecycle = 0;
        	int numbersnakes = SnakeStore.getNumberOfSnakes();
            //for (int i = 0;i<=SnakeStore.getNumberOfSnakes();i++){            
            while (snakecycle < numbersnakes){
        	CurrentSnake = SnakeStore.getSnake(snakecycle);
        	deformSnakeMod(CurrentSnake);
            //SnakeStore.deleteSnake(CurrentSnake);
            //CurrentSnake = SnakeStore.getLastSnake();
            //snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
            //updateImagePanel();
            snakecycle++;
                }
            //snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
            updateImagePanel();
                //CurrentSnake = previousSnake;
                //unsetRememberSnake();
    }
    
  //aa  /**   ADRI 04/13/2013 New Method
  //aa   *    Deforms all snakes in a frame
  //aa   **/
    
   //aa public void deformAllSnakes(){
  //aa  	deformedsnakes=0;
  //aa  	deformAllSnakesRecursive();
  //aa  }
    
  //aa  public void deformAllSnakesRecursive(){
    		   //while ((SnakeStore.getNumberOfSnakes()-deformedsnakes)!=0){
    		      //CurrentSnake=SnakeStore.getSnake(SnakeStore.getNumberOfSnakes()-deformedsnakes);
    			   
    	//aa			   CurrentSnake=SnakeStore.getSnake(deformedsnakes);
    	//aa			   if (CurrentSnake.exists(images.getCounter())){
    			   // ADRI 6 gen 2013: Please NOTE threads in deformSnakeMod2 are currently working in PARALLEL.
    			   // This can be improved.
    	//aa		   deformSnakeMod2();
    	//aa		   }
    	//aa		   else{
    				 //aa			   deformedsnakes++;
    	//aa		   }
    			   //CurrentSnake=SnakeStore.getSnake(deformedsnakes);
    			   //deformSnakeMod2();
    		   //}
    		//SnakeStore.
        	//snakecycle = 0;
        	//int numbersnakes = SnakeStore.getNumberOfSnakes();
            //for (int i = 0;i<=SnakeStore.getNumberOfSnakes();i++){            
            //while (snakecycle < numbersnakes){
        	//CurrentSnake = SnakeStore.getSnake(snakecycle);
        	//deformSnakeMod(CurrentSnake);
            //SnakeStore.deleteSnake(CurrentSnake);
            //CurrentSnake = SnakeStore.getLastSnake();
            //snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
            //updateImagePanel();
            //snakecycle++;
             //   }
            //snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
  //aa       updateImagePanel();
            
                //CurrentSnake = previousSnake;
                //unsetRememberSnake();
          //aa  }

    /**   ADRI 08/01/2013 New Method
     * Counts the intensity of all (closed) snakes in all frames. (ATTENTION, INCOMPLETE!!! WE JUST USE INDEX 1 FOR SNAKE )
     *
     **/
    public void countAllSnakesAllFramesIntensity(){
        int snakesNumber = SnakeStore.getNumberOfSnakes();
        ArrayList<double[]> allframesintensvalues = new ArrayList<double[]>();
        ArrayList<double[]> allframesareavalues = new ArrayList<double[]>();
        ArrayList<double[]> snakesvalues = new ArrayList<double[]>();
        for (int snakeindex=0;snakeindex<snakesNumber;snakeindex++){
        	ArrayList<double[]> tempvalue = countSnakeAllFramesIntensity(snakeindex);	
        	snakesvalues.add(tempvalue.get(0));
        	allframesintensvalues.add(tempvalue.get(1));
        	allframesareavalues.add(tempvalue.get(2));
        }
    	
    	// Now I take those values and write them in a text file
    	// I pass a vector with snake number, first and last frame.
    	// And another vector with all intensity average and area values for all frames in which snake exists. 

    	disableUI();
    	SnakeIO.writeSnakesIntensityData(getFrame(),snakesvalues,allframesintensvalues,allframesareavalues,images.getStackSize());
    	enableUI();

    }


    
    /**   ADRI 08/01/2013 New Method
     * Counts the intensity of a single (closed) snake in all frames. (TEST)
     *
     **/
    public ArrayList<double[]> countSnakeAllFramesIntensity(int snakeindex){
    CurrentSnake=SnakeStore.getSnake(snakeindex);
    	// Get the number of the selected snake.
    	// Travel through all frames and check for intensity value
        /**GD_Debug Temporary, just to show results in a GUI:
        GenericDialog gdn = new GenericDialog("Firstlastframe");
        gdn.addNumericField("FirstFrame: ", CurrentSnake.getFirstFrame(), 0);
        gdn.addNumericField("LastFrame: ", CurrentSnake.getLastFrame(), 0);
        gdn.showDialog();
        /**GD_Debug*/
        
        double[] snakeData={snakeindex,CurrentSnake.getFirstFrame(),CurrentSnake.getLastFrame()};
    	double[] allframesintensities = new double[(CurrentSnake.getLastFrame()-CurrentSnake.getFirstFrame())+1];
    	double[] allframesareas = new double[(CurrentSnake.getLastFrame()-CurrentSnake.getFirstFrame())+1];
     	
     	for (int frame=CurrentSnake.getFirstFrame();frame<=CurrentSnake.getLastFrame();frame++){
     		int value = (frame/CurrentSnake.getLastFrame())*(snakeindex/SnakeStore.getNumberOfSnakes())*100;
    	    snake_panel.updateProgressBar(value);     	    
     		/**GD_Debug
    		GenericDialog framedata2 = new GenericDialog("Frame count (ctsnkallfrmsintens)");
       		framedata2.addNumericField("frame count (ctsnkallfrmsintens): ",frame ,0);
       		framedata2.showDialog();
       		/**GD_Debug*/
     		double[] values = calcSnakeIntensity(CurrentSnake, frame); //Values contains {averageintensity, pixelcounter} of a given snake in a given frame

     		allframesintensities[frame-CurrentSnake.getFirstFrame()] = values[0]; //NOTE: Very important to put (frame-CurrentSnake.getFirstFrame()) as index starts from 0 and frames from first frame of the snake!!!
     		allframesareas[frame-CurrentSnake.getFirstFrame()] = values[1]; 
     		/**GD_Debug
    		GenericDialog framedata = new GenericDialog("framedata");
       		framedata.addNumericField("snake: ",snakeindex ,0);
       		framedata.addNumericField("frame: ",frame ,0);
       		framedata.addNumericField("allframesintensities: ",allframesintensities[frame-CurrentSnake.getFirstFrame()]  ,0);
       		framedata.addNumericField("allframesareas: ",allframesareas[frame-CurrentSnake.getFirstFrame()] ,0);
       		framedata.showDialog();
       		GD_Debug*/
    	}
    	ArrayList<double[]> allframesvalues = new ArrayList<double[]>();
    	allframesvalues.add(snakeData);
    	allframesvalues.add(allframesintensities);
    	allframesvalues.add(allframesareas);
     
    	return allframesvalues;
    }

   /** Adri New Method 09 Gen 2013 
    * Writes intensity of one single snake in all frames
    */
    public void countSnakeIntensity(){
    	//Not working because I still miss a method to retrieve the index of currentsnake.
    	
    	/**
        ArrayList<double[]> allframesintensvalues = new ArrayList<double[]>();
        ArrayList<double[]> allframesareavalues = new ArrayList<double[]>();
        ArrayList<double[]> snakesvalues = new ArrayList<double[]>();
        	ArrayList<double[]> tempvalue = countSnakeAllFramesIntensity(HERE WE NEED INDEX OF CURRENT SNAKE);	
        	snakesvalues.add(tempvalue.get(0));
        	allframesintensvalues.add(tempvalue.get(1));
        	allframesareavalues.add(tempvalue.get(2));
    	
    	// Now I take those values and write them in a text file
    	// I pass a vector with snake number, first and last frame.
    	// And another vector with all intensity average and area values for all frames in which snake exists. 

    	disableUI();
    	SnakeIO.writeSnakesIntensityData(getFrame(),snakesvalues,allframesintensvalues,allframesareavalues,images.getStackSize());
    	enableUI();
       */

    }
    	

    /**   ADRI 07/01/2013 New Method
     * Exports the intensity of a single snake in a given frame. (TEST)
     * Returns the average intensity and area values in a double[2] vector
     **/
  public double[] calcSnakeIntensity(Snake CurrentSnake, int frame){
	  int[] xCoord;
	  int[] yCoord;
	  int xmax;
	  int xmin;
	  int ymax;
	  int ymin;
	  // I get two double vectors, with x and y coordinates of a given snake in a given frame
	  //(get_xyCoordinates is a new Snake method created by ADRI on 07 gen 2013).
      //xyCoord=CurrentSnake.get_xyCoordinates(images.getCounter());
	  xyCoord=CurrentSnake.get_xyCoordinates(frame);
	  xCoord=xyCoord.get(0);  
      yCoord=xyCoord.get(1);

      // I create a polygon with the snake shape
      Polygon snakeShape= new Polygon(xCoord, yCoord, xCoord.length);
      
      // Question: all this could be done maybe using Shape class, and then getting the rectangle containing the AREA?
      // First way to find a minimum and a maximum
      // Please note in this way the position of values into xCoord and yCoord vectors will be changed!!
      // This is not a problem, provided one doesn't use again those vectors into this method.
      Arrays.sort(xCoord);
      Arrays.sort(yCoord);
      xmin=xCoord[0];  
      xmax=xCoord[xCoord.length-1]; 
      ymin=yCoord[0];  
      ymax=yCoord[yCoord.length-1];
      
      /** Alternative way to find min and max:
      Object xmin = Collections.min(xCoord);
      Object ymin = Collections.min(yCoord);
      Object xmax = Collections.max(xCoord);
      Object ymax = Collections.max(yCoord);
      **/

      //Let's check if we builded the ROI in the right way:
      /**GD_Debug
      GenericDialog gd = new GenericDialog("ROI rectangle xymaxmin");
      gd.addNumericField("Xmin: ", xmin, 0);
      gd.addNumericField("Xmax: ", xmax, 0);
      gd.addNumericField("Ymin: ", ymin, 0);
      gd.addNumericField("Ymax: ", ymax, 0);
      gd.showDialog();
	  GD_Debug*/	      
      // Now we send the xyminmax values to a method which checks, for every pixel in the rectangle,
      // if this pixel is into the snake contour, and if so, calculates average intensity.
      // CurrentSnake.calculateSnakeAverageIntensity();
      
      double averageintensity=0;
      double pixelcounter=0;
		/**GD_Debug
	    GenericDialog framedata4 = new GenericDialog("Frame count (calcsnkintens)");
		framedata4.addNumericField("frame count (calcsnkintens): ",frame ,0);
		framedata4.showDialog();
		/**GD_Debug*/
		images.setNoDraw(true);
	    images.gotoImage(frame);
        updateImagePanel();
        images.setNoDraw(false);
	  /**GD_Debug
  GenericDialog framedata3 = new GenericDialog("Double frame count (calcsnkintens)");
		framedata3.addNumericField("getCounter count (calcsnkintens): ", images.getCounter(),0);
		framedata3.showDialog();
		/**GD_Debug*/
      //for (int xindex=xmin;xindex<=xmax;xindex++){ //Note i am including borders
         // for (int yindex=ymin;yindex<=ymax;yindex++){ //Note i am including borders
              for (int xindex=xmin+1;xindex<xmax;xindex++){ //Note i am NOT including borders
                  for (int yindex=ymin+1;yindex<ymax;yindex++){ //Note i am NOT including borders
        	  if (snakeShape.contains(xindex,yindex)){
        		  //I get the intensity of this pixel and add it to averageintensity variable
        		  // For the moment is working with a getPixels method intended for 8bits channels (i.e. tempvalues needs to be int[] and not double[])

        		  int[] tempvalues = images.getPixels(xindex, yindex);
        		  averageintensity += tempvalues[0];
        		  /**GD_Debug
        		  //if (yindex==ymin+8 && xindex==xmin+8 && pixelcounter==0){
        		  if (pixelcounter==0){
        			   GenericDialog gd4 = new GenericDialog("Check if it works ");
        			      gd4.addNumericField("tempvalues[0]: ", tempvalues[0], 0);
        			      gd4.addNumericField("tempvalues[1]: ", tempvalues[1], 0);
        			      gd4.addNumericField("tempvalues[2]: ", tempvalues[2], 0);
        			      gd4.addNumericField("tempvalues[3]: ", tempvalues[3], 0);
        			      gd4.addNumericField("Image: ", images.getCounter(), 0);
        			      gd4.showDialog(); 
        			  
        		  } /**GD_Debug*/
        		  
        		
        		  //Then I increase by one the pixel counter (it will be used for normalization and for Area)
        		  pixelcounter++;
        	  }
          }
      }
      averageintensity=(averageintensity/pixelcounter);
      //double[] values=new double[]{averageintensity, pixelcounter};
      double[] values={averageintensity, pixelcounter};
      //Let's check if we calculated average intensity in the right way:
      /**GD_Debug
      GenericDialog gd22 = new GenericDialog("Average intensity / Area");
      gd22.addNumericField("Averageintensity: ", averageintensity, 0);
      gd22.addNumericField("Pixelcounter: ", pixelcounter, 0);
      gd22.showDialog();
      /**GD_Debug*/
      
      return values;
      
  }
    
    
    /**   ADRI 21/12/2012 New Method
     *    Goes to the next frame and allows user to draw a new snake.  
     *    This new snake is linked to the selected snake of the previous frame.
     *
     **/
  public void continueSnakeInNextFrame(){
  
      if(checkForCurrentSnake()){
          int frame = images.getCounter();
          
          ArrayList<double[]> Xs = new ArrayList<double[]>(CurrentSnake.getCoordinates(frame));
                  
          nextImage();
          
          setRememberSnake(CurrentSnake);
          addSnake();
          //updateImagePanel();
      }
  }
  
  /**   ADRI 22/12/2012 New Method
   *    Allows user to redraw a snake in this frame.  
   *    This new snake is written over to the previously selected snake and remains linked to other frames.
   *
   **/
  public void redrawSnakeInThisFrame(){
	  
      if(checkForCurrentSnake()){
          int frame = images.getCounter();
          
          ArrayList<double[]> Xs = new ArrayList<double[]>(CurrentSnake.getCoordinates(frame));
                            
          setRememberSnake(CurrentSnake);
          addSnake();
          //updateImagePanel();
      }
  }
    
    /**
       *    Disables the UI and begins the Delete Middle fix
       */
    public void deleteMiddleFix(){
        if(checkForCurrentSnake()){
            SnakeInteraction si = new DeleteMiddleFixer(this,images,CurrentSnake);
            registerSnakeInteractor(si);

        }
    }
    
    /**
       *    Disables the UI and begins the Move Middle fix
       */
    public void moveMiddleFix(){
        if(checkForCurrentSnake()){
            SnakeInteraction si = new MoveMiddleFixer(this,images,CurrentSnake);
            registerSnakeInteractor(si);

        }
    }
    
    
    /**
       *    Disables UI and Deletes and end
       */
    public void deleteEndFix(){
        if(checkForCurrentSnake()){
            SnakeInteraction si = new DeleteEndFixer(this, images, CurrentSnake);
            registerSnakeInteractor(si);
        }
    }
    
    // Adri 10/01/2013 Method to go to a given image in the frame
    public void gotoImage(int i){
        //This displays the chosen image in the stack to the panel.
       images.gotoImage(i);
       updateImagePanel();
       
   }
    
    /** moves to the previous image */
    public void previousImage(){
    
         //This displays the previous image in the stack to the panel.
        images.previousImage();
        updateImagePanel();
        
    }
    
    /** moves to the next image */
    public void nextImage(){
        
        images.nextImage();
        updateImagePanel();
    
    }
    
    /**
       *    Disables UI and begins a zoom in routine
       */
    public void initializeZoomIn(){
        SnakeInteraction si = new Zoomer(this, images);
        registerSnakeInteractor(si);
        images.resetZoom();
        updateImagePanel();

        disableUI();
    }
    
    /** deletes the current snake and selects the next one */
    public void deleteSnake(){
            
            SnakeStore.deleteSnake(CurrentSnake);
            CurrentSnake = SnakeStore.getLastSnake();
            snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
            updateImagePanel();

    }
    
    // Adri 03 Gen 2013
    //public void deleteSnakeMod(int snakecycle){
       /// CurrentSnake = SnakeStore.getSnake(snakecycle);
       /// snakecycle++;
        /// SnakeStore.deleteSnake(CurrentSnake);
        //CurrentSnake = SnakeStore.getLastSnake();
        //snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
        //updateImagePanel();
        //delflag=true;
/// }
    
    public void deleteAllSnakes(){
    	/**GD_Debug Temporary, just to show results in a GUI:*/
        GenericDialog gdyn = new GenericDialog("Delete All Snakes?");
        gdyn.addMessage("Are you sure you want to delete all snakes in all frames?");
        gdyn.enableYesNoCancel("Yes", "No");
        gdyn.showDialog();
        if (gdyn.wasCanceled()){
            //IJ.log("User clicked 'Cancel'");
        }
        else if (gdyn.wasOKed()){
            //IJ. log("User clicked 'Yes'");
      	 SnakeStore.deleteAllSnakes2();
         snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
         updateImagePanel();
        }
        else{
        	//IJ. log("User clicked 'No'");
        }
        
            

    }
    
    public void deleteAllSnakesOld(){
    	 SnakeStore.deleteAllSnakes();
    	///	snakecycle = 0;
    ///	int numbersnakes = SnakeStore.getNumberOfSnakes();
        //for (int i = 0;i<=numbersnakes;i++){            
   ///     while (snakecycle < numbersnakes){
   ///     	if (delflag==true){
    ///    		delflag=false;	
    ///    		deleteSnakeMod(snakecycle);
    ///    	}
        //CurrentSnake = SnakeStore.getSnake(i);
        //SnakeStore.deleteSnake(CurrentSnake);
        //snakecycle++;
        //CurrentSnake = SnakeStore.getLastSnake();
        //snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
        //updateImagePanel();
        snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
        updateImagePanel();
        
        	//deformSnake();
            //}
        //snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
        //updateImagePanel();
            //CurrentSnake = previousSnake;
            //unsetRememberSnake();
        }


    /**
       *    Sets the flag so the next time the snake panel is clicked foreground
       *    intesity will be calcularted
       *    
       **/
    public void getForegroundIntensity(){        
       
        SnakeInteraction si = new ForegroundMean(this,snake_panel,images);
        registerSnakeInteractor(si);

    }
    
    
    /**
       *    Sets the flag so the next time the snake panel is clicked background
       *    intesity will be calcularted
       *    
       **/
    public void getBackgroundIntensity(){
        
        SnakeInteraction si = new BackgroundMean(this, images, snake_panel);
        registerSnakeInteractor(si);
        

    }
    
    /** resets the zoom to the orginal image size */
    public void zoomOut(){

        //resets zoom settings
        images.resetZoom();
        
        updateImagePanel();


    }
    
    
    /**  
       *    Starts the initialize snake process where SnakeRaw?
       *    are the transient snake coordinates.
       *
       */
    public void addSnake(){

        SnakeInteraction si = new InitializeSnake(this, images, snake_panel.getSnakeType());
        registerSnakeInteractor(si);
    }
    
    /**
     * Adri 22/12/2012 New Method
     * Allows to draw a circular snake starting from a single click.
     * 
     */
    public void addPointToCircleSnake(){
        //GDADRI START OF CODE TO SET RADIUS THROUGH GENERIC DIALOG
        //GenericDialog gd = new GenericDialog("enter radiu of this set of circles");
        //gd.addNumericField("radius",0,2);
        //gd.showDialog();
        //double radii  = gd.getNextNumber();
        //GDADRI END OF CODE TO SET RADIUS THROUGH GENERIC DIALOG
        SnakeInteraction si = new PointToCircleSnake(this, images, snake_panel.getSnakeType(), circleradius);
        //SnakeInteraction si = new PointToCircleSnake(this, images, snake_panel.getSnakeType());
        registerSnakeInteractor(si);
    }
    
    /**   ADRI 21 Dic 2012 New Method
     *    Method called when we want to add the drawn snake to a previously existing one
     */
    public void setRememberSnake(Snake CurrentSnake){
    	previousSnake = CurrentSnake;
        addToPrevious = true;
        }

    /**    ADRI 21 Dic 2012 New Method
     *    After adding the drawn snake to a previously existing snake, we want to reset variables.
     */
    public void unsetRememberSnake(){
    	previousSnake = null;
        addToPrevious = false;
        }


    /**
     * Sets the raw snake for drawing purposes.
     *
     * @param raw  @nullable
     */
    public void setSnakeRaw(ArrayList<double[]> raw){
        SnakeRaw = raw;
    }

    /** ADRI 21 Dic 2012 Modified Method
     * If condition is false, method stores the drawn snake into a new snake (already existing part).
     * In the modified part, if condition is true the drawn snake is appended to the selected snake 
     * of the previous frame by calling addToPreviousSnake method, 
     * and then the unsetRememberSnake() method is called to reset variables.
     * @param s
     */
    public void addNewSnake(Snake s){
    	if(!addToPrevious){
    		CurrentSnake = s;
        	SnakeStore.addSnake(s);
    	}
        else {
        	addToPreviousSnake(SnakeRaw);//mettiamo questo snake connesso al precedente tramite un metodo in model      	    
        	unsetRememberSnake();	
        }
    }
    
    /**
     * Adri 22/12/2012 New Method
     * Allows to draw a circular snake starting from a single click.
     * It can be modified to include the possibility to redraw circles also for already existing snakes.
     * It could be done with a radius button to ask if one wants circular or handdrawn snake.
     */
    public void addNewPointToCircleSnake(Snake s){
    	// ADRI 22/12/2012 To add if we want to allow redrawing or continuation using circles.
    	//if(!addToPrevious){
    		CurrentSnake = s;
        	SnakeStore.addSnake(s);
    	//}
        //else {
        //	addToPreviousSnake(SnakeRaw);//mettiamo questo snake connesso al precedente tramite un metodo in model      	    
        //	unsetRememberSnake();	
        //}
    }
    
    /** ADRI 21 Dic 2012 New Method.
     * Links snake created in the actual frame to the previous frame snake.
     */
    public void addToPreviousSnake(ArrayList<double[]> SnakeRaw){
    	previousSnake.addCoordinates(images.getCounter(),SnakeRaw);
    }

    /**   ADRI 26 Dic 2012 deformSnake Method Modified
     *    It now receives the name of snake. Used in loop
     *    Causes the deformation of a snake to occur
     **/
  public void deformSnakeMod(Snake snk){
	  final Snake snak = snk;
      //if(checkForCurrentSnake()){
          if(!RUNNING){
              RUNNING = true;
              disableUI();
              Thread x = new Thread(){
                 public void run(){
                     try{
                     
                          deformRunningMod(snak);
                     
                     } catch(IllegalAccessException e){
                          
                          JOptionPane.showMessageDialog(
                                  getFrame(),
                                  "Snake too long The maximum length is "+SnakeModel.MAXLENGTH+"  "+ e.getMessage()
                          );
                          
                          
                     } catch(IllegalArgumentException e){
                          snak.clearSnake(images.getCounter());
                     } catch(ArrayIndexOutOfBoundsException e){
                          snak.clearSnake(images.getCounter());
                     }
                     finally {
                      
                          SnakeStore.purgeSnakes();
                          snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
                          RUNNING=false;
                          enableUI();
                     
                     }
                 }
              };
              x.start();
          }
          
      //}
  }
    
  /**   ADRI 10 Gen 2013 deformSnake Method Modified in order to launch things with NO THREADS! 
   *    (therefore it becomes SERIAL and not parallel)
   *    Causes the deformation of a snake to occur
   **/
public void deformSnakeNoThread(){
    if(checkForCurrentSnake()){
        if(!RUNNING){
            RUNNING = true;
            //Adri//if(!TRACK) disableUI();
            disableUI();
            //Thread x = new Thread(){
            //   public void run(){
                   try{
                   
                        deformRunning();
                   
                   } catch(IllegalAccessException e){
                        
                        JOptionPane.showMessageDialog(
                                getFrame(),
                                "Snake too long The maximum length is "+SnakeModel.MAXLENGTH+"  "+ e.getMessage()
                        );
                        
                        
                   } catch(IllegalArgumentException e){
                	   CurrentSnake.clearSnake(images.getCounter());
                   } catch(ArrayIndexOutOfBoundsException e){
                        CurrentSnake.clearSnake(images.getCounter());
                   }
                   finally {
                    
                        SnakeStore.purgeSnakes();
                        snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
                        RUNNING=false;
                        //Adri//if(!TRACK) enableUI();
                        enableUI();
                   
                   }
               //}
            //};
            //x.start();
        }
        
    }
}

    /**  Adri 4 Gen 2013
     * 	   // ADRI 4 gen 2013: Please NOTE threads in deformSnakeMod2 are currently working in SERIAL (really? check).
    			   // This can be improved.
       *    Causes the deformation of a snake to occur
       **/
   // public void deformSnakeMod2(){
	public void deformAllSnakes(){
    	for (int defsnak=0;defsnak<SnakeStore.getNumberOfSnakes();defsnak++){
 	    CurrentSnake=SnakeStore.getSnake(defsnak);
	    if (CurrentSnake.exists(images.getCounter())){
        //if(checkForCurrentSnake()){
	    	TRACK = true;
            if(!RUNNING){
                RUNNING = true;
                disableUI();
          //      Thread x = new Thread(){
          //         public void run(){
                       
                       try{
                       
                            deformRunning();
                       
                       } catch(IllegalAccessException e){
                            
                            JOptionPane.showMessageDialog(
                                    getFrame(),
                                    "Snake too long The maximum length is "+SnakeModel.MAXLENGTH+"  "+ e.getMessage()
                            );
                            
                            
                       } catch(IllegalArgumentException e){
                            CurrentSnake.clearSnake(images.getCounter());
                       } catch(ArrayIndexOutOfBoundsException e){
                            CurrentSnake.clearSnake(images.getCounter());
                       }
                       finally {
                        
                            SnakeStore.purgeSnakes();
                            snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
                            RUNNING=false;
                            enableUI();
                       
                       }
           //        }
           //     };
           //     x.start();
           //     while (x.isAlive()){
           //   	  }
           //     if(x.isAlive()==false){
           //   	  if (deformedsnakes<(SnakeStore.getNumberOfSnakes()-1)){
           //   		  deformedsnakes++;
           //   	      deformAllSnakesRecursive();
           //   	  }
              	  
           //   	  }
            TRACK = false;
            }
            
        }
    }
    	updateImagePanel();
    }
    
    
    /**   Adri modified method 10/01/2013 
     *    It's like deformsnake but it allows multithreading for snakes of the same frame
     *    Causes the deformation of a snake to occur
     **/
  public void deformSnakeOneFrame(){

      if(checkForCurrentSnake()){
          if(!RUNNING){
              RUNNING = true;
              disableUI();
              Thread x = new Thread(){
                 public void run(){
                     
                     try{
                 	    int frame = images.getCounter();
                	    ArrayList<double[]> Xs = new ArrayList<double[]>(CurrentSnake.getCoordinates(frame));
                	    nextImage();
                	    CurrentSnake.addCoordinates(images.getCounter(),Xs);
                	    updateImagePanel();              
                        deformRunning();
                     
                     } catch(IllegalAccessException e){
                          
                          JOptionPane.showMessageDialog(
                                  getFrame(),
                                  "Snake too long The maximum length is "+SnakeModel.MAXLENGTH+"  "+ e.getMessage()
                          );
                          
                          
                     } catch(IllegalArgumentException e){
                          CurrentSnake.clearSnake(images.getCounter());
                     } catch(ArrayIndexOutOfBoundsException e){
                          CurrentSnake.clearSnake(images.getCounter());
                     }
                     finally {
                      
                          SnakeStore.purgeSnakes();
                          snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
                          RUNNING=false;
                          enableUI();
                     
                     }
                 }
              };
              x.start();
       
          }
          
      }
  }


    /**  
     *    Causes the deformation of a snake to occur
     **/
  public void deformSnake(){
      if(checkForCurrentSnake()){
          if(!RUNNING){
              RUNNING = true;
              disableUI();
              Thread x = new Thread(){
                 public void run(){
                     
                     try{
                     
                          deformRunning();
                     
                     } catch(IllegalAccessException e){
                          
                          JOptionPane.showMessageDialog(
                                  getFrame(),
                                  "Snake too long The maximum length is "+SnakeModel.MAXLENGTH+"  "+ e.getMessage()
                          );
                          
                          
                     } catch(IllegalArgumentException e){
                          CurrentSnake.clearSnake(images.getCounter());
                     } catch(ArrayIndexOutOfBoundsException e){
                          CurrentSnake.clearSnake(images.getCounter());
                     }
                     finally {
                      
                          SnakeStore.purgeSnakes();
                          snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
                          RUNNING=false;
                          enableUI();
                     
                     }
                 }
              };
              x.start();
       
          }
          
      }
  }

    
    /**
       *    Sets the flag so the next click on the image pane extends the snake
       *    to the click location
       **/
    public void setFixSnakePoints(){
        if(checkForCurrentSnake()){
            SnakeInteraction si = new StretchEndFixer(this,images,CurrentSnake);
            registerSnakeInteractor(si);

        }

    }
    
    /**
       *    These are short access to images so the frame doesn't need to know about the
       *    SnakeImages class
       **/
    public void getAndLoadImage(){
    
        images.getAndLoadImage();
                        
        snake_panel.imageLoaded(images.hasImage());
        
        if(images.hasImage()){
            updateImagePanel();
            autoDetectIntensities();
        }
    
    }
    
    //********************************************
    //      OTHER METHODS
    //********************************************
    
    /**
       *    Disables UI and Deletes and end
       */
    public void autoDetectIntensities(){
        try{
            double[] intensities = images.getAutoIntensities();
            forIntMean = intensities[0];
            backIntMean = intensities[1];
                    
            snake_panel.updateBackgroundText(backIntMean + "");
            snake_panel.updateForegroundText(forIntMean + "");
        } catch(java.lang.NullPointerException e){
            //nothing
        }
    }
    /**
       *    short access to images so the frame doesn't need to know about the
       *    SnakeImages class
       **/
    public int getDrawHeight(){
    
        return images.getDrawHeight();
    
    }
    
    /**
       *    short access to images so the frame doesn't need to know about the
       *    SnakeImages class
       **/
    public Image getImage(){
    
        return images.getImage();
        
    }
    /**
       *    These are short access to images so the frame doesn't need to know about the
       *    SnakeImages class
       **/
     public int getDrawWidth(){
        return images.getDrawWidth();
     }
    
    /**
       *    short access to images so the frame doesn't need to know about the
       *    SnakeImages class
       **/
    public boolean hasImage(){
        return images.hasImage();
    }
    
    /*****************************************************************/
    //              SETTERS
    /*****************************************************************/
    
    public void setDeformIterations(int di){
        deformIterations = di;
    }
    
    public void setImageSmoothing(double mon){

        IMAGE_SIGMA = mon;
    
    }
    
    public void setResolution(double res){
        MAXIMUM_SPACING = res;
    }
   
    public void setBackgroundIntensity(double fim){
        
        backIntMean = fim;
    
    }

    public void setForegroundIntensity(double f){
    
        forIntMean = f;    
    }

    public void setAlpha(double a){
        alpha = a;        
    }
    
    public void setCircleRadius(double cr){
        circleradius = cr;        
    }
    
    public void setBeta(double b){
        beta = b;    
    }
    
    public void setGamma(double g){
        gamma = g;
    }
    
    public void setWeight(double w){
        weight = w;
   }
    
    public void setStretch(double s){
        stretch = s;
    
    }
    
    /**
       *        Sets the current contour/curve deformation values to the new image values
       *        and the current contants values
     *        @throws IllegalAccessException
       **/
    private void resetDeformation() throws IllegalAccessException{
        
        curveDeformation.setBeta(beta);
        curveDeformation.setGamma(gamma);
        curveDeformation.setWeight(weight);
        curveDeformation.setStretch(stretch);
        curveDeformation.setAlpha(alpha);
        curveDeformation.setForInt(forIntMean);
        curveDeformation.setBackInt(backIntMean);
        

        curveDeformation.addSnakePoints(MAXIMUM_SPACING);

        curveDeformation.initializeMatrix();


    }


    /**
     *    Convenience to calculate distance between two points
     *
     *
     * @param x1 first point
     * @param x2 second point
     * @return distance between two points based on TwoDDeformation class
     */
     private double pointDistance(double[] x1, double[] x2){
        return TwoDDeformation.pointDistance(x1,x2);
    }

    /**
     * Checks all snakes to make sure they still have valid points.
     *
     */
    public void purgeSnakes(){
        SnakeStore.purgeSnakes();
    }
    /**
       *    Gets the image frame
       **/
    public JFrame getFrame(){
        return snake_panel.getFrame();
    }
    
    /**
       *    Loads an image from an existing image plus
       *    the principle useage of this function is to load an
       *    image when trac
       *    ed through ImageJ.
       *
       *    @param implus   the ImagePlus preloaded via ImageJ
       **/
    public void loadImage(ImagePlus implus){
    
        images.loadImage(implus);
        
        snake_panel.imageLoaded(images.hasImage());
        
        if(images.hasImage()){
            updateImagePanel();
            autoDetectIntensities();
           
        }
    
    }
    
    /**
       *    causes the SnakeImages to redraw any snakes then calls the snake panel
       *    to repaint the image
       *
       **/
    public void updateImagePanel(){

        if(SnakeRaw!=null)
            images.setRawData(SnakeRaw);

        images.setCurrentSnake(CurrentSnake);
        images.updateImagePanel();
        
        snake_panel.updateStackProgressionLabel(images.getCounter(),images.getStackSize());
        snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());

        snake_panel.repaint();
    
    }
    

    
    
    /** stops any other input except for the MouseListener on the displayed Image */
    public void disableUI(){
        
        snake_panel.disableUI();
        
    }
    
    /** enables buttons */
    public void enableUI(){
    
        snake_panel.enableUI();
    
    }
    
    /**
       *    This is used before attempting to modify the current snake such as with
       *    a fix or deform button press.
       **/
    public boolean checkForCurrentSnake(){
        if(CurrentSnake==null){
        
            return false;
        } else {
            return CurrentSnake.exists(images.getCounter());
        }    
    
    }
    
    /**
       *    Ceases a deform iterations
       */
    public void stopRunningNicely(){
        INTERRUPT=true;
        if(interactor!=null) interactor.cancelActions();
    }
    
    public String getImageTitle(){
		
		return images.getTitle();
		
	}

    public void setMaxLength(){
        GenericDialog gd = new GenericDialog("Enter a new Max Length");

        gd.addNumericField("Max Length",SnakeModel.MAXLENGTH,0);

        gd.showDialog();
        if(gd.wasCanceled()) return;

        int value = (int)gd.getNextNumber();

        if(value>0) setMaxLength(value);

    }

    void setMaxLength(int value){

        MAXLENGTH = value;
        
    }

    /**
     * When the menu item 'Set Line Width is used, this shows a dialog
     * for getting a new value from the user.
     *
     * 
     */
    void setLineWidth(){
        GenericDialog gd = new GenericDialog("Enter a new Line Width");

        gd.addNumericField("Max Length",SnakeImages.LINEWIDTH,0);

        gd.showDialog();
        if(gd.wasCanceled()) return;

        try{
            int value = (int)gd.getNextNumber();
            if(value>0) SnakeImages.LINEWIDTH=value;
        } catch(NumberFormatException e){
            //just in case
        }
    }

    /**
     * Pop up a dialog to show jfilament version uses the jfilament3d
     * version information if available.
     *
     */
    void showVersion(){

            String s = "<html>" +
                       "<body>" +
                       "You are using a version of jfilament w/out jfilament3d" +
                       "<br> version info cannot be shown <br>" +
                       "see:  <br> http://athena.physics.lehigh.edu/jfilament<br>" +
                       " for more info " +
                       "</body></html>"+
                       "Please note that this is a modified version of JFilament"+
                       "with some additional features added by adriano.bonforti@upf.edu"+
                       "see attached pdf for details";            
            
            final JFrame shower = new JFrame("JFilament2D About");
            JEditorPane helper = new JEditorPane("text/html",s);
        
            shower.setSize(400,400);
            helper.setEditable(false);

            shower.add(helper);
            shower.setVisible(true);
        

    }


}
