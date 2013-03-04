package snakeprogram;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
   *    This class is used for reading and writing snakes.  It uses file dialogs and
   *    dialogs for errors.  There are two types of files.  Elongation data, Snake
 *      data, and image condition data.
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
   **/
public class SnakeIO{
    static File PWD = new File(".");
    /** Save file dialog */
    public static String getSaveFileName(Frame parent, String title){
        FileDialog fd = new FileDialog(parent,"Save Tab Separate File",FileDialog.SAVE);
        fd.setDirectory(PWD.getAbsolutePath());
        fd.setFile(title);
        fd.setVisible(true);
        String fname = fd.getFile();
        String dirname = fd.getDirectory();
        String fullname = dirname +  fname;
        if(fname!=null){

            PWD = new File(dirname);
            return fullname;

        }
        else{
            return null;
        }
    }

    /**
     * Single argument version for getting a snake file.
     *
     * @param parent
     * @return
     */
    public static String getOpenFileName(Frame parent){
        return getOpenFileName(parent, "Open Snake File");
    }

    /**
     * Added an argument for setting the title, to get files other than just snake files.
     *
      * @param parent
     * @param title
     * @return
     */
    public static String getOpenFileName(Frame parent, String title){
        FileDialog fd = new FileDialog(parent,title,FileDialog.LOAD);
        fd.setDirectory(PWD.getAbsolutePath());
        fd.setVisible(true);
        String fname = fd.getFile();
        String dirname = fd.getDirectory();
        String fullname = dirname +  fname;
        if(fname!=null){
            PWD = new File(dirname);
            return fullname;
        } else
            return null;
    }
    /**
       *    Writes out the elongation data for each snake in px, as a function of time.
       *   No GUI.
       *       
       *    @param fname this is the filename to save the data too.
       *    @param values   This contians the constant values as key values pairs
       *    @param SnakeStore   Contains the snake data
       *    @param total_frames for writing elongation data of every frame even when there isn't a snake
       **/
    public static void writeSnakeElongationData(String fname,HashMap<String,Double> values ,MultipleSnakesStore SnakeStore, int total_frames){
        SnakeStore.purgeSnakes();
        if(fname!=null){
            try{
                BufferedWriter bw = new BufferedWriter( new FileWriter(fname));
                createHeading(values,bw);
                for(Snake s: SnakeStore)
                    writeOneSnakeElongationData(bw,s,total_frames);
                    
                bw.close();
            } catch(Exception e) {
                e.printStackTrace();    
            }    
        }
        
    }
    
    /**
       *    Writes out the elongation data for each snake in px, as a function of time.
       *    included in the output file are the constants from the snake panel.  This version
       *    includes a Dialog for getting the filename
       *       
       *    @param parent This just needs to be a frame so the dialog can appear.  It could be a new frame or null.
       *    @param values   This contians the constant values as key values pairs
       *    @param SnakeStore   Contains the snake data
       *    @param total_frames for writing elongation data of every frame even when there isn't a snake
       **/
    public static void writeSnakeElongationData(Frame parent,HashMap<String,Double> values ,MultipleSnakesStore SnakeStore, int total_frames){
        String fname = getSaveFileName(parent,"elongation.txt");
        SnakeStore.purgeSnakes();
        if(fname!=null){
            try{
                BufferedWriter bw = new BufferedWriter( new FileWriter(fname));
                createHeading(values,bw);
                for(Snake s: SnakeStore)
                    writeOneSnakeElongationData(bw,s,total_frames);
                    
                bw.close();
            } catch(Exception e) {
                JOptionPane.showMessageDialog(parent,"Could not write" + e.getMessage());    
            }    
        }
        
    }
    /**
       *    Writes the constants as tab separated key/value pairs each on a new line
       *    
       **/
    private static void createHeading(HashMap<String,Double>values, BufferedWriter bw) throws Exception{
            
            for(String k: values.keySet()){
                
                bw.write("#\t" + k + "\t" + values.get(k) + "\n");
                
            }
    
    }
    
