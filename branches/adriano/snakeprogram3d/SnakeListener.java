package snakeprogram3d;


import javax.swing.*;
import java.awt.event.*;

/**
 *
 * Handles the dispatch of jbutton events and parameter text field changes.
 * This also handles all of the mouse events on the snake panel to the model.
 *
 * @author Matt Smith
 * 
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
class SnakeListener implements ActionListener,MouseListener,MouseMotionListener{
    SnakeModel snake_model;
    SnakeFrame snake_frame;
    SnakeListener(SnakeModel m, SnakeFrame f){
        snake_model = m;
        snake_frame = f;
    }
    /**
     * Switches the Action event based on the SnakeActions enum, with a value
     * matched to the string of the ActionEvent action command.
     * 
     * @param evt
     */
    public void actionPerformed(ActionEvent evt){
        SnakeActions act = SnakeActions.valueOf(evt.getActionCommand());
        switch(act){
            case getandload:
                snake_model.getAndLoadImage();
                break;
            case addsnake:
                snake_model.addSnake();
                break;
            case deformsnake:
                snake_model.deformSnake();
                break;
            case setalpha:
                snake_frame.setAlpha();
                break;
            case setbeta:
                snake_frame.setBeta();
                break;
            case setgamma:
                snake_frame.setGamma();
                break;
            case setweight: 
                snake_frame.setWeight();
                break;
            case getforeground:
                snake_model.getForegroundIntensity();
                break;
            case setforeground:
                snake_frame.setForegroundIntensity();
                break;
            case setstretch:
                snake_frame.setStretch();
                break;
            case stretchfix:
                snake_model.setFixSnakePoints();
                break;
            case getbackground:
                snake_model.getBackgroundIntensity();
                break;
            case setbackground:
                snake_frame.setBackgroundIntensity();
                break;
            case clearscreen:
                snake_model.clearScreen();
                break;
            case setiterations:
                snake_frame.setDeformIterations();
                break;
            case savesnakes:
                snake_model.saveSnake();
                break;
            case loadsnakes:
                snake_model.loadSnake();
                break;
            case deletesnake:
                snake_model.deleteSnake();
                break;
            case initializezoom:
                snake_model.initializeZoomIn();
                break;
            case zoomout:
                snake_model.zoomOut();
                break;
            case nextimage:
                snake_model.nextImage();
                break;
            case previousimage:
                snake_model.previousImage();
                break;
            case deleteend:
                snake_model.deleteEndFix();
                break;
            case deletemiddle:
                snake_model.deleteMiddleFix();
                break;
            case tracksnake:
                snake_model.trackSnake();
                break;
            case savedata:
                snake_model.saveElongationData();
                break;
            case setresolution:
                snake_frame.setResolution();
                break;
            case setsigma:
                snake_frame.setImageSmoothing();
                break;
            case setzresolution:
                snake_frame.setZResolution();
                break;
            case nextframe:
                snake_model.nextFrame();
                break;
            case previousframe:
                snake_model.previousFrame();
                break;
            case pmax:
                snake_model.increaseMax();
                break;
            case mmax:
                snake_model.decreaseMax();
                break;
            case pmin:
                snake_model.increaseMin();
                break;
            case mmin:
                snake_model.decreaseMin();
                break;
            case viewselected:
                snake_frame.setView();
                break;
            case showhelp:
                HelpMessages.showHelp();
                break;
            case showabout:
                HelpMessages.showAbout();
                break;
            case reduce3d:
                try{
                    JCheckBoxMenuItem s = (JCheckBoxMenuItem)evt.getSource();
                    snake_model.reduce3D(s.getState());
                }catch(ClassCastException e){
                    System.out.println("not happy");
                }

                break;
            case tracksnakeback:
                snake_model.trackSnakeBackwards();
                break;
            case clearsnake:
                snake_model.clearCurrentSnake();
                break;
            case setmaxlength:
                snake_model.setMaxLength();
                break;
            case deformupdate:
                snake_frame.setDeformType();
                break;
        }
    }
        
    
    public void mouseExited(MouseEvent evt){
        
    }
    
    public void mouseMoved(MouseEvent evt){
        snake_model.snakePanelMouseMoved(evt);
    }
    
    public void mousePressed(MouseEvent evt){
        snake_model.snakePanelMousePressed(evt);
    }
    
    public void mouseReleased(MouseEvent evt){
        
    }
    
    public void mouseClicked(MouseEvent evt){
    }
    
    public void mouseEntered(MouseEvent evt){
    
    }
    
    public void mouseDragged(MouseEvent evt){
        
    }
}
