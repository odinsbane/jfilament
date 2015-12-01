package snakeprogram3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.Color;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *    A class for accessing original and or smoothed image data.
 *
 *   @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 **/
public class SnakeImages{
    double MAXW = 400;
    double MAXH = 400;
    
    private double ZRESOLUTION; //This is the spacing between slices in PIXELS!!!
    private double SIGMA;
    
    public int SLICES, FRAMES;
    
    private ImagePlus imageOriginal;
    private ImagePlus imageDrawSnake;

    private GaussianBlur gb = new GaussianBlur();
    private double OW,OH;
    private double DH,DW;
    
    private ImageStack stackLoad;
    private int CURRENT_SLICE, CURRENT_FRAME;

    private HashMap<Integer, ImageProcessor> BLURRED_IMAGES;
    
    public double MAXPIXEL, MINPIXEL;

    /** determines how quickly the image falls to zero out of bounds*/
    double FALLOFF;
    //int squareSize = 6;

    File IMAGEFILE;

    //These are for tracking the last point
    Double mouseX,mouseY;
    
    boolean DRAW_SNAKE,     //if we need to draw a snake
                ZOOMIN,            //is the current view 'zoomed' in
                ZOOMINBOX,      //whether to draw the zoom box
                FOLLOW,             //follow the moust for an extra pt.
                HASIMAGE,
                INITIALIZING;
    
    private Rectangle ZoomBox;
    
    private MultipleSnakesStore SnakeStore;
    private Snake CurrentSnake;
    
    /** Z-gaussian filter kernel */
    float[] KERNEL;
    //This is the snake data
    
    List<double[]> SnakeRaw;

    /**
     * Creates a new snake images which keeps track of the 2D display and
     * some geometry issues.
     *
     * @param ss the snake store this keeps synchronized with.
     */
    SnakeImages(MultipleSnakesStore ss){

        BLURRED_IMAGES = new HashMap<>();

        DRAW_SNAKE = false;
        ZOOMIN = false;
        ZOOMINBOX = false;
        FOLLOW = false;
        HASIMAGE=false;
        SnakeStore = ss;
    
    }
    
    public boolean hasImage(){
    
        return HASIMAGE;
    }
    
    Image getImage(){
        return imageDrawSnake.getImage();
    }
    
    int getDrawHeight(){
        return imageDrawSnake.getHeight();
    }
    
    int getDrawWidth(){
        return imageDrawSnake.getWidth();
    }
    
    public double getZResolution(){
        return ZRESOLUTION;
    }
    ImageProcessor getCurrentProcessor(){
        return stackLoad.getProcessor(CURRENT_FRAME*SLICES + CURRENT_SLICE);
    }
    
    public void updateImagePanel(){
    
        ImageProcessor improc = stackLoad.getProcessor(CURRENT_FRAME*SLICES + CURRENT_SLICE);
        
        ImageProcessor imp = improc.duplicate().convertToRGB();
        imp.setColor(Color.RED);
        imp.setLineWidth(2);

        if(ZOOMIN){
            
            imp.setRoi( ZoomBox );
            
            double scalex = MAXW/ZoomBox.width;
            double scaley = MAXH/ZoomBox.height;
            
            
            if(scalex>scaley){
               DH = MAXH;
               DW = scaley*ZoomBox.width;
            }
            else{
                DH = scalex*ZoomBox.height;
                DW = MAXW;
            }

            //resizes the zoomed section
            imp = imp.crop();

        } else{
            if(OH<MAXH&&OW<MAXW){
                DH = OH;
                DW = OW;
            } else{
                double scalex = MAXW/OW;
                double scaley = MAXH/OH;
            
            
                if(scalex>scaley){
                   DH = MAXH;
                   DW = scaley*OW;
                }
                else{
                    DH = scalex*OH;
                    DW = MAXW;
                }
            }
        
        }
        
        imp = imp.resize((int)DW, (int)DH);
        
        //draws a box around the area to be zoomed in on
        if(ZOOMINBOX){
            imp.setColor(Color.ORANGE);
            imp.drawRect( (int)toZoomX(ZoomBox.x),(int)toZoomY(ZoomBox.y),
                                  (int)toZoomX(ZoomBox.width),(int)toZoomY(ZoomBox.height));
        }
        //draws the snake to the screen
        if(INITIALIZING&&SnakeRaw!=null){
                drawRawSnake(imp);
        } else{
            drawSnakes(imp);
        }

        

        imageDrawSnake.setProcessor("update", imp);
    }
    

    
    public void trackingZoomBox(int x, int y){
        int zw = (int)fromZoomX(x) - ZoomBox.x;
        int zh = (int)fromZoomY(y) - ZoomBox.y;
        if(zw>0&&zh>0)
            ZoomBox.setSize(zw,zh);       
    }
    public void setZoomInBox(boolean v){
        ZOOMINBOX = v;
    }
    
