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
   public int indexOf(Snake s){
       if(s!=null){
           return linkSnake.indexOf(s);
       } else{
           return -1;
       }
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
