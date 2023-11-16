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
package crick.salbreuxlab.segmentation2d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;


public class Vertex{
    final public int x;
    final public int y;
    final int hash;
    int connections;
    Color color = Color.BLUE;

    Set<Vertex> neighbors = new HashSet<>();
    public Vertex(int x, int y, int connections){
        this.x = x;
        this.y = y;
        this.connections = connections;
        //unique hash for x & y < 65k elements. .
        hash = x + ((y&0xffff)<<16) + (y&0xffff0000);
    }


    public void draw(Graphics2D g2d){
        g2d.setColor(color);
        g2d.fillRect(x-2, y-2, 5, 5);
    }

    public boolean contains(Point2D point) {
        if(point.getX()>x-2 && point.getX()<x+3 && point.getY()>y-2 && point.getY()<y+3){
            return true;
        }
        return false;
    }

    @Override
    public int hashCode(){
        return hash;
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof Vertex){
            Vertex v = (Vertex)o;
            return v.x==x && v.y==y;
        }
        return false;
    }

    @Override public String toString(){
        return "vertex:" + x + ", " + y;
    }

}
