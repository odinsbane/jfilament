/*-
 * #%L
 * JFilament 2D active contours.
 * %%
 * Copyright (C) 2010 - 2023 University College London
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the UCL LMCB nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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


