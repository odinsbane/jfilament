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
package crick.salbreuxlab.segmentation2d.util;

/**
 * A set of primitive ints that stays sorted.
 *
 */
public class SortedIntSet{
    int[] back = new int[8];
    int count = 0;

    public boolean add(int a){
        int s = -1;
        for(int i = 0; i<count; i++){
            if(a==back[i]){
                return false;
            } else if(a<back[i]){
                //Insert the first time a is less than the value contained.
                s = i;
                break;
            }

        }

        //grow array
        if(count==back.length){
            int[] oldBack = back;
            back = new int[back.length + 8];
            System.arraycopy(oldBack, 0, back, 0, oldBack.length);
        }

        //insert
        if(s!=-1){
            System.arraycopy(back, s, back, s+1, count - s);
            back[s] = a;
        } else{
            back[count] = a;
        }

        count++;
        return true;
    }

    public void add(SortedIntSet set) {
        for(int j = 0; j<set.count; j++){
            add(set.back[j]);
        }
    }


    public boolean contains(SortedIntSet set) {
        if (set.count > count) {
            return false;
        }

        int i = 0;
        boolean found = false;
        for (int j = 0; j < set.count; j++) {
            found = false;
            for (; i < count; i++) {
                if (back[i] == set.back[j]) {
                    found = true;
                    i++;
                    break;
                }
            }
        }
        return found;


    }

    public int getCount(){
        return count;
    }
    public int size(){
        return count;
    }

    public SortedIntSet intersection(SortedIntSet b) {
        SortedIntSet ret = new SortedIntSet();

        for(int i = 0; i<count; i++){

            if(b.contains(back[i])){
                ret.add(back[i]);
            }

        }


        return ret;

    }

    public boolean contains(int i){
        for(int j = 0; j<count; j++){
            if(i==back[j]){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof SortedIntSet){
            SortedIntSet sis = (SortedIntSet)obj;
            if(count==sis.count){
                for(int i = 0; i<count; i++){
                    if(back[i]!=sis.back[i]){
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode(){
        int hash = 0;

        for(int i: back){
            hash += i;
        }

        return hash;
    }
    @Override
    public String toString(){
        StringBuilder build = new StringBuilder("SortedSet: {");
        String sep = " ";
        for(int i = 0; i<count; i++){
            build.append(sep);
            build.append(back[i]);
            sep = ", ";
        }
        build.append(" }");
        return build.toString();
    }


    public int get(int i) {
        return back[i];
    }
}
