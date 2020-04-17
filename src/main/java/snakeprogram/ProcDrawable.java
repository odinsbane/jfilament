package snakeprogram;

import ij.process.ImageProcessor;

/**
 * An interface to add and remove items from the window for drawing.
 *
 */

public interface ProcDrawable{
    /**
     * The image processor is already scaled, the transform is the nescessary transform to go from real coordinates to
     * displayed coordinates.
     *
     * @param proc
     * @param transform transforms points from image coordinates to processor cooridnates.
     */
    public void draw(ImageProcessor proc, Transform transform);
}
