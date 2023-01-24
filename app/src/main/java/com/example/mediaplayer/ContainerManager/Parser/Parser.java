package com.example.mediaplayer.ContainerManager.Parser;

import com.example.mediaplayer.ContainerManager.Parser.WavParser.WavFileException;
import com.example.mediaplayer.Data.Container.Container;

import java.io.File;
import java.io.IOException;

public abstract class Parser {


    protected Container mContainer;

    public Parser(Container container) {
        this.mContainer = container;
    }

    abstract public void parse() throws IOException, WavFileException;

    public Container getContainer(){
        return mContainer;
    }

}
