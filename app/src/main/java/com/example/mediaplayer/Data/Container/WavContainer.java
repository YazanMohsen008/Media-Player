package com.example.mediaplayer.Data.Container;

import com.example.mediaplayer.Data.Frame.Frame;

import java.io.InputStream;

public class WavContainer extends Container  {
    byte[] AudioStream;
    long NumberOfFrames;
    long NumberOfChannels;
    long SampleRate;
    long BlockAlign;
    long ValidBits;
    long CompressionCode;
    long ChunkID;
    long ChunkSize;
    long BytePerSample;
    long FloatOffset;
    long FloatScale;
    long ByteRate;
    InputStream IStream;

    public WavContainer( InputStream iStream,
                         long floatOffset, long floatScale,
                         long byteRate,byte[] audioStream, long numberOfFrames,
                         long numberOfChannels, long sampleRate, long blockAlign, long validBits,
                         long compressionCode, long chunkID, long chunkSize,
                         long bytePerSample) {

        super(iStream);
        AudioStream = audioStream;
        NumberOfFrames = numberOfFrames;
        NumberOfChannels = numberOfChannels;
        SampleRate = sampleRate;
        BlockAlign = blockAlign;
        ValidBits = validBits;
        CompressionCode = compressionCode;
        ChunkID = chunkID;
        ChunkSize = chunkSize;
        BytePerSample = bytePerSample;
        FloatOffset = floatOffset;
        FloatScale = floatScale;
        ByteRate = byteRate;
        IStream = iStream;

    }

    public WavContainer(InputStream in) {
        super(in);
    }

    public InputStream getIStream() {
        return IStream;
    }

    public void setIStream(InputStream IStream) {
        this.IStream = IStream;
    }

    public long getFloatOffset() {
        return FloatOffset;
    }

    public void setFloatOffset(long floatOffset) {
        FloatOffset = floatOffset;
    }

    public long getFloatScale() {
        return FloatScale;
    }

    public void setFloatScale(long floatScale) {
        FloatScale = floatScale;
    }

    public long getByteRate() {
        return ByteRate;
    }

    public void setByteRate(long byteRate) {
        ByteRate = byteRate;
    }

    public byte[] getAudioStream() {
        return AudioStream;
    }

    public void setAudioStream(byte[] audioStream) {
        AudioStream = audioStream;
    }

    public Long getNumberOfFrames() {
        return NumberOfFrames;
    }

    public void setNumberOfFrames(Long numberOfFrames) {
        NumberOfFrames = numberOfFrames;
    }

    public Long getNumberOfChannels() {
        return NumberOfChannels;
    }

    public void setNumberOfChannels(Long numberOfChannels) {
        NumberOfChannels = numberOfChannels;
    }

    public Long getSampleRate() {
        return SampleRate;
    }

    public void setSampleRate(Long sampleRate) {
        SampleRate = sampleRate;
    }

    public Long getBlockAlign() {
        return BlockAlign;
    }

    public void setBlockAlign(Long blockAlign) {
        BlockAlign = blockAlign;
    }

    public Long getValidBits() {
        return ValidBits;
    }

    public void setValidBits(Long validBits) {
        ValidBits = validBits;
    }

    public Long getCompressionCode() {
        return CompressionCode;
    }

    public void setCompressionCode(Long compressionCode) {
        CompressionCode = compressionCode;
    }

    public Long getChunkID() {
        return ChunkID;
    }

    public void setChunkID(Long chunkID) {
        ChunkID = chunkID;
    }

    public Long getChunkSize() {
        return ChunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        ChunkSize = chunkSize;
    }

    public Long getBytePerSample() {
        return BytePerSample;
    }

    public void setBytePerSample(Long bytePerSample) {
        BytePerSample = bytePerSample;
    }
}
