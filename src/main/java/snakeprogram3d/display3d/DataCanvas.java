package snakeprogram3d.display3d;

import org.scijava.java3d.AmbientLight;
import org.scijava.java3d.Background;
import org.scijava.java3d.BoundingSphere;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.GraphicsConfigTemplate3D;
import org.scijava.java3d.Group;
import org.scijava.java3d.ImageComponent;
import org.scijava.java3d.ImageComponent2D;
import org.scijava.java3d.PointLight;
import org.scijava.java3d.Screen3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.java3d.View;
import org.scijava.java3d.utils.picking.PickCanvas;
import org.scijava.java3d.utils.picking.PickResult;
import org.scijava.java3d.utils.picking.PickTool;
import org.scijava.java3d.utils.universe.SimpleUniverse;
import org.scijava.vecmath.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 * Shows the 3d scenes
 */
public class DataCanvas extends Canvas3D {
    SimpleUniverse universe;

    BoundingSphere bounds;
    BranchGroup group;

    private PickCanvas pickCanvas;
    
    double ZOOM = 1;
    double ROTX = 0;
    double ROTY = 0;
    double ROTZ = 0;
    AxisAngle4d aa = new AxisAngle4d(0, 0, 1, 0);

    double DX = 0;
    double DY = 0;
    
    CanvasView CV;
    
    Color3f backgroundColor = new Color3f(0f,0f,0f);
    Background background;
    CanvasController controller;

    private OffScreenCanvas3D offscreen;

    public DataCanvas(GraphicsConfiguration gc,Color3f back){
        super(gc,false);
        offscreen = new OffScreenCanvas3D(gc, true);
        backgroundColor = back;
        createUniverse();
        }


    public DataCanvas(GraphicsConfiguration gc){
        super(gc,false);
        offscreen = new OffScreenCanvas3D(gc, true);
        createUniverse();
    }

    void createUniverse(){
        universe = new SimpleUniverse(this);

        universe.getViewingPlatform().setNominalViewingTransform();
        universe.getViewer().getView().setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);

        
        group = new BranchGroup();
        
        group.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        group.setCapability(Group.ALLOW_CHILDREN_WRITE);
        bounds =  new BoundingSphere(new Point3d(0.0,0.0,0.0), 10000.0);

        background = new Background();
        background.setCapability(Background.ALLOW_COLOR_WRITE);
        background.setColor(backgroundColor);
        background.setApplicationBounds(bounds);
        group.addChild(background);

        universe.addBranchGraph(group);
        universe.getViewer().getView().setMinimumFrameCycleTime(5);
        controller = new CanvasController(this);
        