    /**
       *    Writes out the elongation data for one snake.  Writes a row for
       *    each frame even if the snake doesn't exist.  The data is tab
       *    separate with a heading
       *    
       **/
    private static void writeOneSnakeElongationData(BufferedWriter bw, Snake s, int total_frames) throws Exception{
        bw.write("\n\n#Snake Data\n#slice\tlength(px)\tdisplacement(tip)\tdisplacement(tail)\n");
        
        int last = -1;
        for(int i = 1; i<=total_frames; i++){
            if(s.exists(i)){
                
                //checks to see if there was in a previsou frame
                if(last>0){
                    
                    double[] end_pts = getEndpointChanges(s,i,last);
                    bw.write("" + i + "\t" + s.findLength(i) + "\t" +
                                end_pts[0] + "\t" + end_pts[1] + "\n");
                    
                } else {
                    bw.write("" + i + "\t" + s.findLength(i) + "\n");
                }
                last = i;
            } else{
                bw.write("" + i + "\t\n");
            }
        
        }
    
    }
    /**     Finds the 'overlapping' coordinates as determined by the
     *      shortest distance, without transforming the snake, by lining
     *      indicies.  Then it separates the lengths into three parts
     *      body differences and end differences.  The body difference
     *      is subsequently divided in have and added to both parts of
     *      the filament.
     **/
    public static double[] getEndpointChanges(Snake s, int cur, int prev){
        double[] ret_value = { 0, 0};
        ArrayList<double[]> current = s.getCoordinates(cur);
        ArrayList<double[]> previous = s.getCoordinates(prev);
        ArrayList<double[]> longer,shorter;
        
        double factor = 1;      //keep the change consistent
        
        //find longer filament
        if(current.size()>previous.size()){
            
            longer = current;
            shorter = previous;
            
        } else {
            
            longer = previous;
            shorter = current;
            factor = -1;
            
        }
        
        //find the index where points line up.
        int bestdex = 0;
        double min = 1e8;
        for(int i = 0; i<=(longer.size() - shorter.size()); i++){
            
            double cum = 0;
            for(int j = 0; j<shorter.size(); j++){
                double[] ptA = shorter.get(j);
                double[] ptB = longer.get(j+i); 
                for(int k = 0; k<ptA.length; k++)
                    cum += Math.pow((ptA[k] - ptB[k]),2);
                
            }
            if(cum<min){
                min = cum;
                bestdex = i;
            }
            
        }
        
        //Changes in distance for regions of filament
        double start_dist, body_dist, end_dist;
        int ep = bestdex + shorter.size() - 1;
        
        body_dist = measureDistance(longer,bestdex, ep) -
                        measureDistance(shorter,0,shorter.size()-1);
        start_dist = measureDistance(longer, 0, bestdex);
        end_dist = measureDistance(longer, ep, longer.size()-1);
        
        
        ret_value[0] = (start_dist + 0.5* body_dist)*factor;
        ret_value[1] = (end_dist + 0.5*body_dist)*factor;
        
        return ret_value;
    }
    
    public static double measureDistance(ArrayList<double[]> pts, int start, int stop){
        double d = 0;
        for(int i = start; i<stop; i++){
            double[] xi = pts.get(i);
            double[] xip1 = pts.get(i+1);
            double s = 0;
            for(int j = 0; j<xi.length; j++)
                s += Math.pow((xi[j] - xip1[j]),2);
            d += Math.sqrt(s);
        }
        
        return d;
    }
    
    /**
       *    Writes out the Snake Data for saving and restoring the actual traced snakes.
       *    Includes the constants and all of the snake points.  No GUI.
       *   
       *    @param fname This is the file name
       *    @param values This hashmap stores all of the constants for the current simulation.
       *    @param SnakeStore   Contains the snake data
       *
       **/
    public static void writeSnakes(String fname,HashMap<String,Double> values ,MultipleSnakesStore SnakeStore){
        SnakeStore.purgeSnakes();
        
        if(fname!=null){
            try{
                BufferedWriter bw = new BufferedWriter( new FileWriter(fname));
                writeConstants(bw,values);
                for(Snake s: SnakeStore)
                    writeASnake(bw,s);
                bw.close();
            } catch(Exception e) {
                System.out.println("Could not write" + e.getMessage());
                e.printStackTrace();
            }    
        }
      }
    
    /**
       *    Writes out the Snake Data for saving and restoring the actual traced snakes.
       *    Includes the constants and all of the snake points.
       *   
       *    @param parent This is a Frame for showing dialogs this could be null or a new Frame
       *    @param values This hashmap stores all of the constants for the current simulation.
       *    @param SnakeStore   Contains the snake data
       *
       **/
    public static void writeSnakes(Frame parent,HashMap<String,Double> values ,MultipleSnakesStore SnakeStore){
        String fname = getSaveFileName(parent,"snakes.txt");
        SnakeStore.purgeSnakes();
        
        if(fname!=null){
            try{
                BufferedWriter bw = new BufferedWriter( new FileWriter(fname));
                writeConstants(bw,values);
                for(Snake s: SnakeStore)
                    writeASnake(bw,s);
                bw.close();
            } catch(Exception e) {
                JOptionPane.showMessageDialog(parent,"Could not write" + e.getMessage());    
            }    
        }
      }
      
    /**
       *    This writes a snake, no headings are used because this is meant to be read by
       *    the program only.  The first line is a "#", followed by a line with an int which 
       *    represents the snake type.  Then each line following is a point in the snake
       *    <frame> \t <index> \t <x> \t <y> 
       *    the index is used as a check to make sure the snake has been saved and loaded 
       *    correctly
       *    
       **/
    private static void writeASnake(BufferedWriter bw,Snake s) throws Exception{
        bw.write("#\n");
        bw.write("" + s.TYPE + "\n");
        for(Integer i: s){
            ArrayList<double[]> x = s.getCoordinates(i);
            for(int j=0;j<x.size();j++)
                bw.write(i + "\t" + j + "\t" + x.get(j)[0] + "\t" + x.get(j)[1] + "\t" + 0 + "\n");
        }
        
    }
    
