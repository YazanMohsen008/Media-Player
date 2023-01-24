package com.example.mediaplayer.Data.Container;

import com.example.mediaplayer.Data.Frame.Frame;
import com.example.mediaplayer.MediaControl.PlaybackAudio;

import java.io.InputStream;

public class MB3Container extends Container {
    Frame[] AudioStream;
    private PlaybackAudio playback;
    public MB3Container(InputStream in){
        super(in);
    }
    public MB3Container(InputStream in , PlaybackAudio playback){
        super(in);
        this.playback = playback;
    }

    public PlaybackAudio getPlayback() {
        return playback;
    }

    public void setAudioStream(Frame[] audioStream) {
        AudioStream = audioStream;
    }

    public InputStream getAudioStream() {
        return mInputStream;
    }
}
