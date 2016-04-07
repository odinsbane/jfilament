package snakeprogram3d;

import javax.swing.*;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Mostly static class for reading and writing snakes.  Also used for opening dialogs
 *
 * @author Matt Smith
 *
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class SnakeIO{
    static String BACKUP_NAME = "snake_working.tmp";
    /** Save file dialog */
    public static String getSaveFileName(Frame parent, String title){
        FileDialog fd = new FileDialog(parent,"Save Tab Separate File",FileDialog.SAVE);
        fd.setFile(title);
        fd.setVisible(true);
        String fname = fd.getFile();
        String dirname = fd.getDirectory();
        String fullname = dirname +  fname;
        if(fname!=null)
            return fullname;
        else
            return null;
    }

    /** Open file dialog */    
    public static String getOpenFileName(Frame parent){
        FileDialog fd = new FileDialog(parent,"Open File",FileDialog.LOAD);
        
        fd.setVisible(true);
        String fname = fd.getFile();
        String dirname = fd.getDirectory();
        String fullname = dirname +  fname;
        if(fname!=null)
            return fullname;
        else
            return null;
    }
    
    /** Open file dialog */    
    public static String getOpenFileName(Frame parent, String s){
        FileDialog fd = new FileDialog(parent,s,FileDialog.LOAD);
        
        fd.setVisible(true);
        String fname = fd.getFile();
        String dirname = fd.getDirectory();
        String fullname = dirname +  fname;
        if(fname!=null)
            return fullname;
        else
            return null;
    }
    /**
       *    Writes out the elongation data for each snake in px, as a function of time.
       *    included in the output file are the constants from the snake panel.
       *       
       *    @param parent This is a snake frame, it is nescessary to get the constants
       *    @param SnakeStore   Contains the snake data
       *    @param total_frames for writing elongation data of every frame even when there isn't a snake
       **/
    public static void writeSnakeElongationData(SnakeFrame parent, MultipleSnakesStore SnakeStore, int total_frames){
        String fname = getSaveFileName(parent.getFrame(),"elongation.txt");
        SnakeStore.purgeSnakes();
        if(fname!=null){

            try{
                BufferedWriter bw = new BufferedWriter( new OutputStreamWriter(new FileOutputStream(fname), StandardCharsets.UTF_8));
                createHeading(parent,bw);
                for(Snake s: SnakeStore)
                    writeOneSnakeElongationData(bw,s,total_frames);
                    
                bw.close();
            } catch(Exception e) {
                JOptionPane.showMessageDialog(parent.getFrame(),"Could not write" + e.getMessage());    
            }    
        }
        
    }
    /**
       *    Writes the snake_panel constants as tab separated key/value pairs
       *    
       **/
    private static void createHeading(SnakeFrame parent, BufferedWriter bw) throws Exception{
            HashMap<String,Double> values = parent.getConstants();
            
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
    private static void writeOneSnakeElongationData(BufferedWriter bw, Snake s, int total_frames) throws IOException{
        bw.write("\n\n#Snake Data\n#time\tlength\n");
        for(int i = 1; i<=total_frames; i++){
            if(s.exists(i)){
                bw.write("" + i + "\t" + s.findLength(i) + "\n");
            } else{
                bw.write("" + i + "\t\n");
            }
        
        }
    
    }
    
    /**
       *    Writes out the Snake Data for saving and restoring the actual traced snakes.
       *    Includes the constants and all of the snake points.
       *   
       *    @param parent This is a snake frame, it is nescessary to get the constants
       *    @param SnakeStore   Contains the snake data
       **/
    public static void writeSnakes(SnakeFrame parent, MultipleSnakesStore SnakeStore){
        String fname = getSaveFileName(parent.getFrame(),"snakes.txt");
        SnakeStore.purgeSnakes();
        
        if(fname!=null){
            BACKUP_NAME=fname + "~";
            try{
                writeSnakes(parent.getConstants(), SnakeStore, new File(fname));
            } catch(IOException e) {
                JOptionPane.showMessageDialog(parent.getFrame(),"Could not write" + e.getMessage());    
            }    
        }
      }
      
      /**
       *    Writes out the Snake Data for saving and restoring the actual traced snakes.
       *    Includes the constants and all of the snake points. Doesn't pop up a window
       *    but uses the parent for constants.
       *   
       *    @param parent This is a snake frame, it is nescessary to get the constants
       *    @param SnakeStore   Contains the snake data
       *    @param f    uses this file.
       **/
    public static void writeSnakes(SnakeFrame parent, MultipleSnakesStore SnakeStore, File f){
        SnakeStore.purgeSnakes();
        
        if(f!=null){
            try{
                writeSnakes(parent.getConstants(), SnakeStore, f);
            } catch(IOException e) {
                JOptionPane.showMessageDialog(parent.getFrame(),"Could not write" + e.getMessage());    
            }    
        }
      }

    /**
     *    Writes out the Snake Data for saving and restoring the actual traced snakes.
     *    Includes the constants and all of the snake points. Doesn't pop up a window
     *    but uses the parent for constants.
     *
     *    @param values input values for deforming snakes.
     *    @param SnakeStore   Contains the snake data
     *    @param f    uses this file.
     **/
    public static void writeSnakes(HashMap<String, Double> values, MultipleSnakesStore SnakeStore, File f) throws IOException {
        SnakeStore.purgeSnakes();
        BufferedWriter bw = new BufferedWriter( new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8) );
        writeConstants(bw,values);
        for(Snake s: SnakeStore) {
            writeASnake(bw, s);
        }
        bw.close();
    }


    /**
       *    This writes a snake, no headings are used because this is meant to be read by
       *    the program only.  The first line is a "#", followed by a line with an int which 
       *    represents the snake type.  Then each line following is a point in the snake
       *    frame \t index \t x \t y \t z
       *    the index is used as a check to make sure the snake has been saved and loaded 
       *    correctly
       *    
       **/
    private static void writeASnake(BufferedWriter bw,Snake s) throws IOException{
        bw.write("#\n");
        bw.write("" + s.TYPE + "\n");
        for(Integer i: s){
            List<double[]> x = s.getCoordinates(i);
            for(int j=0;j<x.size();j++){
                
                double[] pt = x.get(j);
                bw.write(i + "\t" + j + "\t" + pt[0] + "\t" + pt[1] + "\t" + pt[2] +"\n");
                
            }
        }
        
    }
    
    /**
       *    This writes the constants as key/value pairs one each line separated by
       *    tabs.
       *    
       **/
    private static void writeConstants(BufferedWriter bw, HashMap<String,Double> values) throws IOException{
        for(String k: values.keySet()) {
            bw.write("" + k + "\t" + values.get(k) + "\n");
        }
    }

    /**
     * For loading snakes from api/plugin context.
     *
     * @param frame for showing dialogs, could be null if nescessary
     * @param constants will be filled/updated with constants from the snake file.
     * @return the snakes, lots and lots of snakes.
     */
    public static MultipleSnakesStore loadSnakes(Frame frame, HashMap<String,Double> constants){
        String fname = getOpenFileName(frame);
        MultipleSnakesStore SS = null;
        if(fname!=null){

            try {
                SS = loadSnakes(fname, constants);
            } catch(Exception e){
                JOptionPane.showMessageDialog(frame,"Could not Load file" + e.getMessage());
                e.printStackTrace();
            }
        }

        return SS;
    }

    /**
       *    This loads the snakes from a snake store file.  SnakeFrame is nescessary to set the constants.
       *    This reads the file for constants first, then when it finds the first snake it begins to load snake
       *    data.
       *    
       **/
    public static MultipleSnakesStore loadSnakes(SnakeFrame parent){
        
        String fname = getOpenFileName(parent.getFrame());
        MultipleSnakesStore SS = null;
        if(fname!=null){
            BACKUP_NAME=fname + "~";
            try {
                HashMap<String,Double> constants = new HashMap<>();
                SS = loadSnakes(fname, constants);

                parent.setConstants(constants);
                

            } catch(IOException e){
                JOptionPane.showMessageDialog(parent.getFrame(),"Could not Load file" + e.getMessage());
                e.printStackTrace();
            }
        }
    
        return SS;
    }

    private static void loadConstants(BufferedReader br, Map<String, Double> constants) throws IOException {
        boolean CONSTANTS=true;
        String s;
        while(CONSTANTS){
            s = br.readLine();
            if(s==null||s.charAt(0)=='#') {
                CONSTANTS = false;
            } else{
                StringTokenizer ft = new StringTokenizer(s,"\t");
                String k = ft.nextToken();
                Double v = Double.parseDouble(ft.nextToken());
                constants.put(k,v);
            }
        }
    }

    public static MultipleSnakesStore loadSnakes(String fname) throws IOException {
        return loadSnakes(fname, new HashMap<>());
    }
    public static MultipleSnakesStore loadSnakes(String fname, HashMap<String, Double> constants) throws IOException {

        BACKUP_NAME=fname + "~";
        FileInputStream fis = new FileInputStream(fname);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));

        loadConstants(br, constants);

        MultipleSnakesStore SS = new MultipleSnakesStore();

        loadSnakes(br,SS);

        return SS;
    }



    /**
       *    Loads the actual snake data. #'s separate snakes returns a test to make sure that
       *    the points are inserted in the same order.
       *    @param br is the buffered reader doing the readin.
       *    @param SS is modified in place
       *
       **/
    private static int loadSnakes(BufferedReader br,MultipleSnakesStore SS) throws IOException{
        int tally = 0;
        String s;
        Snake ns = new Snake();
        
        while(!((s=br.readLine()) == null)){
        
            if(s.charAt(0)=='#'){
                SS.addSnake(ns);
                ns = new Snake();
                
            } else {
                StringTokenizer ft = new StringTokenizer(s,"\t");
                int f = Integer.parseInt(ft.nextToken());
                if(!ft.hasMoreTokens())
                    continue;
                int test = Integer.parseInt(ft.nextToken());
                double x = Double.parseDouble(ft.nextToken());
                double y = Double.parseDouble(ft.nextToken());
                double z = Double.parseDouble(ft.nextToken());
                if(ns.exists(f)){
                    ns.getCoordinates(f).add(new double[] {x,y,z});
                } else{
                    ArrayList<double[]> xs = new ArrayList<double[]>();
                    xs.add(new double[] {x,y,z} );
                    ns.addCoordinates(f, xs);
                }
                
                tally += Math.abs(test +1 - ns.getCoordinates(f).size());

            }
        }
        SS.addSnake(ns);
        return tally;
    }
    
    
  
}
