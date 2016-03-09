package snakeprogram3d;

/**
 *
 * Keeps track of the click type and the position of the click
 * in 3d space.   It is used as a transition for a mouse event.
 * 
 * @author Matt Smith
 * 
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 *
 */
public class ThreeDEvent{
    
        final public static int LEFTCLICK = 1;
        final public static int RIGHTCLICK = 3;
        
        public double x,y,z;
        public int type;
        
        
        public ThreeDEvent(double x, double y, double z){
            
            type = 0;
            
            this.x = x;
            this.y = y;
            this.z = z;
            
        }

        public void setType(int t){
            this.type = t;
        }

}
