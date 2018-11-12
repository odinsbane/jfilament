package snakeprogram.interactions;

import ij.process.ImageProcessor;
import snakeprogram.*;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SnakeSculptor implements SnakeInteraction, ProcDrawable {
    SnakeModel model;
    SnakeImages images;
    List<double[]> modifying = new ArrayList<>();
    Snake snake;
    boolean[] ignoring;
    double[] cursorPosition = {0,0};
    double radius = 20;

    int n;
    public SnakeSculptor(SnakeModel model, SnakeImages images, Snake current){
        this.model = model;
        this.images = images;
        snake = current;
        duplicatePoints(current.getCoordinates(model.getCurrentFrame()));
        images.addDrawable(this);
        model.updateImagePanel();
    }
    public void accept(){
        snake.addCoordinates(model.getCurrentFrame(), modifying);
        images.removeDrawable(this);
        model.unRegisterSnakeInteractor(this);
        model.updateImagePanel();
    }

    @Override
    public void draw(ImageProcessor proc,Transform transform){
        double[] last = null;

        if(snake.TYPE == Snake.CLOSED_SNAKE){
            last = transform.transform(modifying.get(modifying.size() - 1));
        }

        proc.setColor(Color.BLUE);
        for(double[] d: modifying){
            double[] pt = transform.transform(d);
            if(last!=null){
                proc.drawLine((int)last[0], (int)last[1], (int)pt[0], (int)pt[1]);
            }
            last = pt;
        }
        if(cursorPosition==null) return;
        double[] drawDimensionsA = transform.transform(new double[]{cursorPosition[0] - radius, cursorPosition[1] - radius});
        double[] drawDimensionsB = transform.transform(new double[]{cursorPosition[0] + radius, cursorPosition[1] + radius});
        proc.setColor(Color.CYAN);
        proc.drawOval((int)(drawDimensionsA[0]), (int)(drawDimensionsA[1]), (int)(drawDimensionsB[0] - drawDimensionsA[0]), (int)(drawDimensionsB[1] - drawDimensionsA[1]));

    }


    void duplicatePoints(List<double[]> points){
        n = points.size();
        modifying = points.stream().map(pt-> Arrays.copyOf(pt, 2)).collect(Collectors.toList());
        ignoring = new boolean[n];

    }

    @Override
    public void cancelActions() {
        images.removeDrawable(this);
        model.unRegisterSnakeInteractor(this);
        model.updateImagePanel();

    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getButton()==MouseEvent.BUTTON3 || (e.getModifiers()&MouseEvent.CTRL_MASK)!=0){
            accept();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        double x = images.fromZoomX(e.getX());
        double y = images.fromZoomY(e.getY());
        cursorPosition = new double[]{x, y};
        //ignore points.
        for(int i = 0; i<n; i++){
            if (contains(modifying.get(i))) {
                ignoring[i] = true;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Arrays.fill(ignoring, false);
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        cursorPosition[0] = images.fromZoomX(e.getX());
        cursorPosition[1] = images.fromZoomY(e.getY());

        for(int i = 0; i<n; i++){
            if(!ignoring[i] && contains(modifying.get(i))){

                move(modifying.get(i));


            }



        }
        model.updateImagePanel();
    }

    private void move(double[] pt) {
        double u = pt[0] - cursorPosition[0];
        double v = pt[1] - cursorPosition[1];
        if(u==0 && v==0){
            //TODO use a history of positions to find how this point made it to the center.
            return;
        } else{
            double m = Math.sqrt(u*u + v*v);
            u = u*radius/m;
            v = v*radius/m;
            pt[0] = cursorPosition[0] + u;
            pt[1] = cursorPosition[1] + v;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        cursorPosition[0] = images.fromZoomX(e.getX());
        cursorPosition[1] = images.fromZoomY(e.getY());
        model.updateImagePanel();
    }

    boolean contains(double[] pt){
        double u = pt[0] - cursorPosition[0];
        double v = pt[1] - cursorPosition[1];
        return u*u + v*v < radius*radius;
    }



}
