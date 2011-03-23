package snakeprogram3d.display3d;

import javax.media.j3d.*;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.util.ArrayList;

/**
 *  This is a minimal surface type, it can only be deformed by giving it a new dataset, it is also
 *  only visible from 1 side
 *
 * @author Matt Smith
 *
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
  **/
public class BoundaryLine implements DataObject{
    
    float LINEWIDTH = 1;
    
    Shape3D line3d;
    private BranchGroup BG;

    Vector3d OFFSET;
    
    TransformGroup tg;
    
    public BoundaryLine(ArrayList<Point3d> spts){
        
        LineArray line = new LineArray(2*(spts.size()-1),GeometryArray.COORDINATES);
        
        for(int i=0; i<spts.size()-1; i++){
            line.setCoordinate(2*i,spts.get(i));
            line.setCoordinate(2*i+1,new Point3d(spts.get(i+1)));

       }
       
        line3d = new Shape3D(line);
        line3d.setPickable(false);
        
        
        
        
        line3d.setAppearance(createAppearance());
        
        
        
    }
    
    public void setOffset(double x, double y, double z){
        
        OFFSET = new Vector3d(x,y,z);
        
        
    }    
    
    public void moveTo(double x, double y, double z){
        //get old transform
            Transform3D tt = new Transform3D();
            tg.getTransform(tt);
            
            Vector3d n = new Vector3d(x,y,z);
            
            n.add(OFFSET);
            
            tt.setTranslation(n);
            
            tg.setTransform(tt);
     }
    
    public BranchGroup getBranchGroup(){
        if(BG==null){
            tg = new TransformGroup();
            tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
            
            Transform3D scale = new Transform3D();
            scale.setTranslation(OFFSET);
            tg.setTransform(scale);
            
            tg.addChild(line3d);
            BG = new BranchGroup();
            BG.setCapability(BranchGroup.ALLOW_DETACH);
            BG.addChild(tg);
            
        }
        return BG;
        
    }
    
    public Appearance createAppearance(){
        Appearance a = new Appearance();
        
        ColoringAttributes c_at = new ColoringAttributes(1f, 1f, 1f, ColoringAttributes.NICEST);
        
        LineAttributes la = new LineAttributes();
        la.setLineWidth(LINEWIDTH);
        //la.setLineAntialiasingEnable(true);
        a.setColoringAttributes(c_at);
        a.setLineAttributes(la);
        
        return a;
        
    }
    
    
    

}
