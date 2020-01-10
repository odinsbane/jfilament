package snakeprogram3d.display3d;

import org.scijava.java3d.BadTransformException;
import org.scijava.java3d.Node;
import org.scijava.java3d.utils.picking.PickIntersection;
import org.scijava.java3d.utils.picking.PickResult;
import org.scijava.vecmath.Point3d;
import snakeprogram3d.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * For display snakes in a Volume view of the image.
 *@author Matt Smith
 *
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class ThreeDSnake implements CanvasView{
    DataCanvas y;
    
    MoveableSphere cursor;

    /** holds the psuedo 3d rendering data */
    ThreeDSurface layers;

    /** all of the snakes in the current scene */
    private ArrayList<PolyLine> snakes;

    /** holds the 3d texture, any variation in image data is changed through the SnakeBufferedImages by the SnakeModel*/
    SnakeBufferedImages snake_buffered_images;

    /** dimensions in image coordinates*/
    double HEIGHT,WIDTH,DEPTH;

    /** The longest axis, this will effectively have the length of 1 when displayed*/
    double principle_axis;
    
     /** Currently selected snake*/
    PolyLine SELECTED;

    /** reference to model for performing callbacks.*/
    SnakeModel PARENT;

    public ThreeDSnake(GraphicsConfiguration gc){


        
        y = new DataCanvas(gc);
        
        cursor = new MoveableSphere(0.01);
        y.addObject(cursor);
        snakes = new ArrayList<PolyLine>();
        
        layers = null;
        
    }
    public Component getComponent(){
        return y;
    }
    
    
    public void addSnake(Snake s){
        PolyLine snakeline = new PolyLine(s,(int)HEIGHT,(int)WIDTH,(int)DEPTH, snake_buffered_images.CURRENT_FRAME);
        y.addObject(snakeline);
        snakes.add(snakeline);
    }
    
    
    
    public void updateSnakePositions(){
        for(PolyLine snakeline: snakes)
            snakeline.updateGeometry();
    
    }
    
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
     * Removes all snakes and displays the snakes in ss.
     * @param ss the snakes to be display, should be callled by the snake model.
     */
    public void synchronizeSnakes(MultipleSnakesStore ss){
        for(PolyLine l: snakes)
            y.removeObject(l);
        snakes.clear();

        //only displays if snake is in this frame.
        for(Snake s: ss){
            if(s.exists(snake_buffered_images.CURRENT_FRAME))
                addSnake(s);
            
        }
    }

    /**
     * Moves the 'cursor', which is a sphere.
     *
     * @param x position
     * @param y ''
     * @param z ''
     */
    public void updateCursor(double x, double y, double z){
       
        Point3d p = new Point3d(x/principle_axis - 0.5*WIDTH/principle_axis,-y/principle_axis + 0.5*HEIGHT/principle_axis,z/principle_axis);
        
        try{
            cursor.moveTo(p);
        } catch(BadTransformException e){
            System.out.println(x + "::" + y + "::" + z);
        }
    }

    /**
     * Anytime the image might have changed this is called.
     *
     * @param sbi Volume data.
     */
    public void reset(SnakeBufferedImages sbi){
        
        snake_buffered_images = sbi;

        //if the geometry has changed then recreate the surfaces otherwise just the texture
        if(          HEIGHT!=snake_buffered_images.getHeight()||
                      WIDTH!=snake_buffered_images.getWidth()||
                      DEPTH!=snake_buffered_images.getDepth()    )   {

            if(layers!=null)
                y.removeObject(layers);

            for(PolyLine l: snakes)
                y.removeObject(l);
            snakes.clear();

            HEIGHT = snake_buffered_images.getHeight();
            WIDTH = snake_buffered_images.getWidth();
            DEPTH = snake_buffered_images.getDepth();

            principle_axis = HEIGHT>WIDTH?(HEIGHT>DEPTH?HEIGHT:DEPTH):(WIDTH>DEPTH?WIDTH:DEPTH);

            layers = new ThreeDSurface(sbi.getVolumeTexture(), (int)WIDTH, (int)HEIGHT, snake_buffered_images.getNSlices(), sbi.getZResolution());

            y.addObject(layers);
        } else{
            refreshImages();
        }
        
    }
    
    public void refreshImages(){
        
        layers.setTexture(snake_buffered_images.getVolumeTexture());
        
    }
    
    public void addSnakeListener(SnakeModel sm){
        
        y.addSnakeListener(this);
        PARENT = sm;

    }
    
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
     * Find the coordinates of an intersection for a mouse event and a displayed object.
     * for the ThreeDSnake the only interactions are with the selected snake.
     *
     * @param results all of the intersections with a mouse event
     * @param evt the event that generated the intersections.
     * @param clicked whether it was generated by a click.
     */
    public void updatePick(PickResult[] results, MouseEvent evt, boolean clicked){
        if(clicked&&PARENT.isSelecting()){

            selectByPick(results,evt,clicked);
            return;
        }
        double[] point_found = null;
        
        if(SELECTED==null)
            return;

        Node snake_node=SELECTED.getNode();

        //go through all of the interactions to see if the selected snake is amongst them.
        //the planes for the 3d rendering shouldn't appear.
        for(PickResult result: results){
            
            Node r = result.getObject();
            
            if(r == snake_node){
                
                PickIntersection pint = result.getIntersection(0);
            
                Point3d p = pint.getPointCoordinates();
                point_found = new double[] { p.x ,  p.y, p.z};
                break;
            }
            
            
        }

        //if found create a 3d event and pass back to the SnakeModel.
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

    public void selectByPick(PickResult[] results, MouseEvent evt, boolean clicked){

        //go through all of the interactions to see if the selected snake is amongst them.
        //the planes for the 3d rendering shouldn't appear.
        for(PickResult result: results){

            Node r = result.getObject();

            for(PolyLine snake: snakes){

                if(r==snake.getNode()){
                    PARENT.selectSnake(snake.getSnake());
                    return;
                }


            }


        }
    }

    @Override
    public void updatePressed(PickResult[] results, MouseEvent evt) {
    }

    @Override
    public void updateReleased(PickResult[] results, MouseEvent evt) {

    }

    @Override
    public void updateClicked(PickResult[] results, MouseEvent evt) {
        updatePick(results, evt, true);
    }

    @Override
    public void updateMoved(PickResult[] results, MouseEvent evt) {
        updatePick(results, evt, false);
    }

    @Override
    public void updateDragged(PickResult[] results, MouseEvent evt) {

    }
}
