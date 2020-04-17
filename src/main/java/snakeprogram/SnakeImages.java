package snakeprogram;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import snakeprogram.util.AreaAndCentroid;

import javax.swing.*;
import java.awt.Color;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
/**
   *    A class for controlling the image data.
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
   **/
public class SnakeImages{
    double DISPLAY_RANGE_MIN = 0;
    double DISPLAY_RANGE_MAX = 255;
    double DISPLAY_MAX = Double.MAX_VALUE;
    double DISPLAY_MIN = 0;

    //TODO These don't need to be final.
    final double MAXW = 672;
    final double MAXH = 512;
    
    String FILENAME;
    
    private ImagePlus imageOriginal;
    private ImagePlus imageDrawSnake;

    final private GaussianBlur gb;
    private double OW,OH;
    private double DH,DW;
    
    private ImageStack stackLoad;
    private int imagecounter;

    static public int LINEWIDTH = 3;
    //These are for tracking the last point
    double[] mouseP;
    
    boolean DRAW_SNAKE,     //if we need to draw a snake
                ZOOMIN,            //is the current view 'zoomed' in
                ZOOMINBOX,      //whether to draw the zoom box
                FOLLOW,             //follow the mouse for an extra pt.
                HASIMAGE,
                INITIALIZING,
                STRETCHFIX,
                MARKED;
    double[] MARK;
    final List<double[]> STATIC_MARKERS;
    
    private Rectangle zoomBox;
    
    private MultipleSnakesStore snakeStore;
    private Snake currentSnake;
    
    //This is the snake data
    
    List<double[]> snakeRaw;
    HashSet<ProcDrawable> drawables = new HashSet<ProcDrawable>();
    ArrayList<ImageCounterListener> counterListeners = new ArrayList<ImageCounterListener>();
    boolean DRAW_IDS = false;

    SnakeImages(MultipleSnakesStore ss){
        DRAW_SNAKE = false;
        ZOOMIN = false;
        ZOOMINBOX = false;
        FOLLOW = false;
        HASIMAGE=false;
        MARKED=false;
        STATIC_MARKERS = new ArrayList<double[]>();
        snakeStore = ss;

        gb = new GaussianBlur();
    }

    public void setDisplayRange(double min, double max) {
        DISPLAY_RANGE_MIN = min;
        DISPLAY_RANGE_MAX = max;

        updateImagePanel();

    }

    public double getExtremeDisplayMax(){
        return DISPLAY_MAX;
    }

    public double getExtremeDisplayMin(){
        return DISPLAY_MIN;
    }

