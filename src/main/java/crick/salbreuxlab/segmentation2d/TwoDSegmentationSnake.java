package crick.salbreuxlab.segmentation2d;

import crick.salbreuxlab.segmentation2d.labelling.DistanceTransformer;
import crick.salbreuxlab.segmentation2d.util.SortedIntSet;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Rectangle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Receives a skeletized image of an epithelial cell. Separates the skeleton into regions. Locates Vertexes and
 * interfaces.
 *
 * Created by smithm3 on 05/06/18.
 */
public class TwoDSegmentationSnake {
    ImageProcessor proc;
    public List<Vertex> vertices = new ArrayList<>();
    List<TwoDLabeledRegion> regions;
    ConnectedComponents2D cc2d;
    TwoDLabeledRegion skeletonRegion;


    Map<Integer, List<Vertex>> regionToVertexes = new HashMap<>();
    Map<Vertex, SortedIntSet> vertexToRegion = new HashMap<>();

    List<TwoDLabeledRegion> vertexRegions;

    public TwoDSegmentationSnake(ImageProcessor proc){
        this.proc = proc;
    }

    /**
     * Labels the skeleton network where 0 represents the regions being labeled.
     *
     */
    public void labelRegions(){
        ImageProcessor threshed = new ByteProcessor(proc.getWidth(), proc.getHeight());
        int n = proc.getHeight()*proc.getWidth();
        int w = proc.getWidth();
        List<int[]> skeletonPts = new ArrayList<>();
        for(int i = 0; i<n; i++){
            if(proc.get(i)==0){
                threshed.set(i, 255);
            } else{
                skeletonPts.add(new int[]{i%w, i/w});
            }
        }
        System.out.println(skeletonPts.size() + "/" + n);
        skeletonRegion = new TwoDLabeledRegion(0, skeletonPts);
        cc2d = new ConnectedComponents2D(threshed);
        cc2d.process();
        regions = cc2d.getRegions().entrySet().stream().map(e->new TwoDLabeledRegion(e.getKey(), e.getValue())).collect(Collectors.toList());
    }




    List<Vertex> issues = new ArrayList<>();




    /**
     * CW Ring about the center, used for 'entering' a region and determine
     */
    final static int[][] STEPS = {
            {-1, -1},
            {0, -1},
            {1, -1},
            {1, 0},
            {1, 1},
            {0, 1},
            {-1, 1},
            {-1, 0}
    };
    /**
     * Starting from the top left and going clockwise.
     *
     * ( -1, -1 ) -> (0, -1) -> (1, -1) -> ( 1, 0 ) -> (1, 1) -> ( 0, 1 ) -> ( -1, 1) -> ( -1, 0) and repeat.
     *
     *
     *
     * @param from
     * @param to
     * @return
     */

    int[] enterVertex(int[] from, int[] to){
        int dx = from[0] - to[0];
        int dy = from[1] - to[1];
        if(dx*dx + dy*dy > 2) return new int[]{};
        int i;
        for( i = 0; i<8; i++){
            if (dx == STEPS[i][0] && dy == STEPS[i][1]){
                break;
            }
        }
        int[] left_to_right = new int[2];
        int found = 0;
        for(int j = 1; j<8; j++){
            int[] delta = STEPS[(i + j)%8];
            int xi = to[0] + delta[0];
            int yi = to[1] + delta[1];
            int l;
            if(xi<0 || yi<0 || xi >= cc2d.getWidth() || yi>=cc2d.getHeight()){
                l = -1;
            } else {
                l = cc2d.get(to[0] + delta[0], to[1] + delta[1]);
            }
            if(l!=0){

                if( found==0 || left_to_right[0]!=l){
                    if(found==left_to_right.length){
                        int[] old = left_to_right;
                        left_to_right = new int[old.length + 1];
                        System.arraycopy(old, 0, left_to_right, 0, old.length);
                    }
                    left_to_right[found++] = l;
                }
            }
        }
        /*
        */
        if(left_to_right[left_to_right.length-1] < 0 ){
            for(int j = -1; j<2; j++){
                for(int k =-1; k<2; k++){
                    if(j==dx && k==dy){
                        System.out.print("X");
                    } else {
                        //System.out.print(cc2d.get(to[0] + j, to[1] + k) + " ");
                    }
                }
                System.out.println();
            }
        }
        return found>1 ? left_to_right : new int[]{left_to_right[0]};
    }

