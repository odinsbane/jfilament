package snakeprogram.interactions;

import snakeprogram.Snake;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * For adding a new snake, Left click to add points, or left
 * click and drag to add a lot of points.
 * 
 *
 * User: mbs207
 * Date: 12/12/11
 * Time: 2:30 PM
 */
public class InitializeSnake implements SnakeInteraction {
    SnakeImages images;
    SnakeModel model;
    int type;
    ArrayList<double[]> SnakeRaw;

    /**
     * The type of snake being initialized.
     *
     * @param model - main model
     * @param images - the image that will draw features
     * @param snake_type - the type of snake being created (closed or open contour)
     */
    public InitializeSnake(SnakeModel model, SnakeImages images, int snake_type ){
        this.model = model;
        this.images = images;
        this.type = snake_type;

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



    public void mouseClicked(MouseEvent evt) {
         //adding the left-click coordinates to the SnakeRawX and SnakeRawY vectors
        if(SwingUtilities.isLeftMouseButton(evt)){
            double[] pt = {images.fromZoomX((double)evt.getX()),images.fromZoomY((double)evt.getY())};
            SnakeRaw.add(pt);
        }

        //adding the right-click coordinate to the coordinate vectors
        if(SwingUtilities.isRightMouseButton(evt)){
            // double[] pt = {images.fromZoomX((double)evt.getX()),images.fromZoomY((double)evt.getY())};
            //SnakeRaw.add(pt);

            Snake snake = new Snake(SnakeRaw, images.getCounter(), type);

            model.addNewSnake(snake);

            model.setSnakeRaw(null);
            model.unRegisterSnakeInteractor(this);
            images.setFollow(false);
            images.setInitializing(false);

        }

        model.updateImagePanel();
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