    /**
       *    This writes the constants as key/value pairs one each line separated by
       *    tabs.
       *    
       **/
    private static void writeConstants(BufferedWriter bw,HashMap<String,Double> values) throws Exception{
        for(String k: values.keySet())
            bw.write("" + k + "\t" + values.get(k)+"\n");
    
    }
    
    
    /**
       *    This loads the snakes from a snake store file.  
       *    This reads the file for constants first, then when it finds the first snake it begins to load snake
       *    data.  NO GUI
       *    @param fname The name of the file to be opened
       *    @param values A hashmap that the key,value pairs will be put into.  If there are already values they will be replaced.
       *    
       **/
    public static MultipleSnakesStore loadSnakes(String fname,HashMap<String,Double> values){
        
        MultipleSnakesStore SS = null;
        if(fname!=null){
        
            try {
            
                FileInputStream fis = new FileInputStream(fname);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                boolean CONSTANTS=true;
                String s=null;
                
                //load constants
                while(CONSTANTS){
                    s = br.readLine();
                    if(s==null||s.charAt(0)=='#')
                        CONSTANTS=false;
                    else{
                        StringTokenizer ft = new StringTokenizer(s,"\t");
                        String k = ft.nextToken();
                        Double v = Double.parseDouble(ft.nextToken());
                        values.put(k,v);
                    }
                }
                                
                SS = new MultipleSnakesStore();
                int tally=0;
                if(s!=null)
                    tally = loadSnakes(br,SS);
                
                if(tally>0)
                    System.out.println("Some of the data did not load correctly");
            
            } catch(Exception e){
                System.out.println("Could not Load file" + e.getMessage());
                e.printStackTrace();
            }
        }
    
        return SS;
    }
    
    
    /**
       *    This loads the snakes from a snake store file.  SnakeFrame is nescessary to set the constants.
       *    This reads the file for constants first, then when it finds the first snake it begins to load snake
       *    data.
       *    @param parent Just a frame for the dialogs.  It could a new frame or null if you don't have one handy
       *    @param values A hashmap that the key,value pairs will be put into.  If there are already values they will be replaced.
       *    
       **/
    public static MultipleSnakesStore loadSnakes(Frame parent,HashMap<String,Double> values){
        
        String fname = getOpenFileName(parent);
        MultipleSnakesStore SS = null;
        if(fname!=null){
        
            try {
            
                FileInputStream fis = new FileInputStream(fname);
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                boolean CONSTANTS=true;
                String s=null;
                
                //load constants
                while(CONSTANTS){
                    s = br.readLine();
                    if(s==null||s.charAt(0)=='#')
                        CONSTANTS=false;
                    else{
                        StringTokenizer ft = new StringTokenizer(s,"\t");
                        String k = ft.nextToken();
                        Double v = Double.parseDouble(ft.nextToken());
                        values.put(k,v);
                    }
                }
                                
                SS = new MultipleSnakesStore();
                int tally=0;
                if(s!=null)
                    tally = loadSnakes(br,SS);
                
                if(tally>0)
                    JOptionPane.showMessageDialog(parent,"Some of the data did not load correctly");
            
            } catch(Exception e){
                JOptionPane.showMessageDialog(parent,"Could not Load file" + e.getMessage());
            }
        }
    
        return SS;
    }

    /**
       *    Loads the actual snake data. #'s separate snakes returns a test to make sure that
       *    the points are inserted in the same order.
       *    @param br is the buffered reader doing the readin.
       *    @param SS is modified in place, and each new snake is added.
       *
       **/
    private static int loadSnakes(BufferedReader br,MultipleSnakesStore SS) throws Exception{
        int tally = 0;
        String s = br.readLine();
        int t = Integer.parseInt(s);
        Snake ns = new Snake(t);
        
        while((s=br.readLine()) != null){
        
            if(s.charAt(0)=='#'){
                SS.addSnake(ns);
                s = br.readLine();
                t = Integer.parseInt(s);
                ns = new Snake(t);
                
            } else {
                StringTokenizer ft = new StringTokenizer(s,"\t");
                int f = Integer.parseInt(ft.nextToken());
                int test = Integer.parseInt(ft.nextToken());
                double x = Double.parseDouble(ft.nextToken());
                double y = Double.parseDouble(ft.nextToken());
                if(ns.exists(f)){
                    ns.getCoordinates(f).add(new double[]{x,y});
                } else{
                    ArrayList<double[]> xs = new ArrayList<double[]>();
                    xs.add(new double[]{x,y});
                    ns.addCoordinates(f, xs);
                }
                
                tally += Math.abs(test +1 - ns.getCoordinates(f).size());
            
            }
        }
        SS.addSnake(ns);
        return tally;
    }
    
    
  
}
