package com.example.mediaplayer.ContainerManager.Decoder.Mjpeg;

import java.util.ArrayList;

public class BitReader {
    ArrayList<Long> Data;
    int nextByte=0;
    int nextBit=0;

    public BitReader(ArrayList<Long> Data) {
        this.Data = Data;
    }

    public long readBit()
    {
        if(nextByte>= Data.size())
            return -1;

        long bit =(Data.get(nextByte)>>(7-nextBit))&1;
        nextBit++;
        if(nextBit==8)
        {nextBit=0;
            nextByte+=1;}
        return bit;

    }

    public  long readBit( long length)
    {
        long bits=0;
        for(int i=0;i<length;i++)
        {
            long bit=readBit();
            if(bit==-1)
            {
                bits=-1;
                break;
            }
            bits=(bits<<1)|bit;
        }

        return bits;
    }


}
