package snakeprogram;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
/**
 * @author Lisa
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 *
 */
public class Snake implements Iterable<Integer>{
    
    public static final int OPEN_SNAKE = 0;
    public static final int CLOSED_SNAKE=1;
    
    /** The Integer is the frame in time and the ArrayList are the coordinates */
    final TreeMap<Integer,List<double[]>> Coordinates;
    
    final public int TYPE;
    
        
    /**
       *    Creates a new snake does not have any position data
       *    @param  type    could be either an OPEN_SNAKE or a CLOSED_SNAKE for the respective curve types
       **/

    public Snake(int type){
        Coordinates = new TreeMap<Integer,List<double[]>>();
        TYPE=type;
    }
    
    /**
       *    Creates a new snake with position data.
       *    @param  coord  xcoordinates
       *    @param  i       frame that the current coordinates correspond too.
       *    @param  type    could be either an OPEN_SNAKE or a CLOSED_SNAKE for the respective curve types
       **/
    public Snake(List<double[]> coord,int i,int type){
        
        Coordinates = new TreeMap<Integer,List<double[]>>();
        Coordinates.put(i,coord);
        
        TYPE=type;
        
    }

    
    public List<double[]> getCoordinates(int i){
        return Coordinates.get(i);
    }

    /**
       *    Finds the length, cumulative distance between points in a given frame.
       **/

    public double findLength(int time){
    
        List<double[]> coordinates = Coordinates.get(time);
        
        int size = coordinates.size();
        double distance = 0;
        for(int i = 0; i < (size-1); i++){
            distance+=TwoDDeformation.pointDistance(    coordinates.get(i), coordinates.get(i+1)  );
        }
        if(TYPE==CLOSED_SNAKE){
            distance += TwoDDeformation.pointDistance(coordinates.get(size-1), coordinates.get(0));
        }
        return distance;
    }
    
    /**
       *    Gets the number of points in the given frame   
       **/

    public int getSize(int frame){
        if(exists(frame))
            return Coordinates.get(frame).size();
        else
            return 0;
    }
        
    /**
       * Checks if snake exists in frame
       **/

    public boolean exists(int frame){
        return Coordinates.containsKey(frame);
    }
    
    /**
       *    Removes the snake from frame, note frames start at 1
       *    @param frame the frame to be removed from
       **/

    public void clearSnake(int frame){
        Coordinates.remove(frame);
    }
    
    /**
       *    Checks every frame to see if the snake is too short
       *    then returns whether or not there are snakes left
       *
       **/
    public boolean isEmpty(){
        for(Integer k: Coordinates.keySet()){
            if(getSize(k)<2)
                clearSnake(k);
        }
        return Coordinates.isEmpty();
    
    }
    
    /**
       *    Adds or replaces the coordinates in the frame.
       *    @param frame frame that will receive the new coordinates
       *    @param Xs xy coordinates
       *
       **/
    public void addCoordinates(int frame, List<double[]> Xs){
        
        Coordinates.put(frame,Xs);
    }
    
    public Iterator<Integer> iterator(){
    
        return Coordinates.keySet().iterator();
    
    }

    public int getLastFrame(){
        return Coordinates.lastKey();
    }
}
