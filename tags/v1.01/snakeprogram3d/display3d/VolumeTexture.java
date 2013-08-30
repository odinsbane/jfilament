package snakeprogram3d.display3d;

import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture3D;
import javax.vecmath.Color3f;
import javax.vecmath.Vector4f;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;


/**
 *
 * The volume texture stores some state information concerning the max
 * min values for changing the contrast, but mostly it is just a Texture3D.
 * @author Matt Smith
 * 
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class VolumeTexture extends Texture3D{
    private double[][][] double3d;
    
    //image indecies
    private int xDim,yDim,zDim;
    
    //float XDIM, YDIM, ZDIM;
 
	//WHITE:
	private Color3f color = new Color3f(1,1,1);
 
    
	private double cal_min;
	private double cal_max;
	private double clampedMin;
	private double clampedMax;
    
    private double CLAMP_MIN;
    private double CLAMP_MAX;

    /**
     * Creates the volume texture.
     *
     *
     * @param double3d - the data that will be used to create a 3d texture
     * @param cl_min - lower bounds to display, anything below is transparent.
     * @param cl_max - upper bounds anything above is opaque white
     */
    public VolumeTexture(double[][][] double3d, double cl_min, double cl_max) {

        super(Texture.BASE_LEVEL, Texture.RGBA, double3d.length, double3d[0].length, double3d[0][0].length);
        
        CLAMP_MIN = cl_min;
        
        CLAMP_MAX = cl_max;
        
        this.double3d = double3d;

        this.xDim = double3d.length;
        this.yDim = double3d[0].length;
        this.zDim = double3d[0][0].length;



        findMinAndMaxValues();
        
        setClamps();
		
 
        clamp();
 
    }

    /**
     * Finds the max and min values in the image.
     *
     */
    private void findMinAndMaxValues() {
 
		cal_min = Double.MAX_VALUE;
		cal_max = -Double.MAX_VALUE;
                
		for (int k = 0; k < zDim; k++) {
			for (int j = 0; j < yDim; j++) {
				for (int i = 0; i < xDim; i++) {
					if (double3d[i][j][k] > cal_max) cal_max = double3d[i][j][k];
					if (double3d[i][j][k] < cal_min) cal_min = double3d[i][j][k];
				}
			}
		}
	}

    /**
     * Sets the clamping values according to what the CLAMP_MIN/CLAMP_MAX
     *
     */
    private void setClamps(){
        
            clampedMin = cal_min + (cal_max - cal_min)*CLAMP_MIN;
            clampedMax = cal_min + (cal_max - cal_min)*CLAMP_MAX;

        
    }

    /**
     *
     * Creates the data for the Texture3D
     *
     */
    private void clamp() {

                ImageComponent3D pArray = new ImageComponent3D(ImageComponent.FORMAT_RGBA, xDim, yDim, zDim);
 
		ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
 
		ComponentColorModel colorModel =
			new ComponentColorModel(colorSpace, true, false,
					Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
 
		WritableRaster raster =
			colorModel.createCompatibleWritableRaster(xDim, yDim);
 
		BufferedImage bImage =
			new BufferedImage(colorModel, raster, false, null);
 
		byte[] byteData = ((DataBufferByte)raster.getDataBuffer()).getData();
 
		//COLORS: [0;255] 0 - black, 255 - white
		//TRANSP: [0;255] 0 - fully transparent, 255 - opaque
		final Vector4f color4f = new Vector4f(color.x, color.y, color.z, 1);

                

		for (int z = 0; z < zDim; z++) {
			int index = 0;
			for (int  y = 0; y < yDim ; y++) {
				for (int x = 0; x < xDim; x++) {
 
					double data = double3d[x][y][z];
					if (data < clampedMin) data = clampedMin;
					if (data > clampedMax) data = clampedMax;
					double scale = (data - clampedMin)/(clampedMax - clampedMin);

                                        
					Vector4f v = new Vector4f(color4f);
					v.scale((float)scale);
 
					//R
					byteData[index++] = (byte)(255*v.x);
					//G
					byteData[index++] = (byte)(255*v.y);
					//B
					byteData[index++] = (byte)(255*v.z);
					//transparency
					byteData[index++] = (byte)(255*v.w);
 
				}
			}
 
			pArray.set(z, bImage);
 
		}
 
		
		setImage(0, pArray);
		setEnable(true);
		setMinFilter(Texture.BASE_LEVEL_LINEAR);
		setMagFilter(Texture.BASE_LEVEL_LINEAR);
		setBoundaryModeS(Texture.CLAMP);
		setBoundaryModeT(Texture.CLAMP);
		setBoundaryModeR(Texture.CLAMP);
 
		
        }


}