    /**
     * Finds all the skeleton intersections assumed to be vertexes.
     */
    public void findVertices(){
        if(cc2d == null){
            labelRegions();
        }
        //find all of the non-border vertexes.
        ImageProcessor vertProc = new ShortProcessor(proc.getWidth(), proc.getHeight());
        for(int i = 1; i<proc.getWidth()-1; i++){
            for(int j = 1; j<proc.getHeight()-1; j++){

                int c = countBorderingPoints(proc, i,j);
                if(c>2){

                    vertices.add(new Vertex(i,j,c));
                    vertProc.set(i, j, vertices.size());

                }
            }
        }

        findEdgeVertexes(vertProc, vertices);
        //finds all of the touching vertexes.
        vertexRegions = labelRegions(vertProc);

        // vertices is a list of vertexes
        List<Vertex> finished = new ArrayList<>();
        for(TwoDLabeledRegion region: vertexRegions){
            List<Vertex> group = new ArrayList<>(region.px.size());
            //accumulates all of the Vertexes found in the clustered region.
            for(int[] p: region.px){
                group.add(vertices.get(vertProc.get(p[0], p[1]) - 1));
            }

            finished.addAll(reduceVertices(cc2d, group));
        }



        for(Vertex vert: finished){
            SortedIntSet neighbors = getNeighbors(vert, cc2d);
            vertexToRegion.put(vert, neighbors);
            for(int i = 0; i<neighbors.getCount(); i++){
                List<Vertex> vs = regionToVertexes.computeIfAbsent(neighbors.get(i), key-> new ArrayList<>());
                vs.add(vert);
            }
        }
        vertices = finished;
    }

    void findEdgeVertexes(ImageProcessor vertProc, List<Vertex> vertices){
        //TODO check the edges.
        int top = proc.getHeight() - 1;
        int right = proc.getWidth() -1;
        int start = vertices.size();
        for(int i = 0; i<proc.getWidth(); i++){
            if(proc.get(i, 0)!=0) {
                //possible bottom edge.
                int sum = 0;
                if (i > 0) {
                    sum += proc.get(i - 1, 1) == 0 ? 0 : 1;
                }
                sum += proc.get(i, 1) == 0 ? 0 : 1;
                if (i < right) {
                    sum += proc.get(i + 1, 1);
                }
                if(sum >= 1){
                    vertices.add(new Vertex(i, 0, sum));
                    vertProc.set(i, 0, vertices.size());
                }
            }

            if(proc.get(i, top)!=0){
                //possible top edge.
                int sum = 0;
                if (i > 0) {
                    sum += proc.get(i - 1, top-1) == 0 ? 0 : 1;
                }
                sum += proc.get(i, top - 1) == 0 ? 0 : 1;
                if (i < right) {
                    sum += proc.get(i + 1, top - 1);
                }
                if(sum >= 1){
                    vertices.add(new Vertex(i, top, sum));
                    vertProc.set(i, top, vertices.size());
                }
            }
        }

        //remove the ends since we've scanned them above.
        for(int j = 1; j<proc.getHeight()-1; j++){
            if(proc.get(0, j)!=0) {
                //possible right edge.
                int sum = 0;
                if (j > 0) {
                    sum += proc.get(1, j-1) == 0 ? 0 : 1;
                }
                sum += proc.get(1, j) == 0 ? 0 : 1;
                if (j < top) {
                    sum += proc.get(1, j+1);
                }
                if(sum >= 1){
                    vertices.add(new Vertex(0, j, sum));
                    vertProc.set(0, j, vertices.size());
                }
            }

            if(proc.get(right, j)!=0) {
                //possible right edge.
                int sum = 0;
                if (j > 0) {
                    sum += proc.get( right - 1, j-1) == 0 ? 0 : 1;
                }
                sum += proc.get( right - 1 , j) == 0 ? 0 : 1;
                if (j < top) {
                    sum += proc.get( right - 1, j+1);
                }
                if(sum >= 1){
                    vertices.add(new Vertex(right, j, sum));
                    vertProc.set(right, j, vertices.size());
                }
            }
        }


        int end = vertices.size();

    }

    /**
     * Reduces the number of redundant vertexes.
     *
     * @param labeledRegions Contains the labelled image and labelled regions.
     * @param cluster
     * @return
     */
    List<Vertex> reduceVertices(ConnectedComponents2D labeledRegions, List<Vertex> cluster){
        if(cluster.size()==1) return cluster;


        SortedIntSet labels = new SortedIntSet();

        Queue<SortedIntSet> sets = new PriorityQueue<>(cluster.size(), Comparator.comparingInt(SortedIntSet::size).reversed());
        Map<SortedIntSet, Vertex> comparison = new HashMap<>();

        for(Vertex v: cluster){
            SortedIntSet set = getNeighbors(v, labeledRegions);
            labels.add(set);
            sets.add(set);
            comparison.put(set, v);
        }


        List<Vertex> complete = new ArrayList<>();
        while(comparison.size()>0){
            SortedIntSet set = sets.poll();
            complete.add(comparison.get(set));
            comparison.remove(set);
            for(SortedIntSet s2: sets){
                if(set.contains(s2)){
                    comparison.remove(s2);
                }
            }
            sets.clear();
            sets.addAll(comparison.keySet());
        }


        return complete;
    }


