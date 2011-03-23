package snakeprogram3d;

import snakeprogram3d.display3d.QueryDevice;
import snakeprogram3d.display3d.VolumeTexture;

import javax.swing.*;
import java.awt.*;

/**
 * Class for linking the 2D image plus image stack with the 3D volume stacks.
 * Originally used a buffered image, now creates 3D data from the snake_images.
 *
 * @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 * 
 */
public class SnakeBufferedImages{
    //State
    public int CURRENT_FRAME, FRAMES, SLICES;
    int HEIGHT, WIDTH;
    double DEPTH, ZRESOLUTION;
    
    SnakeImages snake_images;

    Frame frame;
    
    double[][][] data;
    
    VolumeTexture VT;
    
    double CLAMP_MIN, CLAMP_MAX;

    private QueryDevice PROPERTIES;

    /** If hardware support is sufficient */
    public boolean TEST;

    /** REAL uses a resolution equivalent to the image being displayed*/
    public boolean REAL;

    /** toggled by model */
    public boolean USER_REAL;

    /** Size of image if 'not real' */
    int N = 32;


    /**
     * Sets up image geomtry for creating volume data.  This class is created
     * after an image has successfully been loaded, then it performs the checks
     * to determine if JFilament3D is able to run.  It check for java3d
     * installation, further it checks if the graphics card can handle the
     * current image dimensions.
     *
     * @param frame a frame to parent error dialogs.
     * @param snake2d Stores the image plus data.
     */
    SnakeBufferedImages(Frame frame, SnakeImages snake2d){
        PROPERTIES = null;
        this.frame = frame;
        //check for java3d installation
        try{
            PROPERTIES = new QueryDevice();
        } catch(NoClassDefFoundError e){
            String message = "Error Loading Java3D.  This is a classpath error\n" +
                    "either you don't have java3d or the jar files are not in your classpath \n";
            JOptionPane.showMessageDialog(frame,message + e.getMessage());

        } catch(UnsatisfiedLinkError e){
            String message = "Error Loading Java3D.  This is a native library error\n" +
                               "windows users need the java3d .dll files in their path\n";
            JOptionPane.showMessageDialog(frame,message + e.getMessage());

        }
        USER_REAL = true;
        snake_images = snake2d;
        REAL = true;
        CURRENT_FRAME = snake2d.getCurrentFrame();
        CLAMP_MIN = 0.1;
        CLAMP_MAX = 0.7;

        data = new double[snake_images.getWidth()][snake_images.getHeight()][snake_images.getNSlices()];
        updateGeometry();
        
    }
    public void updateGeometry(){

        //set up geometry.
        ZRESOLUTION = snake_images.getZResolution();

        HEIGHT = snake_images.getHeight();
        WIDTH = snake_images.getWidth();
        DEPTH = snake_images.getDepth();


        SLICES = snake_images.getNSlices();
        FRAMES = snake_images.getNFrames();
        
        //see if the image is supported by graphics card. failures here can still allow 3d deformation but not visualization.
        if(PROPERTIES!=null){
            //3d texture test.
            if(!PROPERTIES.test()) {


                TEST=false;
                JOptionPane.showMessageDialog(frame,"Error Loading 3D display.  Your hardware does not support 3D textures:\n");


            //3d texture size check
            }else if(PROPERTIES.MAXH<HEIGHT||PROPERTIES.MAXW<WIDTH||PROPERTIES.MAXD<SLICES){

                if(REAL)
                    JOptionPane.showMessageDialog(frame,"Error Loading 3D display.  Your texture is too large using a smaller\n"+
                                                         "version of texture so details may be lost\n" +
                                                         " H: " + HEIGHT + " W: " + WIDTH + " D: " + SLICES + " max values\n"+
                                                         "H: " + PROPERTIES.MAXH + " W: " + PROPERTIES.MAXW + " D: " + PROPERTIES.MAXD);
                TEST=true;
                REAL = false;

            //3d texture power of two, and if image is a power of 2 check
            }else if(!PROPERTIES.nonpowersoftwo){
                TEST=true;

                int i = 1;

                boolean fail = false;
                while(i<HEIGHT)
                    i = i<<2;
                if(i!=HEIGHT)
                    fail=true;

                i = 1;
                while(i<WIDTH)
                    i=i<<2;
                if(i!=WIDTH)
                    fail=true;

                if(fail){

                    if(REAL)
                        JOptionPane.showMessageDialog(frame,"Error Loading 3D display.  Your hardware requires powers of two \n"
                            + " for texture dimensions.  Using a smaller version of texture some details \n" +
                            "maybe lost");
                    REAL = true;
                }
            //all systems go.
            } else{
                
                TEST=true;
                
            }
        }

        /** re-initialize data */
        if(REAL && USER_REAL){
            if(data.length!=WIDTH||data[0].length!=HEIGHT||data[0][0].length!=SLICES)
                data = new double[WIDTH][HEIGHT][SLICES];
        } else{
            if(data.length!=N||data[0].length!=N||data[0][0].length!=N)
                data = new double[N][N][N];
        }


    }
    
