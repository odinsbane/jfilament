package snakeprogram3d.display3d;

import org.scijava.java3d.BadTransformException;
import org.scijava.java3d.Node;
import org.scijava.java3d.TexCoordGeneration;
import org.scijava.java3d.utils.picking.PickIntersection;
import org.scijava.java3d.utils.picking.PickResult;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector4f;
import snakeprogram3d.MultipleSnakesStore;
import snakeprogram3d.Snake;
import snakeprogram3d.SnakeBufferedImages;
import snakeprogram3d.SnakeModel;
import snakeprogram3d.ThreeDEvent;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/**
 * Shows 3 perpendicular planes, that can be clicked on during certain times.
 * @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class InteractiveView implements CanvasView{
    //The Canvas3D
    DataCanvas y;
    
    MoveableSphere cursor;
    
    private ArrayList<PolyLine> snakes;

    PolyLine SELECTED;
    
    SnakeBufferedImages snake_buffered_images;
    SnakeModel PARENT;

    //sizes in image coordinates
    double HEIGHT,WIDTH,DEPTH,ZRESOLUTION;

    //scaled sizes
    double principle_axis,xs, ys, zs;

    //for positioning respective planes
    double[][] dataxy, datayz, datazx;
    
    /** CURRENT is the current slice*/
    int CURRENT;
    
    /** INDEX_YZ is the current x index of vertical slice */
    int INDEX_YZ;

    /** current y index of the other vertical slice */
    int INDEX_ZX;

    /** the various surfaces */
    VariableTextured3dSurface XY, YZ, ZX;

    /** for display as a new snake is being added */
    Snake raw_snake;
    PolyLine raw_poly;

    /** for keeping track of a new snake being added */
    boolean FOLLOWING = false;
    double[] LAST_POINT;
    PolyLine FOLLOW_LINE;
    ArrayList<double[]> FOLLOW_POINTS;

    /** line around the outside of a slice */
    BoundaryLine BOUNDS_XY, BOUNDS_YZ, BOUNDS_ZX;
    
    /** for accumulating markers when editing snakes */
    HashMap<String, MoveableSphere> MARKERS;
    
    /** 
     * Interactive view displays 3 perpendicular planes used for interacting
     *  with the 3d scene.
     *
     * @param gc used for getting the best graphics configuration.
     * */
    public InteractiveView(GraphicsConfiguration gc){

         y = new DataCanvas(gc,new Color3f(0.06f,0.3f,0.06f));
        
        cursor = new MoveableSphere(0.01);
        MARKERS = new HashMap<>();
        
        y.addObject(cursor);

        snakes = new ArrayList<>();
        
    }
    
    /**
     * Gets the component that this is being drawn on.]
     *
     * @return the datacanvas as an awt component for being added to a border layout.
     */
    public Component getComponent(){
        return y;
    }
    
    /**
     * Add a Snake by creating a poly linea polyline that is backed by the added snake.
     *
     * @param s snake that will be drawn on 3d section view
     */
    public void addSnake(Snake s){
        PolyLine snakeline = new PolyLine(s,HEIGHT,WIDTH,DEPTH,snake_buffered_images.CURRENT_FRAME);
        y.addObject(snakeline);
        snakes.add(snakeline);
    }
    
    
    /**
     * The polylines are backed by a snake but their geometry is based on
     * a different object so they need to be updated after the snake
     * deforms.
     *
     */
    public void updateSnakePositions(){
        for(PolyLine snakeline: snakes)
            snakeline.updateGeometry();

    }
    
    /** 
     * deletes the chosen snake by removing it from the scene and from the list of stored snakes
     *
     *@param s the snake that will be removed
     *
     */
    public void deleteSnake(Snake s){
        PolyLine removee = null;
        
        for(PolyLine snakeline: snakes){
            if(snakeline.getSnake()==s){
                removee = snakeline;
                break;
            }
        }
        if(removee!=null){
            snakes.remove(removee);
            y.removeObject(removee);
            
        }
    }
    
    /**
     * Recieves a coordinate in image space and coverts it to the scene space,moves the
     * cursor to that location.
     *
     * @param x image or 'real' space coordinate.
     * @param y image or 'real' space coordinate.
     * @param z image or 'real' space coordinate.
     *
     */
    public void updateCursor(double x, double y, double z){
        if(principle_axis!=0){
            Point3d p = new Point3d(x/principle_axis -0.5*xs,-y/principle_axis + 0.5*ys,z/principle_axis);
            try{
                cursor.moveTo(p);
            } catch(BadTransformException e){
                cursor.moveTo(new Point3d(0,0,0));
                System.out.println("fault");
            }
            if(FOLLOWING)
                followCursor(x, y, z);
        }
    }
    
    
    /**
     * During initialization this follows the cursor with a polyline
     *
     * @param x coordinate
     * @param y coordinate
     * @param z coordinate
     */
    public void followCursor(double x, double y, double z){
        if(FOLLOW_POINTS==null)
            FOLLOW_POINTS = new ArrayList<double[]>();
        FOLLOW_POINTS.clear();
        FOLLOW_POINTS.add(LAST_POINT);
        FOLLOW_POINTS.add(new double[]{x,y,z});
        if(FOLLOW_LINE==null){
            FOLLOW_LINE=new PolyLine( new Snake(FOLLOW_POINTS, -1),HEIGHT,WIDTH,DEPTH,-1);
            this.y.addObject(FOLLOW_LINE);
        }
        FOLLOW_LINE.updateGeometry();

    }
    
    /**
     * Use the PickResults to find the point on the plane that the pick result
     * occured over.  Returns the first plane encountered in array so
     * the results should be sorted by closest to pick source.
     *
     *@param results sorted list of PickResults
     *@param evt the moust event that generated the pick
     *@param clicked if it was a click
     */
    public void updatePick(PickResult[] results, MouseEvent evt, boolean clicked){
        double[] point_found = null;
        
        Node xy_node = XY.getPlaneNode();
        Node yz_node = YZ.getPlaneNode();
        Node zx_node = ZX.getPlaneNode();
        
        Node snake_node = null;
        if(SELECTED!=null)
            snake_node=SELECTED.getNode();

        //cycles through all of the 'pickable' objects to find the closest picked object
        for(PickResult result: results){
            
            Node r = result.getObject();
            
            if(r == xy_node){
                //finds the intersection
                PickIntersection pint = result.getIntersection(0);

                //gets the coordinates in the objects reference cnets.
                Point3d p = pint.getPointCoordinates();

                //converts to image coordinates
                point_found = new double[] { p.x*principle_axis , (ys - p.y)*principle_axis, CURRENT*ZRESOLUTION };
                break;
                
            } else if(r == yz_node){
                
                PickIntersection pint = result.getIntersection(0);
            
                Point3d p = pint.getPointCoordinates();
                point_found = new double[] { INDEX_YZ ,  (ys-p.y)*principle_axis, p.z*principle_axis};
                break;
            } else if(r == snake_node){
                
                PickIntersection pint = result.getIntersection(0);
            
                Point3d p = pint.getPointCoordinates();
                point_found = new double[] { p.x ,  p.y, p.z};
                break;
            } else if(r == zx_node){

                PickIntersection pint = result.getIntersection(0);

                Point3d p = pint.getPointCoordinates();
                point_found = new double[] { p.x*principle_axis , INDEX_ZX  , p.z*principle_axis};
                break;


            }
            
            
        }

        //if a point was found a threed event is used for a callback to the snakemodel.
        if(point_found!=null){
            ThreeDEvent tde = new ThreeDEvent(point_found[0], point_found[1], point_found[2]);
            
            if(SwingUtilities.isLeftMouseButton(evt))
                tde.setType(ThreeDEvent.LEFTCLICK);
            else if(SwingUtilities.isRightMouseButton(evt))
                tde.setType(ThreeDEvent.RIGHTCLICK);
            if(clicked)
                PARENT.mousePressed(tde);
            else
                PARENT.mouseMoved(tde);
        }
    }
    
    /**
     * Resets the current display, clears all snakes, planes and bounding boxes
     * resets the scale, creates new planes and bounding boxes then applies
     * the threed texture
     *
     * @param sbi contains the dimensions and the volume texture
     * 
     */
    public void reset(SnakeBufferedImages sbi){
        
        snake_buffered_images = sbi;

        //if the geometry has changed then recreate the surfaces otherwise just the texture
        if(          HEIGHT!=snake_buffered_images.getHeight()||
                      WIDTH!=snake_buffered_images.getWidth()||
                      DEPTH!=snake_buffered_images.getDepth()    )   {

            for(PolyLine l: snakes)
                y.removeObject(l);

            snakes.clear();

            HEIGHT = snake_buffered_images.getHeight();
            WIDTH = snake_buffered_images.getWidth();
            DEPTH = snake_buffered_images.getDepth();

            principle_axis = HEIGHT>WIDTH?(HEIGHT>DEPTH?HEIGHT:DEPTH):(WIDTH>DEPTH?WIDTH:DEPTH);

            ZRESOLUTION = snake_buffered_images.getZResolution();



            xs = WIDTH/principle_axis;
            ys = HEIGHT/principle_axis;
            zs = DEPTH/principle_axis;

            double xnot = -0.5*xs;
            double ynot = -0.5*ys;
            double znot = 0;


            dataxy = new double[4][];
            dataxy[0] = new double[] {0,0,0};
            dataxy[1] = new double[] {1*xs,0,0};
            dataxy[2] = new double[] {1*xs,ys,0};
            dataxy[3] = new double[] {0,1*ys,0};



            if(XY!=null)
                y.removeObject(XY);




            TexCoordGeneration tg = createTexCoordinteGeneration((float)xs, (float)ys, (float)zs);



            XY = new VariableTextured3dSurface(sbi.getVolumeTexture(),tg,dataxy);
            XY.setOffset(xnot,ynot,znot);

            y.addObject(XY);

            //create boundary line
            List<Point3d> pts = new ArrayList<Point3d>();
            pts.add(new Point3d(0,0,0));
            pts.add(new Point3d(xs,0,0));
            pts.add(new Point3d(xs,ys,0));
            pts.add(new Point3d(0,ys,0));
            pts.add(new Point3d(0,0,0));

            if(BOUNDS_XY!=null)
                y.removeObject(BOUNDS_XY);

            BOUNDS_XY = new BoundaryLine(pts);
            BOUNDS_XY.setOffset(xnot,ynot,znot);

            y.addObject(BOUNDS_XY);


            datayz = new double[4][];
            datayz[1] = new double[] {0,0,1*zs};
            datayz[2] = new double[] {0,0,0};
            datayz[3] = new double[] {0,1*ys,0};
            datayz[0] = new double[] {0,1*ys,1*zs};


            if(YZ!=null)
                y.removeObject(YZ);
            YZ = new VariableTextured3dSurface(sbi.getVolumeTexture(),tg, datayz);
            YZ.setOffset(xnot,ynot, znot);
            y.addObject(YZ);

            pts.clear();
            pts.add(new Point3d(0,0,0));
            pts.add(new Point3d(0,ys,0));
            pts.add(new Point3d(0,ys,zs));
            pts.add(new Point3d(0,0,zs));
            pts.add(new Point3d(0,0,0));

            if(BOUNDS_YZ!=null)
                y.removeObject(BOUNDS_YZ);

            BOUNDS_YZ = new BoundaryLine(pts);
            BOUNDS_YZ.setOffset(xnot,ynot,znot);

            y.addObject(BOUNDS_YZ);

            datazx = new double[4][];
            datazx[1] = new double[] {0,ys,0};
            datazx[2] = new double[] {0,ys,zs};
            datazx[3] = new double[] {xs,ys,zs};
            datazx[0] = new double[] {xs,ys,0};


            if(ZX!=null)
                y.removeObject(ZX);
            ZX = new VariableTextured3dSurface(sbi.getVolumeTexture(),tg, datazx);
            ZX.setOffset(xnot,ynot, znot);
            y.addObject(ZX);

            pts.clear();
            pts.add(new Point3d(0,ys,0));
            pts.add(new Point3d(0,ys,zs));
            pts.add(new Point3d(xs,ys,zs));
            pts.add(new Point3d(xs,ys,0));
            pts.add(new Point3d(0,ys,0));

            if(BOUNDS_ZX!=null)
                y.removeObject(BOUNDS_ZX);

            BOUNDS_ZX = new BoundaryLine(pts);
            BOUNDS_ZX.setOffset(xnot,ynot,znot);

            y.addObject(BOUNDS_ZX);

            CURRENT = 0;
            INDEX_YZ = 0;
            INDEX_ZX = 0;
        } else{

            refreshImages();
        }
        
    }

    /**
     * Uses the vectors x,y,z to scale the volume texture to the proper coordinates.
     *
     * @param x coordinate scaled to principle axis
     * @param y coordinate scaled to principle axis
     * @param z coordinate scaled to principle axis
     * @return TextureCoordinate generation so that the slices can slice a volume texture.
     */
    public TexCoordGeneration createTexCoordinteGeneration(float x, float y, float z){
        
        
        Vector4f X_PLANE = new Vector4f(1/x,0,0,0);
	    Vector4f Y_PLANE = new Vector4f(0,1/y,0,0);
	    Vector4f Z_PLANE = new Vector4f(0,0,1/z,0);
        
        TexCoordGeneration tg = new TexCoordGeneration();
        tg.setFormat(TexCoordGeneration.TEXTURE_COORDINATE_3);
        tg.setPlaneS(X_PLANE);
        tg.setPlaneT(Y_PLANE);
        tg.setPlaneR(Z_PLANE);
        
        return tg;
    }

    /**
     * Updates the XY plane displayed
     *
     * @param cur checks if this index is in range, cur is in reference to
     *             z-slices so its index begins at 1.
     */
    public void updateDisplay(int cur){
    
        if(cur!=CURRENT+1){
            CURRENT = cur-1;
            
            double p = ZRESOLUTION*CURRENT/principle_axis;
            
            
            XY.setPosition(new double[] { 0,0,p});
            
            BOUNDS_XY.moveTo(0,0,p);
        }
    }

    public void moveUp(){
        
        if( INDEX_YZ+1 < WIDTH){
            INDEX_YZ += 1;
            updateYZPosition();
        }
                
    }
    
    public void moveDown(){
        if( INDEX_YZ - 1 > 0){
            INDEX_YZ--;
            updateYZPosition();
        }
        
    }

    /**
     * Updates the position of the yz plane according to the current
     * index.
     * 
     */
    public void updateYZPosition(){

        double p = INDEX_YZ / principle_axis;

        YZ.setPosition(new double[] {p, 0, 0});
        BOUNDS_YZ.moveTo(p,0,0);
    }
    /*
     * Moves the ZX plane down by changing the INDEX_ZX
     */
    public void wipeDown(){
        if(INDEX_ZX - 1>0){
            INDEX_ZX--;
            updateZXPosition();
        }

    }
    
    /*
     * Moves the ZX plane up by changing the INDEX_ZX
     */
    public void wipeUp(){
        if(INDEX_ZX + 1<HEIGHT){
            INDEX_ZX++;
            updateZXPosition();
        }
    }
    
    
    /**
     * Updates the position of the zx plane according to the current
     * index.
     *
     */
    public void updateZXPosition(){

        double p =  - INDEX_ZX / principle_axis;

        ZX.setPosition(new double[] {0, p, 0});
        BOUNDS_ZX.moveTo(0,p,0);
    }

    /**
     * Refreshes the texture being displayed w/out changing the geometry
     *
     */
    public void refreshImages(){
        XY.setTexture(snake_buffered_images.getVolumeTexture());
        YZ.setTexture(snake_buffered_images.getVolumeTexture());
        ZX.setTexture(snake_buffered_images.getVolumeTexture());


    }


    /**
     * Removes all old snakes and adds any snakes that need to be displayed
     * in this frame.
     *
     * @param ss contains snakes.
     */
    public void synchronizeSnakes(MultipleSnakesStore ss){
        for(PolyLine l: snakes)
            y.removeObject(l);
        snakes.clear();
        for(Snake s: ss)
            if(s.exists(snake_buffered_images.CURRENT_FRAME))
                addSnake(s);
    }
    
    /**
     * This is how the 3D clicks are transfered to the snake model.
     *  
     * @param sm runs the show.
     */

    public void addSnakeListener(SnakeModel sm){
        
        y.addSnakeListener(this);
        PARENT = sm;
    }

    /**
     * Snakes are stored as an array list of double[] when they are being
     * initialized.
     *
     * @param raw coordinates representing the snake being initialized.
     */
    public void drawRawSnake(List<double[]> raw){
        if(raw.size()==1){
            FOLLOWING=true;
            LAST_POINT = raw.get(0);
        }
        if(raw.size()>1){
            if(raw_snake==null){
                raw_snake = new Snake(raw, -1);
                raw_poly = new PolyLine(raw_snake,HEIGHT,WIDTH,DEPTH,-1);
                raw_poly.updateGeometry();

                y.addObject(raw_poly);
            } else{
                raw_snake.addCoordinates(-1, raw);
                raw_poly.updateGeometry();

            }
            LAST_POINT = raw.get(raw.size()-1);
        }
    }
    
    /**
     *  Add a marker with a name
     * 
     * @param name of the marker for further access
     **/
    public void addMarker(String name){
        MoveableSphere marker = new MoveableSphere(0.01);
        MARKERS.put(name,marker);
        y.addObject(marker);
    }
    
    /**
     *  move marker to the position pt
     * 
     * @param name name of the marker being moved
     * @param pt position (in image coordinates) of where to put the marker. 
     **/
    public void placeMarker(String name, double[] pt){
        MoveableSphere marker = MARKERS.get(name);
        Point3d p = new Point3d(pt[0]/principle_axis -0.5*xs,-pt[1]/principle_axis + 0.5*ys,pt[2]/principle_axis);
        marker.moveTo(p);
    }
    
    /**
     *  Remove all markers 
     **/
    public void clearMarkers(){
        for(String s: MARKERS.keySet())
            y.removeObject(MARKERS.get(s));
            
        MARKERS.clear();
    }

    /**
     * There is a selected snake, it is red and clickable.  This function takes
     * changing the color, and making it clickable.
     *
     * @param s
     */
    public void setSelected(Snake s){
        for(PolyLine l: snakes){
            if( s==l.getSnake()){
                SELECTED=l;
                SELECTED.setColor(0);
            } else{
                l.setColor(1);
            }
        }
    }
    
    /**
     * While initializing a line is drawn to the cursor.
     *
     * @param v whether or not the snake is actually initializing.
     */
    public void setInitializing(boolean v){
        if(!v){
            if(raw_poly!=null)
                y.removeObject(raw_poly);
            raw_snake=null;
            raw_poly=null;
        
            if(FOLLOW_LINE != null){
                y.removeObject(FOLLOW_LINE);
                FOLLOW_LINE = null;
                FOLLOWING=false;
            
            }
        }
    }
    
}
