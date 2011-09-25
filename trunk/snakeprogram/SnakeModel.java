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

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;

class SnakeModel{

    public static int MAXLENGTH = 1500;

    private SnakeImages images;             //Contains all of the image data
    private SnakeFrame snake_panel;        //Contians the controls
    
    /**These contain the currently displayed snake data. x and y values*/
    private ArrayList<double[]> SnakeRaw;
    private Snake CurrentSnake;
    /**The deformation for an open deformation*/
    private TwoDDeformation curveDeformation;

    
    //state variables.
    private boolean bInitialSnake = false;         //If a new snake is being added
    private boolean bForIntMean = false;          //Whether to aquire a foreground value
    private boolean bBackIntMean = false;        //Flag to aquire data for Background value
    private boolean bFixSnake = false;              //Stretch fix snake flag
    private boolean zoomInInitialize = false;     //Zoom intialization during mouse clicks
    private int zoomCounter = 0;                      //Zoom has multiple stages.
    private boolean bDeleteFix = false;             //Sets the fix for a delete at the ends
    private boolean bDeleteMFix = false;           //Sets the fix for a delete in the middle
    private boolean bZoomInBox = false;          //determines if a box should be drawn on the image while moving the mouse
    private boolean RUNNING = false;               //If the snakeDeformation is running.
    private boolean INTERRUPT = false;
    private boolean bMoveMiddleFix = false;
    
    private double[] deleteFixArray = new double[4];    //Stores all of the points to be use for the delete fix algoright (2pts with x,y)
    private int deleteFixCounter = 0;                           //keeps track of the number of clicks
    
    
    //Deformation values.  These are maintained here for now, but should be moved in the near future
    private double alpha = 15;
    private double beta = 10;
    private double gamma = 40;
    private double weight = 0.5;
    private double stretch = 150;
    private double forIntMean = 255;
    private double backIntMean = 0;

    //Model Values 
    private int squareSize = 3;                       //This is the size of the square that averaging is performed over for the 'getIntForMean' ops
    private int deformIterations = 100;           //Number of iterations when a 'deformSnake' is clicked        
    private double IMAGE_SIGMA = 1.01;              //Number of monomers per micrometer
    public double MAXIMUM_SPACING = 1.;

    
    