    public void setZoomIn(boolean v){
        ZOOMIN = v;
    }
    
    
    public void setRawData(List<double[]> xv){
            SnakeRaw = xv;
            
        if(xv!=null&&xv.size()>0){
            setDrawSnake(true);
        } else{
            setDrawSnake(false);
        }
            
    }
    
    public boolean checkSnake(){
        return DRAW_SNAKE;
    
    }
    public void setDrawSnake(boolean v){
        DRAW_SNAKE = v;
    }
    
    public void setFollow(boolean v){
        FOLLOW = v;
    }
    public ImagePlus getOriginalPlus(){
        return imageOriginal;
    }

    /**
       *    This is used for following the mouse
       *    it requres the image coordinates
       **/
    public void updateMousePosition(double x, double y){
        mouseX = x;
        mouseY = y;
    
    }
    
     /**           
        *    This method is called by updateDisplay() and draws the points in SnakeRaw to the screen
        *    connected by a line. Before doing this, it transforms the coordinates in these vectors based on the current
        *    zoom status. The new coordinates are then stored in the vectors, SnakeDrawX and SnakeDrawY. If the program
        *    is drawing a closed contour, the program will connect the first and last points of the snake.
        **/
    public void drawRawSnake(ImageProcessor improc){

        
        //sets line color
        improc.setColor(Color.RED);
        
        //creates vectors to store transformed coordinates
        ArrayList<double[]> SnakeDraw = new ArrayList<double[]>();
    
        for(int n = 0; n < SnakeRaw.size(); n++){
                //only converts points that are in the area of the frame being zoomed
            double[] o_pt = SnakeRaw.get(n);
            double[] zoom_pt = {toZoomX(o_pt[0]),toZoomY(o_pt[1])};
            SnakeDraw.add(zoom_pt);
                    
        }
                        
        //while the snake is being added, the end of the snake is connected to the current mouse position
        if(FOLLOW&&mouseX!=null){
            SnakeDraw.add(new double[]{ mouseX,mouseY });
        }       

        //draws the snake to the processor
        for(int i = 0; i<(SnakeDraw.size()-1); i++){
            double[] ptA = SnakeDraw.get(i);
            double[] ptB = SnakeDraw.get(i+1);
            improc.drawLine((int)ptA[0],(int)ptA[1],(int)ptB[0],(int)ptB[1]);
        }
        
    }
    
    public void drawSnake(Snake s,ImageProcessor improc){
        
        //sets line color
        if(s==CurrentSnake)
            improc.setColor(Color.RED);
        else
            improc.setColor(Color.YELLOW);
        if(s.exists(getCurrentFrame())){
            List<double[]> psnake = s.getCoordinates(getCurrentFrame());
            //creates vectors to store transformed coordinates
            ArrayList<double[] > SnakeDraw = new ArrayList<double[]>();
            
            //transforms the coordinates based on the zoom
            for(double[] old_pt: psnake)
                SnakeDraw.add(new double[]{toZoomX(old_pt[0]),toZoomY(old_pt[1])});
                    
            
            //draws the snake to the processor
            for(int i = 0; i<(SnakeDraw.size()-1); i++){
                double[] ptA = SnakeDraw.get(i);
                double[] ptB = SnakeDraw.get(i+1);
                
                improc.drawLine((int)ptA[0],(int)ptA[1],(int)ptB[0],(int)ptB[1]);
            }
        }
    }
    
    
    
