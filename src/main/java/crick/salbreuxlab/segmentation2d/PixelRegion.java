package crick.salbreuxlab.segmentation2d;

import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PixelRegion{
    int size;
    int x;
    int y;
    int w;
    int h;
    List<int[]> points;

    byte[] map;

    public PixelRegion(List<int[]> points){
        size = points.size();
        int minx = Integer.MAX_VALUE;
        int maxx = 0;
        int miny = Integer.MAX_VALUE;
        int maxy = 0;
        for(int[] pt: points){
            minx = pt[0]<minx?pt[0]:minx;
            maxx = pt[0]>maxx?pt[0]:maxx;
            miny = pt[1]<miny?pt[1]:miny;
            maxy = pt[1]>maxy?pt[1]:maxy;
        }

        x = minx;
        y = miny;
        w = maxx - minx + 1;
        h = maxy - miny + 1;
        map = new byte[w*h];

        for(int[] pt: points){
            map[dex(pt)] = 1;
        }
        this.points = points;

    }

    int dex(int[] pt){
        return (pt[0] - x) + (pt[1]-y)*w;
    }

    byte get(int[] pt){
        int xl = pt[0] - x;
        int yl = pt[1] - y;
        if(xl>0 && yl>0 && xl<w && yl<h){
            return map[xl + yl*w];
        }

        return 0;
    }
    public int getSize(){
        return size;
    }
    static boolean overlapped(int x1, int w1, int x2, int w2){

        if(x2>x1+w1){
            return false;
        } else if (x1>x2+w2){
            return false;
        }

        return true;

    }
    public List<int[]> overlap(PixelRegion other){
        if(!overlapped(x, w, other.x, other.w) || !overlapped(y, h, other.y, other.h)){
            return new ArrayList<>(0);
        }
        PixelRegion smaller, larger;
        if(other.size<size){
            smaller = other;
            larger = this;
        } else{
            smaller = this;
            larger = other;
        }

        List<int[]> overlap = smaller.points.stream().filter(pt->larger.get(pt)>0).collect(Collectors.toList());
        return overlap;
    }

    public List<int[]> getPoints() {
        return points;
    }

    /**
     *
     * @param value
     * @param points
     * @param proc
     * @return
     */
    static public List<int[]> close(Integer value, List<int[]> points, ImageProcessor proc){
        List<int[]> npts = new ArrayList<>(points.size() * 2);
        int minx = 0;
        int maxx = 0;
        int miny = 0;
        int maxy = 0;
        boolean touches = false;
        int limitX = proc.getWidth()- 1;
        int limitY = proc.getHeight() - 1;
        for(int[] pt: points){
            if( (pt[0] == 0) || (pt[1] == 0) || (pt[0] == limitX) || (pt[1] == limitY) ){
                touches=true;
                break;
            }

            proc.set( pt[0], pt[1], -value);

        }
        int wp = (-value)&0xffff;

        if(touches){
            for(int[] pt: points){
                proc.set(pt[0], pt[1], 0);
            }
            return npts;
        }

        for(int[] pt: points){
            if(proc.get(pt[0]+1, pt[1])!=wp){
                int[] npt = {pt[0] + 1, pt[1]};
                npts.add(npt);
                proc.set(npt[0], npt[1], wp);
            }

            if(proc.get(pt[0]-1, pt[1])!=wp){
                int[] npt = {pt[0] - 1, pt[1]};
                npts.add(npt);
                proc.set(npt[0], npt[1], wp);
            }

            if(proc.get(pt[0], pt[1]+1)!=wp){
                int[] npt = {pt[0], pt[1]+1};
                npts.add(npt);
                proc.set(npt[0], npt[1], wp);
            }

            if(proc.get(pt[0], pt[1]-1)!=wp){
                int[] npt = {pt[0], pt[1]-1};
                npts.add(npt);
                proc.set(npt[0], npt[1], wp);
            }
        }
        List<int[]> temp = new ArrayList<>(npts.size());

        //remove any points that are still an edge.

        for(int[] pt: npts){
            if( (pt[0] == 0) || (pt[1] == 0) || (pt[0] == limitX) || (pt[1] == limitY) ){
                temp.add(pt);
                continue;
            }

            if(proc.get(pt[0]+1, pt[1])!=wp){
                temp.add(pt);
                continue;
            }

            if(proc.get(pt[0]-1, pt[1])!=wp){
                temp.add(pt);
                continue;
            }

            if(proc.get(pt[0], pt[1]+1)!=wp){
                temp.add(pt);
                continue;
            }

            if(proc.get(pt[0], pt[1]-1)!=wp){
                temp.add(pt);
                continue;
            }
        }
        for(int[] pt: temp){
            npts.remove(pt);
            proc.set(pt[0], pt[1], 0);
        }
        npts.addAll(points);

        for(int[] pt: npts){
            proc.set(pt[0], pt[1], value);
        }

        return npts;

    }
}
