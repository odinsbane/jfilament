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
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import snakeprogram.energies.*;
import snakeprogram.interactions.*;
import snakeprogram.util.AreaAndCentroid;
import snakeprogram.util.SnakesToDistanceTransform;
import snakeprogram.util.SnakesToMask;
import snakeprogram.util.TextWindow;

import javax.swing.*;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SnakeModel{

    public static int MAXLENGTH = 1500;

    private SnakeImages images;             //Contains all of the image data
    private SnakeFrame snake_panel;        //Contians the controls
    private ExecutorService executor = Executors.newFixedThreadPool(1);
    /**These contain the currently displayed snake data. x and y values*/
    private List<double[]> SnakeRaw;
    private Snake CurrentSnake;
    /**The deformation for an open deformation*/
    private TwoDDeformation curveDeformation;

    
    //state variables.
    //private boolean zoomInInitialize = false;     //Zoom intialization during mouse clicks
    //private int zoomCounter = 0;                      //Zoom has multiple stages.
    //private boolean bZoomInBox = false;          //determines if a box should be drawn on the image while moving the mouse
    private boolean RUNNING = false;               //If the snakeDeformation is running.
    private boolean INTERRUPT = false;

    //Deformation values.  These are maintained here for now, but should be moved in the near future
    private double alpha = 15;
    private double beta = 10;
    private double gamma = 400;
    private double weight = 0.5;
    private double stretch = 150;
    private double forIntMean = 255;
    private double backIntMean = 0;

    //Model Values 
    public static int squareSize = 3;            //This is the size of the square that averaging is performed over for the 'getIntForMean' ops
    private int deformIterations = 100;          //Number of iterations when a 'deformSnake' is clicked
    private double IMAGE_SIGMA = 1.01;          //For blurring
    public double MAXIMUM_SPACING = 1.;

    
    
    //What to do with the tracking data!?
    private MultipleSnakesStore SnakeStore;
    private SnakeInteraction interactor;
    private AnnotationEnergies annotations;
    /**
       *    Starts the snakes application.
       **/
    public SnakeModel(){
        SnakeStore = new MultipleSnakesStore();
        images = new SnakeImages(SnakeStore);
        snake_panel = new SnakeFrame(this);

    }

    private void submit(final Runnable r){
        executor.submit(new Runnable(){
           public void run(){
               try{
                   r.run();
               }catch(Exception e){
                   System.out.println("exception: " + e.getMessage());
                   e.printStackTrace();
               }
           }
        });
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

        int deformations = deformIterations;

        if(deformIterations<0){
            deformations = 10;
        }

        boolean continueDeforming = true;
        double size_before = 0;
        double size_after = SnakeRaw.size();
        ArrayList<double[]> previous=null;
        while(continueDeforming&&RUNNING){

            if(deformations==deformIterations){
                continueDeforming=false;
            } else{
                //check for changes.
                size_after = size_after/deformations;

                double delta = size_after - size_before;
                if(delta*delta<0.01){
                    //If the change in points is small.
                    if(previous==null){
                        previous=new ArrayList<double[]>(SnakeRaw);
                    } else{
                        if(previous.size()==SnakeRaw.size()){
                            //check the actual displacements.
                            double max_delta=0;
                            for(int i = 0;i<previous.size(); i++){
                                double[] pta = SnakeRaw.get(i);
                                double[] ptb = previous.get(i);
                                double d = (pta[0] - ptb[0])*(pta[0] - ptb[0]) + (pta[1]-ptb[1])*(pta[1] - ptb[1]);
                                max_delta = max_delta>d?max_delta:d;
                            }
                            if(max_delta>-deformIterations){
                                continueDeforming=false;
                            }
                        }
                        previous.clear();
                        previous.addAll(SnakeRaw);
                    }


                    continueDeforming=false;
                }
                size_before = size_after;
                size_after=0;

            }

            for(int j = 0; j<deformations; j++){

                //resets the value of the progress bar based on the iteration number
                int value = (int)(((j+1)*1.0/deformations)*100);
                snake_panel.updateProgressBar(value);

                curveDeformation.addSnakePoints(MAXIMUM_SPACING);
                curveDeformation.deformSnake();

                updateImagePanel();
                if(INTERRUPT){
                    RUNNING=false;
                    break;
                }
                size_after+=SnakeRaw.size();
            }
            ImagePlus original = images.getOriginalImage();
            int wx = original.getWidth() - 1;
            int hx = original.getHeight() - 1;
            //constrain
            boolean constrained = false;
            for(double[] pts: SnakeRaw){
                if(pts[0] < 0){
                    pts[0] = 0;
                    constrained = true;
                } else if(pts[0]> wx){
                    pts[0] = wx;
                    constrained = true;
                }
                if(pts[1] < 0 ){
                    pts[1] = 0;
                    constrained = true;
                } else if(pts[1] > hx){
                    pts[1] = hx;
                    constrained = true;
                }
            }
            if(constrained){
                updateImagePanel();
            }

        }

    }
    public ImageEnergy energyFactory(){

        final Set<ExternalEnergy> active = annotations==null?new HashSet<>():annotations.getActiveForces();
        final ImageEnergy image_energy = imageEnergyFactory();

        if(active.size()==0){
            return image_energy;
        } else{

            return new ImageEnergy(){

                @Override
                public double getMaxPixel(double x, double y) {
                    return image_energy.getMaxPixel(x,y);
                }

                @Override
                public double[] getImageEnergy(double x, double y) {

                    double[] sum = image_energy.getImageEnergy(x,y);
                    for(ExternalEnergy erg: active){
                        double[] f = erg.getForce(x, y);
                        sum[0] += f[0];
                        sum[1] += f[1];
                    }
                    return sum;

                }

                @Override
                public ImageProcessor getProcessor() {
                    return image_energy.getProcessor();
                }
            };

        }
    }

    public ImageEnergy imageEnergyFactory(){
        int item = snake_panel.getEnergyType();
        switch(item){
            case ImageEnergy.INTENSITY:
                return new IntensityEnergy( images.getProcessor(),IMAGE_SIGMA );
            case ImageEnergy.GRADIENT:
                return new GradientEnergy( images.getProcessor(), IMAGE_SIGMA);
            case ImageEnergy.BALLOON:
                if(checkForCurrentSnake()){
                    BalloonGradientEnergy balloon = new BalloonGradientEnergy(
                            images.getProcessor(), IMAGE_SIGMA,
                            CurrentSnake.getCoordinates(images.getCounter()) );

                    balloon.FOREGROUND = forIntMean;
                    balloon.BACKGROUND = backIntMean;
                    balloon.MAGNITUDE = stretch;

                    return balloon;
                }
            case ImageEnergy.BALLOON2:
                if(checkForCurrentSnake()){
                    BalloonIntensityEnergy balloon = new BalloonIntensityEnergy(
                            images.getProcessor(), IMAGE_SIGMA,
                            CurrentSnake.getCoordinates(images.getCounter()) );

                    balloon.FOREGROUND = forIntMean;
                    balloon.BACKGROUND = backIntMean;
                    balloon.MAGNITUDE = stretch;

                    return balloon;
                }
        }

        return null;

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
                List<double[]> cx = s.getCoordinates(frame);
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
        if(!CurrentSnake.exists(images.getCounter())) return result;
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
        List<double[]> all = CurrentSnake.getCoordinates(images.getCounter());

        if(all==null) return new double[]{0,0};

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
     * Creates a text window with the area vs time for the snakes.
     */
    public void showArea(){
        String title = "enclosed area";

        disableUI();

        StringBuilder building = new StringBuilder("#Area of closed snakes\n#frame");
        int n = 1;
        List<Snake> snakes = getSnakes();
        for(Snake snake: snakes){
            building.append("\tsnake_" + n);
            n++;
        }

        for(int i = 1; i<= images.getStackSize(); i++ ) {
            building.append("\n" + i);
            for (Snake snake : snakes) {
                double v;
                if(snake.TYPE==Snake.CLOSED_SNAKE && snake.exists(i)){
                    v = AreaAndCentroid.calculateArea(snake.getCoordinates(i));
                } else{
                    v = 0;
                }
                building.append("\t" + v);
            }
        }
        enableUI();
        new TextWindow(title, building.toString()).display();
    }

    /**
       *    Save all of the snakes data, so that they may be reloaded
       *    
       **/
    public void saveSnake(){
        disableUI();
        HashMap<String,Double> values = snake_panel.getConstants();
        String filenameSuggestion = getImageTitle().replaceAll("\\..+$", ".snakes");

        SnakeIO.writeSnakes(getFrame(),values,SnakeStore, filenameSuggestion);
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
        MultipleSnakesStore ss = SnakeIO.loadSnakes(getFrame(), values);
        snake_panel.setConstants(values);
        if(ss != null){
            SnakeStore = ss;
            images.setSnakes(SnakeStore);
            CurrentSnake = SnakeStore.getLastSnake();
        }
        enableUI();
        updateImagePanel();
    }

    public void importSnakes(MultipleSnakesStore store){

        SnakeStore = store;
        images.setSnakes(store);
        CurrentSnake = store.getLastSnake();

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

    public void repositionEnd() {

        if(checkForCurrentSnake()&&CurrentSnake.TYPE==Snake.CLOSED_SNAKE){
            SnakeInteraction si = new RepositionContourEnds(this, images, CurrentSnake);
            registerSnakeInteractor(si);
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
     * Set the current frame.
     * @param i frame to select (1-based).
     */
    public void setImageFrame(int i){
        images.setImage(i);
        updateImagePanel();
    }

    /**
     * Gets the current frame.
     *
     * @return number of the current frame (1-based)
     */
    public int getCurrentFrame(){
        return images.getCounter();
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
            snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes(), SnakeStore.indexOf(CurrentSnake));
            updateImagePanel();

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
     * Sets the raw snake for drawing purposes.
     *
     * @param raw
     */
    public void setSnakeRaw(ArrayList<double[]> raw){
        SnakeRaw = raw;
    }

    /**
     * Store a new snake.
     * @param s
     */
    public void addNewSnake(Snake s){
        CurrentSnake = s;
        SnakeStore.addSnake(s);
    }


    /**
       *    Causes the deformation of a snake to occur
       **/
    public void deformSnake(){
        if(checkForCurrentSnake()){
            if(!RUNNING){
                RUNNING = true;
                disableUI();
                submit(new DeformingRunnable() {
                    void modifySnake() throws IllegalAccessException {
                        deformRunning();
                        RUNNING = false;
                        enableUI();

                    }
                });
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

        if(SnakeRaw!=null)
            images.setRawData(SnakeRaw);
        if(annotations!=null){
            annotations.setFrame(images.getCounter());
        }
        images.setCurrentSnake(CurrentSnake);
        images.updateImagePanel();

        snake_panel.updateStackProgressionLabel(images.getCounter(),images.getStackSize());
        snake_panel.setNumberOfSnakesLabel(SnakeStore.getNumberOfSnakes(), SnakeStore.indexOf(CurrentSnake));

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
     * Clears the current snake from the current frame, without deleting the whole snake.
     */
    public void clearCurrentSnake(){
        if(CurrentSnake != null){
            Integer f = getCurrentFrame();
            if(CurrentSnake.exists(f)){
                CurrentSnake.Coordinates.remove(f);
            }
            purgeSnakes();
            updateImagePanel();
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

        gd.addNumericField("Max Length", SnakeModel.MAXLENGTH, 0);

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

    /**
     * Takes the current snake and tracks its shape for all frames forwards. This will start from the
     * current snake and proceed both forwards and backwards.
     *
     *
     */
    public void trackAllFrames(int modifiers) {

        boolean f = true;
        boolean b = false;


        if((modifiers&ActionEvent.SHIFT_MASK)>0){
            b = true;
            if((modifiers&ActionEvent.CTRL_MASK)>0){
                f=true;
            } else{
                f=false;
            }
        }

        final boolean forwards = f;
        final boolean backwards = b;
        if((!RUNNING)&&checkForCurrentSnake()){
            RUNNING = true;
            disableUI();

            submit(new DeformingRunnable() {
                void modifySnake() throws IllegalAccessException {

                    int start = images.getCounter();

                    if (forwards) {
                        while (RUNNING && images.getCounter() < images.getStackSize()) {

                            ArrayList<double[]> Xs = new ArrayList<double[]>(CurrentSnake.getCoordinates(images.getCounter()));

                            nextImage();

                            CurrentSnake.addCoordinates(images.getCounter(), Xs);

                            updateImagePanel();
                            deformRunning();
                        }

                    }
                    if (backwards) {

                        images.setImage(start);
                        while (RUNNING && images.getCounter() > 1) {

                            ArrayList<double[]> Xs = new ArrayList<double[]>(CurrentSnake.getCoordinates(images.getCounter()));

                            previousImage();

                            CurrentSnake.addCoordinates(images.getCounter(), Xs);

                            updateImagePanel();
                            deformRunning();
                        }
                    }

                    RUNNING = false;
                    enableUI();
                }
            });
        }


    }

    /**
     * Tracks the current snake on frame backwards.
     *
     */
    public void trackBackwards() {
        int frame = images.getCounter();
        if(checkForCurrentSnake()&&(frame-1)>0){

            ArrayList<double[]> Xs = new ArrayList<double[]>(CurrentSnake.getCoordinates(frame));

            previousImage();

            CurrentSnake.addCoordinates(images.getCounter(),Xs);

            updateImagePanel();
            deformSnake();
        }
    }

    /**
     * Deforms the current snake through all of the frames it exists in. Especially useful
     * for changing point sizes and ensuring that the parameter is set in every frame.
     *
     */
    public void deformAllFrames(int modifiers) {

        int b = 0;

        if((modifiers&ActionEvent.SHIFT_MASK)>0||(modifiers&ActionEvent.CTRL_MASK)>0){
            b = images.getCounter();
        }
        final int before = b;
        if(checkForCurrentSnake()&&!RUNNING){
            RUNNING = true;
            disableUI();

            submit(new DeformingRunnable() {
                void modifySnake() throws IllegalAccessException {
                    for (Integer i : CurrentSnake) {
                        if (i < before) continue;
                        images.setImage(i);
                        updateImagePanel();
                        deformRunning();


                        if (!RUNNING) {
                            break;
                        }
                    }
                    RUNNING = false;
                    enableUI();
                }
            });

        }




    }

    /**
     * Attempts to determine the foreground and background intensities based on the current snake describing the
     * desired curve.
     *
     */
    public void guessForegroundBackground() {
       if(checkForCurrentSnake()&&CurrentSnake.TYPE==Snake.CLOSED_SNAKE){

           ImageProcessor image = images.getProcessor().convertToFloat();
           ImageProcessor blurred_image = image.duplicate();
           GaussianBlur gb = new GaussianBlur();
           gb.blurGaussian(blurred_image, IMAGE_SIGMA, IMAGE_SIGMA, .01);

           double[] means = BalloonGradientEnergy.getAverageIntensity(

                   CurrentSnake.getCoordinates(images.getCounter()),
                   blurred_image
                   );
           snake_panel.ForegroundIntensity.setText(String.format("%2.2f",means[0]));
           snake_panel.BackgroundIntensity.setText(String.format("%2.2f",means[1]));
           snake_panel.setForegroundIntensity();
           snake_panel.setBackgroundIntensity();
       }

    }

    public double getSigma() {
        return IMAGE_SIGMA;
    }

    public void showAnnotationFrame() {
        if(annotations==null){

            annotations=new AnnotationEnergies(this, images);
            annotations.registerButtons(snake_panel);

        }
        annotations.setVisible(true);
    }

    public List<Snake> getSnakes() {
        List<Snake> snakes = new ArrayList();
        for(Snake s: SnakeStore){
            snakes.add(s);
        }
        return snakes;
    }

    public void startSnakeTransform() {
        if(checkForCurrentSnake()){
            SnakeInteraction si = new MoveAndRotate(this,images,CurrentSnake);
            registerSnakeInteractor(si);
        }
    }

    public void setDisplayRange() {
        double boundsLow = images.getExtremeDisplayMin();
        double boundsHigh = images.getExtremeDisplayMax();
        GenericDialog gd = new GenericDialog(String.format("Enter display range %f-%f", boundsLow, boundsHigh));

        gd.addNumericField("MIN",images.getDisplayMin(),0);
        gd.addNumericField("MAX", images.getDisplayMax(), 0);
        gd.showDialog();
        if(gd.wasCanceled()) return;

        try{
            double min = gd.getNextNumber();
            double max = gd.getNextNumber();
            if(min>=boundsLow && min<=max && max<=boundsHigh){
                images.setDisplayRange(min, max);
            }
        } catch(NumberFormatException e){
            //just in case
        }
    }

    public void startSculpting() {
        if(checkForCurrentSnake()){
            SnakeInteraction si = new SnakeSculptor(this,images,CurrentSnake);
            registerSnakeInteractor(si);
        }
    }

    public void snakesToBinaryMask() {

        SnakesToMask.createBinaryMask(images.getOriginalImage(), SnakeStore);

    }

    public void snakesToLabelledImage(){
        SnakesToMask.labelImage(images.getOriginalImage(), SnakeStore);
    }

    public void snakesToDistanceTransform(){
        SnakesToDistanceTransform.showDistanceTransform(images.getOriginalImage(), SnakeStore);
    }


    /**
     * Contains the nescessary exception catches when deforming or modifying a snake.
     *
     */
    abstract private class DeformingRunnable implements Runnable{
        public void run(){

            try{

                modifySnake();

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
                snake_panel.setNumberOfSnakesLabel(
                        SnakeStore.getNumberOfSnakes(),
                        SnakeStore.indexOf(CurrentSnake)
                );
                RUNNING=false;
                enableUI();

            }
        }

        abstract void modifySnake() throws IllegalAccessException;
    }
}