    /**
       *    Draws all of the snakes in SnakeStore
       **/
    public void drawSnakes(ImageProcessor imp){
        for(Snake s: SnakeStore)
            drawSnake(s,imp);
    
    }
    
    /**
       *    Sets the zoom box to be the whole image
       **/
    public void resetZoom(){
        if(HASIMAGE){
            
            ZoomBox = new Rectangle(0,0,1,1);
        
            setZoomIn(false);
        }
    }
    
    public void setZoomLocation(int x, int y){
        ZoomBox.setLocation((int)fromZoomX(x),(int)fromZoomY(y));
    }
    
        
   
    
    /** this method takes an X coordinates and returns its position in the original, un-zoomed, image */
    public double fromZoomX(double newX){

        return (ZOOMIN)?(newX*ZoomBox.width/DW)+ZoomBox.x:(newX*OW/DW);
    }

    /** this method takes an Y coordinates and returns position in the original, un-zoomed, image */
    public double fromZoomY(double newY){
        
        return (ZOOMIN)?(newY*ZoomBox.height/DH)+ZoomBox.y:newY*OH/DH;
    }

    /** this method takes an X coordinate on the original image and finds its new position on a zoomed image */
    public double toZoomX(double oldX){
        
        return (ZOOMIN)?(oldX-ZoomBox.x)*DW/ZoomBox.width:(oldX)*DW/OW;
    }

     /**  takes an Y coordinate on the original image and finds its new position on a zoomed image  */
     public double toZoomY(double oldY){
        return (ZOOMIN)?(oldY-ZoomBox.y)*DH/ZoomBox.height:(oldY)*DH/(OH);
    }
    
    
    /**
       * Gets a file name via the swing file chooser dialog
       **/
    public ImagePlus getAndLoadImage(JFrame xx){
        String fname = SnakeIO.getOpenFileName(xx);

        if (fname!=null) {
                ImagePlus ni = new ImagePlus(fname);

                if(ni.getProcessor()!=null)
                    return ni;
                else{
                    JOptionPane.showMessageDialog(xx,"Could not open: " + fname +" Check filename" );
                    
                }
        }
        
        return null;
    }
    
    /** Returns the total number of slices */
    public int getNSlices(){
        
        return SLICES;
    
    }
    
    public int getNFrames(){
        return FRAMES;
    }
        
    public int getCurrentFrame(){
        return CURRENT_FRAME;
    }
    
    public int getCurrentSlice(){
        return CURRENT_SLICE;
    }
    
    public void nextImage(){
        if(CURRENT_SLICE+1 <= SLICES)
            CURRENT_SLICE++;
    }
    
    public void previousImage(){
        if(CURRENT_SLICE>1)
            CURRENT_SLICE--;
    }
    
    public void nextFrame(){
        if(CURRENT_FRAME<FRAMES-1)
            CURRENT_FRAME++;
    }
    
    public void previousFrame(){
        if(CURRENT_FRAME>0)
            CURRENT_FRAME--;
    }
    
    double getAveragedValue(double x, double y, double z, int ss){
              

          double max = 0;

          int half = ss/2;
          int cc = 0;
          for(int i = -half; i<=half; i++){
               for(int j = -half; j<=half; j++){
                   
                        cc++;
                        max += getPixel(x + i,y + j,z);

                   
               }
           }
            
        
            return max/cc;
    }
    
 
    
    public void loadImage(ImagePlus implus){
            
            imageOriginal = implus;
            
            FileInfo fi = implus.getOriginalFileInfo();
            try{
                IMAGEFILE = new File(fi.directory, fi.fileName);
            }catch(NullPointerException npe){
                npe.printStackTrace();
                IMAGEFILE=null;
            }
            BLURRED_IMAGES.clear();
            
            OW = imageOriginal.getWidth();
            OH = imageOriginal.getHeight();
            
            imageDrawSnake = new ImagePlus("display", imageOriginal.getProcessor().convertToRGB());
            stackLoad = imageOriginal.getImageStack();//creates a stack of the image(s)
            
            HASIMAGE = true;
            CURRENT_SLICE = 1;
            CURRENT_FRAME = 0;
            
            FRAMES = implus.getNFrames();
            SLICES = implus.getNSlices();

            resetZoom();
                
    }
    
