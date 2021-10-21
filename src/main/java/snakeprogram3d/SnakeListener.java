package snakeprogram3d;


import snakeprogram.HelpMessages;

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
     * matched to the string of the ActionEvent actions command.
     * 
     * @param evt
     */
    public void actionPerformed(ActionEvent evt){
        SnakeActions act = SnakeActions.valueOf(evt.getActionCommand());
        switch(act){
            case getAndLoad:
                snake_model.getAndLoadImage();
                break;
            case addSnake:
                snake_model.addSnake();
                break;
            case deformSnake:
                snake_model.deformSnake();
                break;
            case setAlpha:
                snake_frame.setAlpha();
                break;
            case setBeta:
                snake_frame.setBeta();
                break;
            case setGamma:
                snake_frame.setGamma();
                break;
            case setWeight:
                snake_frame.setWeight();
                break;
            case getForeground:
                snake_model.getForegroundIntensity();
                break;
            case setForeground:
                snake_frame.setForegroundIntensity();
                break;
            case setStretch:
                snake_frame.setStretch();
                break;
            case stretchFix:
                snake_model.setFixSnakePoints();
                break;
            case getBackground:
                snake_model.getBackgroundIntensity();
                break;
            case setBackground:
                snake_frame.setBackgroundIntensity();
                break;
            case clearScreen:
                snake_model.clearScreen();
                break;
            case setIterations:
                snake_frame.setDeformIterations();
                break;
            case saveSnakes:
                snake_model.saveSnake();
                break;
            case loadSnakes:
                snake_model.loadSnake();
                break;
            case deleteSnake:
                snake_model.deleteSnake();
                break;
            case initializeZoom:
                snake_model.initializeZoomIn();
                break;
            case zoomOut:
                snake_model.zoomOut();
                break;
            case nextImage:
                snake_model.nextImage();
                break;
            case previousImage:
                snake_model.previousImage();
                break;
            case deleteEnd:
                snake_model.deleteEndFix();
                break;
            case deleteMiddle:
                snake_model.deleteMiddleFix();
                break;
            case trackSnake:
                snake_model.trackSnake();
                break;
            case saveData:
                snake_model.saveElongationData();
                break;
            case setResolution:
                snake_frame.setResolution();
                break;
            case setSigma:
                snake_frame.setImageSmoothing();
                break;
            case setZResolution:
                snake_frame.setZResolution();
                break;
            case nextFrame:
                snake_model.nextFrame();
                break;
            case previousFrame:
                snake_model.previousFrame();
                break;
            case pMax:
                snake_model.increaseMax();
                break;
            case mMax:
                snake_model.decreaseMax();
                break;
            case pMin:
                snake_model.increaseMin();
                break;
            case mMin:
                snake_model.decreaseMin();
                break;
            case viewSelected:
                snake_frame.setView();
                break;
            case showHelp:
                HelpMessages.showHelp();
                break;
            case showAbout:
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
            case trackSnakeback:
                snake_model.trackSnakeBackwards();
                break;
            case clearSnake:
                snake_model.clearCurrentSnake();
                break;
            case setMaxLength:
                snake_model.setMaxLength();
                break;
            case deformUpdate:
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
