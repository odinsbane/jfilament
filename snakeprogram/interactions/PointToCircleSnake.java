package snakeprogram.interactions;

import snakeprogram.Snake;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import ij.gui.GenericDialog;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * New class by Adri 22/12/2012
 * For adding a new circular snake, left-click to add a point, and a circle will be created. 
 * Class duplicated and modified from InitializeSnake class.
 * 
 *
 * User: ADRI
 * Date: 22/12/12
 * Time: 21:05 PM
 */
public class PointToCircleSnake implements SnakeInteraction {
    SnakeImages images;
    SnakeModel model;
    int type;
    double radius;
    //Adri 22/12/2012
    ArrayList<double[]> SnakeRaw;

    /**
     * The type of snake being initialized.
     *
     * @param model - main model
     * @param images - the image that will draw features
     * @param snake_type - the type of snake being created (closed or open contour)
     */
    public PointToCircleSnake(SnakeModel model, SnakeImages images, int snake_type, double circleradius){
    //public PointToCircleSnake(SnakeModel model, SnakeImages images, int snake_type){

        this.model = model;
        this.images = images;
        this.type = snake_type;
        this.radius = circleradius;

        images.setInitializing(true);

        SnakeRaw = new ArrayList<double[]>();
        model.setSnakeRaw(SnakeRaw);
        model.updateImagePanel();

    }
    
    public void cancelActions() {
        SnakeRaw = null;
        model.unRegisterSnakeInteractor(this);
        images.setFollow(false);
        images.setInitializing(false);
        model.updateImagePanel();
    }


    // Adri 22/12/2012
    // For every click a new circle will be created.
    // To end creating new circles, click with the right button.
    public void mouseClicked(MouseEvent evt) {
         //adding the left-click coordinates to the SnakeRawX and SnakeRawY vectors
        if(SwingUtilities.isLeftMouseButton(evt)){
            // double[] pt = {images.fromZoomX((double)evt.getX()),images.fromZoomY((double)evt.getY())};
        	double[] pt = {images.fromZoomX((double)evt.getX()),images.fromZoomY((double)evt.getY())};
            //SnakeRaw.add(pt);
            Snake snake = new Snake(initializeContour(pt), images.getCounter(), type);
            model.addNewPointToCircleSnake(snake);
            model.setSnakeRaw(null);
            //model.unRegisterSnakeInteractor(this);
            images.setFollow(false);
            images.setInitializing(false);
            model.updateImagePanel();
        }
        // Adri 22/12/2012
        //With this implementation, right click only is used to end creation of circles.
        if(SwingUtilities.isRightMouseButton(evt)){
            //double[] pt = {images.fromZoomX((double)evt.getX()),images.fromZoomY((double)evt.getY())};
            //SnakeRaw.add(pt);
            //Snake snake = new Snake(initializeContour(pt), images.getCounter(), type);
            //model.addNewPointToCircleSnake(snake);
            //model.setSnakeRaw(null);
            model.unRegisterSnakeInteractor(this);
            //images.setFollow(false);
            //images.setInitializing(false);
            //model.updateImagePanel();
        }
    }
    
    // Adri 22/12/2012
    // Method taken from Snakes_from_speckles plugin, by user melkor.
    // It allows to draw a circular snake starting from a single point (click).
    // radius is fixed for the moment.
    public ArrayList<double[]> initializeContour(double[] point){
        ArrayList<double[]> initialized_points = new ArrayList<double[]>();
        //double radius=11; //Line to have fixed radius.
        //point separation is 1 px so there are 2 pi r points
        int steps = (int) (2*3.14159*radius);
        for(int i = 0; i<steps; i++){

            double theta = (2*3.14159*i)/steps;
            double x = point[0] + radius*Math.sin(theta);
            double y = point[1] + radius*Math.cos(theta);

            initialized_points.add(new double[]{x, y});
        }
        return initialized_points;

    }

    public void mousePressed(MouseEvent e) {
        images.setFollow(false);
    }

    public void mouseReleased(MouseEvent e) {
        images.setFollow(true);
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        mouseClicked(e);
    }

    public void mouseMoved(MouseEvent evt) {
        images.updateMousePosition(evt.getX(),evt.getY());
        model.updateImagePanel();
    }
}
