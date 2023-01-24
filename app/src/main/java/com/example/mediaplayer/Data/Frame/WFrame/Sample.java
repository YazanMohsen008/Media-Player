package com.example.mediaplayer.Data.Frame.WFrame;

import java.nio.Buffer;

public class Sample {
    Buffer [] Bits;

    public Sample(Buffer [] bits){
        Bits = bits;
    }

    public void setBits(Buffer[] bits) {
        Bits = bits;
    }

    public Buffer[] getBits() {
        return Bits;
    }
}
