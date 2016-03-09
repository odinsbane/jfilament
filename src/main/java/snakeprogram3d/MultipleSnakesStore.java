package snakeprogram3d;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 *
 * This class should be replaced with a linkedlist since that is all
 * it contains.
 *
 * @author Lisa Vasko
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
   public void deleteSnake(int index){
       linkSnake.remove(index);
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

    public Snake getNextSnake(Snake current){
        int dex = 0;
        if(contains(current)){
            dex = linkSnake.indexOf(current);
            dex++;
            dex = dex<linkSnake.size()?dex:0;
            return getSnake(dex);

        }
        return getLastSnake();
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
    
    public boolean contains(Snake s){
    
        return linkSnake.contains(s);
        
        
    }
    


}
