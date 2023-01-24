package com.example.mediaplayer.Data.Frame.mp3Frame;

import com.example.mediaplayer.Data.Frame.Frame;

import java.io.InputStream;

public class Mp3Data extends Frame {

    public Buffer buffer;
    private MainDataReader mainDataReader;
    private float[] store;
    private float[] vector;
    private int stereo =-1;
    private static byte[] samplesBuffer;
    private Mp3FrameHeader mp3Header;
    public Mp3FrameSideInfo mp3SideInfo;
    private byte[] mainData;

    public Mp3Data(InputStream in) {
        buffer = new Buffer(in);
        this.mp3Header = new Mp3FrameHeader();
    }
   public static final class MainDataReader {
        public final byte[] mainData;
        public int top = 0;
        public int index = 0;
        public int current = 0;

        public MainDataReader(byte[] mainData) {
            this.mainData = mainData;
        }

    }
    public static final class Buffer {
        public final InputStream in;
        public int current = 0;
        public int lastByte = -1;

        public Buffer(InputStream inputStream) {
            in = inputStream;
        }

    }


    public void initMainData(int size){
        mainData = new byte[size];
    }
    public void initStore (int size){
        store = new float[size];
    }
    public void initV(int size){
        vector = new float[size];
    }
    public void initMainDataReader(){
        mainDataReader = new MainDataReader(mainData);
    }
    public void initSampleBuffer(int size){
        samplesBuffer = new byte[size];
    }








    public MainDataReader getMainDataReader() {
        return mainDataReader;
    }


    public float[] getStore() {
        return store;
    }

    public void setStore(float[] store) {
        this.store = store;
    }

    public float[] getVector() {
        return vector;
    }

    public void setVector(float[] vector) {
        this.vector = vector;
    }

    public byte[] getSamplesBuffer() {
        return samplesBuffer;
    }

    public void setSamplesBuffer(byte[] samplesBuffer) {
        Mp3Data.samplesBuffer = samplesBuffer;
    }

    public void setMainData(byte[] mainData) {
        this.mainData = mainData;
    }

    public int getStereo() {
        return stereo;
    }

    public void setStereo(int stereo) {
        this.stereo = stereo;
        mp3SideInfo = new Mp3FrameSideInfo(stereo ==1?2:1);
    }









    public Mp3FrameHeader getMp3Header() {
        return mp3Header;
    }


}
