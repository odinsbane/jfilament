import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import snakeprogram.MultipleSnakesStore;
import snakeprogram.Snake;
import snakeprogram.SnakeIO;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * New imagej plugin that ...
 * User: mbs207
 * Date: 4/1/11
 * Time: 10:29 AM
 */
public class SVG_Draw_Snakes implements PlugInFilter {
    public void run(String arg) {
    }

    public int setup(String arg, ImagePlus imp) {
        return DOES_ALL;
    }

    public void run(ImageProcessor ip) {
        MultipleSnakesStore snakes = SnakeIO.loadSnakes(IJ.getInstance(),new HashMap<String,Double>());

        SvgAnimatedPainter painter = new SvgAnimatedPainter(ip.getHeight(), ip.getWidth());
        painter.setColor(Color.BLACK);

        Snake s = snakes.getLastSnake();

        boolean first_frame = true;

        for(Integer frame: s){
            ArrayList<double[]> points = s.getCoordinates(frame);
            Path2D line = new Path2D.Double();
            double[] pt;

            boolean first = true;
            
            for(int i=0; i<points.size();i++){
                if(first){

                    pt = points.get(i);
                    line.moveTo(pt[0],pt[1]);
                    first=false;
                } else{
                    pt = points.get(i);
                    line.lineTo(pt[0],pt[1]);
                }


            }

            if(first_frame){
                painter.startAnimatedPath(line);
                first_frame=false;
            }
            else
                painter.addAnimatedPath(line);
        }

        painter.finishAnimatedCurve();

        FileDialog fd = new FileDialog(IJ.getInstance(),"Save SVG File",FileDialog.SAVE);
        fd.setFile("data.svg");
        fd.setVisible(true);
        String fname = fd.getFile();
        if(fname==null)
            return;
        String dirname = fd.getDirectory();
        File output = new File(dirname,fname);
        painter.finish(output);
    }
}
class SvgAnimatedPainter{
    StringBuilder OUTPUT;
    Color COLOR;
    boolean CLIPPING = false;
    Rectangle clip;

    int COUNT;
    static final String DOCTYPE = "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \n" +
                "  \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n";
    static final String XML = "<?xml version=\"1.0\" standalone=\"no\"?>\n";
    static final String SVG_TAG = "<svg width=\"%.2fpx\" height=\"%.2fpx\" viewBox=\"0 0 %s %s\"\n"+
                "    xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">\n";
    public SvgAnimatedPainter(int height, int width){
        double page_width = width;
        double page_height = height;
        OUTPUT = new StringBuilder();
        OUTPUT.append(XML);
        OUTPUT.append(DOCTYPE);
        String dec = String.format(SVG_TAG,page_width,page_height,width,height);
        OUTPUT.append(dec);

    }

    public static String svgColorString(Color c){
        String red = Integer.toString(c.getRed(),16);
        if(red.length()==1)
            red = "0" + red;

        String green = Integer.toString(c.getGreen(),16);
        if(green.length()==1)
            green = "0" + green;

        String blue = Integer.toString(c.getBlue(),16);
        if(blue.length()==1)
            blue = "0" + blue;
        return MessageFormat.format("#{0}{1}{2}", red, green, blue);
    }

    public void startAnimatedPath(Shape s) {
        Rectangle r = s.getBounds();

        if(CLIPPING&&!r.intersects(clip))
            return;
        int COUNT=1;
        OUTPUT.append("<path d=\"\n");
        PathIterator pit = s.getPathIterator(null);
        double[] p = new double[2];
        while(!pit.isDone()){
            char c;
            int t = pit.currentSegment(p);
            switch(t){
                case PathIterator.SEG_MOVETO:
                    c = 'M';
                    OUTPUT.append(c + " " + p[0] + "," + p[1] + "\n");
                    break;
                case PathIterator.SEG_LINETO:
                    c = 'L';
                    OUTPUT.append(c + " " + p[0] + "," + p[1] + "\n");
                    break;
                case PathIterator.SEG_CLOSE:
                    c = 'Z';
                    OUTPUT.append(c + "\n");
                    break;
                case PathIterator.SEG_CUBICTO:
                    c = 'C';
                    OUTPUT.append(c + " " + p[0] + "," + p[1] + ' ' +
                                            p[2] + "," + p[3] + ' ' +
                                            p[4] + "," + p[5] + "\n");
                    break;
                default:
                    c = 'L';
                    OUTPUT.append(c + " " + p[0] + "," + p[1] + "\n");
                    break;
            }
            pit.next();

        }
        OUTPUT.append("\"");
        OUTPUT.append(" stroke=\"" + svgColorString(COLOR)  + '"');
        OUTPUT.append(" fill=\"none\"");
        OUTPUT.append(" stroke-width=\"1\" >\n");
        OUTPUT.append("<animate attributeName=\"d\" values=\"");
    }

    public void addAnimatedPath(Shape s){
        PathIterator pit = s.getPathIterator(null);
        double[] p = new double[2];
        while(!pit.isDone()){
            char c;
            int t = pit.currentSegment(p);
            switch(t){
                case PathIterator.SEG_MOVETO:
                    c = 'M';
                    OUTPUT.append(c + " " + p[0] + "," + p[1] + "\n");
                    break;
                case PathIterator.SEG_LINETO:
                    c = 'L';
                    OUTPUT.append(c + " " + p[0] + "," + p[1] + "\n");
                    break;
                case PathIterator.SEG_CLOSE:
                    c = 'Z';
                    OUTPUT.append(c + "\n");
                    break;
                case PathIterator.SEG_CUBICTO:
                    c = 'C';
                    OUTPUT.append(c + " " + p[0] + "," + p[1] + ' ' +
                                            p[2] + "," + p[3] + ' ' +
                                            p[4] + "," + p[5] + "\n");
                    break;
                default:
                    c = 'L';
                    OUTPUT.append(c + " " + p[0] + "," + p[1] + "\n");
                    break;
            }
            pit.next();

        }
        COUNT++;
        OUTPUT.append(";\n");

    }

    public void finishAnimatedCurve(){
        OUTPUT.append("\" begin=\"1\" dur=\""+ 4 + "\" calcMode=\"discrete\" fill=\"freeze\"" +
                " repeatCount=\"indefinite\"/>\n</path> ");
    }

    public void setColor(Color c) {
        COLOR = c;
    }


    public void finish(File f){
        System.out.println("finsihed");
        if(CLIPPING)
            OUTPUT.append("</g>\n");
        OUTPUT.append("</svg>");

        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(OUTPUT.toString());
            bw.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}