package snakeprogram3d.display3d;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.LineArray;
import org.scijava.java3d.LineAttributes;
import org.scijava.java3d.Node;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;
import snakeprogram3d.Snake;

import java.util.ArrayList;
import java.util.List;

/**
 *     A Line for representing snakes.
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

    /**
     * Snakes exist in the image coordinates where x-y-z all correspond to the same distance.
     * They image space is defined by the height, width, and depth.
     *
     * Each axis is scaled by the longest, principle, axis.
     *
     *
     * The center of the box is at 0, 0, depth/(2*principle). The image coordinates w/2, h/2, 0 would correspond to 0,0,0 in
     * @param s
     * @param height
     * @param width
     * @param depth
     * @param frame
     */
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
            LineArray line = new LineArray(nodes, GeometryArray.COORDINATES);

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