    //What to do with the tracking data!?
    private MultipleSnakesStore SnakeStore;

    
    /**
       *    Starts the snakes application.
       **/
    SnakeModel(){
        SnakeStore = new MultipleSnakesStore();
        images = new SnakeImages(SnakeStore);
        snake_panel = new SnakeFrame(this);
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
        
        snake_panel.initializeProgressBar();

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
    public ImageEnergy energyFactory(){
        
        if(snake_panel.getEnergyType())
            return new IntensityEnergy( images.getProcessor(),IMAGE_SIGMA );
        else
            return new GradientEnergy( images.getProcessor(),IMAGE_SIGMA );

    }
    /**
       *    When the mouse is pressed over the snake panel
       **/   
    public void snakePanelMousePressed(MouseEvent evt){
        
        //create a zoom-in area
        if(zoomInInitialize)
            zoomStep(evt);            
        
        //the following if statement is to add a snake to the image
        if(bInitialSnake)
            initializeSnakeClicked(evt);
        

        //This gets the foreground mean intensity.
        if(bForIntMean && SwingUtilities.isLeftMouseButton(evt))
            getForegroundClicked(evt);
            
        //background 
        if(bBackIntMean && SwingUtilities.isLeftMouseButton(evt))
            getBackgroundClicked(evt);
       
        //This "stretch fix" adds a point onto the nearest end.  Then it resamples the snake.
        if(bFixSnake)
            stretchFixClicked(evt);

        //cut off the end
        if(bDeleteFix){
            deleteEndFixClicked(evt);
        }

        //delete from the middle
        if(bDeleteMFix)
            deleteMiddleFixClicked(evt);
        
        if(bMoveMiddleFix)
            moveMiddleFixClicked(evt);
        
    }
    
    /**
       *    Used for tracking mouse motion events on the image panel
       *    if a snake is being initialized a line will be drawn to the pointer
       *    if a zoom in is taking place a box will be drawn
       **/
    public void snakePanelMouseMoved(MouseEvent evt){
       
         if(bInitialSnake){

            images.updateMousePosition(evt.getX(),evt.getY());
            updateImagePanel();
        }
        if(bZoomInBox){
           
           images.trackingZoomBox(evt.getX(),evt.getY());
           
            updateImagePanel();
            
        }
        if(bDeleteMFix||bMoveMiddleFix){
            double[] pt = findClosestPoint(images.fromZoomX(evt.getX()),
                                images.fromZoomY(evt.getY())
                                            );
            
            images.setMarker(pt);
            updateImagePanel();
        }
        if(bDeleteFix){
            if(deleteFixCounter==0)
                images.setMarker(
                    findClosestPoint(
                                images.fromZoomX(evt.getX()),
                                images.fromZoomY(evt.getY())
                    )
                );
            if(deleteFixCounter==1)
                images.setMarker(
                    findClosestEnd(
                                images.fromZoomX(evt.getX()),
                                images.fromZoomY(evt.getY())
                    )
                );
            updateImagePanel();
        }
        if(bFixSnake){
            images.clearStaticMarkers();
            images.addStaticMarker(
                findClosestEnd(
                                images.fromZoomX(evt.getX()),
                                images.fromZoomY(evt.getY())
                    )
                );
            images.addStaticMarker(new double[]{images.fromZoomX(evt.getX()),
                                images.fromZoomY(evt.getY())});
            updateImagePanel();
        }
    }
    
    public void snakePanelMouseDragged(MouseEvent evt){
        if(bInitialSnake)
            initializeSnakeClicked(evt);
        
    }
    
    
    /*****************************************************************/
    //                  PANEL ACTIONS
    /*****************************************************************/
    
    /**
       *    This performs one of two actions while the zoom is being initialized
       *    it will start the zoom box or it will finish it, Depending on the zoomCounter
       *    This button expects a left click and right click from the user. These
       *    points become the corners of the rectangle that we will zoom in on
       *
       **/  
    private void zoomStep(MouseEvent evt){
        int x = evt.getX();
        int y = evt.getY();
        if(zoomInInitialize && SwingUtilities.isLeftMouseButton(evt)&&zoomCounter==0){
                
                images.setZoomLocation(x,y);
                images.trackingZoomBox(x,y);
                zoomCounter++;
                
                bZoomInBox = true;
                images.setZoomIn(false);                //stop zooming in otherwise the whole image will follow the box.
                images.setZoomInBox(true);
            }
            
            if(zoomInInitialize && SwingUtilities.isRightMouseButton(evt)&&zoomCounter==1){
            
                bZoomInBox = false;
                images.setZoomInBox(false);
            
                images.trackingZoomBox(x,y);
                zoomCounter++;
                
                images.setZoomIn(true);

                enableUI();                
                updateImagePanel();
                
            }
    
    }
    
    /**
       *
       *    To delete a portion of the snake specified by the user with two clicks
       *    The first click must be somewhere in the middle of the snake  to specify 
       *    what is clicked.  The end closest to the second click will be removed.
       *
       **/
    private void deleteEndFixClicked(MouseEvent evt){

        SnakeRaw = CurrentSnake.getCoordinates(images.getCounter());

        if(deleteFixCounter == 0){
            deleteFixArray[0] = evt.getX();
            deleteFixArray[1] = evt.getY();
            
            images.addStaticMarker(
                findClosestPoint(
                                images.fromZoomX(evt.getX()),
                                images.fromZoomY(evt.getY())
                    )
                );
            updateImagePanel();
        }
        
        //reads in the second click coordinates
        if(deleteFixCounter == 1){
            deleteFixArray[2] = evt.getX();
            deleteFixArray[3] = evt.getY();

            //converting points based on zoom status
            double point1X = images.fromZoomX(deleteFixArray[0]);
            double point1Y = images.fromZoomY(deleteFixArray[1]);
            double point2X = images.fromZoomX(deleteFixArray[2]);
            double point2Y = images.fromZoomY(deleteFixArray[3]);
            
           //finds the closest point in the snake to the user's first click
            double[] pt1 = {point1X, point1Y};
            double min = pointDistance(pt1,SnakeRaw.get(0));
            double distance;
            int closestPoint = 0;
                
            for(int k = 1; k < SnakeRaw.size(); k++){
                distance = pointDistance(pt1, SnakeRaw.get(k));
                    if(min > distance){
                        min = distance;
                        closestPoint = k;
                    }
                }
                               

            //determines which end of the snake is closer to the user's second click
            //it then removes the specified portion of the snake
           int size = SnakeRaw.size();
           double[] pt2 = {point2X,point2Y};
            double distance1 = pointDistance(pt2, SnakeRaw.get(0));
            double distance2 = pointDistance(pt2, SnakeRaw.get(size-1));
            if(distance1 < distance2){
                for(int l = 0; l < (closestPoint); l++ ){
                      
                    SnakeRaw.remove(0);
                }
            } else{
                    
                for(int l = 1; l <= (size-closestPoint); l++ ){
                    SnakeRaw.remove(closestPoint);
                }
                
            }
                
            //redraws image
            SnakeStore.purgeSnakes();
            images.clearStaticMarkers();
            enableUI();
            updateImagePanel();
            bDeleteFix = false;


        }
        deleteFixCounter++;
        
    }
    
    /**
       *
       *    This method allows to delete a middle portion of a snake by clicking at the left end
       *    and then the right end of the section to be deleted.
       *
       **/
    private void deleteMiddleFixClicked(MouseEvent evt){
        
        SnakeRaw = CurrentSnake.getCoordinates(images.getCounter());

        if(deleteFixCounter == 0){
            deleteFixArray[0] = evt.getX();
            deleteFixArray[1] = evt.getY();
            
            images.addStaticMarker(
                findClosestPoint(
                                images.fromZoomX(evt.getX()),
                                images.fromZoomY(evt.getY())
                    )
                );
            updateImagePanel();

        }
        //reads in the second click coordinates
        if(deleteFixCounter == 1){
            deleteFixArray[2] = evt.getX();
            deleteFixArray[3] = evt.getY();

           //resets the points based on the zoom status
           double point1X = images.fromZoomX(deleteFixArray[0]);
           double point1Y = images.fromZoomY(deleteFixArray[1]);
           double point2X = images.fromZoomX(deleteFixArray[2]);
           double point2Y = images.fromZoomY(deleteFixArray[3]);

            //finds the closest point in the snake to the user's first click
            double[] pt1 = {point1X,point1Y};
            double[] pt2 = {point2X, point2Y};
            double min1 = pointDistance(pt1, SnakeRaw.get(0));
            double min2 = pointDistance(pt2, SnakeRaw.get(0));
            
            double distance1,distance2;
            
            int closestPoint1 = 0;
            int closestPoint2 = 0;
            
            
            for(int k = 1; k < SnakeRaw.size(); k++){
            
                    distance1 = pointDistance(pt1, SnakeRaw.get(k));
                    distance2 = pointDistance(pt2, SnakeRaw.get(k));

                    if(distance1<min1){
                        min1 = distance1;
                        closestPoint1 = k;
                    }
                        
                    if(distance2<min2){
                        min2 = distance2;
                        closestPoint2 = k;
                    }
            }
        
            int l = closestPoint1 < closestPoint2?closestPoint1:closestPoint2;
            int h = closestPoint1 > closestPoint2?closestPoint1:closestPoint2;
            
            for(int i = h; i>=l; i--){
                SnakeRaw.remove(i);
            }
            
            images.clearStaticMarkers();
            SnakeStore.purgeSnakes();
            enableUI();
            updateImagePanel();
            bDeleteMFix = false;
        }
        deleteFixCounter++;

    }
    
    /**
       *
       *    Executes the move middle fix process.  Three clicks to finish.
       *
       *
       * @param evt its something
       * */
    private void moveMiddleFixClicked(MouseEvent evt){
        
        SnakeRaw = CurrentSnake.getCoordinates(images.getCounter());

        if(deleteFixCounter == 0){
            deleteFixArray[0] = evt.getX();
            deleteFixArray[1] = evt.getY();
            
            images.addStaticMarker(
                findClosestPoint(
                                images.fromZoomX(evt.getX()),
                                images.fromZoomY(evt.getY())
                    )
                );
            updateImagePanel();

        }
        if(deleteFixCounter == 1){
            //reads in the second click stores coordinates
            deleteFixArray[2] = evt.getX();
            deleteFixArray[3] = evt.getY();
            images.addStaticMarker(
                findClosestPoint(
                                images.fromZoomX(evt.getX()),
                                images.fromZoomY(evt.getY())
                    )
                );
            updateImagePanel();
        }
        if(deleteFixCounter == 2){

           //resets the points based on the zoom status
           double point1X = images.fromZoomX(deleteFixArray[0]);
           double point1Y = images.fromZoomY(deleteFixArray[1]);
           double point2X = images.fromZoomX(deleteFixArray[2]);
           double point2Y = images.fromZoomY(deleteFixArray[3]);

            //finds the closest point in the snake to the user's first click
            double[] pt1 = {point1X,point1Y};
            double[] pt2 = {point2X, point2Y};
            double min1 = pointDistance(pt1, SnakeRaw.get(0));
            double min2 = pointDistance(pt2, SnakeRaw.get(0));
            
            double distance1,distance2;
            
            int closestPoint1 = 0;
            int closestPoint2 = 0;
            
            
            for(int k = 1; k < SnakeRaw.size(); k++){
            
                    distance1 = pointDistance(pt1, SnakeRaw.get(k));
                    distance2 = pointDistance(pt2, SnakeRaw.get(k));

                    if(distance1<min1){
                        min1 = distance1;
                        closestPoint1 = k;
                    }
                        
                    if(distance2<min2){
                        min2 = distance2;
                        closestPoint2 = k;
                    }
            }
        
            int l = closestPoint1 < closestPoint2?closestPoint1:closestPoint2;
            int h = closestPoint1 > closestPoint2?closestPoint1:closestPoint2;
            
            for(int i = h; i>=l; i--){
                SnakeRaw.remove(i);
            }
            double[] np = new double[] { 
                                
                                images.fromZoomX( evt.getX() ) , 
                                images.fromZoomY( evt.getY() ) 
                                
                                };
            SnakeRaw.add(l,np);
            
            images.clearStaticMarkers();
            SnakeStore.purgeSnakes();
            enableUI();
            updateImagePanel();
            bMoveMiddleFix = false;
        }
        deleteFixCounter++;

    }
    
    /**
       *    Will extend the nearest end of the snake to the place clicked
       *
       **/
    private void stretchFixClicked(MouseEvent evt){
    
        SnakeRaw = CurrentSnake.getCoordinates(images.getCounter());

        double x = images.fromZoomX(evt.getX());
        double y = images.fromZoomY(evt.getY());
        
        double[] stretch_fix = new double[] { x, y};
             
        int last = SnakeRaw.size()-1;
        
        if(last>0){
            double to_tail = pointDistance(SnakeRaw.get(0), stretch_fix);
            double to_head = pointDistance(SnakeRaw.get(last),stretch_fix);
 
            //if it is closer to the tail 0 then it inserts it first otherwise it inserts it last.
 
            int j = to_tail<to_head?0:last+1;
 
            SnakeRaw.add(j, stretch_fix);
        }
        
        images.clearStaticMarkers();
        images.setStretchFix(false);
        
        enableUI();
        
        updateImagePanel();
        bFixSnake = false;
    
    }
    
    
    /**
       *    When initializing a snake this will add points a right click will end
       *    the process and re-enable the UI.
       **/
    private void initializeSnakeClicked(MouseEvent evt){
        //adding the left-click coordinates to the SnakeRawX and SnakeRawY vectors
        if(SwingUtilities.isLeftMouseButton(evt)){
            double[] pt = {images.fromZoomX((double)evt.getX()),images.fromZoomY((double)evt.getY())};
            SnakeRaw.add(pt);

            images.setFollow(true);

           
        }
        
        //adding the right-click coordinate to the coordinate vectors
        if(SwingUtilities.isRightMouseButton(evt)){
            // double[] pt = {images.fromZoomX((double)evt.getX()),images.fromZoomY((double)evt.getY())};
            //SnakeRaw.add(pt);

            CurrentSnake = new Snake(SnakeRaw, images.getCounter(), snake_panel.getSnakeType());
            
            SnakeStore.addSnake(CurrentSnake);
            
            
            bInitialSnake = false;
            images.setFollow(false);
            images.setInitializing(false);
            enableUI();
            
        }
        
        updateImagePanel();

    }
    
    /**
       *    Finds the location of the current mouse click and finds the 
       *    mean intensity about that point.  Then sets the value for 
       *    background intensity, and updates the UI.
       **/
    private void getBackgroundClicked(MouseEvent evt){
        Double x = images.fromZoomX(evt.getX());
        Double y = images.fromZoomY(evt.getY());
        backIntMean = images.getAveragedValue(x,y,squareSize);
        
        bBackIntMean = false;

        //prints this mean intensity to the text box on the user interface
        String S = "" + backIntMean;
        enableUI();
        snake_panel.updateBackgroundText(S);

    }
    
    /**
       *    Finds the location of the current mouse click and finds the 
       *    mean intensity about that point.  Sets the forground intensity
       *    value and updates the UI to display the value.
       *
       **/
    private void getForegroundClicked(MouseEvent evt){
        Double x = images.fromZoomX(evt.getX());
        Double y = images.fromZoomY(evt.getY());
        
        forIntMean = images.getAveragedValue(x,y,squareSize);
            
          

        
        bForIntMean = false;
        
        //prints this mean intensity to the text box on the user interface
        String S = "" + forIntMean;
        
        enableUI();
        snake_panel.updateForegroundText(S);
        
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
      **/
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
    
    /**
       *    Disables the UI and begins the Delete Middle fix
       */
    public void deleteMiddleFix(){
        if(checkForCurrentSnake()){
            bDeleteMFix = true;

            deleteFixArray = new double [4];
            deleteFixCounter = 0;

            disableUI();
            if(CurrentSnake.TYPE==Snake.CLOSED_SNAKE){
                ArrayList<double[]> points = CurrentSnake.getCoordinates(images.getCounter());
                images.addStaticMarker(points.get(0));
                images.addStaticMarker(points.get(points.size()-1));
                updateImagePanel();
            }
        }
    }
    
    /**
       *    Disables the UI and begins the Move Middle fix
       */
    public void moveMiddleFix(){
        if(checkForCurrentSnake()){
            bMoveMiddleFix = true;

            deleteFixArray = new double [4];
            deleteFixCounter = 0;

            disableUI();

            if(CurrentSnake.TYPE==Snake.CLOSED_SNAKE){
                
                ArrayList<double[]> points = CurrentSnake.getCoordinates(images.getCounter());
                images.addStaticMarker(points.get(0));
                images.addStaticMarker(points.get(points.size()-1));
                updateImagePanel();

            }
        }
    }
    
    
    /**
       *    Disables UI and Deletes and end
       */
    public void deleteEndFix(){
        if(checkForCurrentSnake()){
            bDeleteFix = true;

            deleteFixArray = new double [4];
            deleteFixCounter = 0;
            
            disableUI();
        }
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
            this.zoomInInitialize = true;
            zoomCounter = 0;
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


    /**
       *    Sets the flag so the next time the snake panel is clicked foreground
       *    intesity will be calcularted
       *    
       **/
    public void getForegroundIntensity(){        
       
        bForIntMean = true;
        
        disableUI();
        
    }
    
    
    /**
       *    Sets the flag so the next time the snake panel is clicked background
       *    intesity will be calcularted
       *    
       **/
    public void getBackgroundIntensity(){
        
        bBackIntMean = true;
        
        disableUI();

    }
    
    /** resets the zoom to the orginal image size */
    public void zoomOut(){
        zoomInInitialize = false;
        
        //resets zoom settings
        images.resetZoom();
        
        updateImagePanel();
        //redraws the image in the panel
        

    }
    
    /**
       *    Starts the initialize snake process where SnakeRaw?
       *    are the transient snake coordinates.
       *
       */
    public void addSnake(){
    
        bInitialSnake = true;
        images.setInitializing(true);
        
        //initializes the coordinate vectors, SnakeRawX and SnakeRawY
        SnakeRaw = new ArrayList<double[]>();

        disableUI();
        updateImagePanel();
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
            bFixSnake = true;
            images.setStretchFix(bFixSnake);
            disableUI();
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
       *    Convenience to calculate distance
       **/
     private double pointDistance(double[] x1, double[] x2){
        return TwoDDeformation.pointDistance(x1,x2);
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
       *    image when started through ImageJ.
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
        
        if(bInitialSnake){
            images.setRawData(SnakeRaw);
        }
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
       *    Causes a deform iterations
       */
    public void stopRunningNicely(){
        INTERRUPT=true;
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

        try{
            snakeprogram3d.HelpMessages.showAbout();
        } catch(Exception e){
            String s = "<html>" +
                       "<body>" +
                       "You are using a version of jfilament w/out jfilament3d" +
                       "<br> version info cannot be shown <br>" +
                       "see:  <br> http://athena.physics.lehigh.edu/jfilament<br>" +
                       " for more info " +
                       "</body></html>";
            final JFrame shower = new JFrame("JFilament2D About");
            JEditorPane helper = new JEditorPane("text/html",s);
            shower.setSize(400,400);
            helper.setEditable(false);

            shower.add(helper);
            shower.setVisible(true);
        }

    }

}
