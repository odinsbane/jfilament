package snakeprogram;


import java.awt.event.*;

/**
 * starts actions that have been initiated via the UI are managed here.
 *
 *
 *       Copyright (c) 2010, Lehigh University
 *       All rights reserved.
 *       see COPYING for license.
 *
 */
public class SnakeListener implements ActionListener{
    final SnakeModel snake_model;
    final SnakeFrame snake_frame;
    SnakeListener(SnakeModel m, SnakeFrame f){
        snake_model = m;
        snake_frame = f;
    }
    
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
            case moveAndRotate:
                snake_model.startSnakeTransform();
                break;
            case getBackground:
                snake_model.getBackgroundIntensity();
                break;
            case setBackground:
                snake_frame.setBackgroundIntensity();
                break;
            case deformFix:
                snake_model.moveMiddleFix();
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
                snake_model.trackSnakes();
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
            case setMaxLength:
                snake_model.setMaxLength();
                break;
            case setLineWidth:
                snake_model.setLineWidth();
                break;
            case trackAllFrames:
                snake_model.trackAllFrames(evt.getModifiers());
                break;
            case trackBackwards:
                snake_model.trackBackwards();
                break;
            case deformAllFrames:
                snake_model.deformAllFrames(evt.getModifiers());
                break;
            case guessForeBackground:
                snake_model.guessForegroundBackground();
                break;
            case showVersion:
                snake_model.showVersion();
                break;
            case repositionEnd:
                snake_model.repositionEnd();
                break;
            case showAnnotations:
                snake_model.showAnnotationFrame();
                break;

        }
    }
}
