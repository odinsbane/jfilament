package crick.salbreuxlab.segmentation2d;

import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.nio.file.Paths;
import java.util.*;

/**
 * Connect components for 2D. Connectivity is assumed to be 4 fold, eg connected pixes are neighbors
 * either directly above or below.
 *
 */
public class ConnectedComponents2D{
    ImageProcessor proc;
    int[] workspace;
    final int w,h;
    //filled after first pass, this has all of the mappings
    List<int[]> premap;

    //This is the actual map that has all of the mappings after they are reduced
    Map<Integer, Integer> final_map;

    //Contains a map of label to collection of pixels it corresponds to.
    Map<Integer,List<int[]>> log;

    //list of centroids, x,y,weight
    List<double[]> centroids;

    int last_added;

    /**
     * Prepares a workspace for performing a connect components routine on the provided image. The image should already
     * be thresholded, so any non-zero value will be treated as foreground, and 0 is background.
     *
     * This image will be modified.
     *
     * @param proc thresholded image that will be modified during processing.
     */
    public ConnectedComponents2D(ImageProcessor proc){
      this.proc = proc;
      w = proc.getWidth();
      h = proc.getHeight();
    }

    /**
     * This method needs to be called before any of the centroids can be accessed.
     */
    public void process(){
        int n = w*h;
        workspace = new int[n];

        if( proc instanceof ShortProcessor ){
            short[] pixels = (short[]) proc.getPixels();
            for(int i = 0; i<n; i++){
                workspace[i] = pixels[i] != 0 ? 1 : 0;
            }
        } else if( proc instanceof ByteProcessor){
            byte[] pixels = (byte[]) proc.getPixels();
            for(int i = 0; i<n; i++){
                workspace[i] = pixels[i] != 0 ? 1 : 0;
            }
        } else {
            for (int i = 0; i < n; i++) {
                workspace[i] = this.proc.get(i) != 0 ? 1 : 0;
            }
        }

        firstPass();
        secondPass();

    }

    /**
     * Returns the centroids associated with this connected components. If this cc2d has not been process processed,
     * then it will be.
     *
     * @return a list of center of regions.
     */
    public List<double[]> getCentroids(){
        if(centroids ==null){
            calculateCentroids();
        }
        return centroids;
    }

    /**
     * Takes a binary and performs a first pass connected regions filter on it.
     * Goes through pixel by pixel and checks its top and left neighbors for values
     * Then marks what value this pixel should be.
     */
    private void firstPass(){

        premap = new ArrayList<int[]>();
        last_added = 0;

        final_map = new HashMap<>();
        final_map.put(0, 0);

        for(int i = 0; i<h; i++){
            for(int j = 0; j<w; j++){
                int x = rowBy(j,i);
                set(j,i,x);
            }
        }
        reduceMap();
    }

    /**
     * Essential the Kernel for the firstPast.  Filters pixel by checking
     * for a value.  If yes it takes the number above or the number to the left.
     *
     * If there is both a number above and a number to the left then a map values
     * is added.
     *
     * If the pixel is zero, then there is now change
     *
     * @param j - x coordinate
     * @param i - y coordinate
     * @return the index for the last added.
     */
    private int rowBy(int j, int i){

        int above,left,now;
        above = get(j,i-1);
        left = get(j-1,i);
        now = get(j,i);
        if(now>0){
            if(above>0 && left>0){
                if(above != left){
                        int[] a = {above,left};
                        premap.add(a);
                    }
                return above;
            } else if(above>0 || left>0) {
                return above>0?above:left;
            } else{
                last_added += 1;
                int[] a = {last_added,last_added};
                premap.add(a);
                return last_added;
            }
        } else {
            return 0;
        }
    }

    /**
     * Goes through the pre-map and groups all of the linking numbers together.
     *
     */
    private void reduceMap(){
        while(premap.size()>0){
            //Set for looping
            int[] next = premap.get(0);
            premap.remove(0);
            IntSet next_set = new IntSet();
            int source = next[0];
            next_set.add(next[0]);
            next_set.add(next[1]);
            IntSet trying = new IntSet();
            for(int i = 0; i<next_set.size(); i++){
                trying.add(next_set.get(i));
            }
            while(trying.size()>0){
                int cur = trying.get(0);
                trying.removeFirst();
                ArrayList<int[]> replacement = new ArrayList<>();
                for(int i=0;i<premap.size(); i++ ){
                    int[] test = premap.get(i);
                    if(cur==test[0]||cur==test[1]){
                        int size = next_set.size();
                        next_set.add(test[0]);
                        if(next_set.size()>size){
                            size += 1;
                            trying.add(test[0]);
                        }
                        next_set.add(test[1]);
                        if(next_set.size()>size)
                            trying.add(test[1]);
                    }
                    else
                        replacement.add(test);
                }
                premap=replacement;
            }
            //place value into hashmaps values
            for(int i = 0; i<next_set.size(); i++){
                final_map.put(next_set.get(i), source);
            }
        }

    }

