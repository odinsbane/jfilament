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