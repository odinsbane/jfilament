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
