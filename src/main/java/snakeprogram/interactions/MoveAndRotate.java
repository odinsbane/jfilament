package snakeprogram.interactions;

import snakeprogram.Snake;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by msmith on 04/01/2017.
 */
public class MoveAndRotate implements SnakeInteraction {
    SnakeModel model;
    SnakeImages images;

    ArrayList<double[]> rawSnake;
    List<double[]> original;
    boolean dragging = true;

    //current transformation state.
    double rotate = 0;
    double tx = 0;
    double ty = 0;



    Snake target;
    Rectangle2D bounds;
    double cx = 0;
    double cy = 0;

    public MoveAndRotate(SnakeModel model, SnakeImages images, Snake current){
        this.model = model;
        this.images = images;
        this.target = current;
        initializeRawSnake();
        model.setSnakeRaw(rawSnake);

    }

    /**
     * Creates a copy of the points that will be used for drawing and allowing translations.
     *
     */
    void initializeRawSnake(){
        rawSnake = new ArrayList<>();
        original = new ArrayList<>(target.getCoordinates(images.getCounter()));

        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;

        for(double[] p: original){
            double[] xy = Arrays.copyOf(p, 2);

            maxX = xy[0]>maxX?xy[0]:maxX;
            minX = xy[0]<minX?xy[0]:minX;

            maxY = xy[1]>maxY?xy[1]:maxY;
            minY = xy[1]<minY?xy[1]:minY;

            cx += xy[0];
            cy += xy[1];

            rawSnake.add(xy);
        }
        bounds = new Rectangle2D.Double(minX, minY, maxX - minX, maxY-minY);
        cx = cx/rawSnake.size();
        cy = cy/rawSnake.size();
    }

    void updateState(){

        double s = Math.sin(rotate);
        double c = Math.cos(rotate);
        double dx, dy, nx, ny;
        for(int i = 0; i<rawSnake.size(); i++){
            double[] o = original.get(i);
            double[] pt = rawSnake.get(i);

            dx = o[0] - cx;
            dy = o[1] - cy;

            nx = c*dy - s*dy;
            ny = s*dx + c*dy;

            pt[0] = nx + cx + tx;
            pt[1] = ny + cy + ty;

        }
    }

    @Override
    public void cancelActions() {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

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
    public void mouseMoved(MouseEvent e) {

    }
}
