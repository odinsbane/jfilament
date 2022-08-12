package snakeprogram.util;

import crick.salbreuxlab.segmentation2d.TwoDSegmentationSnake;
import crick.salbreuxlab.segmentation2d.Vertex;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The current version will take a skeletonized 2d image and label it to be used with a cerberus model.
 *
 * It will have a membrane label, vertex label and distance transform. There are some more options, but
 * those are the default.
 *
 * Created by smithm3 on 19/06/18.
 */
public class Label2D_Skeleton implements PlugInFilter {
    ImageStack labelledStack;
    int membraneLabel = 1;
    int vertexLabel = 3;
    int max = -1;

    ImagePlus original;

    public void setMembraneLabel(int v){
        membraneLabel = v;
    }

    public void setVertexLabel(int v){
        vertexLabel = v;
    }

    public void setDistanceTransform(int max){
        this.max = max;
    }


    public void removeEdgePixels(ImageProcessor proc){
        int w = proc.getWidth();
        int h = proc.getHeight();
        for(int i = 0; i<w; i++){
            int pxT = proc.get(i, 0);
            if(pxT != 0){
                if(proc.get(i, 1)!=0){
                    //leave it
                } else{
                    proc.set(i, 0, 0);
                }
            }

            int pxB = proc.get(i, h-1);
            if(pxB != 0){
                if(proc.get(i, h-2)!=0){
                    //leave it
                } else{
                    proc.set(i, h-1, 0);
                }
            }

        }

        for(int i = 0; i<h; i++){
            int pxL = proc.get(0, i);
            if(pxL != 0){
                if(proc.get(1, i)!=0){
                    //leave it
                } else{
                    proc.set(0, i, 0);
                }
            }

            int pxR = proc.get(w-1, i);
            if(pxR != 0){
                if(proc.get(w-2, i)!=0){
                    //leave it
                } else{
                    proc.set(w-1, i, 0);
                }
            }

        }


    }
    ImageProcessor process(ImageProcessor proc){
        removeEdgePixels(proc);

        int w = proc.getWidth();
        int h = proc.getHeight();

        TwoDSegmentationSnake tdss = new TwoDSegmentationSnake(proc);
        ImageProcessor bytes = new ShortProcessor(w, h);
        int shift = 0;
        if(membraneLabel>0) {
            shift += 1;
            for (int i = 0; i < w * h; i++) {
                if (proc.get(i) != 0) {
                    bytes.set(i, membraneLabel); //membrane
                }
            }
        }



        if(vertexLabel>0) {
            shift += 1;
            tdss.findVertices();
            System.out.println(tdss.vertices.size());
            for (Vertex v : tdss.vertices) {
                for (int i = -2; i <= 2; i++) {
                    for (int j = -2; j <= 2; j++) {
                        if (v.x + i < w && v.y + j < h && v.x + i >= 0 && v.y + j >= 0) {
                            if (bytes.get(v.x + i, v.y + j) == 1) {
                                bytes.set(v.x + i, v.y + j, vertexLabel);
                            }
                        }

                    }
                }
            }
        }

        if(max>0){
            int levels = max;
            tdss.distanceTransformActual(levels, max, bytes, shift);
        }

        return bytes;

    }

    public ImagePlus labelImagePlus(ImagePlus plus) throws ExecutionException, InterruptedException {
        labelledStack = new ImageStack(plus.getWidth(), plus.getHeight());
        ImageStack input = plus.getStack();
        List<Future<ImageProcessor>> futures = new ArrayList<>();
        ExecutorService service = Executors.newFixedThreadPool(2);
        for(int i = 1; i<=input.size(); i++){
            final int frame = i;
            final ImageProcessor proc = input.getProcessor(i);
            futures.add( service.submit(()->{
                System.out.println("frame: " + frame);
                return process(proc);
            }) );
        }
        for(int i = 1; i<=input.size(); i++){
            labelledStack.addSlice(input.getSliceLabel(i), futures.get(i-1).get());
        }

        service.shutdown();
        return new ImagePlus("labelled", labelledStack);
    }


    @Override
    public int setup(String s, ImagePlus imagePlus) {

        GenericDialog gd = new GenericDialog("set label parameters");
        gd.addNumericField("membrane label", membraneLabel, 0);
        gd.addNumericField("vertex label", vertexLabel, 0);
        gd.addNumericField("distance transform: max", max, 2);

        gd.showDialog();
        int ml = (int)gd.getNextNumber();
        int vl = (int)gd.getNextNumber();

        int cdt_max = (int)gd.getNextNumber();

        if(gd.wasCanceled()){
            return -1;
        }
        original = imagePlus;
        setMembraneLabel(ml);
        setVertexLabel(vl);

        setDistanceTransform(cdt_max);
        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        ImagePlus labelled = null;
        try {
            labelled = labelImagePlus(original);
            labelled.show();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
