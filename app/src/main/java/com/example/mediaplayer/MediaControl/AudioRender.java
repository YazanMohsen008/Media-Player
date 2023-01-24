package com.example.mediaplayer.MediaControl;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import com.example.mediaplayer.Data.Container.Container;
import com.example.mediaplayer.Data.Container.WavContainer;
import java.io.InputStream;


public class AudioRender {
    byte[] byteData = null;
    int bufSize;
    AudioTrack myAT = null;
    Thread playThread = null;
    WavContainer RenderingContainer;


    public AudioRender(Container container) {
        RenderingContainer = (WavContainer)container;
    }

    public void renderAudio(){

    InputStream inputStream = null;
    RenderingContainer.getAudioStream();
    byteData = RenderingContainer.getAudioStream();


    bufSize = android.media.AudioTrack.getMinBufferSize(44100,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT);

    myAT = new AudioTrack(AudioManager.STREAM_MUSIC,
            44100, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT, bufSize,
            AudioTrack.MODE_STREAM);
    myAT.setVolume(.2f);

        if (myAT != null)
        {
            myAT.play();
            myAT.setLoopPoints(0, byteData.length, 6);
            myAT.write(byteData, 0, byteData.length);
        }

}
    public void setVolum(){ }
}
