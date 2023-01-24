package com.example.mediaplayer.ContainerManager;

import android.util.Log;

import com.example.mediaplayer.ContainerManager.Decoder.Decoder;
import com.example.mediaplayer.ContainerManager.Decoder.Mjpeg.MjpegDecoder;
import com.example.mediaplayer.ContainerManager.Decoder.WavDecoder;
import com.example.mediaplayer.ContainerManager.Parser.MB4Parser;
import com.example.mediaplayer.ContainerManager.Parser.Parser;
import com.example.mediaplayer.ContainerManager.Parser.WavParser.WavFileException;
import com.example.mediaplayer.ContainerManager.Parser.WavParser.WavParser;
import com.example.mediaplayer.Data.Container.Container;
import com.example.mediaplayer.Data.Container.MB3Container;
import com.example.mediaplayer.Data.Container.WavContainer;
import com.example.mediaplayer.Data.Container.mp4.MB4Container;
import com.example.mediaplayer.MediaControl.PlaybackAudio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ContainerManager {
    Container mContainer;
    Parser parser;
    Decoder decoder;
    File file;

    public ContainerManager(Container mContainer) {
        this.mContainer = mContainer;
        Log.d("INFO", mContainer instanceof MB3Container ?"yes":"no");
    }

    public void Decode() throws IOException, WavFileException {
        Log.d("INFO" , "decode");

        if(mContainer instanceof WavContainer){
            decoder = new WavDecoder(mContainer);
            decoder.decode();
        }
        else if(mContainer instanceof MB3Container){
            PlaybackAudio play = ((MB3Container) mContainer).getPlayback();
            InputStream in = mContainer.getInputStream();

            play.startPlayback(in);
        }
        else if(mContainer instanceof MB4Container){
            decoder = new MjpegDecoder(mContainer, file);
            decoder.decode();
        }
    }
    public void Parse() throws IOException, WavFileException {
        if(mContainer instanceof WavContainer){
            parser = new WavParser(mContainer);
            parser.parse();
        }
        else if (mContainer instanceof MB4Container){
            parser = new MB4Parser(mContainer);
            parser.parse();
        }
    }
    public void StartManaging() throws IOException, WavFileException {
        Parse();
        Decode();
    }
}
