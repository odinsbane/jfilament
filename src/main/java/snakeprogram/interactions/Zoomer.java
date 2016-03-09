package snakeprogram.interactions;

import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * For zooming in or zooming in on a region of the image. Click first on the upper
 * left corner then right click on the lower right corner.
 *
 * User: mbs207
 * Date: 12/12/11
 * Time: 2:30 PM
 */
public class Zoomer implements SnakeInteraction {
    int zoomCounter=0;
    boolean bZoomInBox=false;
    SnakeImages images;
    SnakeModel model;
    public Zoomer(SnakeModel model, SnakeImages images){
        this.model = model;
        this.images = images;
    }

    public void cancelActions() {
        images.setZoomInBox(false);
        images.setZoomIn(false);
        model.unRegisterSnakeInteractor(this);
        model.updateImagePanel();
    }

    /**
        *    This performs one of two actions while the zoom is being initialized
        *    it will start the zoom box or it will finish it, Depending on the zoomCounter
        *    This button expects a left click and right click from the user. These
        *    points become the corners of the rectangle that we will zoom in on
        *
        * @param evt
    **/
    public void mouseClicked(MouseEvent evt) {
        int x = evt.getX();
        int y = evt.getY();
        if(SwingUtilities.isLeftMouseButton(evt)&&zoomCounter==0){

                images.setZoomLocation(x,y);
                images.trackingZoomBox(x,y);
                zoomCounter++;

                bZoomInBox = true;
                images.setZoomIn(false);
                images.setZoomInBox(true);
        }

        if(SwingUtilities.isRightMouseButton(evt)&&zoomCounter==1){

            //bZoomInBox = false;
            images.setZoomInBox(false);

            images.trackingZoomBox(x,y);
            zoomCounter++;

            images.setZoomIn(true);

            model.unRegisterSnakeInteractor(this);
            model.updateImagePanel();

        }

    }

    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseDragged(MouseEvent e) {}

    /**
     * Passes the mouses movements to the image panel for updating
     * the zoom box.
     *
     * @param evt panel generated events.
     */
    public void mouseMoved(MouseEvent evt) {
        if(bZoomInBox){
            images.trackingZoomBox(evt.getX(),evt.getY());
            model.updateImagePanel();
        }
    }
}
