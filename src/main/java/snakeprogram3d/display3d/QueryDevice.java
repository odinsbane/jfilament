package snakeprogram3d.display3d;

/**
 *
 * 
 */

import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.GraphicsConfigTemplate3D;
import org.scijava.java3d.VirtualUniverse;

import java.awt.*;
import java.util.Map;

/**
 *
 * Queries the system to see if texture3d properties are supported
 * texture3DWidthMax
 * texture3DDepthMax
 * texture3DHeightMax
 * texture3DAvailable
 * textureNonPowerOfTwoAvailable
 *
 * @author Matt Smith
 * 
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class QueryDevice {
    final boolean test;
    public boolean nonpowersoftwo;

    public int MAXW, MAXH, MAXD;

    public QueryDevice(){
        GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();

        /*
         * We need to set this to force choosing a pixel format that support the
         * canvas.
         */
        template.setStereo(GraphicsConfigTemplate3D.PREFERRED);
        template.setSceneAntialiasing(GraphicsConfigTemplate3D.PREFERRED);

        GraphicsConfiguration config = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getDefaultScreenDevice()
            .getBestConfiguration(template);

        Map<?, ?> c3dMap = new Canvas3D(config).queryProperties();

        if((Boolean)c3dMap.get("texture3DAvailable")){
            test=true;
            MAXW = (Integer)c3dMap.get("texture3DWidthMax");
            MAXH = (Integer)c3dMap.get("texture3DHeightMax");
            MAXD = (Integer)c3dMap.get("texture3DDepthMax");
            nonpowersoftwo = (Boolean)c3dMap.get("textureNonPowerOfTwoAvailable");
        } else{
            test=false;
        }


    }

    /**
     * Just a test for texture 3D in general. Does not determine the current texture3d.
     *
     * @return teture3d support
     */
    public boolean test(){

        return test;

    }

  /**
   * Prints out the systems 3D capabilities.
   *
   * @param args - whatever usually comes in from the cmd line. it isn't used.
   */
  public static void main(String[] args) {
    VirtualUniverse vu = new VirtualUniverse();
    Map vuMap = vu.getProperties();

    System.out.println("version = " + vuMap.get("j3d.version"));
    System.out.println("vendor = " + vuMap.get("j3d.vendor"));
    System.out.println("specification.version = "
        + vuMap.get("j3d.specification.version"));
    System.out.println("specification.vendor = "
        + vuMap.get("j3d.specification.vendor"));
    System.out.println("renderer = " + vuMap.get("j3d.renderer") + "\n");

    GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();

    /*
     * We need to set this to force choosing a pixel format that support the
     * canvas.
     */
    template.setStereo(template.PREFERRED);
    template.setSceneAntialiasing(template.PREFERRED);

    GraphicsConfiguration config = GraphicsEnvironment
        .getLocalGraphicsEnvironment().getDefaultScreenDevice()
        .getBestConfiguration(template);

    Map<?, ?> c3dMap = new Canvas3D(config).queryProperties();

   


    for(Object key: c3dMap.keySet()){
        System.out.println(key + " " + c3dMap.get(key));
        
    }

  }
}
