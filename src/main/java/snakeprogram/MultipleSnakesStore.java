package snakeprogram;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 *  This class stores multiple snakes, it has some convenience functions for checking if
 *  snakes exist or not.
 * @author Lisa, Matt
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class MultipleSnakesStore implements Iterable<Snake>{


   LinkedList<Snake> linkSnake = new LinkedList<Snake>();
   
   public MultipleSnakesStore(){
        linkSnake = new LinkedList<Snake>();
   }

   public void addSnake(Snake snk){
       linkSnake.add(snk);
   }


   public void deleteSnake(Snake snk){
       linkSnake.remove(snk);
   }
   
   public Snake getSnake(int index){
       return linkSnake.get(index);
    
   }

    public int getNumberOfSnakes(){
        return linkSnake.size();
    }

    public Snake getLastSnake(){
        try{
            return linkSnake.getLast();
        } catch(NoSuchElementException e){
            return null;
        }
    }
    
    public Iterator<Snake> iterator(){
    
        return linkSnake.iterator();
    
    }
    
    /**
       *    remove empty snakes
       **/
    public void purgeSnakes(){
        Iterator<Snake> si = linkSnake.iterator();
        while(si.hasNext()){
            if(si.next().isEmpty()){
                si.remove();
            }
        
        }
    
    }
    


}
