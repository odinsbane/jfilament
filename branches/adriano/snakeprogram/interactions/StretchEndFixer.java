package snakeprogram.interactions;

import snakeprogram.Snake;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import java.awt.event.MouseEvent;
import java.util.ArrayList;

import static snakeprogram.TwoDDeformation.pointDistance;

/**
 * for adding to the end of a snake.
 *
 * User: mbs207
 * Date: 12/12/11
 * Time: 2:33 PM
 */
public class StretchEndFixer implements SnakeInteraction {
        ArrayList<double[]> SnakeRaw;
        SnakeModel model;
        SnakeImages images;

        public StretchEndFixer(SnakeModel model, SnakeImages images, Snake current){
            this.model = model;
            this.images = images;
            
            images.setStretchFix(true);
            SnakeRaw = current.getCoordinates(images.getCounter());
        }
        /**
         * Adds a point to the closest end of the snake, essentially stretching it.
         *
         * @param evt event from image panel.
         */
        public void mouseClicked(MouseEvent evt) {


            double x = images.fromZoomX(evt.getX());
            double y = images.fromZoomY(evt.getY());

            double[] stretch_fix = new double[] { x, y};

            int last = SnakeRaw.size()-1;

            if(last>0){
                double to_tail = pointDistance(SnakeRaw.get(0), stretch_fix);
                double to_head = pointDistance(SnakeRaw.get(last),stretch_fix);

                //if it is closer to the tail 0 then it inserts it first otherwise it inserts it last.

                int j = to_tail<to_head?0:last+1;

                SnakeRaw.add(j, stretch_fix);
            }

            images.clearStaticMarkers();
            images.setStretchFix(false);
            model.unRegisterSnakeInteractor(this);

            model.updateImagePanel();

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

        /**
         * Draws a line to the cursor to show what the new segment of snake will look like.
         *
         * @param evt image panel event.
         */
        public void mouseMoved(MouseEvent evt) {

            images.clearStaticMarkers();
            images.addStaticMarker(
                model.findClosestEnd(
                        images.fromZoomX(evt.getX()),
                        images.fromZoomY(evt.getY())
                )
                );
            images.addStaticMarker(new double[]{images.fromZoomX(evt.getX()),
                                images.fromZoomY(evt.getY())});
            model.updateImagePanel();

        }

        public void cancelActions() {

            images.clearStaticMarkers();
            images.setStretchFix(false);
            model.unRegisterSnakeInteractor(this);

            model.updateImagePanel();

        }
    }
