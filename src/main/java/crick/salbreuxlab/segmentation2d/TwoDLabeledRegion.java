package crick.salbreuxlab.segmentation2d;

import java.util.List;

public class TwoDLabeledRegion{
    int label;
    List<int[]> px;
    public TwoDLabeledRegion(int label, List<int[]> px){
        this.label = label;
        this.px = px;
    }

    public List<int[]> getPx() {
        return px;
    }
}
