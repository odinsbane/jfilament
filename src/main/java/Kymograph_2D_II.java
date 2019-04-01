import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import snakeprogram.MultipleSnakesStore;
import snakeprogram.SnakeIO;
import snakeprogram.Snake;
import snakeprogram.util.CurveGeometry;

import java.awt.Frame;
import java.util.HashMap;

public class Kymograph_2D_II implements PlugInFilter {
    /**
     * This is a re-write of the boundary kymograph plugin
     */
    int LINE_WIDTH = 5;
    ImagePlus original;
    @Override
    public int setup(String s, ImagePlus imagePlus) {
        original = imagePlus;
        return DOES_ALL;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {
        GenericDialog gd = new GenericDialog("Constants");
        gd.addNumericField("Width of Line(px): ", LINE_WIDTH, 2);
        gd.showDialog();

        if (gd.wasCanceled())
            return;
        LINE_WIDTH = (int)gd.getNextNumber();

        MultipleSnakesStore store = SnakeIO.loadSnakes(IJ.getInstance(), new HashMap<>());
        if (store==null){
            return;
        }
        int s = 1;
        for(Snake snake: store){
            ImagePlus kimo = CurveGeometry.createKimograph(original, snake, LINE_WIDTH);
            kimo.setTitle("Snake_" + s + "_kimograph_" + original.getShortTitle() + "");
            kimo.show();
            s++;
        }
    }
}
