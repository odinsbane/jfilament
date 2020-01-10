package snakeprogram3d.display3d;

import java.awt.event.*;

/**
 * Mouse listener class for handling the basic interactions with the DataCanvas interactions.
 *
 * */
public class CanvasController extends MouseAdapter {
    DataCanvas dc;
    int start_dragx, start_dragy;
    int click_type;
    boolean disabled=false;

    CanvasController(DataCanvas c){
        dc = c;
        dc.addMouseMotionListener(this);
        dc.addMouseListener(this);
        dc.addMouseWheelListener(this);
        this.disabled= false;

        dc.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped(KeyEvent e) {

                if(e.getKeyChar()=='1'){
                    dc.setView(StationaryViews.XY);
                } else if(e.getKeyChar()=='2'){
                    dc.setView(StationaryViews.XZ);
                } else if(e.getKeyChar()=='3'){
                    dc.setView(StationaryViews.YZ);
                } else if(e.getKeyChar()=='4'){
                    dc.setView(StationaryViews.THREEQUARTER);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
    }
    public void mouseMoved(MouseEvent e){
        dc.moved(e);
    }

    public void mouseReleased(MouseEvent evt){
        dc.released(evt);
    }
    public void mousePressed(MouseEvent e){
        click_type = e.getButton();
        start_dragx = e.getX();
        start_dragy = e.getY();

        dc.pressed(e);

    }

    public void mouseClicked(MouseEvent e){
        dc.clicked(e);
    }

    /**
     * For dragging, when disabled the state of the controller is updated, but the state of the canvas is not modified.
     *
     * @param e
     */
    public void mouseDragged(MouseEvent e){
        int dx = e.getX() - start_dragx;
        start_dragx = e.getX();
        int dy = e.getY() - start_dragy;
        start_dragy = e.getY();

        if(disabled){
            dc.dragged(e);
            return;
        }
        if(click_type==MouseEvent.BUTTON1)
            dc.rotateView(dx,dy);
        else
            dc.translateView(dx,dy);
    }
    public void mouseWheelMoved(MouseWheelEvent e){
        if(disabled){
            //TODO delegate to Canvas for propagation.
            return;
        }
        if(e.getWheelRotation()<0){
            dc.zoomIn();
        } else{
            dc.zoomOut();
        }
    }

    /**
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled){
        this.disabled = !enabled;
    }


}