    public void setInitializing(boolean v){
        INITIALIZING = v;
    }
    
    public void setCurrentSnake(Snake cs){
        CurrentSnake = cs;
    }
    
    public void setSnakes(MultipleSnakesStore mss){
        SnakeStore = mss;
    }
    
    /** not a good function */
    public double[] getAutoIntensities(){
        
        
        
        float sum = 0f;
        float sum_squared = 0f;
        double tally = 0;
        double max = 0;
        double min = 1e6;
        ImageStack istack = imageOriginal.getStack();
        for(int i = 1; i<=imageOriginal.getNSlices(); i++){
            ImageProcessor improc = istack.getProcessor(i).convertToFloat();
            for(float v: (float[])improc.getPixels()){
                sum_squared += v*v;
                sum += v;
                tally++;
                min = min<v?min:v;
                max = max>v?max:v;
            }
        }
        double stdev = Math.sqrt(sum_squared/tally + Math.pow(sum/tally,2));
        double high = sum/tally + stdev;
        double low = sum/tally - stdev;
        high = (high>max)?0.9*max:high;
        low = (low<min)?min:low;
        
        MAXPIXEL = max;
        MINPIXEL = min;
        
        FALLOFF = (MAXPIXEL-MINPIXEL)/16;

        
        return new double[] { high, low };
        
    }
    /** 
       *    returns the height in PIXELS zero height corresponds to frame 1
       *@param slice frame number starts at 1.
       *
       **/
    public double heightFromSlice(double slice){
        return (slice - 1)*ZRESOLUTION;
    
    }
    
    /** finds the height using the current slice as the reference */
    public double heightFromSlice(){
    
        return heightFromSlice(CURRENT_SLICE);
    }
    
    /**
       *    returns the slice as a double, which gives the fraction between slices for interpolating.
       *    a height of zero corresponds to slice of 1, as per ImageJ slice.
       **/
    public double sliceFromHeight(double height){
       return height/ZRESOLUTION + 1;
    }
    
    /** 
       *    returns the height in PIXELS zero height corresponds to frame 1
       *@param value frame number starts at 1.
       *
       **/
    public void setZResolution(double value){
        ZRESOLUTION=value;
    }
    
    /**
     * Gets the Smoothed pixel value from the image.  This value is smoothed in all three
     * directions.
     *
     * @param x cnet in pixels.
     * @param y cnet in pixels.
     * @param z cnet in pixels.
     * @return  pixel value
     */
    public double getPixel(double x, double y, double z){
        double value;
        if(SIGMA>0){

            value = KERNEL[0]*getPixelA(x,y,z);
            double lz = z;
            double hz = z;
            for(int i = 1; i<KERNEL.length; i++){

                lz = lz>=0?lz - 1:lz;
                hz = hz<=SLICES*ZRESOLUTION?hz+1: hz;

                value += KERNEL[i]*getPixelA(x-0.5,y-0.5,lz);
                value += KERNEL[i]*getPixelA(x-0.5,y-0.5,hz);

            }
        } else{
            value = getPixelA(x-0.5,y-0.5,z);
        }
        return value;
    }

