package snakeprogram3d.display3d;

import javax.media.j3d.*;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;



/**This uses a 3D texture for 3 sets of orthoganol planes to simulate a volume
 * texture.
 *
 * @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 * 
 */
public class ThreeDSurface implements DataObject{
    
    //orignal image values
    private float xDim, yDim, zDim;
	
    //normalized image values
    float XDIM, YDIM, ZDIM;
    
    //normals for texture generation 
    private Vector4f X_PLANE,Y_PLANE,Z_PLANE;
   
    private BranchGroup BG;


    Shape3D plane;
    
    TransformGroup tg;
    
    Vector3f OFFSET;
    
    Appearance appear;
    
    BranchGroup surface;
    /**
     *
     * @param texture texture shown
     * @param xDim for scaling
     * @param yDim for scaling
     * @param zDim for scaling
     * @param zresolution still scaling.
     */
    public ThreeDSurface(Texture3D texture, float xDim, float yDim, float zDim, double zresolution){
        
        surface = new BranchGroup();
        
        createSurfaces(xDim, yDim, zDim, (float)zresolution);
        
        setTexture(texture);

        OFFSET = getOffset();
                
        surface.addChild(plane);
        
        
    
    }
    
    
    
	public void createSurfaces(float xDim, float yDim, float zDim, float zresolution) {

            plane = new Shape3D();
            plane.setPickable(false);
            this.xDim = xDim;
            this.yDim = yDim;
            this.zDim = zDim;
 
            createNormalizedCoordinates(xDim, yDim, zDim*zresolution);
        
 
            prepareAppearance();
        
            prepareGeometry();
        
	}
    
    void createNormalizedCoordinates(float x, float y, float z){
        float l = x>y?x:y;
        l = z>l?z:l;
        XDIM = x/l;
        YDIM = y/l;
        ZDIM = z/l;
        
        X_PLANE = new Vector4f(1/XDIM,0,0,0);
	Y_PLANE = new Vector4f(0,1/YDIM,0,0);
	Z_PLANE = new Vector4f(0,0,1/ZDIM,0);
        
    }
 
	private void prepareAppearance() {
 
        PolygonAttributes p = new PolygonAttributes();
        p.setCullFace(PolygonAttributes.CULL_NONE);

        Material m = new Material();
        m.setLightingEnable(false);
        m.setCapability(Material.ALLOW_COMPONENT_WRITE);
 
        TexCoordGeneration texg = new TexCoordGeneration();
        texg.setFormat(TexCoordGeneration.TEXTURE_COORDINATE_3);
        //texg.setGenMode(TexCoordGeneration.EYE_LINEAR);
        texg.setPlaneS(X_PLANE);
        texg.setPlaneT(Y_PLANE);
        texg.setPlaneR(Z_PLANE);
 
        Appearance a = new Appearance();
 
        a.setTexCoordGeneration(texg);
        a.setMaterial(m);
        a.setPolygonAttributes(p);

        a.setTransparencyAttributes(
           new TransparencyAttributes(TransparencyAttributes.NICEST, 1.0f));
        
        a.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
        
        appear = a;
        plane.setAppearance(a);
	}
 
	
 
 
	
        
    private void prepareGeometry(){

		plane.removeAllGeometries();

		generateGeometryX(xDim);
		generateGeometryY(yDim);
		generateGeometryZ(zDim);
 
 
	}
 
	/**
	 * Generates geometry along X
	 *
	 * @param num - number of planes to generate
	 */
	private void generateGeometryX(float num) {
 
		float gridSpacing = 1f/num;
        float xs;
		for (float x = 0; x <= 1; x+=gridSpacing) {
 
        	Point3f[] genCoords = new Point3f[4];
            xs = x*XDIM;
            
        	genCoords[0] = new Point3f(xs,	0,	0);
        	genCoords[1] = new Point3f(xs,	YDIM,	0);
        	genCoords[2] = new Point3f(xs,	YDIM,	ZDIM);
        	genCoords[3] = new Point3f(xs,	0,	ZDIM);
 
        	QuadArray genSquare = new QuadArray(4, QuadArray.COORDINATES);
        	genSquare.setCoordinates(0, genCoords);
 
        	plane.addGeometry(genSquare);
 
        }
	}
 
    /**
     * Generates geometry along Y
     *
     * @param num - number of planes to generate
     */
    private void generateGeometryY(float num) {
 
        float gridSpacing = 1f/num;
        float ys;
        for (float y = 0; y <= 1; y+=gridSpacing) {
            ys = y*YDIM;
            Point3f[] genCoords = new Point3f[4];
 
            genCoords[0] = new Point3f(0,	ys,	0);
            genCoords[1] = new Point3f(XDIM,	ys,	0);
            genCoords[2] = new Point3f(XDIM,	ys,	ZDIM);
            genCoords[3] = new Point3f(0,	ys,	ZDIM);
 
            QuadArray genSquare = new QuadArray(4, QuadArray.COORDINATES);
            genSquare.setCoordinates(0, genCoords);
 
            plane.addGeometry(genSquare);
 
        }
    }
 
	/**
	 * Generates geometry along Z
	 *
	 * @param num - number of planes to generate
	 */
	private void generateGeometryZ(float num) {
 
		float gridSpacing = 1f/num;
        float zs;
		for (float z = 0; z <= 1; z+=gridSpacing) {
 
        	Point3f[] genCoords = new Point3f[4];
            zs = z*ZDIM;
        	genCoords[0] = new Point3f(0,	0,	zs);
        	genCoords[1] = new Point3f(XDIM,	0,	zs);
        	genCoords[2] = new Point3f(XDIM,	YDIM,	zs);
        	genCoords[3] = new Point3f(0,	YDIM,	zs);
 
        	QuadArray genSquare = new QuadArray(4, QuadArray.COORDINATES);
        	genSquare.setCoordinates(0, genCoords);
 
        	plane.addGeometry(genSquare);
 
        }
	}
 
 
	public Vector3f getOffset(){
        
        return new Vector3f(-XDIM/2, -YDIM/2, 0);
    }
    
    public void setTexture(Texture3D tex) {
        appear.setTexture(tex);
    }   
    
    
    
    
    
    
    public BranchGroup getBranchGroup(){
        if(BG==null){
            BG = new BranchGroup();
            BG.setCapability(BranchGroup.ALLOW_DETACH);
            
            tg = new TransformGroup();
            tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            Transform3D tt = new Transform3D();
            
            tt.setTranslation(OFFSET);



            tg.setTransform(tt);
            tg.addChild(surface);
            BG.addChild(tg);
        }
        
        return BG;
        
    }
    
}
