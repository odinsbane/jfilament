package snakeprogram.interactions;

import snakeprogram.SnakeFrame;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import java.awt.event.MouseEvent;

/**
 * Gets the foreground intensity, the intensity at which snakes
 * grow.
 *
 * User: mbs207
 * Date: 12/12/11
 * Time: 2:31 PM
 */
public class ForegroundMean implements SnakeInteraction {
        SnakeModel model;
        SnakeFrame frame;
        SnakeImages images;
        public ForegroundMean(SnakeModel model, SnakeFrame frame, SnakeImages images){
            this.model = model;
            this.frame = frame;
            this.images = images;
        }
        public void cancelActions() {
            model.unRegisterSnakeInteractor(this);
        }

        /**
       *    Finds the location of the current mouse click and finds the
       *    mean intensity about that point.  Sets the forground intensity
       *    value and updates the UI to display the value.
       *
       **/
        public void mouseClicked(MouseEvent evt) {
            Double x = images.fromZoomX(evt.getX());
            Double y = images.fromZoomY(evt.getY());

            double value = images.getAveragedValue(x,y,SnakeModel.squareSize, model.getSigma());




            model.unRegisterSnakeInteractor(this);
            //prints this mean intensity to the text box on the user interface
            String S = value>1?String.format("%2.2f", value):String.format("%2.2e",value);

            frame.updateForegroundText(S);
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
