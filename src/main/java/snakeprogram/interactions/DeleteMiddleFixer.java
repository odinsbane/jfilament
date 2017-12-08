package snakeprogram.interactions;

import snakeprogram.Snake;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static snakeprogram.TwoDDeformation.pointDistance;

/**
 * Removes all points between two selected points.
 * 
 * User: mbs207
 * Date: 12/12/11
 * Time: 2:32 PM
 */
public class DeleteMiddleFixer implements SnakeInteraction {
    double[] deleteFixArray; //keeps track of all of the currently clicked points
    double deleteFixCounter; //keeps track of how many points have been clicked.
    SnakeModel model;
    SnakeImages images;
    List<double[]> SnakeRaw;
    public DeleteMiddleFixer(SnakeModel model, SnakeImages images, Snake current){
            deleteFixArray = new double [4];
            deleteFixCounter = 0;
            this.model = model;
            this.images = images;
            
            if(current.TYPE==Snake.CLOSED_SNAKE){
                List<double[]> points = current.getCoordinates(images.getCounter());
                images.addStaticMarker(points.get(0));
                images.addStaticMarker(points.get(points.size()-1));
                model.updateImagePanel();
            }
            SnakeRaw = current.getCoordinates(images.getCounter());

        }
        /**
       *
       *    This method allows to delete a middle portion of a snake by clicking at the left end
       *    and then the right end of the section to be deleted.
       *
       **/
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

               //resets the points based on the zoom status
               double point1X = images.fromZoomX(deleteFixArray[0]);
               double point1Y = images.fromZoomY(deleteFixArray[1]);
               double point2X = images.fromZoomX(deleteFixArray[2]);
               double point2Y = images.fromZoomY(deleteFixArray[3]);

                //finds the closest point in the snake to the user's first click
                double[] pt1 = {point1X,point1Y};
                double[] pt2 = {point2X, point2Y};
                double min1 = pointDistance(pt1, SnakeRaw.get(0));
                double min2 = pointDistance(pt2, SnakeRaw.get(0));

                double distance1,distance2;

                int closestPoint1 = 0;
                int closestPoint2 = 0;


                for(int k = 1; k < SnakeRaw.size(); k++){

                        distance1 = pointDistance(pt1, SnakeRaw.get(k));
                        distance2 = pointDistance(pt2, SnakeRaw.get(k));

                        if(distance1<min1){
                            min1 = distance1;
                            closestPoint1 = k;
                        }

                        if(distance2<min2){
                            min2 = distance2;
                            closestPoint2 = k;
                        }
                }

                int l = closestPoint1 < closestPoint2?closestPoint1:closestPoint2;
                int h = closestPoint1 > closestPoint2?closestPoint1:closestPoint2;

                for(int i = h; i>=l; i--){
                    SnakeRaw.remove(i);
                }

                images.clearStaticMarkers();
                model.purgeSnakes();
                model.updateImagePanel();
                model.unRegisterSnakeInteractor(this);
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
     * Updates the cursor for showing what point is actually going to be removed.
     *
     * @param evt generated event.
     */
    public void mouseMoved(MouseEvent evt) {

        double[] pt = model.findClosestPoint(
                images.fromZoomX(evt.getX()),
                images.fromZoomY(evt.getY())
        );

        images.setMarker(pt);
        model.updateImagePanel();
    }

    public void cancelActions() {
        images.clearStaticMarkers();
        images.setStretchFix(false);
        model.unRegisterSnakeInteractor(this);

        model.updateImagePanel();
    }
}
