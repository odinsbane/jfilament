package snakeprogram.interactions;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Interface for handling interactions with the image panel.
 *
 * User: mbs207
 * Date: 12/11/11
 * Time: 3:26 PM
 */
public interface SnakeInteraction extends MouseListener, MouseMotionListener {
    public void cancelActions();
}


