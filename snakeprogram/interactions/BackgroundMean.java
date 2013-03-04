package snakeprogram.interactions;

import snakeprogram.SnakeFrame;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import java.awt.event.MouseEvent;

/**
 * For picking background intensity values used when growing the snake.
 *
 * User: mbs207
 * Date: 12/12/11
 * Time: 2:28 PM
 */
public class BackgroundMean implements SnakeInteraction {
    SnakeModel model;
    SnakeImages images;
    SnakeFrame frame;


    /**
     * access to the images and the model.
     * @param m - model for making changes.
     * @param i - images for getting value and coordinates.
     * @param panel - snake frame for setting text when finished.
     */
    public BackgroundMean(SnakeModel m, SnakeImages i, SnakeFrame panel){
        model = m;
        images = i;
        frame = panel;
    }
    public void cancelActions() {
        model.unRegisterSnakeInteractor(this);
    }

    public void mouseClicked(MouseEvent evt) {
        Double x = images.fromZoomX(evt.getX());
        Double y = images.fromZoomY(evt.getY());
        double value = images.getAveragedValue(x,y,SnakeModel.squareSize, model.getSigma());


        //prints this mean intensity to the text box on the user interface
        String S = value>1?String.format("%2.2f", value):String.format("%2.2e",value);
        frame.updateBackgroundText(S);
        model.unRegisterSnakeInteractor(this);
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }
}