    /**
     * Uses the map created from the numbered image processor, creates the log, which contains all
     * of the coordinates for each point in the connected region.
     *
     */
    private void secondPass(){

        log = new HashMap<>();

        for(Integer value: final_map.values()){
            ArrayList<int[]> points = new ArrayList<>();
            log.put(value, points);
        }
        for(int i = 0; i<h; i++){
            for(int j = 0; j<w; j++){
                int cur = get(j,i);
                int rep = final_map.get(cur);
                int[] point = {j, i};
                if(rep!=0){
                    log.get(rep).add(point);
                    set(j, i, rep);
                }
            }
        }
    }

    private void calculateCentroids(){
        if(log==null){
            process();
        }
        centroids = new ArrayList<>();

        for(Integer key: log.keySet()){
            //each key represents a region
            if(!key.equals(0)){
                List<int[]> pts = log.get(key);
                double sumx = 0;
                double sumy = 0;
                double weight = pts.size();
                for(int[] pt: pts){
                    sumx += pt[0];
                    sumy += pt[1];
                }
                double[] next = {sumx/weight,sumy/weight,weight};
                centroids.add(next);
            }
        }
    }

    /**
     * Sets the workspace value.
     *
     * @param x coordinate
     * @param y coordinate
     * @param value value to set to.
     */
    void set(int x, int y, int value){
        workspace[x + y*w] = value;
    }

    public int get(int x, int y){
        if(x<0 || y<0) return 0;
        return workspace[x + y*w];
    }

    /**
     * A map of label -> region for all of the connected components. If this cc2d has not been processed it will be
     * processed. Otherwise it will return the previous results.
     *
     * @return Labe to Region.
     */
    public Map<Integer, List<int[]>> getRegions(){
        if(log == null){
            process();
        }
        return log;
    }

    /**
     * Removes a region from this cc2d by removing the label from the map, and setting all of the pixels associated
     * with the provided label to 0.
     * @param label
     */
    public void removeRegion(int label){
        List<int[]> background = log.get(0);
        if(background == null || background.size()==0){
            System.out.println("no background pixels");
            //not keeping background pixels.
            background = new ArrayList<>();
        }
        if(log.containsKey(label)){
            List<int[]> px = log.get(label);
            for(int[] pt: px){
                set(pt[0], pt[1], 0);
                background.add(pt);
            }
        }
    }

    public static void main(String[] args){
        new ImageJ();
        ImagePlus plus = new ImagePlus(Paths.get(args[0]).toAbsolutePath().toString());
        ImageStack stack = plus.getStack();
        int total = 0;
        long start = System.nanoTime();
        for(int i = 1; i<=stack.size(); i++){
            ImageProcessor proc = stack.getProcessor(i);
            proc.threshold(0);
            proc.invert();
            ConnectedComponents2D cc2d = new ConnectedComponents2D(proc);
            cc2d.process();
            Map<Integer, List<int[]>> regions = cc2d.getRegions();
            total += regions.size();
            System.out.println(".");
        }
        long elapsed = System.nanoTime() - start;
        System.out.println(total + " regions in " + (elapsed/1e9) +"s");
        //29239 regions.
        //using the provided image processor for a workspace, 65 seconds to run.
        // 55 seconds if there isn't a final labelling step !?.
        plus.show();

    }

    public int getWidth() {
        return w;
    }
    public int getHeight(){
        return h;
    }

    public List<int[]> getLabelledPoints(int labelB) {
        return log.get(labelB);
    }

    public int get(int i) {
        return workspace[i];
    }

    /**
     * Moves the unlabelled pixel to a labelled region. Add the int[] to the collection associated with the label.
     * updates the backing workspace.
     *
     * @param p {x, y}
     * @param label region to be associated with.
     */
    public void label(int[] p, int label) {
        log.get(label).add(p);
        set(p[0], p[1], label);
    }



}


/**
 * This is a basic set that just makes sure duplicates aren't added. Elements can be removed from ends only. Insertion
 * complexity is N.
 */
class IntSet{
    final static int MAX = Short.MAX_VALUE;
    int[] backing = new int[8];
    int size = 0;
    int offset = 0;
    public IntSet(){
    }

    public boolean add(int s){
        for(int i = 0; i<size; i++){
            if(backing[i + offset]==s){
                return false;
            }
        }
        checkSize();
        int i = size++ + offset;
        backing[i] = s;
        return true;
    }

    /**
     * Adding a pixel.
     */
    private void checkSize(){

                int n = backing.length*2;
                n = n>MAX ? MAX : n;
                int[] new_backing = new int[n];
                System.arraycopy(backing, offset, new_backing, 0, size);
                offset=0;
                backing = new_backing;

    }
    public void removeFirst(){
        offset++;
        size--;
    }

    public int get(int i ){
        return backing[i + offset];
    }
    public int size(){
        return size;
    }


    @Override
    public String toString(){
        return Arrays.toString(Arrays.copyOf(backing, size));
    }
}