    /**
     * Finds the set of labelled regions neighboring the vertex.
     *
     * @param v vertex of interest.
     * @param proc region labelled processor.
     * @return a set of labelled regions.
     */
    SortedIntSet getNeighbors(Vertex v, ConnectedComponents2D proc){
        return getNeighbors(v.x, v.y, proc);
    }

    /**
     * Finds all off the neighbors of the about the pixel at (x,y). If the point borders an edge, the -1 is
     * included.
     *
     * @param x
     * @param y
     * @param proc
     * @return
     */
    SortedIntSet getNeighbors(int x, int y, ConnectedComponents2D proc){
        SortedIntSet labels = new SortedIntSet();
        for(int i = -1; i<2; i++){

            if(x + i < 0 || x +i >= proc.getWidth()){
                labels.add(-1);
                //add a region that is "out of bounds";
                continue;
            }
            for(int j = -1; j<2; j++){

                if(y + j < 0 || y +j >= proc.getHeight()){
                    labels.add(-1);
                    continue;
                }
                int s = proc.get(x +i, y + j);
                if(s!=0){
                    labels.add(s);
                }
            }
        }

        return labels;
    }
    /**
     * Finds the number of bordering pixels that are not zero about the point of interest (POI) x,y. If the POI value is
     * zero, then it returns 0, otherwise returns the number of non-zero neighboring pixels.
     *
     * @param proc skeletonized processor
     * @param x POI x-coordinate.
     * @param y POI y-coordinate.
     *
     * @return 0 if the POI is not skeleton, or 1-8 for the number of bordering pixels in a 3x3 region that are skeleton.
     */
    public int countBorderingPoints(ImageProcessor proc, int x, int y){
        int sum=0;
        if(proc.get(x,y)==0){
            return 0;
        }
        for(int i = -1; i<2; i++){
            for(int j = -1; j<2; j++){
                sum += proc.get(x + i, y + j)==0?0:1;
            }
        }
        return sum-1;
    }



    /**
     * For labeling an image where not equal to zero is the threshold criteria.
     *
     * @param proc
     * @return
     */
    static List<TwoDLabeledRegion> labelRegions(ImageProcessor proc){
        ImageProcessor threshed = new ByteProcessor(proc.getWidth(), proc.getHeight());
        int n = proc.getHeight()*proc.getWidth();
        for(int i = 0; i<n; i++){
            if(proc.get(i)!=0){
                threshed.set(i, 255);
            }
        }
        ConnectedComponents2D cc2d = new ConnectedComponents2D(proc);
        Map<Integer, List<int[]>> points = cc2d.getRegions();
        return points.entrySet().stream().filter(e->e.getValue().size()>0).map(e-> new TwoDLabeledRegion(e.getKey(), e.getValue())).collect(Collectors.toList());

    }

    /**
     * Writes a distance transform to the provided imageprocessor, assuming this TwoDSegmentationSnake.findVertices has
     * been called
     *
     * @param levels number of graduations to be labelled with.
     * @param max any distance exceeding max will have the same label.
     * @param proc where the image will be drawn.
     * @param shift number of labels already occupied.
     *
     */
    public void distanceTransformCategorical(int levels, int max, ImageProcessor proc, int shift){
        DistanceTransformer dt = new DistanceTransformer(levels, max, shift);
        for(TwoDLabeledRegion region: regions){
            dt.categoricalLabelRegion(region, proc);
        }

    }

    /**
     * This tranform allows bitwise regions to overlap.
     *
     * @param levels
     * @param max
     * @param proc
     * @param shift
     */
    public void distanceTransformActual(int levels, int max, ImageProcessor proc, int shift){
        DistanceTransformer dt = new DistanceTransformer(levels, max, shift);
        for(TwoDLabeledRegion region: regions){
            dt.distanceLabelRegion(region, proc);
        }

    }

    public ImageProcessor getLabelledProcessor() {
        int w = cc2d.getWidth();
        int h = cc2d.getHeight();
        ImageProcessor p = new ShortProcessor(cc2d.getWidth(), cc2d.getHeight());
        for(int i = 0; i<w*h; i++){
            p.set(i, cc2d.get(i));
        }
        return p;
    }

    public Map<Integer,List<int[]>> getLabelledRegions() {
        return cc2d.getRegions();
    }

}


