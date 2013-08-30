package snakeprogram;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import ij.gui.GenericDialog;

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
    final TreeMap<Integer,ArrayList<double[]>> Coordinates;
    
    final public int TYPE;
    
        
    /**
       *    Creates a new snake does not have any position data
       *    @param  type    could be either an OPEN_SNAKE or a CLOSED_SNAKE for the respective curve types
       **/

    public Snake(int type){
        Coordinates = new TreeMap<Integer,ArrayList<double[]>>();
        TYPE=type;
    }
    
    /**
       *    Creates a new snake with position data.
       *    @param  coord  xcoordinates
       *    @param  i       frame that the current coordinates correspond too.
       *    @param  type    could be either an OPEN_SNAKE or a CLOSED_SNAKE for the respective curve types
       **/
    public Snake(ArrayList<double[]> coord,int i,int type){
        
        Coordinates = new TreeMap<Integer,ArrayList<double[]>>();
        Coordinates.put(i,coord);
        
        TYPE=type;
        
    }

    
    public ArrayList<double[]> getCoordinates(int i){
        return Coordinates.get(i);
    }

/** Adri 07 Gen 2013
 *  I create a method to retrieve x and y variables of a given snake in a given frame.
 *  While Coordinates method is and ArrayList<double[]> containing n points, each one with x and y,
 *  this method returns an ArrayList<double[]> containing only 2 vectors of length n, each with x and y values respectively.
 **/
    
    // NOTE: Values are received as double and returned as int
    public ArrayList<int[]> get_xyCoordinates(int frame){
    	ArrayList<double[]> coord_pts = new ArrayList<double[]>();
    	double[] temp_pt;
    	coord_pts = Coordinates.get(frame); //Please NOTE: the method get starts from 0!!!!!
    	/**GD_Debug
        GenericDialog gd3 = new GenericDialog("Coordinates number");
        gd3.addNumericField("coord_pts.size(): ", coord_pts.size(), 0);
        gd3.showDialog();
        GD_Debug*/
        
    	int[] xvect = new int[coord_pts.size()];
    	int[] yvect = new int[coord_pts.size()];
    	// In this way I have an ArrayList<double[]> called coordpts, and each entry is a point.
        // Now I loop over all the list lenght (all the coordinates) and fill the new list.
        for (int j=0; j<coord_pts.size();j++){
          temp_pt=coord_pts.get(j);
          xvect[j] =(int)Math.round(temp_pt[0]); //We convert double to int
          yvect[j] =(int)Math.round(temp_pt[1]);
        }
        ArrayList<int[]> coord_xy = new ArrayList<int[]>();
        coord_xy.add(xvect);
        coord_xy.add(yvect);
        return coord_xy;
    }

    
    /**
       *    Finds the length, cumulative distance between points in a given frame.
       **/

    public double findLength(int time){
    
        ArrayList<double[]> coordinates = Coordinates.get(time);
        
        int size = coordinates.size();
        double distance = 0;
        for(int i = 0; i < (size-1); i++){
            distance+=TwoDDeformation.pointDistance(    coordinates.get(i), coordinates.get(i+1)  );
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
       *    @param frame frame that will recieve the new coordinates
       *    @param Xs x coordinates
       *
       **/
    public void addCoordinates(int frame, ArrayList<double[]> Xs){
        
        Coordinates.put(frame,Xs);
    }
    
    public Iterator<Integer> iterator(){
    
        return Coordinates.keySet().iterator();
    
    }

    public int getLastFrame(){
        return Coordinates.lastKey();
    }
    
    /** Adri new method 08/01/2013
     * Looks for first frame of this particular snake.
     * 
     */
    public int getFirstFrame(){
        return Coordinates.firstKey();
    }
}
