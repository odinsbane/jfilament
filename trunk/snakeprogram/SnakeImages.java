package snakeprogram;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
/**
   *    A class for controlling the image data.
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
   **/
public class SnakeImages{
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

    final public int LINEWIDTH = 3;
    //These are for tracking the last point
    double[] mouseP;
    
    boolean DRAW_SNAKE,     //if we need to draw a snake
                ZOOMIN,            //is the current view 'zoomed' in
                ZOOMINBOX,      //whether to draw the zoom box
                FOLLOW,             //follow the moust for an extra pt.
                HASIMAGE,
                INITIALIZING,
                STRETCHFIX,
                MARKED;
    double[] MARK;
    final ArrayList<double[]> STATIC_MARKERS;
    
    private Rectangle ZoomBox;
    
    private MultipleSnakesStore SnakeStore;
    private Snake CurrentSnake;
    
    //This is the snake data
    
    ArrayList<double[]> SnakeRaw;
    

    SnakeImages(MultipleSnakesStore ss){
        DRAW_SNAKE = false;
        ZOOMIN = false;
        ZOOMINBOX = false;
        FOLLOW = false;
        HASIMAGE=false;
        MARKED=false;
        STATIC_MARKERS = new ArrayList<double[]>();
        SnakeStore = ss;

        gb = new GaussianBlur();
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
    
        ImageProcessor improc = stackLoad.getProcessor(imagecounter);
        
        ImageProcessor imp = improc.duplicate().convertToRGB();
        imp.setColor(Color.RED);
        imp.setLineWidth(LINEWIDTH);

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

        imageDrawSnake.setProcessor("update", imp);
    }
    
    public void setMarker(double[] pt){
        MARK = toZoom(pt);
        MARKED = true;
        
    }
    
    public void trackingZoomBox(int x, int y){
        if(x>OW) x = (int)OW;
        if(y>OH) y = (int)OH;
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
    
    
    public void setRawData(ArrayList<double[]> xv){
            SnakeRaw = xv;
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
    
        for(double[] pt: SnakeRaw){

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
        
        //sets line color
        if(s==CurrentSnake)
            improc.setColor(Color.RED);
        else
            improc.setColor(Color.YELLOW);
        if(s.exists(imagecounter)){
            ArrayList<double[]> snake = s.getCoordinates(imagecounter);
            
            //creates vectors to store transformed coordinates
            ArrayList<double[]> SnakeDraw = new ArrayList<double[]>();
            
            //transforms the coordinates based on the zoom
            for(double[] p: snake)
                SnakeDraw.add(toZoom(p));
                    
            if(s.TYPE==Snake.CLOSED_SNAKE){
                SnakeDraw.add(toZoom(snake.get(0)));
            }

            
            //draws the snake to the processor
            double[] pt1 = SnakeDraw.get(0);
            for(int i = 1; i<SnakeDraw.size(); i++){
                double[] pt2 =SnakeDraw.get(i);
                improc.drawLine((int)pt1[0],(int)pt1[1],(int)pt2[0],(int)pt2[1]);
                pt1 = pt2;
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
    
        
   
    
    /** this method takes an X coordinates and sets it to its position in the original, un-zoomed, image */
    public double fromZoomX(double newX){

        return (ZOOMIN)?(newX*ZoomBox.width/DW)+ZoomBox.x:(newX*OW/DW);
        
    }

    /** this method takes an Y coordinates and sets it to its position in the original, un-zoomed, image */
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
    
    /** transform a 2d array */
    public double[] toZoom(double[] old){
        return new double[] {toZoomX(old[0]),toZoomY(old[1])};
    }
    
    
    /**
       * Gets a file name via the swing file chooser dialog
       **/
    public void getAndLoadImage(){
        String fname = SnakeIO.getOpenFileName(new JFrame());

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
        if(imagecounter+1 <= stackLoad.getSize())
            imagecounter++;
    }
    
    public void previousImage(){
        if(imagecounter>1)
            imagecounter--;
    }
    
    
    public double getAveragedValue(double x, double y, int ss){
              
          ImageProcessor improc = getProcessor();

          ImageProcessor blurred_image =improc.duplicate();
                     
          double max = 0;

          gb.blurGaussian(blurred_image, 1.01, 1.01, .01);

                int half = ss/2;
                int cc = 0;
                
                for(int i = -half; i<=half; i++){
                    for(double d: blurred_image.getLine(x - half, y + i, x + half, y+i)){
                        max += d;
                        cc++;
                    }
                }
        
            return max/cc;
    }
    
    public ImageProcessor getProcessor(){
        return stackLoad.getProcessor(imagecounter);
    }
    
    public void loadImage(ImagePlus implus){
            
            imageOriginal = implus;
            
            OW = imageOriginal.getWidth();
            OH = imageOriginal.getHeight();
            
            imageDrawSnake = new ImagePlus("display", imageOriginal.getProcessor().convertToRGB());
            stackLoad = imageOriginal.getImageStack();//creates a stack of the image(s)
            
            HASIMAGE = true;
            imagecounter = 1;

            resetZoom();
            
            FILENAME=implus.getTitle();
            
    
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
        
        ArrayList<double[]> xs = s.getCoordinates(frame);
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
}
