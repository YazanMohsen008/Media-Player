package com.example.mediaplayer.ContainerManager.Decoder;

import com.example.mediaplayer.ContainerManager.Parser.WavParser.WavFileException;
import com.example.mediaplayer.Data.Container.Container;
import com.example.mediaplayer.Data.Container.WavContainer;

import java.io.IOException;
import java.io.OutputStream;

public class WavDecoder extends Decoder {
    private final static int BUFFER_SIZE = 4096;

    private int bufferPointer;
    private int bytesRead;
    private long frameCounter;
    private WavContainer DecodingContainer;
    private byte[] buffer;
    private OutputStream oStream;
    private byte[] ByteBuffer;

    public WavDecoder(Container container) {
        super(container);
        DecodingContainer = (WavContainer)container;
    }

    @Override
    public void decode() throws IOException, WavFileException {
        long [] val = getSampleBuffer();
        int number = (int) (DecodingContainer.getNumberOfChannels()*DecodingContainer.getNumberOfFrames());
        ByteBuffer = writeBytes( val,number);
        DecodingContainer.setAudioStream(ByteBuffer);
    }

    private long readSample() throws IOException, WavFileException
    {
        long val = 0;

        for (int b=0 ; b<DecodingContainer.getBytePerSample() ; b++)
        {
            if (bufferPointer == bytesRead)
            {
                int read = DecodingContainer.getIStream().read(buffer, 0, BUFFER_SIZE);
                if (read == -1) throw new WavFileException("Not enough data available");
                bytesRead = read;
                bufferPointer = 0;
            }

            int v = buffer[bufferPointer];
            if (b < DecodingContainer.getBytePerSample()-1 || DecodingContainer.getBytePerSample() == 1)
                v &= 0xFF;
            val += v << (b * 8);

            bufferPointer ++;
        }

        return val;
    }
    public long[] getSampleBuffer() throws IOException, WavFileException{
        int offset =0;
        long sampleBufferSize = (DecodingContainer.getNumberOfFrames() * DecodingContainer.getNumberOfChannels());
        long[] sampleBuffer = new long[(int)sampleBufferSize];
        for (int i=0 ; i<DecodingContainer.getNumberOfFrames() ; i++)
        {
            if (frameCounter == DecodingContainer.getNumberOfFrames())
                break;

            for(int j =0 ; j<DecodingContainer.getNumberOfChannels() ; j++)
            {
                sampleBuffer[offset] = readSample();
                offset ++;
            }

            frameCounter ++;
        }
        return sampleBuffer;

    }
    public byte[] writeBytes(long val[],int numSamples) throws IOException
    {
        byte [] FinallBuffer = new byte[numSamples*2];
        int bytePointer =0;
        for(int i=0;i<numSamples;i++){

            for (int b=0 ; b<DecodingContainer.getBytePerSample() ; b++)
            {
                FinallBuffer[bytePointer] = (byte) (val[i] & 0xFF);
                val[i] >>= 8;
                bytePointer ++;
            }

        }
        return FinallBuffer;
    }
    private static void putLE(long val, byte[] buffer, int pos, int numBytes)
    {
        for (int b=0 ; b<numBytes ; b++)
        {
            buffer[pos] = (byte) (val & 0xFF);
            val >>= 8;
            pos ++;
        }
    }

    private void writeSample(long val) throws IOException
    {
        for (int b=0 ; b<DecodingContainer.getBytePerSample() ; b++)
        {
            if (bufferPointer == BUFFER_SIZE)
            {
                oStream.write(buffer, 0, BUFFER_SIZE);
                bufferPointer = 0;
            }

            buffer[bufferPointer] = (byte) (val & 0xFF);
            val >>= 8;
            bufferPointer ++;
        }
    }
}
