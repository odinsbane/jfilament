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

import snakeprogram.SnakeFrame;
import snakeprogram.SnakeImages;
import snakeprogram.SnakeModel;

import java.awt.event.MouseEvent;

/**
 * Gets the foreground intensity, the intensity at which snakes
 * grow.
 *
 * User: mbs207
 * Date: 12/12/11
 * Time: 2:31 PM
 */
public class ForegroundMean implements SnakeInteraction {
        SnakeModel model;
        SnakeFrame frame;
        SnakeImages images;
        public ForegroundMean(SnakeModel model, SnakeFrame frame, SnakeImages images){
            this.model = model;
            this.frame = frame;
            this.images = images;
        }
        public void cancelActions() {
            model.unRegisterSnakeInteractor(this);
        }

        /**
       *    Finds the location of the current mouse click and finds the
       *    mean intensity about that point.  Sets the forground intensity
       *    value and updates the UI to display the value.
       *
       **/
        public void mouseClicked(MouseEvent evt) {
            Double x = images.fromZoomX(evt.getX());
            Double y = images.fromZoomY(evt.getY());

            double value = images.getAveragedValue(x,y,SnakeModel.squareSize, model.getSigma());




            model.unRegisterSnakeInteractor(this);
            //prints this mean intensity to the text box on the user interface
            String S = value>1?String.format("%2.2f", value):String.format("%2.2e",value);

            frame.updateForegroundText(S);
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

        public void mouseMoved(MouseEvent e) {
        }
    }
