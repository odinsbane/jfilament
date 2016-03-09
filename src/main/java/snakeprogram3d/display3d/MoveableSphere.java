package snakeprogram3d.display3d;

import com.sun.j3d.utils.geometry.Sphere;

import javax.media.j3d.*;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 *         The most basic class for creating a point.  A sphere.
 *   @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 **/
public class MoveableSphere implements DataObject{

    TransformGroup tg;
    BranchGroup BG;
    /**
      *     Creates a new moveable sphere at the origin with radius size
      *     @param size     determines the radius of your sphere
      **/
    
    
    public MoveableSphere(double size){
        
        Appearance a = createAppearance();
        Sphere sphere = new Sphere((float)size, Sphere.GENERATE_NORMALS, 50, a);
        sphere.setPickable(false);
        
        Transform3D tt = new Transform3D();
        //tt.setTranslation(new Vector3d(1.0,0.0,0.0));
        tg = new TransformGroup(tt);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg.addChild(sphere);
        
    }
    
    public Appearance createAppearance(){
        Color3f eColor    = new Color3f(0.0f, 0.0f, 0.0f);
        Color3f sColor    = new Color3f(1.0f, 1.0f, 1.0f);
        Color3f objColor  = new Color3f(0.6f, 0.6f, 0.6f);
        
        Material m = new Material(objColor, eColor, objColor, sColor, 100.0f);
        Appearance a = new Appearance();
        

        m.setLightingEnable(true);
        a.setMaterial(m);
        return a;
    }
    

     
     public void moveTo(Point3d p){
        //get old transform
            Transform3D tt = new Transform3D();
            tg.getTransform(tt);
            
            Vector3d n = new Vector3d(p.x,p.y,p.z);
            
            tt.setTranslation(n);
            
            tg.setTransform(tt);
     }
     
        
        

        public BranchGroup getBranchGroup(){
            if(BG==null){
                BG = new BranchGroup();
                BG.setCapability(BranchGroup.ALLOW_DETACH);
                BG.addChild(tg);
            }    
            return BG;
        }



}
