package com.example.mediaplayer.ContainerManager.Parser.WavParser;

import com.example.mediaplayer.ContainerManager.Parser.Parser;
import com.example.mediaplayer.Data.Container.Container;
import com.example.mediaplayer.Data.Container.WavContainer;
import java.io.IOException;

public class WavParser extends Parser {

    private final static int BUFFER_SIZE = 4096;
    private final static int FMT_CHUNK_ID = 0x20746D66;
    private final static int DATA_CHUNK_ID = 0x61746164;
    private final static int RIFF_CHUNK_ID = 0x46464952;
    private final static int RIFF_TYPE_ID = 0x45564157;

    private WavContainer ParsingContainer;
    private byte[] buffer;
    private byte[] DataBuffer;

    public WavParser(Container container) {
        super(container);
        ParsingContainer = (WavContainer) container;
    }

    @Override
    public void parse() throws IOException, WavFileException {

        int bytesRead = 0;
        bytesRead = ParsingContainer.getIStream().read(buffer, 0, 12);
        if (bytesRead != 12) throw new WavFileException("Not enough wav file bytes for header");

        long riffChunkID = getLE(buffer, 0, 4);
        long chunkSize = getLE(buffer, 4, 4);
        long riffTypeID = getLE(buffer, 8, 4);

        if (riffChunkID != RIFF_CHUNK_ID)
            throw new WavFileException("Invalid Wav Header data, incorrect riff chunk ID");
        if (riffTypeID != RIFF_TYPE_ID)
            throw new WavFileException("Invalid Wav Header data, incorrect riff type ID");

        boolean foundFormat = false;
        boolean foundData = false;

        while (true) {
            bytesRead = ParsingContainer.getIStream().read(buffer, 0, 8);
            if (bytesRead == -1)
                throw new WavFileException("Reached end of file without finding format chunk");
            if (bytesRead != 8) throw new WavFileException("Could not read chunk header");


            long chunkID = getLE(buffer, 0, 4);
            chunkSize = getLE(buffer, 4, 4);

            long numChunkBytes = (chunkSize % 2 == 1) ? chunkSize + 1 : chunkSize;
            if (chunkID == FMT_CHUNK_ID) {
                foundFormat = true;


                bytesRead = ParsingContainer.getIStream().read(buffer, 0, 16);
                ParsingContainer.setCompressionCode(getLE(buffer, 0, 2));
                if (ParsingContainer.getCompressionCode() != 1)
                    throw new WavFileException("Compression Code " + ParsingContainer.getCompressionCode() + " not supported");
                ParsingContainer.setNumberOfChannels(getLE(buffer, 2, 2));
                ParsingContainer.setSampleRate(getLE(buffer, 4, 4));
                ParsingContainer.setByteRate(getLE(buffer, 8, 4));
                ParsingContainer.setBlockAlign(getLE(buffer, 12, 2));
                ParsingContainer.setValidBits(getLE(buffer, 14, 2));
                // ParsingContainer.setBytePerSample(getLE(buffer,14,2));
                if (ParsingContainer.getNumberOfChannels() == 0)
                    throw new WavFileException("Number of channels specified in header is equal to zero");
                if (ParsingContainer.getBlockAlign() == 0)
                    throw new WavFileException("Block Align specified in header is equal to zero");
                if (ParsingContainer.getValidBits() < 2)
                    throw new WavFileException("Valid Bits specified in header is less than 2");
                if (ParsingContainer.getValidBits() > 64)
                    throw new WavFileException("Valid Bits specified in header is greater than 64, this is greater than a long can hold");

                ParsingContainer.setBytePerSample((ParsingContainer.getValidBits() + 7) / 8);

                if (ParsingContainer.getBytePerSample() * ParsingContainer.getNumberOfChannels() != ParsingContainer.getBlockAlign())
                    throw new WavFileException("Block Align does not agree with bytes required for validBits and number of channels");

                numChunkBytes -= 16;
                if (numChunkBytes > 0)
                    ParsingContainer.getIStream().skip(numChunkBytes);

            } else if (chunkID == DATA_CHUNK_ID) {
                if (foundFormat == false)
                    throw new WavFileException("Data chunk found before Format chunk");

                if (chunkSize % ParsingContainer.getBlockAlign() != 0)
                    throw new WavFileException("Data Chunk size is not multiple of Block Align");

                ParsingContainer.setNumberOfFrames(chunkSize / (ParsingContainer.getBlockAlign()));
                foundData = true;

                break;
            } else {
                ParsingContainer.getIStream().skip(numChunkBytes);
            }
        }
        if (ParsingContainer.getValidBits() > 8) {
            ParsingContainer.setFloatOffset(0);
            ParsingContainer.setFloatScale(1 << (ParsingContainer.getValidBits() - 1));
        } else {
            ParsingContainer.setFloatOffset(-1);
            ParsingContainer.setFloatScale((long) (0.5 * ((1 << ParsingContainer.getValidBits()) - 1)));
        }

        bytesRead = ParsingContainer.getIStream().read(DataBuffer, 0, (int) chunkSize);
        ParsingContainer.setAudioStream(DataBuffer);


    }

    private static long getLE(byte[] buffer, int pos, int numBytes)
    {
        numBytes --;
        pos += numBytes;

        long val = buffer[pos] & 0xFF;
        for (int b=0 ; b<numBytes ; b++)
            val = (val << 8) + (buffer[--pos] & 0xFF);

        return val;
    }


}
