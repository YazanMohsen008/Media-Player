package com.example.mediaplayer.Data.Container.mp4;

import java.util.List;

public class Stsz {
    private int mSampleSize;
    private int mSampleCount;
    private List<Integer> mSamplesSize;

    public Stsz(int mSampleSize, int mSampleCount, List<Integer> mSamplesSize) {
        this.mSampleSize = mSampleSize;
        this.mSampleCount = mSampleCount;
        this.mSamplesSize = mSamplesSize;
        if (mSampleSize != 0) {
            for (int i = 0; i < mSampleCount; i ++) {
                mSamplesSize.add(mSampleSize);
            }
        }
    }

    public int getmSampleSize() {
        return mSampleSize;
    }

    public int getmSampleCount() {
        return mSampleCount;
    }

    public int getmSamplesSize(int i) {
        return mSamplesSize.get(i);
    }

}