        pickCanvas = new PickCanvas(this, group);
        //pickCanvas = new PickCanvas(getOffscreenCanvas3D(), group);
        pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);
        //pickCanvas.setTolerance(0.1f);
        //pickCanvas.setShapeRay(new Point3d(0,0,-1000), new Vector3d(0,0,2000));

        Screen3D screen = getScreen3D();
        Screen3D off = offscreen.getScreen3D();
        Dimension dim = screen.getSize();
        off.setSize(dim);
        off.setPhysicalScreenWidth(screen.getPhysicalScreenWidth());
        off.setPhysicalScreenHeight(screen.getPhysicalScreenHeight());
        universe.getViewer().getView().addCanvas3D(offscreen);

        setView(StationaryViews.THREEQUARTER);
        updateView();
    }


    /**
     * Adds a "DataObject" which is just an interface for adding a branch group.
     *
     * @param a the data object.
     */
    public void addObject(DataObject a){
        group.addChild(a.getBranchGroup());
    }
    
    
    public void zoomIn(){
        ZOOM = ZOOM*0.9;
        updateView();
    }
    
    public void zoomOut(){
        ZOOM = ZOOM*1.1;
        updateView();
    }

    public void twistView(int dz){
        double rate = 0.005;
        Vector4d q1 = axisAngleToQuarternion(aa);
        Vector4d q2 = axisAngleToQuarternion(new AxisAngle4d(0, 0, 1, -rate*dz));
        aa = new AxisAngle4d(quarternionToAxisAngle(multiplyQuarternions(q1,q2)));

        updateView();
    }

    public void rotateView(int dx,int dy){

        int mx = dx<0?-dx:dx;
        int my = dy<0?-dy:dy;

        if(mx > 3*my){
            dy = 0;
        }
        if(my>3*mx){
            dx = 0;
        }

        Vector4d q1 = axisAngleToQuarternion(aa);
        double rate = 0.005;
        Vector4d q2;
        if(dx!=0 && dy!=0){
            q2 = axisAngleToQuarternion(new AxisAngle4d(0, 1, 0, -rate*dx));
            q2 = multiplyQuarternions(q2, axisAngleToQuarternion(new AxisAngle4d(1, 0, 0, -rate*dy)));
        } else if(dx == 0){
            q2 = axisAngleToQuarternion(new AxisAngle4d(1, 0, 0, -rate*dy));
        } else{
            q2 = axisAngleToQuarternion(new AxisAngle4d(0, 1, 0, -rate*dx));
        }

        aa = new AxisAngle4d(quarternionToAxisAngle(multiplyQuarternions(q1,q2)));

        updateView();

    }
    
    public void translateView(int dx, int dy){
        DX += -dx*0.01;
        DY += dy*0.01;
        updateView();

    }

    /**
     * Removes an object if it exists.
     *
     * @param obj object of interest 
     */
    synchronized public void removeObject(DataObject obj){
    
        group.removeChild(obj.getBranchGroup());
        obj.getBranchGroup().detach();
    
    }
    private void updateView(){
        TransformGroup ctg = universe.getViewingPlatform().getViewPlatformTransform();
        Vector3d displace = new Vector3d(DX,DY,ZOOM);
        Transform3D rot = new Transform3D();
        rot.setRotation(aa);
        rot.transform(displace);
        rot.setTranslation(displace);
        

        ctg.setTransform(rot);
        
        }

    /**
     * Adding a snake listener sets 'picking' events where using the mouse on the 3d view can
     * cause interactions.
     *
     * @param cv the displayed view that will be interacted with.
     */
        public void addSnakeListener(CanvasView cv){
            
            CV = cv;
            
        }
        /**
         *  Gets the 'results' a pick result and send the results on down
         *  the line 
         *  
         **/
        public void clicked(MouseEvent e){
            if(CV!=null){
                
                pickCanvas.setShapeLocation(e);
                

                PickResult[] results = pickCanvas.pickAllSorted();
                if(results != null){
                    
                    CV.updatePick(results, e, true);
                    
                } 
            }
        }
        
        /**
         *  Transforms the coordinates and sends the actions on down
         *  the line to the snake listener.
         *  
         **/
        public void moved(MouseEvent e){
            if(CV!=null){

                try{
                    pickCanvas.setShapeLocation(e);
                

                    PickResult[] result = pickCanvas.pickAllSorted();
                    if(result != null)
                        CV.updatePick(result, e, false);
                } catch(Exception exc){
                    //bug that I don't know what to do with, maybe disable when I disable ui?
                }
            }
        }
        
        /**
         * Gets the best graphics configuration to display on the current device.
         * 
         * @param frame frame that you want to add a Canvas3d to
         * @return a graphics configuration on the current display.
         */
        public static GraphicsConfiguration getBestConfigurationOnSameDevice(Frame frame){
            
            GraphicsConfiguration gc = frame.getGraphicsConfiguration();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] gs = ge.getScreenDevices();
            GraphicsConfiguration good = null;

            GraphicsConfigTemplate3D gct = new GraphicsConfigTemplate3D();

            for(GraphicsDevice gd: gs){

                if(gd==gc.getDevice()){
                    good = gct.getBestConfiguration(gd.getConfigurations());
                    if(good!=null)
                        break;

                }
            }



            return good;
        }

    public BufferedImage snapShot(){
        return offscreen.doRender(getWidth(), getHeight());
    }

    public void changeBackgroundColor(Color color){

        backgroundColor = new Color3f(color);
        background.setColor(backgroundColor);

    }

    public void setView(StationaryViews view){
        switch(view){
            case XY:
                aa = new AxisAngle4d(0, 0, 1, 0);
                break;
            case XZ:
                aa = new AxisAngle4d(1, 0, 0, Math.PI/2);
                break;
            case YZ:
                double l = Math.sqrt(3)/3;
                aa = new AxisAngle4d(l, l, l, 2*Math.PI/3);
                break;
            case THREEQUARTER:
                aa = new AxisAngle4d(0.20032220429878106, 0.5947418035684883, 0.7785584124219587, 2.6296664043138676);
                break;
        }

        ZOOM = 3;
        DX = 0;
        DY = 0;

        updateView();
    }
    double[] quarternionToAxisAngle(Vector4d q){
            double theta = Math.acos(q.w)*2;
            double x, y, z;
            if(theta<0.0001){
                x = 0;
                y = 0;
                z = 1;
                theta = 0;
            } else{
                double s = Math.sqrt(1 - q.w*q.w);
                x = q.x/s;
                y = q.y/s;
                z = q.z/s;

            }

            return new double[]{x, y, z, theta};


    }

    Vector4d axisAngleToQuarternion(AxisAngle4d aa){
        double s = Math.sin(aa.angle/2);
        double c = Math.cos(aa.angle/2);

        return new Vector4d(aa.x*s, aa.y*s, aa.z*s, c);

    }
    Vector4d multiplyQuarternions(Vector4d q1, Vector4d q2){
        double qw = q1.w*q2.w - q1.x*q2.x - q1.y*q2.y - q1.z*q2.z;
        double qx = q1.x*q2.w + q1.w*q2.x + q1.y*q2.z - q1.z*q2.y;
        double qy = q1.w*q2.y - q1.x*q2.z + q1.y*q2.w + q1.z*q2.x;
        double qz = q1.w*q2.z + q1.x*q2.y - q1.y*q2.x + q1.z*q2.w;

        return new Vector4d(qx, qy, qz, qw);


    }

}
enum StationaryViews{
    XY, XZ, YZ, THREEQUARTER;
}
/**
* This is the MouseListener, there is no reason for it to be an
* inner class, really I just haven't moved it out of here
 * */
