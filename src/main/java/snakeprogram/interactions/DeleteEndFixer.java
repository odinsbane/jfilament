package snakeprogram.interactions;

import snakeprogram.Snake;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static snakeprogram.TwoDDeformation.pointDistance;

/**
 * For removing the end of a snake. Involves two clicks, the first click selects where to cut
 * the second click selects which end can be cut off.
 *
 *
 * User: mbs207
 * Date: 12/12/11
 * Time: 2:31 PM
 */
public class DeleteEndFixer implements SnakeInteraction {
        double[] deleteFixArray; //keeps track of all of the currently clicked points
        double deleteFixCounter; //keeps track of how many points have been clicked.
        SnakeModel model;
        SnakeImages images;
        List<double[]> SnakeRaw;
        public DeleteEndFixer(SnakeModel model, SnakeImages images, Snake current){
            this.model = model;
            this.images = images;
            SnakeRaw = current.getCoordinates(images.getCounter());

            deleteFixArray = new double [4];
            deleteFixCounter = 0;

        }
        /**
         * Increments the delete end fix cycle. Step one get the point where the snake
         * will be cropped at, step two get the end to be removed.
         * @param evt - image_panel generated event.
         */
        public void mouseClicked(MouseEvent evt) {

            if(deleteFixCounter == 0){
                deleteFixArray[0] = evt.getX();
                deleteFixArray[1] = evt.getY();

                images.addStaticMarker(
                    model.findClosestPoint(
                            images.fromZoomX(evt.getX()),
                            images.fromZoomY(evt.getY())
                    )
                    );
                model.updateImagePanel();
            }

                //reads in the second click coordinates
                if(deleteFixCounter == 1){
                    deleteFixArray[2] = evt.getX();
                    deleteFixArray[3] = evt.getY();

                    //converting points based on zoom status
                    double point1X = images.fromZoomX(deleteFixArray[0]);
                    double point1Y = images.fromZoomY(deleteFixArray[1]);
                    double point2X = images.fromZoomX(deleteFixArray[2]);
                    double point2Y = images.fromZoomY(deleteFixArray[3]);

                   //finds the closest point in the snake to the user's first click
                    double[] pt1 = {point1X, point1Y};
                    double min = pointDistance(pt1, SnakeRaw.get(0));
                    double distance;
                    int closestPoint = 0;

                    for(int k = 1; k < SnakeRaw.size(); k++){
                        distance = pointDistance(pt1, SnakeRaw.get(k));
                            if(min > distance){
                                min = distance;
                                closestPoint = k;
                            }
                        }


                    //determines which end of the snake is closer to the user's second click
                    //it then removes the specified portion of the snake
                   int size = SnakeRaw.size();
                   double[] pt2 = {point2X,point2Y};
                    double distance1 = pointDistance(pt2, SnakeRaw.get(0));
                    double distance2 = pointDistance(pt2, SnakeRaw.get(size-1));
                    if(distance1 < distance2){
                        for(int l = 0; l < (closestPoint); l++ ){

                            SnakeRaw.remove(0);
                        }
                    } else{

                        for(int l = 1; l <= (size-closestPoint); l++ ){
                            SnakeRaw.remove(closestPoint);
                        }

                    }

                    //redraws image
                    model.purgeSnakes();
                    images.clearStaticMarkers();
                    model.unRegisterSnakeInteractor(this);

                    model.updateImagePanel();


                }
                deleteFixCounter++;

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
         * First finds the closest point that the snake will be cropped to, second
         * shows the end that will be cropped off.
         * @param evt image_panel generated event.
         */
        public void mouseMoved(MouseEvent evt) {
            if(deleteFixCounter==0)
                images.setMarker(
                    model.findClosestPoint(
                            images.fromZoomX(evt.getX()),
                            images.fromZoomY(evt.getY())
                    )
                );
            if(deleteFixCounter==1)
                images.setMarker(
                    model.findClosestEnd(
                            images.fromZoomX(evt.getX()),
                            images.fromZoomY(evt.getY())
                    )
                );
            model.updateImagePanel();

        }

        public void cancelActions() {
            images.clearStaticMarkers();
            model.unRegisterSnakeInteractor(this);

            model.updateImagePanel();

        }
    }
