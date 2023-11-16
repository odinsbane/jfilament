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

import ij.process.ImageProcessor;
import snakeprogram.ProcDrawable;
import snakeprogram.Snake;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;
import snakeprogram.Transform;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by msmith on 04/01/2017.
 */
public class MoveAndRotate implements SnakeInteraction {
    final static int NO_MODE = 0;
    final static int DRAG_MODE = 1;
    final static int ROTATE_MODE = 2;

    SnakeModel model;
    SnakeImages images;
    ProcDrawable drawable;
    ProcDrawable boundsDrawable;
    ArrayList<double[]> rawSnake;
    List<double[]> original;
    int mode = 0;
    double[] last = new double[2];

    //current transformation state.
    double rotate = 0;
    double tx = 0;
    double ty = 0;




    Snake target;
    Ellipse2D bounds;
    double cx = 0;
    double cy = 0;

    public MoveAndRotate(SnakeModel model, SnakeImages images, Snake current){
        this.model = model;
        this.images = images;
        this.target = current;
        initializeRawSnake();
        mode = NO_MODE;
        drawable = new RawSnake();
        boundsDrawable = new BoundDraw();
        images.addDrawable(drawable);
        images.addDrawable(boundsDrawable);
        model.updateImagePanel();
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
        cx = cx/rawSnake.size();
        cy = cy/rawSnake.size();

        double zcx = images.toZoomX(cx);
        zcx = zcx + 30;
        double boundsWidth = images.fromZoomX(zcx) - cx;
        bounds = new Ellipse2D.Double(cx - boundsWidth, cy - boundsWidth, boundsWidth*2, boundsWidth*2);

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

            nx = c*dx - s*dy;
            ny = s*dx + c*dy;

            pt[0] = nx + cx + tx;
            pt[1] = ny + cy + ty;

        }
        double hw = bounds.getWidth();
        bounds.setFrame(cx+tx - hw/2, cy+ty - hw/2, hw, hw);

        model.updateImagePanel();

    }

    @Override
    public void cancelActions() {
        images.removeDrawable(drawable);
        images.removeDrawable(boundsDrawable);
        model.unRegisterSnakeInteractor(this);
        model.updateImagePanel();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getButton()==MouseEvent.BUTTON3 || (e.getModifiers()&MouseEvent.CTRL_MASK)!=0){
            apply();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        double x0 = e.getX();
        double y0 = e.getY();
        double x = images.fromZoomX(x0);
        double y = images.fromZoomY(y0);
        if(bounds.contains(x, y)){
            double dx = x - bounds.getCenterX();
            double dy = y - bounds.getCenterY();
            if(dx*dx + dy*dy < bounds.getWidth()*bounds.getWidth()/16){
                mode = DRAG_MODE;
                last[0] = x;
                last[1] = y;
            } else{
                mode = ROTATE_MODE;
                dx = x - bounds.getCenterX();
                dy = y - bounds.getCenterY();
                double m = dx*dx + dy*dy;
                m = Math.sqrt(m);
                last[0] = dx/m;
                last[1] = dy/m;
            }

        }

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mode = NO_MODE;
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

        double x = images.fromZoomX(e.getX());
        double y = images.fromZoomY(e.getY());
        switch(mode){
            case DRAG_MODE:
                tx += (x - last[0]);
                ty += (y - last[1]);

                last[0] = x;
                last[1] = y;
                updateState();
                break;
            case ROTATE_MODE:
                double dx = x - bounds.getCenterX();
                double dy = y - bounds.getCenterY();
                double m = dx*dx + dy*dy;
                if(m == 0){
                    break;
                }
                m = Math.sqrt(m);
                dx = dx/m;
                dy = dy/m;

                rotate += (-Math.atan2(dx, dy) + Math.atan2(last[0], last[1]));
                last[0] = dx;
                last[1] = dy;
                updateState();
                break;
            default:

        }

    }

    public void apply(){

        List<double[]> v = target.getCoordinates(images.getCounter());
        v.clear();
        v.addAll(rawSnake);
        cancelActions();
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }
    class BoundDraw implements ProcDrawable{

        @Override
        public void draw(ImageProcessor proc, Transform transform) {
            Rectangle2D r = bounds.getBounds2D();
            proc.setColor(Color.YELLOW);
            double[] pt = {r.getX(), r.getY()};
            double[] pt2 = transform.transform(new double[]{r.getX() + r.getWidth(), r.getY() + r.getWidth()});
            double[] r0 = transform.transform(pt);
            int w = (int)(pt2[0] - r0[0]);

            proc.drawOval((int)r0[0], (int)r0[1], w, w);
            proc.setColor(Color.CYAN);
            proc.drawOval((int)r0[0]+w/4, (int)r0[1]+w/4, w/2, w/2);

        }
    }
    class RawSnake implements ProcDrawable{


        @Override
        public void draw(ImageProcessor proc, Transform transform) {
            double[] last = null;
            proc.setColor(Color.BLUE);
            for(double[] d: rawSnake){
                double[] pt = transform.transform(d);
                if(last!=null){
                    proc.drawLine((int)last[0], (int)last[1], (int)pt[0], (int)pt[1]);
                }
                last = pt;
            }
        }
    }
}
