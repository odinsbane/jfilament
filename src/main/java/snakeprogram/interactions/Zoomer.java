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

import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * For zooming in or zooming in on a region of the image. Click first on the upper
 * left corner then right click on the lower right corner.
 *
 * User: mbs207
 * Date: 12/12/11
 * Time: 2:30 PM
 */
public class Zoomer implements SnakeInteraction {
    int zoomCounter=0;
    boolean bZoomInBox=false;
    SnakeImages images;
    SnakeModel model;
    public Zoomer(SnakeModel model, SnakeImages images){
        this.model = model;
        this.images = images;
    }

    public void cancelActions() {
        images.setZoomInBox(false);
        images.setZoomIn(false);
        model.unRegisterSnakeInteractor(this);
        model.updateImagePanel();
    }

    /**
        *    This performs one of two actions while the zoom is being initialized
        *    it will start the zoom box or it will finish it, Depending on the zoomCounter
        *    This button expects a left click and right click from the user. These
        *    points become the corners of the rectangle that we will zoom in on
        *
        * @param evt
    **/
    public void mouseClicked(MouseEvent evt) {
        int x = evt.getX();
        int y = evt.getY();

        if((SwingUtilities.isRightMouseButton(evt)||evt.isControlDown())&&zoomCounter==1){

            //bZoomInBox = false;
            images.setZoomInBox(false);

            images.trackingZoomBox(x,y);
            zoomCounter++;

            images.setZoomIn(true);

            model.unRegisterSnakeInteractor(this);
            model.updateImagePanel();

        } else if(SwingUtilities.isLeftMouseButton(evt)&&zoomCounter==0){

                images.setZoomLocation(x,y);
                images.trackingZoomBox(x,y);
                zoomCounter++;

                bZoomInBox = true;
                images.setZoomIn(false);
                images.setZoomInBox(true);
        }



    }

    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseDragged(MouseEvent e) {}

    /**
     * Passes the mouses movements to the image panel for updating
     * the zoom box.
     *
     * @param evt panel generated events.
     */
    public void mouseMoved(MouseEvent evt) {
        if(bZoomInBox){
            images.trackingZoomBox(evt.getX(),evt.getY());
            model.updateImagePanel();
        }
    }
}
