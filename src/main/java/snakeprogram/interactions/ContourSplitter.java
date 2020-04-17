package snakeprogram.interactions;

import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import snakeprogram.MultipleSnakesStore;
import snakeprogram.ProcDrawable;
import snakeprogram.Snake;
import snakeprogram.SnakeIO;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;
import snakeprogram.Transform;
import snakeprogram.TwoDCurveDeformation;

import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ContourSplitter implements SnakeInteraction, ProcDrawable {
    Snake snake;
    List<double[]> selectedSnake;
    List<double[]> fissionCurve = new ArrayList<>();
    SnakeImages images;
    int startIndex = -1;
    int endIndex = -1;
    SnakeModel model;
    double[] next = null;

    //Limits the squared distance cutoff.
    double SNAP = 10;
    public ContourSplitter(SnakeModel model, SnakeImages img, Snake selectedSnake){
        this.model = model;
        this.images = img;
        this.snake = selectedSnake;
        this.selectedSnake = snake.getCoordinates(model.getCurrentFrame());
        images.addDrawable(this);
    }
    int findClosesPoint(double[] pt){
        double min = Double.MAX_VALUE;
        int mindex = -1;
        for(int i = 0; i<selectedSnake.size(); i++){
            double[] sp = selectedSnake.get(i);
            double d = (sp[0] - pt[0])*(sp[0] - pt[0]) + (sp[1] - pt[1])*(sp[1] - pt[1]);
            if(d==0){
                return i;
            }
            if(d<min){
                min = d;
                mindex = i;
            }
        }
        return mindex;
    }
    @Override
    public void cancelActions() {
        images.clearStaticMarkers();
        images.removeDrawable(this);
        model.unRegisterSnakeInteractor(this);
        model.updateImagePanel();
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
        if(startIndex < 0){
            double xc = images.fromZoomX(evt.getX());
            double yc = images.fromZoomY(evt.getY());

            double[] start = model.findClosestPoint(xc, yc);
            startIndex = findClosesPoint(start);
            fissionCurve.add(start);
        } else if (endIndex < 0){
            double xc = images.fromZoomX(evt.getX());
            double yc = images.fromZoomY(evt.getY());
            // adding an intermediate point.
            fissionCurve.add(new double[]{xc, yc});
        } else{
            fissionCurve.add(next);
            finish();
        }

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
    boolean snap(double xr, double yr, double[] prospect){
        double dx = xr - prospect[0];
        double dy = yr - prospect[1];

        return dx*dx + dy*dy < SNAP;

    }
    @Override
    public void mouseMoved(MouseEvent e) {
        if(startIndex<0){
            double xi = e.getX();
            double yi = e.getY();

            double xr = images.fromZoomX(xi);
            double yr = images.fromZoomY(yi);
            next = model.findClosestPoint(xr, yr);
            images.setMarker(next);
            model.updateImagePanel();
        } else{
            double xi = e.getX();
            double yi = e.getY();

            double xr = images.fromZoomX(xi);
            double yr = images.fromZoomY(yi);
            double[] closest = model.findClosestPoint(xr, yr);
            //snap!
            if(snap(xr, yr, closest)){
                next = closest;
                endIndex = findClosesPoint(closest);
                images.setMarker(next);
            } else{

                next = new double[]{xr, yr};
                endIndex = -1;
            }
            model.updateImagePanel();
        }
    }

    @Override
    public void draw(ImageProcessor proc, Transform transform) {

        if(fissionCurve.size() > 0) {
            double[] prev = transform.transform(fissionCurve.get(0));
            for (int i = 1; i < fissionCurve.size(); i++) {
                double[] b = transform.transform(fissionCurve.get(i));
                proc.drawLine((int)prev[0], (int)prev[1], (int)b[0], (int)b[1]);
                prev = b;
            }

            if (next != null) {
                double[] pt = transform.transform(next);
                proc.drawLine((int)prev[0], (int)prev[1], (int)pt[0], (int)pt[1]);
            }
        }
    }

    /**
     * Remove the old coordinates from the old snake, Split the previous coordinates into two new sets of coordinates,
     * and use the new coodinates to close the loop. Create two new snakes and add them to the model.
     *
     */
    public void finish(){
        if(startIndex==endIndex || fissionCurve.size()<2){
            cancelActions();
        }

        snake.clearSnake(model.getCurrentFrame());
        List<double[]> bonusCurve = model.addCurveSnakePoints(fissionCurve);

        List<double[]> rightPoints = new ArrayList<>(bonusCurve);
        List<double[]> leftPoints = new ArrayList<>();
        boolean leftWorking = true;
        boolean rightWorking = true;
        for(int i = 1; i<selectedSnake.size(); i++){
            if(leftWorking) {
                int left = (startIndex + i) % selectedSnake.size();
                if(left==endIndex){
                    leftWorking = false;
                } else{
                    leftPoints.add(selectedSnake.get(left));
                }

            } if(rightWorking){
                int right = ( endIndex + i ) % selectedSnake.size();
                if(right==startIndex){
                    rightWorking = false;
                } else{
                    rightPoints.add(selectedSnake.get(right));
                }
            }
            if(!leftWorking && !rightWorking){
                break;
            }
        }

        for(int i = bonusCurve.size()-1; i>=0; i--){
            leftPoints.add(bonusCurve.get(i));
        }

        Snake leftSnake = new Snake(Snake.CLOSED_SNAKE);
        leftSnake.addCoordinates(model.getCurrentFrame(), leftPoints);

        Snake rightSnake = new Snake(Snake.CLOSED_SNAKE);
        rightSnake.addCoordinates(model.getCurrentFrame(), rightPoints);

        model.addNewSnake(leftSnake);
        model.addNewSnake(rightSnake);

        images.removeDrawable(this);
        model.unRegisterSnakeInteractor(this);
        model.updateImagePanel();
    }

    /**
     * For development: opens an image and a snake file for updating.
     *
     * @param args { image_file_name, snake_file_name }
     */
    public static void main(String[] args) {
        final SnakeModel sm = new SnakeModel();
        new ImageJ();
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                sm.getFrame().setVisible(true);
                sm.loadImage(new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString()));
                HashMap<String, Double> parameters = new HashMap<>();
                MultipleSnakesStore snakes = SnakeIO.loadSnakes(
                        Paths.get(args[1]).toAbsolutePath().toString(), parameters
                );
                for (Snake s : snakes) {
                    sm.addNewSnake(s);
                }
                sm.updateImagePanel();
            }
        });
    }
}
