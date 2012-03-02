

package snakeprogram3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

/**
 *
 *  Basic snake, essentially a tree map containing all of the points of the snakes.
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 *
 */
public class Snake implements Iterable<Integer>{

    /** The Integer is the frame in time and the ArrayList are the coordinates */
    TreeMap<Integer,ArrayList<double[]>> Coordinates;
    public int TYPE = 0;
    public Snake(){
        Coordinates = new TreeMap<Integer,ArrayList<double[]>>();
        
    }
    

    public Snake(ArrayList<double[]> coord, int i){
        Coordinates = new TreeMap<Integer,ArrayList<double[]>>();
        
        Coordinates.put(i,coord);
                

    }

    
    public ArrayList<double[]> getCoordinates(int i){
        return Coordinates.get(i);
    }

    public double findLength(int time){
    
        ArrayList<double[]> coordinates = Coordinates.get(time);
        
        int size = coordinates.size();
        double distance = 0;
        for(int i = 0; i < (size-1); i++){
            distance += ThreeDCurveDeformation.pointDistance( coordinates.get(i),coordinates.get(i+1));
        }
        return distance;
    }

    public int getSize(int frame){
        if(exists(frame))
            return Coordinates.get(frame).size();
        else
            return 0;
    }
    public boolean exists(int frame){
        return Coordinates.containsKey(frame);
    }
    public void clearSnake(int frame){
        Coordinates.remove(frame);
    }
    
    public boolean isEmpty(){
        
        return Coordinates.isEmpty();
    
    }
    
    public void addCoordinates(int frame, ArrayList<double[]> Xs){
        Coordinates.put(frame,Xs);
    }
    
    public Iterator<Integer> iterator(){
    
        return Coordinates.keySet().iterator();
    
    }
}
