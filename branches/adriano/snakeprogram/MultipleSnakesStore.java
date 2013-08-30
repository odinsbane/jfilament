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

	// Adri 03 gen 2012
    public boolean delallflag;
    public int oldnumber;
    public Snake snak;


   LinkedList<Snake> linkSnake = new LinkedList<Snake>();
   
   public MultipleSnakesStore(){
        linkSnake = new LinkedList<Snake>();
   }

   public void addSnake(Snake snk){
       linkSnake.add(snk);
   }
   
   // Adri 02 Gen2012
   // In this method I would like to be able to perform operations only on selected snakes.
   //public void deleteMultipleSnakes(ArrayList<Snake[]> SelectedSnakes){
   //    for (int i = 0;i<=numbersnakes;i++){            
   //    linkSnake.remove(snk);
   //}
   
   public void deleteAllSnakes(){
	   int numbersnakes=getNumberOfSnakes();
	   delallflag=true;
	   int snakecycle = 0;
	   while (snakecycle < numbersnakes){
		   Snake snk = getSnake(snakecycle);
		   if (delallflag==true){
			   //setflagfalse();
			   delallflag=false;
			   deleteSnakeMod(snk);
			   snakecycle++;   
		   }
	   }
   }
   
   //Adri 4 gen 2013 (working!)
   public void deleteAllSnakes2(){
	   while (getNumberOfSnakes()!=0){
		   snak=getLastSnake();
		   linkSnake.remove(snak);
	   }
   }
   
 
   
   
   public void deleteSnake(Snake snk){
       linkSnake.remove(snk);
   }
   
   // Adri 03 Gen 2013
   public void deleteSnakeMod(Snake snk){
       oldnumber=getNumberOfSnakes();
       linkSnake.remove(snk);
       while (oldnumber==getNumberOfSnakes()){delallflag=false;}
       //setflagtrue();
       if (oldnumber==(getNumberOfSnakes()+1)){delallflag=true;}
}
   
// Adri 03 Gen 2013
   public void setflagtrue(){
	   delallflag=true;
   }
   
// Adri 03 Gen 2013
   public void setflagfalse(){
	   delallflag=false;
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
