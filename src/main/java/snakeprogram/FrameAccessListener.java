package snakeprogram;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;


/**
   *    This class manages turning buttons and text fields on/off when a text field
   *    is being modified or has finished being modified.
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
   *
   **/
      
class FrameAccessListener implements MouseListener{

        private boolean IGNORING;
        private ArrayList<JTextField> text_fields = new ArrayList<JTextField>();
        private ArrayList<AbstractButton> buttons = new ArrayList<AbstractButton>();
        private ArrayList<JMenu> menus = new ArrayList<JMenu>();
        private SnakeSelector selector;
        
        FrameAccessListener(){
            
        
        }
        public void mouseClicked(MouseEvent e){
        
            if(!IGNORING){
                
                e.getComponent().setEnabled(true);
                disableButtons();
                e.getComponent().requestFocus();
                IGNORING = true;
            
            }
        
        }

       public void mousePressed(MouseEvent e){}

        public void mouseReleased(MouseEvent e){}

        public void mouseEntered(MouseEvent e){}

        public void mouseExited(MouseEvent e){}
        
        public void setButtons(AbstractButton[] b){

            for(AbstractButton button: b) buttons.add(button);

        }
        
        public void setFields(JTextField[] jtf){

            for(JTextField j: jtf){
                text_fields.add(j);
                j.setEnabled(false);
                j.addMouseListener(this);
            }
        }

        public void setMenus(JMenu[] jtm){

			for(JMenu j: jtm){
                menus.add(j);
			}
		}
        
        public void valueUpdated(){
            for(AbstractButton b: buttons){
                b.setEnabled(true);
            }
            for(JTextField f: text_fields){
                f.setEnabled(false);
            }
            for(JMenu m: menus) {
                for(int i = 0; i<m.getItemCount(); i++){
                    JMenuItem item = m.getItem(i);
                    if(item!=null) {
                        item.setEnabled(true);
                    }
                }
            }
            menus.get(0).getParent().validate();
				
            IGNORING = false;
        }
        
        public void enableUI(){
            valueUpdated();
            selector.setActive(true);
            
        }
        public void disableUI(){
            for(AbstractButton b: buttons){
                b.setEnabled(false);
            }
            for(JTextField f: text_fields){
                f.setEnabled(false);
            }
            for(JMenu m: menus) {
                for(int i = 0; i<m.getItemCount(); i++){
                    JMenuItem item = m.getItem(i);
                    if(item!=null){
                        item.setEnabled(false);
                    }
                }
            }
            IGNORING = true;
            selector.setActive(false);
        }
        
        public void disableButtons(){
            for(AbstractButton b: buttons){
                b.setEnabled(false);
            }
        }
        
               
        public void addSelector(SnakeModel sm, Component c){
                selector = new SnakeSelector(sm);
                c.addMouseListener(selector);
                c.addMouseMotionListener(selector);
                
        }
        
        public void addPassiveInterrupt(Component c){
            c.addKeyListener(selector);
        }
        
        public void enableOpenImage(){
			String s;
            for(JMenu m: menus){
                s = m.getText();
                if(s.compareTo("image")==0||s.compareTo("help")==0){
                    for(int i = 0; i<m.getItemCount(); i++){
                        JMenuItem item = m.getItem(i);
                        if(item!=null)
                            item.setEnabled(true);
                    }
                }
            }
		}


    public void registerButton(AbstractButton b) {
        buttons.add(b);
    }


}

class SnakeSelector extends MouseAdapter implements KeyListener{
    public boolean ACTIVE=false;
    final public SnakeModel parent;
    SnakeSelector(SnakeModel parent){
        this.parent = parent;
    }
    Point pressed;
    @Override
    public void mouseClicked(MouseEvent evt){
        if(ACTIVE)
            parent.selectSnake(evt);
        
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        if(!ACTIVE){
            return;
        }
        Point drag = e.getPoint();
        parent.dragZoomBox(pressed, drag);
        pressed = drag;
    }

    @Override
    public void mousePressed(MouseEvent e){
        pressed = e.getPoint();
    }




    public void setActive(boolean v){
        ACTIVE=v;
    }
    
    public void keyPressed(KeyEvent e){
        if(e.getKeyCode()==KeyEvent.VK_ESCAPE)
            parent.stopRunningNicely();
        if(ACTIVE){
            if(e.getKeyCode()==KeyEvent.VK_RIGHT)
                parent.nextImage();
            if(e.getKeyCode()==KeyEvent.VK_LEFT)
                parent.previousImage();
            if(e.getKeyCode()==KeyEvent.VK_I){
                parent.toggleShowIds();
            }
        }
    }
    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){}


    
}


