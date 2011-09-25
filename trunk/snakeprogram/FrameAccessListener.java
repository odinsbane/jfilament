package snakeprogram;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;



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
        private JTextField[] text_fields;
        private AbstractButton[] buttons;
        private JMenu[] menus;
        private SnakeSelector selector;
        
        FrameAccessListener(){
            
        
        }
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
        
        public void setButtons(AbstractButton[] b){
            buttons = b;
        }
        
        public void setFields(JTextField[] jtf){
            text_fields = jtf;
            for(JTextField j: jtf){
                j.setEnabled(false);
                j.addMouseListener(this);
            }
        }
        public void setMenus(JMenu[] jtm){
			menus = jtm;
			for(JMenu j: jtm){
				j.setEnabled(false);
			}
		}
        
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
            for(JMenu m: menus)
				m.setEnabled(false);
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
                
        }
        
        public void addPassiveInterrupt(Component c){
            c.addKeyListener(selector);
        }
        
        public void enableOpenImage(){
			String s;
                        for(JMenu m: menus){
                            s = m.getText();
                            System.out.println(s);
                            if(s.compareTo("image")==0||s.compareTo("help")==0)
				m.setEnabled(true);
                        }
		}

        
}

class SnakeSelector extends MouseAdapter implements KeyListener{
    public boolean ACTIVE=false;
    final public SnakeModel parent;
    SnakeSelector(SnakeModel parent){
        this.parent = parent;
    }
    @Override
    public void mouseClicked(MouseEvent evt){
        if(ACTIVE)
            parent.selectSnake(evt);
        
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
        }
    }
    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){}
    
}

