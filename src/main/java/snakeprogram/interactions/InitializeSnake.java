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

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * For adding a new snake, Left click to add points, or left
 * click and drag to add a lot of points.
 * 
 *
 * User: mbs207
 * Date: 12/12/11
 * Time: 2:30 PM
 */
public class InitializeSnake implements SnakeInteraction {
    SnakeImages images;
    SnakeModel model;
    int type;
    ArrayList<double[]> snakeRaw;

    /**
     * The type of snake being initialized.
     *
     * @param model - main model
     * @param images - the image that will draw features
     * @param snake_type - the type of snake being created (closed or open contour)
     */
    public InitializeSnake(SnakeModel model, SnakeImages images, int snake_type ){
        this.model = model;
        this.images = images;
        this.type = snake_type;

        images.setInitializing(true);

        snakeRaw = new ArrayList<double[]>();
        model.setSnakeRaw(snakeRaw);
        model.updateImagePanel();

    }
    public void cancelActions() {
        snakeRaw = null;
        model.unRegisterSnakeInteractor(this);
        images.setFollow(false);
        images.setInitializing(false);
        model.updateImagePanel();
    }



    public void mouseClicked(MouseEvent evt) {
         //adding the left-click coordinates to the SnakeRawX and SnakeRawY vectors
        if(SwingUtilities.isRightMouseButton(evt)||evt.isControlDown()){
            // double[] pt = {images.fromZoomX((double)evt.getX()),images.fromZoomY((double)evt.getY())};
            //snakeRaw.add(pt);

            Snake snake = new Snake(snakeRaw, images.getCounter(), type);

            model.addNewSnake(snake);

            model.setSnakeRaw(null);
            model.unRegisterSnakeInteractor(this);
            images.setFollow(false);
            images.setInitializing(false);

        } else if(SwingUtilities.isLeftMouseButton(evt)){
            double[] pt = {images.fromZoomX((double)evt.getX()),images.fromZoomY((double)evt.getY())};
            snakeRaw.add(pt);
        }
        //adding the right-click coordinate to the coordinate vectors



        model.updateImagePanel();
    }

    public void mousePressed(MouseEvent e) {
        images.setFollow(false);
    }

    public void mouseReleased(MouseEvent e) {
        images.setFollow(true);
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        mouseClicked(e);
    }

    public void mouseMoved(MouseEvent evt) {
        images.updateMousePosition(evt.getX(),evt.getY());
        model.updateImagePanel();
    }
}
