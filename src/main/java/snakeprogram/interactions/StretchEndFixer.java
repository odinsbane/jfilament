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
import java.util.List;

import static snakeprogram.TwoDDeformation.pointDistance;

/**
 * for adding to the end of a snake.
 *
 * User: mbs207
 * Date: 12/12/11
 * Time: 2:33 PM
 */
public class StretchEndFixer implements SnakeInteraction {
        List<double[]> SnakeRaw;
        SnakeModel model;
        SnakeImages images;

        public StretchEndFixer(SnakeModel model, SnakeImages images, Snake current){
            this.model = model;
            this.images = images;
            
            images.setStretchFix(true);
            SnakeRaw = current.getCoordinates(images.getCounter());
        }
        /**
         * Adds a point to the closest end of the snake, essentially stretching it.
         *
         * @param evt event from image panel.
         */
        public void mouseClicked(MouseEvent evt) {


            double x = images.fromZoomX(evt.getX());
            double y = images.fromZoomY(evt.getY());

            double[] stretch_fix = new double[] { x, y};

            int last = SnakeRaw.size()-1;

            if(last>0){
                double to_tail = pointDistance(SnakeRaw.get(0), stretch_fix);
                double to_head = pointDistance(SnakeRaw.get(last),stretch_fix);

                //if it is closer to the tail 0 then it inserts it first otherwise it inserts it last.

                int j = to_tail<to_head?0:last+1;

                SnakeRaw.add(j, stretch_fix);
            }

            images.clearStaticMarkers();
            images.setStretchFix(false);
            model.unRegisterSnakeInteractor(this);

            model.updateImagePanel();

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

        /**
         * Draws a line to the cursor to show what the new segment of snake will look like.
         *
         * @param evt image panel event.
         */
        public void mouseMoved(MouseEvent evt) {

            images.clearStaticMarkers();
            images.addStaticMarker(
                model.findClosestEnd(
                        images.fromZoomX(evt.getX()),
                        images.fromZoomY(evt.getY())
                )
                );
            images.addStaticMarker(new double[]{images.fromZoomX(evt.getX()),
                                images.fromZoomY(evt.getY())});
            model.updateImagePanel();

        }

        public void cancelActions() {

            images.clearStaticMarkers();
            images.setStretchFix(false);
            model.unRegisterSnakeInteractor(this);

            model.updateImagePanel();

        }
    }
