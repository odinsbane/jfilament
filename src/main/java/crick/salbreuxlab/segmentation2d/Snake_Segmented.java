import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import snakeprogram.MultipleSnakesStore;
import snakeprogram.Snake;
import snakeprogram.SnakeModel;


import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Snake_Segmented {
    ImagePlus plus;
    public Snake_Segmented(ImagePlus plus){
        this.plus = plus;
    }

    public void process(){
        ImageStack stack = plus.getStack();
        int count = stack.getSize();
        MultipleSnakesStore collection = new MultipleSnakesStore();
        for(int i = 0; i< count; i++){
            MultipleSnakesStore snakes = initializeSnakes(stack.getProcessor(i+1), i+1);
            snakes.forEach(collection::addSnake);
        }


        SnakeModel model = new SnakeModel();
        model.loadImage(plus);
        model.getFrame().setVisible(true);
        model.importSnakes(collection);

    }

    public MultipleSnakesStore initializeSnakes(ImageProcessor proc, int frame){
        MultipleSnakesStore snakes = new MultipleSnakesStore();
        int w = proc.getWidth();
        int h = proc.getHeight();
        Map<Integer, List<int[]>> regions = getRegions(proc);
        Set<Integer> toRemove = new HashSet<>();
        Integer maxKey = -1;
        int maxSize = -1;


        for(Integer key: regions.keySet()){
            List<int[]> pxls = regions.get(key);
            for(int[] px: pxls) {
                if (px[0] == 0 || px[0] == w - 1 || px[1] == 0 || px[1] == h - 1) {
                    //touches.
                    toRemove.add(key);
                }
                break;
            }

            if(pxls.size()>maxSize){
                maxKey = key;
                maxSize = pxls.size();
            }
        }
        toRemove.add(maxKey);
        toRemove.forEach(regions::remove);
        for(Integer key: regions.keySet()){
            List<double[]> points = getBoundary(regions.get(key));
            if(points.size()<2) continue;
            List<double[]> points2d = points.stream().map(pt2->new double[]{pt2[0], pt2[1]}).collect(Collectors.toList());

            //addSnakePoints(points, 3.0);

            Snake snake = new Snake( Snake.CLOSED_SNAKE);
            snake.addCoordinates(frame, points2d);

            snakes.addSnake(snake);
        }
        return snakes;
    }

    public List<double[]> getBoundary(List<int[]> px){
        List<double[]> curve = new ArrayList<>();
        int lastY = -1;
        int minX = plus.getWidth();
        int maxX = 0;
        int minY = plus.getHeight();
        int maxY = 0;
        for(int i = 0; i<px.size(); i++){
            int[] pt = px.get(i);
            if(pt[0]<minX){
                minX = pt[0];
            }
            if(pt[0]>maxX){
                maxX = pt[0];
            }
            if(pt[1]<minY){
                minY = pt[1];
            }
            if(pt[1]>maxY){
                maxY = pt[1];
            }
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;

        int[] filled = new int[height*width];

        for(int[] pt: px){
            int x = pt[0] - minX;
            int y = pt[1] - minY;

            if(x==0 || y == 0 || filled[y*width + x - 1]==0 || filled[(y-1)*width + x]==0 || filled[(y-1)*width +x - 1]==0){
                filled[x + y*width] = 255;
            } else{
                filled[x + y*width] = 1;
            }
        }
        final int fminX = minX;
        final int fminY = minY;
        for(int[] pt: px){
            int x = pt[0] - fminX;
            int y = pt[1] - fminY;
            int v = filled[x + y*width];
            if(v==255) continue;
            if(  x==width-1 || y == height-1 ||
                                                                       filled[(y+1)*width + x + 1]==0 ||
                                                                           filled[y*width + x + 1]==0 ||
            filled[(y-1)*width +x - 1]==0 || filled[(y-1)*width +x]==0|| filled[(y-1)*width +x + 1]==0

                            ){
                filled[x + y*width] = 255;
            } else{
                filled[x + y*width] = 1;
            }
        }
        List<double[]> all = px.stream().filter( pt->{
            int x = pt[0] - fminX;
            int y = pt[1] - fminY;
            return filled[y*width + x]==255;
        }).map(pt-> new double[]{pt[0], pt[1]}).collect(Collectors.toList());

        double[] latest = all.get(0);
        curve.add(latest);
        all.remove(0);
        while(all.size()>0){
            int next = getPseudoClosest(latest, all);
            latest = all.get(next);
            curve.add(latest);
            all.remove(next);
        }
        //remove ''wierd'' points
        double[] origin = curve.get(0);
        for(int i = curve.size()-1; i>0; i--){

            double[] next = curve.get(i);

            double dx = next[0] - origin[0];
            double dy = next[1] - origin[1];

            double d = dx*dx + dy*dy;

            if(d>5){
                curve.remove(i);
            } else{
                //should be able to break at this point.
                origin = next;
            }


        }


        return curve;
    }

    int getPseudoClosest(double[] pt, List<double[]> rest){
        int n = rest.size();
        double min = Double.MAX_VALUE;
        int best = -1;
        for(int i = 0; i<n; i++){
            double[] opt = rest.get(i);
            double dx = pt[0] - opt[0];
            double dy = pt[1] - opt[1];
            double m = dx*dx + dy*dy;
            if(m==1){
                return i;
            }
            if(m<min){
                min = m;
                best = i;
            }

        }
        return best;

    }

    public Map<Integer, List<int[]>> getRegions(ImageProcessor processor){
        Map<Integer, List<int[]>> result = new HashMap<>();
        int w = processor.getWidth();
        int h = processor.getHeight();
        final byte[] data = (byte[])processor.getPixels();
        for(int i = 0; i<data.length; i++){
            Integer b = data[i]&0xff;
            List<int[]> px = result.computeIfAbsent(b, ArrayList::new);
            px.add(new int[]{i%w, i/w});
        }
        return result;
    }
    //This method finds the distance between two points
    public static double pointDistance(double[] x1, double[] x2 ){
        double dx = x1[0] - x2[0];
        double dy = x1[1] - x2[1];

        return Math.sqrt(dx*dx + dy*dy);
    }

    //This method interpolates to find the value of the point in between those two points
    public static double interpolate(double x1, double x2, double t){
        return (x1 + t*(x2 - x1));
    }

    /** Interpolates arrays of values using the above interpolation function */
    public static double[] interpolate(double[] p1, double[] p2, double t){
        double[] ret_value = new double[p1.length];
        for(int i = 0; i<p1.length; i++)
            ret_value[i] = interpolate(p1[i],p2[i],t);
        return ret_value;
    }

    public void addSnakePoints(List<double[]> vertices, double msl){
        int pointListSize = vertices.size();

        double cumulativeDistance = 0;
        ArrayList<Double> cumulativeDistances = new ArrayList<Double>();
        double distanceValue;

        for(int i = 0; i < pointListSize; i++){
            cumulativeDistances.add(i, cumulativeDistance);
            distanceValue = pointDistance(vertices.get(i), vertices.get((i+1)%pointListSize));
            cumulativeDistance += distanceValue;
        }

        cumulativeDistances.add(cumulativeDistance);

        int newPointListSize = (int)(cumulativeDistance/msl);



        double segmentLength = cumulativeDistance/(double)newPointListSize;

        int i = 0;
        int i_last = pointListSize - 1;

        ArrayList<double[]> newVertices = new ArrayList<double[]>(newPointListSize);

        newVertices.add(i, vertices.get(i));

        for(int s = 1; s < newPointListSize; s++){
            while(cumulativeDistances.get(i) < s*segmentLength){
                i_last = i;
                i = (i+1)%(pointListSize+1);
            }
            double t = (s*segmentLength - cumulativeDistances.get(i_last))/(cumulativeDistances.get(i) - cumulativeDistances.get(i_last));
            newVertices.add(s, interpolate(vertices.get(i_last), vertices.get(i%pointListSize), t));
        }



        vertices.clear();
        vertices.addAll(newVertices);

    }

    /**
     * This plugin will load an image of
     * @param args
     */
    public static void main(String[] args){
        ImageJ.main(new String[] {});
        ImagePlus plus = new ImagePlus(args[0]);
        plus.show();

        new Snake_Segmented(plus).process();


    }

}