    public double getDisplayMin() {
        return DISPLAY_RANGE_MIN;
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



    public void updateImagePanel(){
        ImageProcessor improc = stackLoad.getProcessor(imagecounter).duplicate();
        improc.setMinAndMax(DISPLAY_RANGE_MIN, DISPLAY_RANGE_MAX);
        ImageProcessor imp = improc.convertToRGB();
        imp.setColor(Color.RED);
        imp.setLineWidth(LINEWIDTH);

        if(ZOOMIN){
            
            imp.setRoi(zoomBox);
            
            double scalex = MAXW/ zoomBox.width;
            double scaley = MAXH/ zoomBox.height;
            
            
            if(scalex>scaley){
               DH = MAXH;
               DW = scaley* zoomBox.width;
            }
            else{
                DH = scalex* zoomBox.height;
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
            imp.drawRect( (int)toZoomX(zoomBox.x),(int)toZoomY(zoomBox.y),
                                  (int)toZoomX(zoomBox.width),(int)toZoomY(zoomBox.height));
        }
        //draws the snake to the screen
        if(INITIALIZING&& snakeRaw !=null){
                drawRawSnake(imp);
        } else{
            drawSnakes(imp);
        }

        if(MARKED){
            imp.setColor(Color.BLUE);
            imp.setLineWidth(4);
            imp.drawDot((int)MARK[0],(int)MARK[1]);
            MARKED=false;
        }
        
        if(!STRETCHFIX&&STATIC_MARKERS.size()>0){
            imp.setColor(Color.BLUE);
            imp.setLineWidth(4);
            for(double[] m: STATIC_MARKERS){
                double[] mark = toZoom(m);
                imp.drawDot((int)mark[0],(int)mark[1]);
                
            }
        }
        
        if(STRETCHFIX&&STATIC_MARKERS.size()>1){
            imp.setColor(Color.BLUE);
            double[] markA = toZoom(STATIC_MARKERS.get(0));
            double[] markB = toZoom(STATIC_MARKERS.get(1));
            imp.drawLine((int)markA[0], (int)markA[1], (int)markB[0], (int)markB[1]);
        }

        if(drawables.size()>0){
            Transform tranny = new Transform(){

                @Override
                public double[] transform(double[] pt) {
                    return toZoom(pt);
                }
            };
            for(ProcDrawable drawable: drawables){
                drawable.draw(imp, tranny);
            }
        }
        imageDrawSnake.setProcessor("update", imp);
    }

    public void setMarker(double[] pt){
        MARK = toZoom(pt);
        MARKED = true;
        
    }
    
    public void trackingZoomBox(int x, int y){
        if(x>OW) x = (int)OW;
        if(y>OH) y = (int)OH;
        int zw = (int)fromZoomX(x) - zoomBox.x;
        int zh = (int)fromZoomY(y) - zoomBox.y;
        if(zw>0&&zh>0)
            zoomBox.setSize(zw,zh);
    }
    public void setZoomInBox(boolean v){
        ZOOMINBOX = v;
    }
    
    public void setZoomIn(boolean v){
        ZOOMIN = v;
    }
    
    
    public void setRawData(List<double[]> xv){
            snakeRaw = xv;
        if(xv!=null&&xv.size()>0){
            setDrawSnake(true);
        } else{
            setDrawSnake(false);
        }
            
    }
    
    public void addStaticMarker(double[] pt){
        STATIC_MARKERS.add(pt);
    }
    
    public void clearStaticMarkers(){
        STATIC_MARKERS.clear();
    }

    public void setDrawSnake(boolean v){
        DRAW_SNAKE = v;
    }
    
    public void setFollow(boolean v){
        FOLLOW = v;
    }
    
    public void setStretchFix(boolean v){
        STRETCHFIX = v;
    }

    /**
       *    This is used for following the mouse
       *    it requres the image coordinates
       **/
    public void updateMousePosition(double x, double y){
        mouseP = new double[]{x,y};
    
    }
    
     /**           
        *    This method is called by updateImagePanel() and draws the points in SnakeRawX and SnakeRawY to the screen
        *    connected by a line. Before doing this, it transforms the coordinates in these vectors based on the current
        *    zoom status. The new coordinates are then stored in the vectors, SnakeDrawX and SnakeDrawY. If the program
        *    is drawing a closed contour, the program will connect the first and last points of the snake.
        **/
    public void drawRawSnake(ImageProcessor improc){

        
        //sets line color
        improc.setColor(Color.RED);
        
        //creates vectors to store transformed coordinates
        ArrayList<double[]> SnakeDraw = new ArrayList<double[]>();
    
        for(double[] pt: snakeRaw){

            SnakeDraw.add(new double[]{toZoomX(pt[0]),toZoomY(pt[1])});
                    
        }
                        
        //while the snake is being added, the end of the snake is connected to the current mouse position
        if(FOLLOW&&mouseP!=null){
            SnakeDraw.add(mouseP);
        }       

        Iterator<double[]> it = SnakeDraw.iterator();
        //draws the snake to the processor
        double[] pt1,pt2;
        if(it.hasNext()){
             pt1 = it.next();
            while(it.hasNext()){
                pt2 =it.next();
                improc.drawLine((int)pt1[0],(int)pt1[1],(int)pt2[0],(int)pt2[1]);
                pt1 = pt2;
            }
        }
        
    }
    
    public void drawSnake(Snake s,ImageProcessor improc){

        if(s.exists(imagecounter)){
            List<double[]> snake = s.getCoordinates(imagecounter);

            //creates vectors to store transformed coordinates
            List<double[]> SnakeDraw = new ArrayList<double[]>();

            //transforms the coordinates based on the zoom
            for(double[] p: snake)
                SnakeDraw.add(toZoom(p));

            if(s.TYPE==Snake.CLOSED_SNAKE){
                if(snake.size()>0) {
                    SnakeDraw.add(toZoom(snake.get(0)));
                }
            }

            improc.setLineWidth(2*LINEWIDTH);
            improc.setColor(Color.GREEN);
            for(double[] p: SnakeDraw){
                improc.drawDot((int)p[0], (int)p[1]);
            }

            if(s== currentSnake)
                improc.setColor(Color.RED);
            else
                improc.setColor(Color.YELLOW);
            improc.setLineWidth(LINEWIDTH);

            //draws the snake to the processor
            if(SnakeDraw.size()==0){
                return;
            }
            double[] pt1 = SnakeDraw.get(0);
            for(int i = 1; i<SnakeDraw.size(); i++){
                double[] pt2 =SnakeDraw.get(i);
                improc.drawLine((int)pt1[0],(int)pt1[1],(int)pt2[0],(int)pt2[1]);
                pt1 = pt2;
            }

        }
    }
    
    public void drawIds(MultipleSnakesStore ss, ImageProcessor imp){
        int frame = getCounter();
        int w = imp.getWidth();
        int h = imp.getHeight();
        for(Snake snake: ss){
            if(snake.exists(frame)){
                int id = ss.indexOf(snake);
                List<double[]> points = snake.getCoordinates(frame);
                double area = AreaAndCentroid.calculateArea(points);

                double[] center = AreaAndCentroid.calculateCentroid(area, points);
                double[] zoomed = toZoom(center);
                double[] zoomed2 = toZoom(new double[]{ center[0] + 1, center[1] + 1});
                int blockWidth = (int)(zoomed2[0] - zoomed[0]);
                int blockHeight = (int)(zoomed2[1] - zoomed[1]);

                if(zoomed[0] > 0 && zoomed[0] < w - 4*blockWidth && zoomed[1] > 0 && zoomed[1] < h - 4*blockHeight) {
                    int on = (0xff << 16) + (0xff << 8);
                    int off = (0x0 << 16) + (0x0 << 8) + (0xff);

                    for (int i = 0; i < 16; i++) {
                        int x = blockWidth*(i % 4) + (int) zoomed[0];
                        int y = blockHeight*(i / 4) + (int) zoomed[1];
                        int p = (id & (0x1 << i)) != 0 ? on : off;
                        imp.setColor(p);
                        imp.fillOval(x, y, blockWidth, blockHeight);
                    }
                }
            }
        }
    }
    
    /**
       *    Draws all of the snakes in snakeStore
       **/
    public void drawSnakes(ImageProcessor imp){
        Snake selected = null;

        for(Snake s: snakeStore) {
            if(s== currentSnake){
                continue;
            }
            drawSnake(s, imp);
        }
        if(currentSnake !=null){
            drawSnake(currentSnake, imp);
        }
        if(DRAW_IDS){
            drawIds(snakeStore, imp);
        }

    }
    
    /**
       *    Sets the zoom box to be the whole image
       **/
    public void resetZoom(){
        if(HASIMAGE){
            
            zoomBox = new Rectangle(0,0,1,1);
        
            setZoomIn(false);
        }
    }

    /**
     * Sets the location of the zoom box based on coordinates in the zoomed frame. The zoomed coordinates are used
     * because there could be initial scaling if the image is too large to display.
     *
     * @param x
     * @param y
     */
    public void setZoomLocation(int x, int y){

        zoomBox.setLocation( (int)fromZoomX(x),(int)fromZoomY(y));
    }
    
        
   
    
    /** this method takes an X coordinates and sets it to its position in the original, un-zoomed, image */
    public double fromZoomX(double newX){

        return (ZOOMIN)?(newX* zoomBox.width/DW)+ zoomBox.x:(newX*OW/DW);
        
    }

    /** this method takes an Y coordinates and sets it to its position in the original, un-zoomed, image */
    public double fromZoomY(double newY){
        
        return (ZOOMIN)?(newY* zoomBox.height/DH)+ zoomBox.y:newY*OH/DH;
    }

    /** this method takes an X coordinate on the original image and finds its new position on a zoomed image */
    public double toZoomX(double oldX){
    
        return (ZOOMIN)?(oldX- zoomBox.x)*DW/ zoomBox.width:(oldX)*DW/OW;
    }
    
     /**  takes an Y coordinate on the original image and finds its new position on a zoomed image  */
     public double toZoomY(double oldY){
        return (ZOOMIN)?(oldY- zoomBox.y)*DH/ zoomBox.height:(oldY)*DH/(OH);
    }
    
    /** transform a 2d array */
    public double[] toZoom(double[] old){
        return new double[] {toZoomX(old[0]),toZoomY(old[1])};
    }
    
    
    /**
       * Gets a file name via the swing file chooser dialog
       **/
    public void getAndLoadImage(){
        String fname = SnakeIO.getOpenFileName(new JFrame(),"Select Image to Open");

        if (fname!=null) {
                ImagePlus ni = new ImagePlus(fname);
                if(ni.getProcessor()!=null)
                    loadImage(ni);
                else
                    JOptionPane.showMessageDialog(new JFrame(),"Could not open: " + fname +" Check filename" );    
        }
        
    
    }
    
    public int getStackSize(){
        
        return stackLoad.getSize();
    
    }
    
    public int getCounter(){
        return imagecounter;
    }
    
    public void nextImage(){
        if(imagecounter<stackLoad.getSize())
            setImage(imagecounter+1);
    }
    
    public void previousImage(){
        if(imagecounter>1)
            setImage(imagecounter-1);
    }

    public void setImage(int i){
        if(i>0&&i<=stackLoad.getSize()){
            imagecounter=i;
            for(ImageCounterListener peon: counterListeners){
                peon.setFrame(i);
            }
        } else{
            throw new IllegalArgumentException("index of: " + i + "is out of image bounds[1:" + stackLoad.getSize()+ "])");
        }


    }

    /**
     * Gets the average pixel value, using the current image blurred with a gaussian.
     * @param x
     * @param y
     * @param ss
     * @param sigma
     * @return
     */
    public double getAveragedValue(double x, double y, int ss, double sigma){
              
          ImageProcessor improc = getProcessor();

          ImageProcessor blurred_image =improc.convertToFloat();
          if(blurred_image==improc){
              blurred_image = improc.duplicate();
          }

          double max = 0;

          gb.blurGaussian(blurred_image, sigma, sigma, .01);

        int half = ss/2;
        int cc = 0;

        for(int i = -half; i<=half; i++){
            for(int j = -half; j<=half; j++){
                double d = blurred_image.getInterpolatedValue(x+i, y+j);
                max += d;
                cc++;
            }
        }

            return max/cc;
    }
    
    public ImageProcessor getProcessor(){
        return stackLoad.getProcessor(imagecounter);
    }

    public ImagePlus getOriginalImage(){
        return imageOriginal;
    }

    public void loadImage(ImagePlus implus){
            int stack_size = -1;
            if(hasImage()){
                stack_size = getStackSize();
            }
            imageOriginal = implus;

            DISPLAY_RANGE_MIN = implus.getDisplayRangeMin();
            DISPLAY_RANGE_MAX = implus.getDisplayRangeMax();

            DISPLAY_MAX = DISPLAY_RANGE_MAX;
            DISPLAY_MIN = DISPLAY_RANGE_MIN;

            imageDrawSnake = new ImagePlus("display", imageOriginal.getProcessor().convertToRGB());
            stackLoad = imageOriginal.getImageStack();

            int nw = imageOriginal.getWidth();
            int nh = imageOriginal.getHeight();

            if(nw==OW && nh==OH && stackLoad.getSize() == stack_size ){
                //keep the same geometry.
            } else{
                OW = imageOriginal.getWidth();
                OH = imageOriginal.getHeight();
                imagecounter = 1;

                resetZoom();
            }
            HASIMAGE = true;
            FILENAME=implus.getTitle();
            
    
    }
    
    public void setInitializing(boolean v){
        INITIALIZING = v;
    }
    
    public void setCurrentSnake(Snake cs){
        currentSnake = cs;
    }
    
    public void setSnakes(MultipleSnakesStore mss){
        snakeStore = mss;
    }
    
    public double[] getAutoIntensities() throws java.lang.NullPointerException{
        
        ImageProcessor improc = imageOriginal.getProcessor();
        
        try{
            int value = improc.getAutoThreshold();
            
            double lowsum = 0;
            double lowcount = 0;
            
            double highsum = 0;
            double highcount = 0;
            
            for(int i = 0;i<improc.getHeight(); i++){
                for(int j = 0; j<improc.getWidth(); j++){
                    float v = improc.getf(j,i);
                    if(v>value*1.05){
                        highsum += v;
                        highcount += 1;
                    } else {
                        lowsum += v;
                        lowcount += 1;
                    }
                }
            }
            
            highcount = highcount==0?1:highcount;
            lowcount = lowcount==0?1:lowcount;
            
            
            return new double[] { highsum/highcount,
                                                lowsum/lowcount
                                               };
        } catch(java.lang.NullPointerException e){
            throw e;
        }
    }
    
    /**
       *    This is a static version of drawSnake for use with an external program.
       *    It doesn't set the color or anything, it just draws the snake.
       **/
    static public void drawSnake(Snake s, ImageProcessor ip,int frame){
        
        List<double[]> xs = s.getCoordinates(frame);
        Iterator<double[]> it = xs.iterator();
        //draws the snake to the processor
        double[] pt1,pt2;
        if(it.hasNext()){
             pt1 = it.next();
            while(it.hasNext()){
                pt2 =it.next();
                ip.drawLine((int)pt1[0],(int)pt1[1],(int)pt2[0],(int)pt2[1]);
                pt1 = pt2;
            }
        }
    }
    
    public String getTitle(){
		return FILENAME;
	}

    public void addDrawable(ProcDrawable drawable){

        drawables.add(drawable);

    }

    public void removeDrawable(ProcDrawable drawable){
        drawables.remove(drawable);
    }

    public double getDisplayMax() {
        return DISPLAY_RANGE_MAX;
    }

    public int[] getZoomLocation() {
        return new int[] { (int) zoomBox.getX(), (int) zoomBox.getY() };
    }

    public boolean isZoom() {

        return ZOOMIN;
    }

    public int getZoomWidth() {
        return (int)zoomBox.getWidth();
    }

    public int getZoomHeight(){
        return (int)zoomBox.getHeight();
    }

    public boolean getDrawIds() {
        return DRAW_IDS;
    }

    public void setDrawIds(boolean t){
        DRAW_IDS = t;
    }
}

