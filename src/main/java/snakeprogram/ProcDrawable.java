package snakeprogram;

import ij.process.ImageProcessor;

/**
 * An interface to add and remove items from the window for drawing.
 *
 */

public interface ProcDrawable{
    /**
     * The image processor is already scaled, the transform is the nescessary transform to go from real coordinates to
     * displayed coordantes.
     *
     * @param proc
     * @param transform
     */
    public void draw(ImageProcessor proc, Transform transform);
}
