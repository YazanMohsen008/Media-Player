package com.example.mediaplayer.MediaControl;


import java.io.InputStream;

public interface MediaController {
    public void play();
    public void pause();
    public void seek();
    public void reverse();
    public void speed();
    public void previous(InputStream in);
    public void next(InputStream in);
    public void stop();
    public void resume();
}

