package snakeprogram3d.display3d;

import com.sun.j3d.utils.picking.PickResult;

import java.awt.event.MouseEvent;

/*
    *  Interface to allow receiving 'pickresults'
    * */
public interface CanvasView {

    void updatePick(PickResult[] result, MouseEvent evt, boolean clicked);

}