    public void createVolumeData(){
       if(TEST){
            if(REAL && USER_REAL){
                
                for(int k = 0; k<SLICES; k++){

                    for(int i = 0; i<WIDTH; i++){
                        for(int j = 0; j<HEIGHT; j++){
                            data[i][j][k] = snake_images.getPixel(i,j,ZRESOLUTION*k);
                        }
                    }

                }
            } else {
                


                double dx = WIDTH*1./N;
                double dy = HEIGHT*1./N;
                double dz = SLICES*ZRESOLUTION/N;
                for(int k = 0; k<N; k++){

                    for(int i = 0; i<N; i++){
                        for(int j = 0; j<N; j++){
                            data[i][j][k] = snake_images.getPixel(i*dx,j*dy,k*dz);
                        }
                    }

                }
            }
       }
             
           
    }

    /**
     * Provided there is volume data, this creates the texture.
     */
    public void createVolumeTexture(){

        if(data!=null)
            VT = new VolumeTexture(data, CLAMP_MIN, CLAMP_MAX);
        else
            VT = null;
        
    }

    /**
     *
     * @return the volume texture if supported otherwise null.
     */
    public VolumeTexture getVolumeTexture(){
        return VT;
        
    }
    
    public double getHeight(){
        return HEIGHT;
    }
    
    public double getWidth(){
        return WIDTH;
    }
    
    public double getDepth(){
        return DEPTH;
    }
    
    public double getZResolution(){
        return snake_images.getZResolution();
    }
    
    public int getNSlices(){
        return SLICES;
    }
    /**
     * For changing the Brightness/Contrast of the Image.  This raises the max
     * cutoff that pixels saturate at.
     */
    public void increaseMax(){
        double test = CLAMP_MAX + 0.05;
        if(test<=1){
            CLAMP_MAX = test;
        }
        
        
    }


    /**
     * For changing the Brightness/Contrast of the Image.  This lowers the max
     * cutoff that pixels saturate at.
     */
    public void decreaseMax(){
        
        double test = CLAMP_MAX-0.05;

        if(test>CLAMP_MIN){
            CLAMP_MAX = test;
        }
    }

    /**
     * For changing the Brightness/Contrast of the Image.  This raises the min
     * cutoff that pixels are considered completely transparent.
     */
    public void increaseMin(){

        double test = CLAMP_MIN+0.05;

        if(test<CLAMP_MAX){
            CLAMP_MIN = test;
        }
        
        
    }

     /**
     * For changing the Brightness/Contrast of the Image.  This lowers the min
     * cutoff that pixels are considered completely transparent.
     */
    public void decreaseMin(){

        double test = CLAMP_MIN - 0.05;

        if(test>=0){
            CLAMP_MIN = test;
        }

    }
    
    /**
     * Updates the image data for the volume displays.  Synces the frame
     * with the snake_images.
     */
    public void updateFrame(){
        
        CURRENT_FRAME = snake_images.getCurrentFrame();
        createVolumeData();
        createVolumeTexture();
        
    }


}
