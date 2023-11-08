package crick.salbreuxlab.segmentation2d;

import snakeprogram.Snake;

public class LabelledSnake{
    final public int id;
    final public Snake snake;
    public LabelledSnake(int i, Snake s){
        id = i;
        snake = s;
    }
}