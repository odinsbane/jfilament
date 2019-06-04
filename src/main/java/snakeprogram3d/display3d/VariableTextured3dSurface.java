package snakeprogram3d.display3d;


import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Geometry;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.Material;
import org.scijava.java3d.Node;
import org.scijava.java3d.PolygonAttributes;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.TexCoordGeneration;
import org.scijava.java3d.Texture3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.java3d.TriangleArray;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3d;
import org.scijava.vecmath.Vector3f;

/**
 *
 * VariableTextured3dSurface uses a Texture3d and the texture appears to change as the
 * surface moves accordingly.  Essentially it shows a slice of the volume.
 *@author Matt Smith
 * 
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 **/
public class VariableTextured3dSurface implements DataObject{

    
    private BranchGroup BG;

    BranchGroup surface;
    
    Shape3D PLANE;

    TransformGroup tg;
    
    TriangleArray plane;
    
    Vector3f OFFSET;
    
    Point3f[] originals;
    
    Appearance appear;
    /**
      * Creats a surface composed of 2 triangle arrays at the four points specified in points.
      *     @param texture if this is a top surface or a bottom surface where true is top and false is bottom
      *     points are the x,y,z values
      **/
    
    public VariableTextured3dSurface(Texture3D texture, TexCoordGeneration tg, double[][] points){
        
        
        
        
        surface = new BranchGroup();
        
        OFFSET = new Vector3f(0,0,0);
                
        originals = new Point3f[6];
        
        Point3f A = new Point3f(new Point3d(points[0]));
        Point3f B = new Point3f(new Point3d(points[1]));
        Point3f C = new Point3f(new Point3d(points[2]));
        Point3f D = new Point3f(new Point3d(points[3]));
        
        PLANE = new Shape3D();
        
        PLANE.setGeometry(createGeometry(A, B, C, D));
        
        
        PLANE.setAppearance(createAppearance(texture, tg));
                
        
        
        surface.addChild(PLANE);
        
           
    }    
    
    Geometry createGeometry(Point3f A, Point3f B, Point3f C, Point3f D){

        plane = new TriangleArray(12, GeometryArray.COORDINATES
                                    | GeometryArray.NORMALS );
        
        plane.setCapability(TriangleArray.ALLOW_COORDINATE_WRITE);
        plane.setCoordinate(0, A);
        plane.setCoordinate(1, B);
        plane.setCoordinate(2, D);
        plane.setCoordinate(3, B);
        plane.setCoordinate(4, C);
        plane.setCoordinate(5, D);
        
        originals[0] = A;
        originals[1] = B;
        originals[2] = D;
        originals[3] = B;
        originals[4] = C;
        originals[5] = D;
        
        
        Vector3f a = new Vector3f(A.x - B.x, A.y - B.y, A.z - B.z);
        Vector3f b = new Vector3f(C.x - B.x, C.y - B.y, C.z - B.z);
        Vector3f n = new Vector3f();
        n.cross(b, a);

        n.normalize();

        plane.setNormal(0, n);
        plane.setNormal(1, n);
        plane.setNormal(2, n);
        plane.setNormal(3, n);
        plane.setNormal(4, n);
        plane.setNormal(5, n);
        

        return plane;
    }

    
    
    Appearance createAppearance(Texture3D texture,TexCoordGeneration tg) {
        appear = new Appearance();
            
        appear.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
        
        appear.setTexCoordGeneration(tg);
        
        appear.setTexture(texture);
        
        PolygonAttributes p = new PolygonAttributes();
        p.setCullFace(PolygonAttributes.CULL_NONE);
        
        Material material = new Material();
        //material.setAmbientColor(new Color3f(0f,0.3f,0.3f));
        material.setLightingEnable(false);
        appear.setMaterial(material);
        appear.setPolygonAttributes(p);

        return appear;
    }
    
    
    
    public void setPosition(double[] xyz){
        Transform3D tt = new Transform3D();
        tt.setTranslation(new Vector3d(xyz[0],xyz[1],xyz[2]));
        
        //tg.setTransform(tt);
        Point3f A;
        for(int i = 0; i<6; i++){
            A = new Point3f(originals[i]);
            tt.transform(A);
            plane.setCoordinate(i, A);
        }
        
        
        
    }
    
    public void setOffset(double x, double y, double z){
        
        OFFSET.x = (float)x;
        OFFSET.y = (float)y;
        OFFSET.z = (float)z;
        
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
    public Node getPlaneNode(){
        return PLANE;
    }
    
    public void setTexture(Texture3D tex){
        
        appear.setTexture(tex);
        
    }

    
    

}
