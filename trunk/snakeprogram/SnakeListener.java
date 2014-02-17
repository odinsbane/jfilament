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
            case deformfix:
                snake_model.moveMiddleFix();
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
                snake_model.trackSnakes();
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
            case setmaxlength:
                snake_model.setMaxLength();
                break;
            case setlinewidth:
                snake_model.setLineWidth();
                break;
            case trackallframes:
                snake_model.trackAllFrames(evt.getModifiers());
                break;
            case trackbackwards:
                snake_model.trackBackwards();
                break;
            case deformallframes:
                snake_model.deformAllFrames(evt.getModifiers());
                break;
            case guessforebackground:
                snake_model.guessForegroundBackground();
                break;
            case showversion:
                snake_model.showVersion();
                break;
            case repositionend:
                snake_model.repositionEnd();
                break;
            case showannotations:
                snake_model.showAnnotationFrame();
                break;

        }
    }
}
