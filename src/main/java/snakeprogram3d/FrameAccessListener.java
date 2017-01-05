package snakeprogram3d;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;


/**
 *    This class manages turning buttons and text fields on/off when a text field
 *    is being modified or has finished being modified.
 *  @author Matt Smith
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 **/
      
class FrameAccessListener implements MouseListener{

        private boolean IGNORING;
        private JTextField[] text_fields;
        private AbstractButton[] buttons;
        private HashSet<AbstractButton> directions;
        private JMenu[] menus;
        private SnakeSelector selector;
        
        FrameAccessListener(){
            
        
        }
        /**
         * Enables the components for editing
         *
         * @param e - the event generated we want the component.
         */
        public void mouseClicked(MouseEvent e){
        
            if(!IGNORING){

                e.getComponent().setEnabled(true);
                disableButtons();
                IGNORING = true;

            }
        
        }

       public void mousePressed(MouseEvent e){}

        public void mouseReleased(MouseEvent e){}

        public void mouseEntered(MouseEvent e){}

        public void mouseExited(MouseEvent e){}

        /**
         * Sets a list of buttons for this lister, which it disable/enables en mass.
         *
         * @param b the buttons
         */
        public void setButtons(AbstractButton[] b){
            buttons = b;
            directions = new HashSet<AbstractButton>();

            for(AbstractButton but: buttons){
                but.setFocusable(false);
                SnakeActions sa = SnakeActions.valueOf(but.getActionCommand());
                switch(sa){
                    case previousImage:
                    case nextImage:
                        directions.add(but);
                        break;

                }
            }


        }

        /**
         * Sets the text fields that are monitored by this component.
         * 
         * @param jtf monitored text fields
         */
        public void setFields(JTextField[] jtf){
            text_fields = jtf;
            for(JTextField j: jtf){
                j.setEnabled(false);
                j.addMouseListener(this);
                
            }
        }

        /**
         * Sets the menuts that are enabled/disabled.
         *
         * @param jtm the menus that are used.
         */
        public void setMenus(JMenu[] jtm){
			menus = jtm;
			for(JMenu j: jtm){
				j.setEnabled(false);
			}
        }
        
        /**
         * Enables the User interface.  Disables all of the text fields.
         */
        public void valueUpdated(){

            for(AbstractButton b: buttons){
                b.setEnabled(true);
            }
            for(JTextField f: text_fields){
                f.setEnabled(false);
            }
            for(JMenu m: menus)
                m.setEnabled(true);

            IGNORING = false;

        }

        /**
         * Sets the UI to a read position.  Starts listing for image panel clicks.
         */
        public void enableUI(){
            valueUpdated();
            selector.setActive(true);
            
        }

        /**
         * Disables UI.  Stops the selector, buttons textfields and ignores clicks to
         * text fields.
         */
        public void disableUI(){

            for(AbstractButton b: buttons){
                b.setEnabled(false);
            }
            for(JTextField f: text_fields){
                f.setEnabled(false);
            }
            for(JMenu m: menus)
                m.setEnabled(false);
            IGNORING = true;
            selector.setActive(false);
            

        }

        /**
         * Disables all of the buttons.
         */
        public void disableButtons(){
            for(AbstractButton b: buttons){
                b.setEnabled(false);
            }
            selector.DIRECTIONS=false;
        }

        /**
         * Ignoring means that text fields cannot be clicked.
         *
         * @param v set to true to disable setting text field values.
         */
        public void setIgnore(boolean v){
            IGNORING = v;
        }

        /**
         * Creates a new selector that is a mouse listener for the SnakeFrame image
         * panel.
         *
         * @param sm main application
         * @param c image panel that recieves clicks.
         */
        public void addSelector(SnakeModel sm, Component c){
                selector = new SnakeSelector(sm);
                c.addMouseListener(selector);
                
        }

        /**
         * Sets the selector to be a key listener, originally only used for
         * canceling a long running actions, now used for moving images too.
         *
         * @param c
         */
        public void addPassiveInterrupt(Component c){
            c.addKeyListener(selector);
        }

        /**
         * On startup the image menu and help menu should be enabled.
         */
        public void enableOpenImage(){
            String s;
			for(JMenu m: menus){
                s = m.getText();
                if(m.getText().compareTo("image")==0||s.compareTo("help")==0)
				m.setEnabled(true);
            }
        }

        /**
         * For certain actions the direction buttons are still available.
         */
        public void enableImageDirections(){
            for(AbstractButton b: directions)
                b.setEnabled(true);
            selector.DIRECTIONS=true;
        }

        /**
         *
         * @returns A Snake Selector which is a key listener.
         */
        public KeyListener getKeyListener(){
            return selector;
        }
        
}

/**
 * This monitors events generated by the image panel.  It is for selecting snakes,
 * moving frames, and planes in the zx directions.
 * 
 * @author mbs207
 */
class SnakeSelector extends MouseAdapter implements KeyListener{
    private boolean ACTIVE=false;
    public boolean DIRECTIONS=false;
    public SnakeModel parent;
    SnakeSelector(SnakeModel parent){
        this.parent = parent;
    }

    @Override
    public void mouseClicked(MouseEvent evt){
        if(ACTIVE)
            parent.selectSnake(evt);
        
    }

    /**
     * When active allows changing directions and selecting snakes.
     * 
     * @param v
     */
    public void setActive(boolean v){
        ACTIVE=v;
        parent.setSelecting(v);
        DIRECTIONS=true;

    }
    
    public void keyPressed(KeyEvent e){
        switch(e.getKeyCode()){
            case KeyEvent.VK_ESCAPE:
                parent.stopRunningNicely();
                break;
            case KeyEvent.VK_LEFT:
                if(DIRECTIONS) parent.moveDown();
                break;
            case KeyEvent.VK_RIGHT:
                if(DIRECTIONS) parent.moveUp();
                break;
            case KeyEvent.VK_UP:
                if(DIRECTIONS) parent.wipeDown();
                break;
            case KeyEvent.VK_DOWN:
                if(DIRECTIONS) parent.wipeUp();
                break;
            case KeyEvent.VK_V:
                parent.switchCards();
                break;
            case KeyEvent.VK_COMMA:
                if(DIRECTIONS) parent.previousImage();
                break;
            case KeyEvent.VK_PERIOD:
                if(DIRECTIONS) parent.nextImage();
                break;
            case KeyEvent.VK_N:
                if(ACTIVE) parent.selectNextSnake();
                break;
            case KeyEvent.VK_F:
                if(ACTIVE) parent.nextFrame();
                break;
            case KeyEvent.VK_B:
                if(ACTIVE) parent.previousFrame();
                break;
        }

    }
    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){}
    
}