    /**
     * Gets the interpolated point corresponding tot he following position.  It is
     * important to notice that z corresponds to the height in pixels, as obtained from
     * sliceFromHeight.
     *
     * @see double getPixel(double, double, int)
     *
     * @param x
     * @param y
     * @param z
     * @return
     */
    private double getPixelA(double x, double y, double z){
        double slice = sliceFromHeight(z);
        if(slice<=1){
            double I = getPixel(x,y,1 + CURRENT_FRAME*SLICES);
            slice = 1 - slice;
            return (- slice*slice)*(FALLOFF) + I;

        } else if(slice>=SLICES){
            
            double I = getPixel(x,y,SLICES + CURRENT_FRAME*SLICES);
            double dz = SLICES  - slice;
            return (- dz*dz)*(FALLOFF) + I;
        } else{
            int low = (int)slice + CURRENT_FRAME*SLICES;
            int high = low + 1;
            double a = getPixel(x,y,low);
            double b = getPixel(x,y,high);
            double t = CURRENT_FRAME*SLICES + slice - low;
            
            
            return ThreeDCurveDeformation.interpolate(a,b,t);
            
        
        }
    
    }
    
    
    /** returns a value corresponding to the specific slice.  This will check for a blurred image and create
       * one if it doesn't already exist. IMPORTANT this does not scale, so passing an int can cause trouble
       **/
    private double getPixel(double x, double y, int z){
        if(!BLURRED_IMAGES.containsKey(z)){
            ImageProcessor blurred_image = stackLoad.getProcessor(z).duplicate();
            gb.blurGaussian(blurred_image, SIGMA, SIGMA, .01);
            BLURRED_IMAGES.put(z,blurred_image);
            
        }
        
        return BLURRED_IMAGES.get(z).getInterpolatedPixel(x,y);
    }
    /**
     * Overload
     *
     * @param xyz
     * @return
     */
    double getPixel(double[] xyz){
        return getPixel(xyz[0],xyz[1],xyz[2]);
    }
    /**
        *
        *   This will use the class variables square size, and the blurred image to find
        *   the maximum pixel value in the square.  This is used for determining the stretch
        *   force on the filament.
        *   
        *   doesn't count region, inside of snake.
        *   @param xyz starting position
        *   @param dir direction of filament end
        *   
        **/
    double getMaxPixel(double[] xyz, double[] dir){
        double[] a = new double[3];
        double[] b = new double[3];
        //create 2 axis perpendicular to the direction
        if(dir[0]!=0 || dir[1]!=0){
            a[1] = Math.pow(dir[0],2) /(Math.pow(dir[0],2) + Math.pow(dir[1],2));
            a[0] = Math.sqrt(1 - a[1]);
            a[1] = Math.sqrt(a[1]);
            
            b[0] = - dir[2]*a[1];
            b[1] = dir[2]*a[0];
            b[2] = dir[0]*a[1] - dir[1]*a[0];
        } else {
            
            a[0] = 1;
            b[1] = 1;
            
        }
        
        
        double max_pixel = 0;
        int half = 2;
        double[] pt = new double[3];
        int k,i,j,l;
        double np;
        for(k = 1; k<=half; k++){
            for(i = -half; i<=half; i++){
                for(j = -half; j<=half; j++){
                    for(l = 0; l<3; l++)
                        pt[l] = xyz[l] + i*a[l] + j*b[l] + k*dir[l];
                    np = getPixel(pt);
                    max_pixel = (np>max_pixel)?np:max_pixel;
                }
            }
        }
        return max_pixel;
    }
    
    /**
     *
     * @return height of original image in px
     */
    public int getHeight(){
        return imageOriginal.getHeight();
    }

    /**
     *
     * @return width of original image in px
     */
    public int getWidth(){
        return imageOriginal.getWidth();
    }

    /**
     *
     * @return slices times zresolution
     */
    public double getDepth(){
        return (imageOriginal.getNSlices() - 1)*ZRESOLUTION;
    
    }
    
    
    public String getTitle(){   
        if(IMAGEFILE != null)
            return IMAGEFILE.getName();
        else
            return "";
    }
    
    public File getFile(){
        return IMAGEFILE;
    }
    
    public void setImageSmoothing(double v){
        SIGMA = v;

        KERNEL = gb.makeGaussianKernel(SIGMA, 0.1, 3*(int)ZRESOLUTION)[0];

        BLURRED_IMAGES.clear();
        
    }
    public void setMaxDrawingBounds(int w, int h){
        MAXW = w;
        MAXH = h;
    }
}
