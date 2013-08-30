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
            case setcircleradius:
                snake_frame.setCircleRadius();
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
            // Adri 26/12/2012
            case deleteallsnakes:
                snake_model.deleteAllSnakes();
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
            //ADRI 09/01/2013   
            case tracksnakeallframes:
                snake_model.trackSnakeAllFrames();
                break;
            //ADRI 09/01/2013 
            case trackallsnakesallframes:
                snake_model.trackAllSnakesAllFrames();
                break;
            //ADRI 10/01/2013 
            //case trackallsnakesinoneframe:
            //    snake_model.trackAllSnakesInOneFrame();
            //    break;
            //ADRI 21/12/2012     
            // I add button continuesnake, which allows to draw in the next frame a snake linked with the selected snake of the previous frame
            case continuesnake:
                snake_model.continueSnakeInNextFrame();
                break;
           //ADRI 22/12/2012     
                // I add button redrawsnake, which allows to draw an already existing snake in the frame without loosing connection with linked snake in neighbour frames
           case redrawsnake:
               snake_model.redrawSnakeInThisFrame();
               break;
           //ADRI 22/12/2012     
               // I add button Add CircleSnake, which allows to draw a circular snake with a single click.
           case pointtocirclesnake:
               snake_model.addPointToCircleSnake();
               break;
           //ADRI 22/12/2012     
               // I add button Deform All Snakes, to deform all snakes in a frame.
           case deformallsnakes:
               snake_model.deformAllSnakes();
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
            case showversion:
                snake_model.showVersion();
                break;
            //ADRI 07/01/2013     
            // I add button countsnakeintensity
            //case countsnakeintensity:
            //     snake_model.countSnakeIntensity();
            //     break;
            //ADRI 08/01/2013     
            // I add button countallsnakesallframesintensity
            case countallsnakesallframesintensity:
                 snake_model.countAllSnakesAllFramesIntensity();
                break;
        }
    }
}
