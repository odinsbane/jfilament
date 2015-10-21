package snakeprogram3d.display3d;

import snakeprogram3d.Snake;

import javax.media.j3d.*;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.util.ArrayList;
import java.util.List;

/**
 *     This is a minimal surface type, it can only be deformed by giving it a new dataset, it is also
 *     only visible from 1 side
 *
 * @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 **/
public class PolyLine implements DataObject{
    
    float LINEWIDTH = 3;
    
    Snake snake;
    Shape3D line3d;
    Transform3D scale;
    int FRAME;
    private BranchGroup BG;
    ColoringAttributes c_at;
    
    
    public PolyLine(Snake s,double height,double width,double depth, int frame){
        double principle = (height>width)?(height>depth?height:depth):(width>depth?width:depth);
        snake = s;
        List<double[]> spts = s.getCoordinates(frame);
        FRAME = frame;
        LineArray line = new LineArray(2*(spts.size()-1),GeometryArray.COORDINATES);
        for(int i=0; i<spts.size()-1; i++){
            line.setCoordinate(2*i,new Point3d(spts.get(i)));
            line.setCoordinate(2*i+1,new Point3d(spts.get(i+1)));

       }
        line3d = new Shape3D(line);
        
        
        
        
        
        line3d.setAppearance(createAppearance());
        
        line3d.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
        
        Vector3d saxis = new Vector3d(new double[]{1./principle,-1./principle,1./principle});
        scale = new Transform3D();
        scale.setTranslation(new Vector3d(-0.5*width/principle,0.5*height/principle,0));
        scale.setScale( saxis);
        
    }    

    
    public BranchGroup getBranchGroup(){
        if(BG==null){
            TransformGroup tg = new TransformGroup();
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
        
        c_at = new ColoringAttributes(1f, 0f, 0f, ColoringAttributes.NICEST);
        c_at.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
        
        LineAttributes la = new LineAttributes();
        la.setLineWidth(LINEWIDTH);
        //la.setLineAntialiasingEnable(true);
        a.setColoringAttributes(c_at);
        a.setLineAttributes(la);
        
        return a;
        
    }

    /**
     * updates the geometry of snakes, not safe if there are zero points.
     */
    public void updateGeometry(){

        ArrayList<double[]> spts = new ArrayList<double[]>(snake.getCoordinates(FRAME));
        int N =spts.size();
        if(N>1){
            int nodes = 2*(N-1);
            LineArray line = new LineArray(nodes,GeometryArray.COORDINATES);

            for(int i=0; i<N-1; i++){
                line.setCoordinate(2*i,new Point3d(spts.get(i)));
                line.setCoordinate(2*i+1,new Point3d(spts.get(i+1)));

            }
            line3d.setGeometry(line);
        } else{
            line3d.setGeometry(null);
        }

    }
    public Node getNode(){
        return line3d;
    }
    public Snake getSnake(){
        return snake;
    
    }
    
    public void setColor(int v){
        if(v==0){
            c_at.setColor(1f,0f,0f);
            
        } else{
            
           c_at.setColor(0f,1f,0f);
        }
        
    }

}
