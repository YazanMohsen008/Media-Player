package com.example.mediaplayer.Data.Frame.WFrame;

import com.example.mediaplayer.Data.Frame.Frame;

public class WavFrame extends Frame {
    Sample[] Samples;

    public WavFrame(Sample [] samples){
        Samples = samples;
    }
    public Sample[] getSample() {
        return Samples;
    }

    public void setSample(Sample[] sample) {
        this.Samples = sample;
    }
}
