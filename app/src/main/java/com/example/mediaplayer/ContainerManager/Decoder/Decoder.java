package com.example.mediaplayer.ContainerManager.Decoder;

import com.example.mediaplayer.ContainerManager.Parser.WavParser.WavFileException;
import com.example.mediaplayer.Data.Container.Container;

import java.io.IOException;
import java.io.InputStream;

abstract public class Decoder {
    Container container;
    protected InputStream in;

    public Decoder(Container container) {
        this.container = container;
    }
    public Decoder(InputStream inputStream){this.in = inputStream;}
    abstract public void decode() throws IOException, WavFileException;

    public Container getContainer(){
        return container;
    }

}
