package snakeprogram.interactions;

import snakeprogram.Snake;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * When dealing with a contour, the ends of the snake can be troublesome. When this actions is started, it will
 * highlight the closest snake point and if the user clicks the ends of the snake will be repositioned to that point.
 * The order of the snake should not change, merely the location of the first and last points in the backing arraylist.
 *
 * Created by msmith on 2/12/14.
 */
public class RepositionContourEnds implements SnakeInteraction{
    SnakeModel model;
    SnakeImages images;
    Snake snake;
    public RepositionContourEnds(SnakeModel model, SnakeImages images, Snake current){
        this.model = model;
        this.images = images;
        this.snake = current;
    }

    @Override
    public void cancelActions() {
        model.updateImagePanel();
        model.unRegisterSnakeInteractor(this);
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
        double[] pt = model.findClosestPoint(
                images.fromZoomX(evt.getX()),
                images.fromZoomY(evt.getY())
        );

        List<double[]> points = snake.getCoordinates(images.getCounter());
        List<double[]> other = new ArrayList<double[]>();
        Iterator<double[]> iter = points.iterator();
        while(iter.hasNext()){
            double[] d = iter.next();
            if(d[0] == pt[0] && d[1]==pt[1]){
                break;
            }
            iter.remove();
            other.add(d);
        }

        points.addAll(other);

        model.updateImagePanel();
        model.unRegisterSnakeInteractor(this);

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent evt) {
        double[] pt = model.findClosestPoint(
                images.fromZoomX(evt.getX()),
                images.fromZoomY(evt.getY())
        );

        images.setMarker(pt);
        model.updateImagePanel();
    }
}