class CanvasController extends MouseAdapter{
    DataCanvas dc;
    int start_dragx, start_dragy;
    int click_type;
    CanvasController(DataCanvas c){
        dc = c;
        dc.addMouseMotionListener(this);
        dc.addMouseListener(this);
        dc.addMouseWheelListener(this);
        dc.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped(KeyEvent e) {

                if(e.getKeyChar()=='1'){
                    dc.setView(StationaryViews.XY);
                } else if(e.getKeyChar()=='2'){
                    dc.setView(StationaryViews.XZ);
                } else if(e.getKeyChar()=='3'){
                    dc.setView(StationaryViews.YZ);
                } else if(e.getKeyChar()=='4'){
                    dc.setView(StationaryViews.THREEQUARTER);
                }


            }

            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode()==KeyEvent.VK_UP){
                    dc.twistView(1);
                } else if(e.getKeyCode()==KeyEvent.VK_DOWN){
                    dc.twistView(-1);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
    }
    public void mouseMoved(MouseEvent e){
        dc.moved(e);
    }
    public void mousePressed(MouseEvent e){
        click_type = e.getButton();
        start_dragx = e.getX();
        start_dragy = e.getY();
    }

    public void mouseClicked(MouseEvent e){
        dc.clicked(e);
    }
    public void mouseDragged(MouseEvent e){
        int dx = e.getX() - start_dragx;
        start_dragx = e.getX();
        int dy = e.getY() - start_dragy;
        start_dragy = e.getY();

        if(click_type==MouseEvent.BUTTON1)
            dc.rotateView(dx,dy);
        else
            dc.translateView(dx,dy);
    }
    public void mouseWheelMoved(MouseWheelEvent e){
        if(e.getWheelRotation()<0){
            dc.zoomIn();
        } else{
            dc.zoomOut();
        }
    }


}



class OffScreenCanvas3D extends Canvas3D {
    ImageComponent2D buffer;
    OffScreenCanvas3D(GraphicsConfiguration graphicsConfiguration,
                      boolean offScreen) {

        super(graphicsConfiguration, offScreen);

    }

    BufferedImage doRender(int width, int height) {

        if(buffer==null) {
            BufferedImage bImage = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);
            buffer = new ImageComponent2D(
                    ImageComponent.FORMAT_RGBA, bImage);
            setOffScreenBuffer(buffer);
        }


        renderOffScreenBuffer();
        waitForOffScreenRendering();

        return getOffScreenBuffer().getImage();
    }

    public void postSwap() {
        // No-op since we always wait for off-screen rendering to complete
    }


}