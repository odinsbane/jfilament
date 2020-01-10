package snakeprogram3d.display3d;


import org.scijava.java3d.utils.picking.PickResult;

import java.awt.event.MouseEvent;

/*
    *  Interface to allow receiving 'pickresults'
    * */
public interface CanvasView {

    void updatePressed(PickResult[] results, MouseEvent evt);
    void updateReleased(PickResult[] results, MouseEvent evt);
    void updateClicked(PickResult[] results, MouseEvent evt);
    void updateMoved(PickResult[] results, MouseEvent evt);
    void updateDragged(PickResult[] results, MouseEvent evt);

}