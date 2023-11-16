/*-
 * #%L
 * JFilament 2D active contours.
 * %%
 * Copyright (C) 2010 - 2023 University College London
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the UCL LMCB nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package snakeprogram.interactions;

import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import snakeprogram.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SnakeSculptor implements SnakeInteraction, ProcDrawable {
    SnakeModel model;
    SnakeImages images;
    List<double[]> modifying = new ArrayList<>();
    Snake snake;
    boolean[] ignoring;
    double[] cursorPosition = {0,0};
    static double radius = 20;

    int n;

    boolean pushPoints = true;

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

    void setRadius(double r){
        this.radius = r;
        model.updateImagePanel();

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
    public void showSizeDialogue(Point position){
        JDialog dialog = new JDialog(model.getFrame());
        dialog.setUndecorated(true);
        dialog.setOpacity(0.5f);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        JSlider slides = new JSlider(JSlider.VERTICAL);
        JButton finish = new JButton("finish");
        final double starting = radius;
        double factor = Math.log(10)/50.0;

        slides.addChangeListener(evt->{
           int i = slides.getValue();
           setRadius(starting * Math.exp(factor * i)*0.1);
        });
        Container c = dialog.getContentPane();
        c.add(slides, BorderLayout.CENTER);
        c.add(finish, BorderLayout.SOUTH);

        dialog.pack();
        int w = dialog.getWidth();
        int h = dialog.getHeight();

        Point loc =new Point( position.x - w/2 + (int)radius,position.y -h/2);
        dialog.setLocation(loc);


        finish.addActionListener(evt->{
            dialog.setVisible(false);
        });
        EventQueue.invokeLater(()->{
            dialog.setVisible(true);
        });


    }
    @Override
    public void mousePressed(MouseEvent e) {
        if(e.isControlDown()){
            showSizeDialogue(e.getLocationOnScreen());
            return;
        }
        if(e.isShiftDown()){
            pushPoints = false;
        }
        double x = images.fromZoomX(e.getX());
        double y = images.fromZoomY(e.getY());
        cursorPosition = new double[]{x, y};
        //ignore points.
        if( pushPoints) {
            //ignore inside points;
            for (int i = 0; i < n; i++) {
                if (contains(modifying.get(i))) {
                    ignoring[i] = true;
                }
            }
        } else{
            //ignore outside points;
            for (int i = 0; i < n; i++) {
                if ( !contains(modifying.get(i))) {
                    ignoring[i] = true;
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        pushPoints = true;
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
            if(ignoring[i]) continue;

            if(pushPoints) {
                //Moving points that are in circle, to edge of the circle.
                if ( contains( modifying.get(i) ) ) {
                    push(modifying.get(i));
                }
            } else{
                if( !contains( modifying.get(i) ) ){
                    drag(modifying.get(i));
                }
            }
        }
        model.updateImagePanel();
    }

    private void drag(double[] pt) {
        double u = pt[0] - cursorPosition[0];
        double v = pt[1] - cursorPosition[1];
        double m = Math.sqrt(u * u + v * v);
        if(u==0 && v==0){
            //this is only possible if the radius is too small.
            return;
        } else{
            u = u * radius / m;
            v = v * radius / m;
            pt[0] = cursorPosition[0] + u;
            pt[1] = cursorPosition[1] + v;
        }
    }

    private void push(double[] pt) {
        double u = pt[0] - cursorPosition[0];
        double v = pt[1] - cursorPosition[1];
        double m = Math.sqrt(u * u + v * v);
        if(u==0 && v==0){
            return;
        } else{
                u = u * radius / m;
                v = v * radius / m;
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

    /**
     * For development: opens an image and a snake file for updating.
     *
     * @param args { image_file_name, snake_file_name }
     */
    public static void main(String[] args){
        final SnakeModel sm = new SnakeModel();
        new ImageJ();
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                sm.getFrame().setVisible(true);
                sm.loadImage(new ImagePlus( Paths.get(args[0]).toAbsolutePath().toString()) );
                HashMap<String, Double> parameters = new HashMap<>();
                MultipleSnakesStore snakes = SnakeIO.loadSnakes(
                        Paths.get(args[1]).toAbsolutePath().toString(), parameters
                );
                for(Snake s: snakes){
                    sm.addNewSnake(s);
                }
                sm.setZoom(0, 0, sm.getImageWidth(), sm.getImageHeight());
                sm.updateImagePanel();
                sm.setParameters(parameters);
            }
        });

    }


}
