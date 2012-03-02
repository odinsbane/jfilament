package snakeprogram3d;
/**  This class stores all of the state data and variables, it will make the decisions and final call.  It is designed to be the immediate
 *    executioner of ui implemented function calls.
 *    @author Matt Smith, Lisa Vasko
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 *  
 **/

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import snakeprogram3d.display3d.DataCanvas;
import snakeprogram3d.display3d.InteractiveView;
import snakeprogram3d.display3d.ThreeDSnake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SnakeModel{

    private SnakeImages images;             //Contains all of the image data
    private SnakeFrame snake_panel;        //Contians the controls
    /**These contain the currently displayed snake data. x and y values*/
    private ArrayList<double[]> SnakeRaw;
    private Snake CurrentSnake;
    /**The deformation for an open deformation*/
    private ThreeDDeformation curveDeformation;

    private ThreeDSnake TDS;
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
    private boolean INTERRUPT = false;           //if escape has been pressed during an iteration.
    private boolean SELECTING = false;
    private double[] deleteFixArray;    //Stores all of the points to be use for the delete fix algoright (2pts with x,y)
    private int deleteFixCounter = 0;                           //keeps track of the number of clicks
    
    
    //Deformation values.  These are maintained here for now, but should be moved in the near future
    private double alpha,beta,gamma,weight,stretch,forIntMean,backIntMean, resolution;
    private int deformIterations;           //Number of iterations when a 'deformSnake' is clicked

    //Model Values 
    private int squareSize = 3;                       //Size of the square that averaging is performed over for the 'getIntForMean' ops
    
    private MultipleSnakesStore SnakeStore;

    private InteractiveView VTS;
    
    private SnakeBufferedImages SBI;

    private PowderQueue PROC;

    private int deformation_type=0;

    public final static int CURVE_DEFORMATION = 0;
    public final static int CONTOUR_DEFORMATION=1;

    /**
       *    Starts the snakes application.
       **/
    SnakeModel(){
        SnakeStore = new MultipleSnakesStore();
        
        images = new SnakeImages(SnakeStore);
        
        snake_panel = new SnakeFrame(this);
        setDefaultConstants();

        PROC = new PowderQueue();
        PROC.start();
    }
    
     /**    
     *   Deforms the Current Snake.  This method should not be executed on the
     *     event thread but instead should be placed on the Snake event loop, PROC.
      *
     *
     * @throws IllegalAccessException occurs when the snake is too short.
     **/
    public void deformRunning() throws java.lang.IllegalAccessException{
        
        snake_panel.initializeProgressBar();
        
        SnakeRaw = CurrentSnake.getCoordinates(images.getCurrentFrame());

        if(deformation_type==CURVE_DEFORMATION){

            curveDeformation = new ThreeDCurveDeformation(SnakeRaw, images, resolution);

        } else{

            curveDeformation = new ThreeDContourDeformation(SnakeRaw, images, resolution);

        }

        //initializes curveDeformation with appropriate constants
        resetDeformation();

        //checks for a passive interrupt.
        INTERRUPT=false;
        for(int j = 0; j<deformIterations; j++){
           
            //resets the value of the progress bar based on the iteration number
            int value = (int)(((j+1)*1.0/deformIterations)*100);
            snake_panel.updateProgressBar(value);
            
            curveDeformation.addSnakePoints();
            curveDeformation.deformSnake();
            updateDisplay();

            if(INTERRUPT)
                break;
        }

      RUNNING = false;
    }
    
    /**
     *    When the mouse is pressed over the snake panel, transforms the click
     *    to a 3d event
     *
     * @param evt the generated mouse event.
     **/
    public void snakePanelMousePressed(MouseEvent evt){
        //create a zoom-in area
        if(zoomInInitialize){
            zoomStep(evt);            
            return;
        }
        
        ThreeDEvent tde = new ThreeDEvent(images.fromZoomX(evt.getX()),images.fromZoomY(evt.getY()),images.heightFromSlice());
        if(SwingUtilities.isLeftMouseButton(evt))
            tde.setType(ThreeDEvent.LEFTCLICK);
        else if(SwingUtilities.isRightMouseButton(evt))
            tde.setType(ThreeDEvent.RIGHTCLICK);
        
        mousePressed(tde);
    }
    
    /**
     *    Handles all mouse events in the 3d image coordnate space.
     *
     * @param tde Event that is handled in 3d image space.
     **/
    public void mousePressed(ThreeDEvent tde){
        
        //add a snake to the image
        if(bInitialSnake)
            initializeSnakeClicked(tde);
        

        //gets the foreground mean intensity.
        if(bForIntMean && tde.type==ThreeDEvent.LEFTCLICK)
            getForegroundClicked(tde);
            
        //background 
        if(bBackIntMean && tde.type==ThreeDEvent.LEFTCLICK)
            getBackgroundClicked(tde);
       
        //"stretch fix" adds a point onto the nearest end.
        if(bFixSnake)
            stretchFixClicked(tde);

        //cut off the end
        if(bDeleteFix){
            deleteEndFixClicked(tde);
        }

        //delete from the middle
        if(bDeleteMFix)
            deleteMiddleFixClicked(tde);
        
        
    }
    
    /**
     *    Handles two types of mouse moved events.  2d Events are processed
     *    for zooming and adding a snake.  Then a 3d event is generated for
     *    updating the 3d views.
     *
     * @param evt mouse event.
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
        
        ThreeDEvent tde = new ThreeDEvent(images.fromZoomX(evt.getX()),images.fromZoomY(evt.getY()),images.heightFromSlice());
        mouseMoved(tde);
    }

    /**
     *  Generated during a mouse move event, from 2d panel or the 3d views.
     *
     * @param tde event in image space
     */
    public void mouseMoved(ThreeDEvent tde){
        if(TDS!=null){
            final double[] p;
            if(bDeleteFix||bDeleteMFix)
                p = getClosestSnakePoint(tde.x,tde.y,tde.z);
            else
                p = new double[] {tde.x,tde.y,tde.z};

            PROC.submitJob(new Runnable(){
                public void run(){
                    TDS.updateCursor(p[0],p[1],p[2]);
                    VTS.updateCursor(p[0],p[1],p[2]);
                }
            });
        }
        
    }

    /**
     * For getting the closest point on the current snake.
     *
     * @param x cnet
     * @param y cnet
     * @param z cnet
     * @return xyz array.
     */
    public double[] getClosestSnakePoint(double x, double y, double z){
        double[] n = new double[]{x,y,z};
        ArrayList<double[]> pts = CurrentSnake.getCoordinates(images.getCurrentFrame());
        double min = Double.MAX_VALUE;
        double d;
        double[] ret = null;
        for(double[] pt: pts){
            d = pointDistance(pt,n);
            if(d<min){
                ret = pt;
                min = d;
            }
        }
        return ret;
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
     *
     * @param evt 2d event in panel space.
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
        }else if(zoomInInitialize && SwingUtilities.isRightMouseButton(evt)&&zoomCounter==1){
            
                bZoomInBox = false;
                images.setZoomInBox(false);
            
                images.trackingZoomBox(x,y);
                zoomCounter++;
                
                images.setZoomIn(true);

                enableUI();                

                zoomInInitialize = false;
                updateImagePanel();

                
        }

    }
    
    /**
       *
       *    To delete a portion of the snake specified by the user with two clicks
       *    The first click must be somewhere in the middle of the snake  to specify 
       *    what is removed.  The end closest to the second click will be removed.
       *
       *
       * @param tde event in image space
     **/
    private void deleteEndFixClicked(ThreeDEvent tde){

        SnakeRaw = CurrentSnake.getCoordinates(images.getCurrentFrame());

        if(deleteFixCounter == 0){
            final double[] pt = getClosestSnakePoint(tde.x, tde.y, tde.z);
            deleteFixArray[0] = pt[0];
            deleteFixArray[1] = pt[1];
            deleteFixArray[2] = pt[2];
            PROC.submitJob(new Runnable(){
                public void run(){
                    VTS.addMarker("dend");
                    VTS.placeMarker("dend",pt);
                }
            });
        }
        
        //reads in the second click coordinates
        if(deleteFixCounter == 1){
            
            //finds the closest point in the snake to the user's first click
            double[] first_pt = {deleteFixArray[0], deleteFixArray[1], deleteFixArray[2]};
            double min = pointDistance(first_pt, SnakeRaw.get(0));
            double distance;
            int closestPoint = 0;
                
            for(int k = 1; k < SnakeRaw.size(); k++){
                distance = pointDistance(first_pt, SnakeRaw.get(k));
                    if(min > distance){
                        min = distance;
                        closestPoint = k;
                    }
            }
                               

            //determines which end of the snake is closer to the user's second click
            //it then removes the specified portion of the snake
            int sizeX = SnakeRaw.size();
            double[] second_pt = {tde.x, tde.y, tde.z};
            double distance1 = pointDistance(second_pt, SnakeRaw.get(0));
            double distance2 = pointDistance(second_pt, SnakeRaw.get(sizeX-1));
            if(distance1 < distance2){
                for(int l = 0; l < (closestPoint); l++ ){
                      
                    SnakeRaw.remove(0);
                }
            } else{
                    
                int size = SnakeRaw.size();
                for(int l = 1; l <= (size-closestPoint); l++ ){
                    SnakeRaw.remove(closestPoint);
                }
                
            }
            if(SnakeRaw.size()<=1)
                clearCurrentSnake();
                
            //redraws image
            enableUI();
            PROC.submitJob(new Runnable(){ public void run(){ updateDisplay();}});

            PROC.submitJob(new Runnable(){
                public void run(){
                    VTS.clearMarkers();}
            });
            bDeleteFix = false;


        }
        deleteFixCounter++;
        
    }
    
    /**
       *
       *    This method allows to delete a middle portion of a snake by clicking at the left end
       *    and then the right end of the section to be deleted.
       *
       *
       * @param tde image space
     **/
    private void deleteMiddleFixClicked(ThreeDEvent tde){
        

        if(deleteFixCounter == 0){
            final double[] pt = getClosestSnakePoint(tde.x, tde.y, tde.z);
            deleteFixArray[0] = pt[0];
            deleteFixArray[1] = pt[1];
            deleteFixArray[2] = pt[2];
            PROC.submitJob(new Runnable(){
                public void run(){
                    VTS.addMarker("dmid");
                    VTS.placeMarker("dmid",pt);
                }
            });
            
        }
        //reads in the second click coordinates
        if(deleteFixCounter == 1){
            final double[] pt = getClosestSnakePoint(tde.x, tde.y, tde.z);
            deleteFixArray[3] = pt[0];
            deleteFixArray[4] = pt[1];
            deleteFixArray[5] = pt[2];
            
            PROC.submitJob(new Runnable(){
                public void run(){
                    VTS.addMarker("dmid2");
                    VTS.placeMarker("dmid2",pt);
                }
            });
        }
        //third click to place a point or just delete old points.
        if(deleteFixCounter == 2){
           
            //finds the closest point in the snake to the user's first click
            double[] p1 = {deleteFixArray[0],deleteFixArray[1],deleteFixArray[2]};
            double[] p2 = {deleteFixArray[3],deleteFixArray[4],deleteFixArray[5]};

            SnakeRaw = CurrentSnake.getCoordinates(images.getCurrentFrame());
            double min1 = pointDistance(p1, SnakeRaw.get(0));
            double min2 = pointDistance(p2, SnakeRaw.get(0));
            
            double distance1,distance2;
            
            int closestPoint1 = 0;
            int closestPoint2 = 0;
            
            
            for(int k = 1; k < SnakeRaw.size(); k++){
                    
                    
                    distance1 = pointDistance(p1, SnakeRaw.get(k));
                    distance2 = pointDistance(p2, SnakeRaw.get(k));

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
            
            if(tde.type==ThreeDEvent.LEFTCLICK){
                SnakeRaw.add(l,new double[] {tde.x, tde.y, tde.z});
            }

            if(SnakeRaw.size()>0){

                try{
                    ThreeDDeformation tdd = getThreeDDeformation(SnakeRaw, images, resolution);
                    tdd.addSnakePoints();
                } catch(java.lang.IllegalAccessException e){
                    e.printStackTrace();
                }
            } else{
                clearCurrentSnake();
            }
            
            //redraws image
            enableUI();

            PROC.submitJob(new Runnable(){ public void run(){ updateDisplay();}});

            PROC.submitJob(new Runnable(){
                public void run(){
                    VTS.clearMarkers();}
            });

            bDeleteMFix = false;
        }
        deleteFixCounter++;

    }
    
    /**
       *    Will extend the nearest end of the snake to the place clicked
       *
       *
       * @param tde image space event
     **/
    private void stretchFixClicked(ThreeDEvent tde){
    
        SnakeRaw = CurrentSnake.getCoordinates(images.getCurrentFrame());

        double[] stretch_fix = new double[] {tde.x, tde.y, tde.z};
             
        int last = SnakeRaw.size()-1;
        
        if(last>0){
            double to_tail = pointDistance(SnakeRaw.get(0), stretch_fix);
            double to_head = pointDistance(SnakeRaw.get(last), stretch_fix);
 
            //if it is closer to the tail 0 then it inserts it first otherwise it inserts it last.
 
            int j = to_tail<to_head?0:last+1;
 
            SnakeRaw.add(j, stretch_fix);
        }

        ThreeDDeformation tdd = getThreeDDeformation(SnakeRaw, images, resolution);
        try{
            tdd.addSnakePoints();
        } catch(java.lang.IllegalAccessException e){
            //its fine snake was not segmented after point was added
        }
        
        enableUI();
        PROC.submitJob(new Runnable(){ public void run(){ updateDisplay();}});
        bFixSnake = false;
    
    }
    
    
    /**
       *    When initializing a snake this will add points a right click will end
       *    the process and re-enable the UI.
       *
       * @param tde image space event
     **/
    private void initializeSnakeClicked(ThreeDEvent tde){
        //adding the left-click coordinates to the SnakeRaw vectors
        if(tde.type==ThreeDEvent.LEFTCLICK){
            double[] np = {tde.x, tde.y, tde.z};
            SnakeRaw.add(np);

            images.setFollow(true);

           
        } else if (tde.type==ThreeDEvent.RIGHTCLICK){
            
            double[] np = {tde.x, tde.y, tde.z};
            SnakeRaw.add(np);
            
            if(SnakeRaw.size()>=2){
                
            
                CurrentSnake = new Snake(SnakeRaw,images.getCurrentFrame());

                SnakeStore.addSnake(CurrentSnake);

                PROC.submitJob(new ThreeDSynchronizer());
                PROC.submitJob(new ThreeDSelector());

                

            }
            
            bInitialSnake = false;
            images.setFollow(false);
            images.setInitializing(false);
            VTS.setInitializing(false);
            enableUI();

            
        }
        
        PROC.submitJob(new Runnable(){ public void run(){ updateDisplay();}});

    }
    
    /**
       *    Finds the location of the current mouse click and finds the 
       *    mean intensity about that point.  Then sets the value for 
       *    background intensity, and updates the UI.
       *
       * @param tde image space event.
     **/
    private void getBackgroundClicked(ThreeDEvent tde){
        
        backIntMean = images.getAveragedValue(tde.x, tde.y, tde.z, squareSize);
        
        bBackIntMean = false;

        //prints this mean intensity to the text box on the user interface

        String S = formatNumber(backIntMean);
        enableUI();
        snake_panel.updateBackgroundText(S);

    }

    
    /**
       *    Finds the location of the current mouse click and finds the 
       *    mean intensity about that point.  Sets the forground intensity
       *    value and updates the UI to display the value.
       *
       *
       * @param tde image space event
     **/
    private void getForegroundClicked(ThreeDEvent tde){
        
        forIntMean = images.getAveragedValue(tde.x, tde.y, tde.z, squareSize);
        
        bForIntMean = false;
        
        String S = formatNumber(forIntMean);
        
        enableUI();
        snake_panel.updateForegroundText(S);
        
    }

    /**
     * Limits the display to two decimal points, or scientific notation if
     * the number is out of a certain range.
     *
     * @param value number to be represented by a string
     * @return string
     */
    public String formatNumber(double value){
        double abs = Math.abs(value);
        String S;
        if(abs>1e6||abs<1e-2){
            S = String.format("%.1e", value);
        } else{

            S = String.format("%.2f", value);

        }
        return S;
    }
    
    /**
     *  A MouseEvent version of the snake selector
     *  @param evt original mouse event.
     * */
    public void selectSnake(MouseEvent evt){
        ThreeDEvent tde = new ThreeDEvent(images.fromZoomX(evt.getX()),images.fromZoomY(evt.getY()),images.heightFromSlice());
        if(SwingUtilities.isLeftMouseButton(evt))
            tde.setType(ThreeDEvent.LEFTCLICK);
        else if(SwingUtilities.isRightMouseButton(evt))
            tde.setType(ThreeDEvent.RIGHTCLICK);
            
        selectSnake(tde);
        
    }
    
    /**
       *    Chooses the nearest snake.  This method is called
       *    by another listener added by the snake_frame which is
       *    enabled or disable along with the rest of the GUI.
       *
       *
       * @param tde image space
     **/
    public void selectSnake(ThreeDEvent tde){
        if(SELECTING){
            double[] mouse_pt = {tde.x, tde.y, tde.z};
            double min = 1e6;

            int frame = images.getCurrentFrame();

            for(Snake s: SnakeStore){
                double distance = 1e6;
                if(s.exists(frame)){
                    ArrayList<double[]> cx = s.getCoordinates(frame);
                    int size = s.getSize(frame);
                    for(int i = 0; i<size;i++){
                        double cd = ThreeDCurveDeformation.pointDistance(mouse_pt,cx.get(i));
                        distance = distance>cd?cd:distance;
                    }
                }
                if(distance<min){
                    min = distance;
                    CurrentSnake = s;

                }

            }
            //PROC.submitJob(new Runnable(){ public void run(){ updateDisplay();}});
            PROC.submitJob(new ThreeDSelector());
            updateImagePanel();
        }
    }

    public void selectNextSnake(){
        if(SELECTING){
            CurrentSnake = SnakeStore.getNextSnake(CurrentSnake);

            PROC.submitJob(new ThreeDSelector());
            updateImagePanel();
        }
    }

    public void setSelecting(boolean v){

        SELECTING=v;

    }

    /**
     *
     * @return if the model is read to have a snake selected.
     */
    public boolean isSelecting(){
        return SELECTING;
    }

    public void selectSnake(Snake s){

        if(SnakeStore.contains(s)){
            CurrentSnake = s;

            PROC.submitJob(new ThreeDSelector());
            updateImagePanel();

        }

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
        SnakeIO.writeSnakeElongationData(snake_panel,SnakeStore,images.getNSlices());
        enableUI();
    }
    
    /**
       *    Save all of the snakes data, so that they may be reloaded
       *    
       **/
    public void saveSnake(){
        disableUI();
        SnakeIO.writeSnakes(snake_panel,SnakeStore);
        enableUI();
    }
    
    /**
       *    Loads a snake from a file and sets the constant values in 
       *    the snake panel.
       *    
       **/
    public void loadSnake(){
        disableUI();
        MultipleSnakesStore ss = SnakeIO.loadSnakes(snake_panel);
        if(ss != null){
            
            SnakeStore = ss;
            images.setSnakes(SnakeStore);
            CurrentSnake = SnakeStore.getLastSnake();
            PROC.submitJob(new ThreeDSynchronizer());
            updateImagePanel();

        }
        enableUI();
    }

    public void setMaxLength(){
        GenericDialog gd = new GenericDialog("Enter a new Max Length");

        gd.addNumericField("Max Length",SnakeApplication.MAXLENGTH,0);

        gd.showDialog();
        if(gd.wasCanceled()) return;

        int value = (int)gd.getNextNumber();

        if(value>0) SnakeApplication.setMaxLength(value);

    }
    
    /**
       *    Goes to the next frame and copies the current snake.  
       *    then deforms that snake.
       **/
    public void trackSnake(){
    
        if(checkForCurrentSnake()){
            int frame = images.getCurrentFrame();
            
            ArrayList<double[]> Xs = new ArrayList<double[]>(CurrentSnake.getCoordinates(frame));

            nextFrame();

            CurrentSnake.addCoordinates(images.getCurrentFrame(),Xs);


            deformSnake();
        }
    }
    /**
       *    Goes to the previous frame and copies the current snake.
       *    then deforms that snake.
       **/
    public void trackSnakeBackwards(){

        if(checkForCurrentSnake()){
            int frame = images.getCurrentFrame();

            ArrayList<double[]> Xs = new ArrayList<double[]>(CurrentSnake.getCoordinates(frame));

            previousFrame();


            CurrentSnake.addCoordinates(images.getCurrentFrame(),Xs);

            deformSnake();
        }
    }
    /**
       *    Disables the UI and begins the Delete Middle fix
       */
    public void deleteMiddleFix(){
        if(checkForCurrentSnake()){
            bDeleteMFix = true;

            deleteFixArray = new double [6];
            deleteFixCounter = 0;

            disableUI();
            
            snake_panel.enableImageDirections();

        }
    }
    
    /**
       *    Disables UI and Deletes and end
       */
    public void deleteEndFix(){
        if(checkForCurrentSnake()){
            bDeleteFix = true;

            deleteFixArray = new double [6];
            deleteFixCounter = 0;
            
            disableUI();
            snake_panel.enableImageDirections();
            
        }
    }
    
    /** moves to the previous image */
    public void previousImage(){
    
        images.previousImage();
        PROC.submitJob(new Runnable(){ public void run(){ updateDisplay();}});
        
    }
    
    /** moves to the next image */
    public void nextImage(){
        
        images.nextImage();
        PROC.submitJob(new Runnable(){ public void run(){ updateDisplay();}});
    
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
            
            deleteSnake(CurrentSnake);
            
    }

    /**
     * Delets a snake
     * @param s the snake that will be deleted.
     */
    public void deleteSnake(Snake s){
        SnakeStore.deleteSnake(s);
        if(s==CurrentSnake)
            CurrentSnake = SnakeStore.getLastSnake();

        snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());

        PROC.submitJob(new ThreeDSynchronizer());
        //PROC.submitJob(new ThreeDSelector());

        
        PROC.submitJob(new Runnable(){ public void run(){ updateDisplay();}});
        
    }
    
    /** removes all snakes from the current frame */
    public void clearScreen(){
        SnakeRaw = new ArrayList<double[]>();
        zoomInInitialize = false;
                
        //resets zoom settings
        images.resetZoom();
        int frame = images.getCurrentFrame();
        for(Snake s: SnakeStore){
        
            s.clearSnake(frame);
            
        }

        validateSnakes();

        snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());

        PROC.submitJob(new Runnable(){ public void run(){ updateDisplay();}});

    
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
        
        images.resetZoom();
        
        updateImagePanel();
        

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
        snake_panel.enableImageDirections();
        //PROC.submitJob(new Runnable(){ public void run(){ updateDisplay();}});
    }
    
    /**
       *    Causes the deformation of a snake to occur
       **/
    public void deformSnake(){
        if(checkForCurrentSnake()){
            if(!RUNNING){

                RUNNING = true;


                //Creates a runnable that is submitted to the main thread.
                Runnable x = new Runnable(){

                   public void run(){
                       disableUI();
                       try{
                       
                            deformRunning();


                       //too long.
                       } catch(IllegalAccessException e){
                            JOptionPane.showMessageDialog(snake_panel.getFrame(),
                            "Your snake is too long, max length: " + SnakeApplication.MAXLENGTH
                                 + " , your snake: " + e.getMessage() + "\n change the max length in the data menu.");


                           //too short
                       }catch(ArrayIndexOutOfBoundsException e){

                           JOptionPane.showMessageDialog(snake_panel.getFrame(),
                            "Your snake died this could be due to a too high forground intensity.");

                            CurrentSnake.clearSnake(images.getCurrentFrame());
                            validateSnakes();
                       
                       } catch(IllegalArgumentException e){
                           JOptionPane.showMessageDialog(snake_panel.getFrame(),
                            "Your snake died this could be due to a too high forground intensity.\n" +
                            "or a background intensity higher than your forground intensity.");

                            CurrentSnake.clearSnake(images.getCurrentFrame());
                            validateSnakes();

                       }catch(Exception e){
                            e.printStackTrace();
                       }finally{
                            RUNNING=false;
                            //updateDisplay();
                            enableUI();
                           
                       }
                   }
           
                };
                PROC.submitJob(x);
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
            
            disableUI();
            snake_panel.enableImageDirections();

        }

    }
    
    /**
       *    These are short access to images so the frame doesn't need to know about the
       *    SnakeImages class
       **/
    public void getAndLoadImage(){
    
        ImagePlus new_plus = images.getAndLoadImage(snake_panel.FRAME);
        
        if(new_plus!=null)
            loadImage(new_plus);
        
        
    
    }
    
    
    //********************************************
    //      OTHER METHODS
    //********************************************
    
    /**
     *    Tries to determine the forground/background values
     * automatically.  Usually fails.
     */
    public void autoDetectIntensities(){
        double[] intensities = images.getAutoIntensities();
        forIntMean = intensities[0];
        backIntMean = intensities[1];
                
        snake_panel.updateBackgroundText(formatNumber(backIntMean));
        snake_panel.updateForegroundText(formatNumber(forIntMean));
        
    }

    /**
       *    short access to images so the frame doesn't need to know about the
       *    SnakeImages class
       *
       * @return height of space for drawing image.
     **/
    public int getDrawHeight(){
    
        return images.getDrawHeight();
    
    }
    

    /**
       *    short access to images so the frame doesn't need to know about the
       *    SnakeImages class
       *
       * @return awt version of image.
     **/
    public Image getImage(){
    
        return images.getImage();
        
    }


    /**
       *    These are short access to images so the frame doesn't need to know about the
       *    SnakeImages class
       *
       * @return width available for drawing image.
     **/
     public int getDrawWidth(){
        return images.getDrawWidth();
     }
    
    /**
       *    Gets the frame used for dialogs.
       *
       * @return main frame of application
     **/
    public JFrame getFrame(){
        return snake_panel.getFrame();
    }


    /**
     *
     * @return number of slices per frame
     */
    public int getNSlices(){
        return SBI.SLICES;

    }

    /**
     *
     * @return number of frames.
     */
    public int getNFrames(){
        return SBI.FRAMES;
    }

    

     /**
       *    After an image has been opened to make sure it has
       *     properly been loaded.
       *    @return image ready
      **/
    public boolean hasImage(){
        return images.hasImage();
    }
    
    /**
     * 
     * @return the number of iterations when deform iterations is clicked.
     */
    public int getDeformIterations(){
        return deformIterations;

    }
    /*****************************************************************/
    //              SETTERS
    /*****************************************************************/
    /**
     *
     * @param di number of iterations per deform iteration click.
     */
    public void setDeformIterations(int di){
        deformIterations = di;
    }

    /**
     * Sets the zresolution and refreshes the display.
     *
     * @param mon zspacing
     */
    public void setZResolution(double mon){
        
        images.setZResolution(mon);
        
        finishDisplay();
    
    }
    
    public void setResolution(double res){
        this.resolution = res;
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
     *  sets the sigma of the image smoothing term.
     *  @param v standard deviation of the gaussian filter.
     **/
    public void setImageSmoothing(double v){

        images.setImageSmoothing(v);

        updateFrame();
    }

    /**
       *        Sets the current contour/curve deformation values to the new image values
       *        and the current constants values
       *
       * @throws IllegalAccessException when the snake is too short the matrix is not init'd.
     **/
    private void resetDeformation() throws IllegalAccessException{
        
        curveDeformation.setBeta(beta);
        curveDeformation.setGamma(gamma);
        curveDeformation.setWeight(weight);
        curveDeformation.setStretch(stretch);
        curveDeformation.setAlpha(alpha);
        curveDeformation.setForInt(forIntMean);
        curveDeformation.setBackInt(backIntMean);
        
        
        curveDeformation.addSnakePoints();
            
        curveDeformation.initializeMatrix();


    }

    
    /**
       *    Calls ThreeDDeformation.pointDistance
       *
       * @param x1 xyz point
       * @param x2 xyz point
       * @return distance between two points.
     **/
     private double pointDistance(double[] x1,double[] x2){
        return ThreeDCurveDeformation.pointDistance(x1,x2);
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
            snake_panel.updateProgressBar(1);
            autoDetectIntensities();

        }
        finishDisplay();
    }

    /**
     * Creates volume data and submits the job to the main process.  this is called
     * whenever the zresolution changes, image smoothing changes, +/- MIN & MAX changes.
     * 
     */
    public void finishDisplay(){
        final SnakeModel local = this;
        if(images.hasImage()){

            Runnable x = new Runnable(){
                    
                    public void run(){
                        disableUI();
                        snake_panel.updateProgressBar(3);

                        
                        System.out.println("before" + (IJ.currentMemory()/1e6));
                        if(SBI==null)
                            SBI = new SnakeBufferedImages(snake_panel.getFrame(),images);
                        SBI.CURRENT_FRAME = images.getCurrentFrame();
                        
                        snake_panel.updateProgressBar(5);

                        SBI.updateGeometry();
                        
                        snake_panel.updateProgressBar(9);

                        SBI.createVolumeData();

                        System.out.println("created raster: " + (IJ.currentMemory()/1e6));

                        snake_panel.updateProgressBar(30);

                        SBI.createVolumeTexture();
                        
                        System.out.println("textured : " + (IJ.currentMemory()/1e6));

                        snake_panel.updateProgressBar(65);


                        if(TDS==null){
                            GraphicsConfiguration gc = DataCanvas.getBestConfigurationOnSameDevice(snake_panel.FRAME);
                            TDS = new ThreeDSnake(gc);
                            EventQueue.invokeLater(new Runnable(){
                                public void run(){
                                    snake_panel.set3DPanel(TDS.getComponent());
                                }
                            });
                            VTS = new InteractiveView(gc);
                            EventQueue.invokeLater(new Runnable(){
                                public void run(){
                                    snake_panel.setInteractPanel(VTS.getComponent());
                                }
                            });

                            VTS.addSnakeListener(local);
                            TDS.addSnakeListener(local);
                        }

                        TDS.reset(SBI);
                        System.out.println("ThreeD: " + (IJ.currentMemory()/1e6));

                        snake_panel.updateProgressBar(75);

                        VTS.reset(SBI);

                        System.out.println("Interactive: " + (IJ.currentMemory()/1e6));

                        snake_panel.updateProgressBar(95);

                        PROC.submitJob(new ThreeDSynchronizer());

                        updateDisplay();

                        snake_panel.updateProgressBar(100);

                        enableUI();



                        System.out.println("Finished: " + (IJ.currentMemory()/1e6));

                        
                    }
            };

            PROC.submitJob(x);
        }
    }
    
    /**
       *    Updates both displays.
       *
       **/
    public void updateDisplay(){


        updateImagePanel();

        if(VTS!=null)
            updateThreeDDisplay();
    }

    /**
     * Updates the image panel and relevant properties.
     */
    public void updateImagePanel(){
        if(bInitialSnake)
            images.setRawData(SnakeRaw);
        images.setCurrentSnake(CurrentSnake);
        images.updateImagePanel();
        java.awt.EventQueue.invokeLater(new Runnable(){
            public void run(){
                    snake_panel.updateStackProgressionLabel(images.getCurrentFrame(),images.getNFrames() - 1, images.getCurrentSlice(), images.getNSlices());
                    snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes());
                    snake_panel.lightRepaint();
                }
            }
        );
    }

    /**
     * updates the snakes positions, should be called from the PROC thread.
     */
    public void updateThreeDDisplay(){
        if(bInitialSnake)
            VTS.drawRawSnake(SnakeRaw);
        TDS.updateSnakePositions();
        VTS.updateSnakePositions();
        VTS.updateDisplay(images.getCurrentSlice());

    }


    /** stops any other input except for the MouseListener on the displayed Image */
    public void disableUI(){
        EventQueue.invokeLater(new Runnable(){
            public void run(){
                snake_panel.disableUI();
            }

        });

    }
    
    /** enables buttons */
    public void enableUI(){
        EventQueue.invokeLater(new Runnable(){
            public void run(){

                snake_panel.enableUI();
            }
        });
    }
    
    /**
       *    This is used before attempting to modify the current snake such as with
       *    a fix or deform button press.
       *
       * @return current snake is set.
     **/
    public boolean checkForCurrentSnake(){
        if(CurrentSnake==null){
        
            return false;
        } else {
            return CurrentSnake.exists(images.getCurrentFrame());
        }    
    
    }
    
    /**
       *    Causes a deform iterations
       */
    public void stopRunningNicely(){
        INTERRUPT=true;
    }

    /** gets the filename of the open imageplus.
     * @return name of image
     **/
    public String getImageTitle(){
        return images.getTitle();
    }

    /** Moves the YZ plane*/
    public void moveUp(){
        VTS.moveUp();
    }

    /** Moves the YZ plane*/
    public void moveDown(){
        VTS.moveDown();
    }

    /*
     * Moves the ZX plane down
     */
    public void wipeDown(){
        VTS.wipeDown();
    }

    /*
     * Moves the ZX plane up
     */
    public void wipeUp(){
        VTS.wipeUp();
    }

    /**
     * For changing the view configuration.
     */
    public void switchCards(){
        EventQueue.invokeLater(new Runnable(){
            public void run(){

                snake_panel.switchCards();
            }
        });
    }

    /**
     * When the view changes size this can be 800 or 400, should be scalable though.
     * 
     *
     * @param W width
     * @param H height
     */
    public void setMaxDrawingBounds(int W, int H){
        images.setMaxDrawingBounds(W,H);
    }

    /**
     * Next volume stack in time.
     */
    public void nextFrame(){
        disableUI();

        images.nextFrame();
        
        updateFrame();

        updateImagePanel();
    }

    /**
     * Previous volume stack in time.
     */
    public void previousFrame(){
        disableUI();

        images.previousFrame();
        
        updateFrame();

        updateImagePanel();
    
    }

    /** for adjusting brightness/contrast */
    public void increaseMax(){
        disableUI();
        SBI.increaseMax();

        updateFrame();
    }
    
    /** for adjusting brightness/contrast */
    public void decreaseMax(){
        disableUI();
        SBI.decreaseMax();

        updateFrame();
    }
    /** for adjusting brightness/contrast */
    public void increaseMin(){
        disableUI();
        SBI.increaseMin();

        updateFrame();
    }


    /** for adjusting brightness/contrast */
    public void decreaseMin(){
        disableUI();
        SBI.decreaseMin();
        updateFrame();
    }

    public void updateFrame(){
        Runnable x = new Runnable(){
            public void run(){
                disableUI();
                snake_panel.updateProgressBar(1);

                SBI.updateFrame();
                snake_panel.updateProgressBar(60);

                TDS.refreshImages();
                snake_panel.updateProgressBar(75);


                VTS.refreshImages();

                snake_panel.updateProgressBar(90);
                
            }
        };
        PROC.submitJob(x);
        PROC.submitJob(new ThreeDSynchronizer());

        x = new Runnable(){
            public void run(){
                snake_panel.updateProgressBar(99);

                enableUI();
                snake_panel.updateProgressBar(100);
            }
        };
        PROC.submitJob(x);
    }

    public void setDefaultConstants(){
        HashMap<String,Double> ret_value = new HashMap<String,Double>();
        ret_value.put("alpha",1.0);
        ret_value.put("beta",1.0);
        ret_value.put("background",0.0);
        ret_value.put("weight",1.0);
        ret_value.put("stretch",100.0);
        ret_value.put("spacing",1.0);
        ret_value.put("smoothing",0.0);
        ret_value.put("gamma",100.0);
        ret_value.put("foreground", 255.0);
        ret_value.put("zresolution", 1.0);
        snake_panel.setConstants(ret_value);
        snake_panel.disableUI();
        snake_panel.field_watcher.enableOpenImage();
    }

    /**
     * Causes the model to use a reduced version of the 3d image.
     *
     * @param t Real resolution when false, and reduced resolution when true.
     */
    public void reduce3D(boolean t){
        SBI.USER_REAL = !t;
        finishDisplay();
    }



    public void clearCurrentSnake(){
        CurrentSnake.clearSnake(images.getCurrentFrame());
        validateSnakes();
        updateImagePanel();

    }
    public void validateSnakes(){
        SnakeStore.purgeSnakes();
        PROC.submitJob(new ThreeDSynchronizer());
    }

    private ThreeDDeformation getThreeDDeformation(ArrayList<double[]> verts, SnakeImages images, double max_seg){
        ThreeDDeformation tdd;
        switch(deformation_type){
            case CURVE_DEFORMATION:
                tdd = new ThreeDCurveDeformation(verts, images, max_seg);
                break;
            case CONTOUR_DEFORMATION:
                tdd = new ThreeDContourDeformation(SnakeRaw, images, resolution);
                break;
            default:
                tdd = new ThreeDCurveDeformation(SnakeRaw, images, resolution);
        }
        return tdd;
    }

    void setDeformType(int i){
        deformation_type = i;

    }

    class ThreeDSynchronizer implements Runnable{
        public void run(){
            TDS.synchronizeSnakes(SnakeStore);
            VTS.synchronizeSnakes(SnakeStore);
            VTS.setSelected(CurrentSnake);
            TDS.setSelected(CurrentSnake);
        }

    }

    class ThreeDSelector implements Runnable{

        public void run(){
            VTS.setSelected(CurrentSnake);
            TDS.setSelected(CurrentSnake);
        }

    }
}


/**
 * This should take all of the effort off of the Swing event loop and
 * perform any modifications here.  The ideal is to inline all operations
 * that access and modify snake operations.
 */
class PowderQueue extends Thread{
    ConcurrentLinkedQueue<Runnable> JOBS;
    public PowderQueue(){

        JOBS = new ConcurrentLinkedQueue<Runnable>();
        setName("Snake Loop");
    }

    /**
     * Runs a loop removing all jobs, first in first out until they are completed
     * then it waits for the jobs to be finished.
     * 
     */
    @Override
    public void run(){
        for(;;){

            if(JOBS.isEmpty())
                 waitForJobs();

            JOBS.poll().run();

        }

    }
    public synchronized void submitJob(Runnable job){
        JOBS.add(job);
        notify();
    }

    /** This appears to have a possibility for dead lock. */
    public synchronized void waitForJobs(){

        try{
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }



}