package com.example.mediaplayer.Data.Frame;

public class TrakFrame extends Frame{

    byte[] frame;

    public TrakFrame(byte[] frame) {
        this.frame = frame;
    }

    public byte[] getFrame() {
        return frame;
    }

}
