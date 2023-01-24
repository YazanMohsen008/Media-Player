package com.example.mediaplayer.Data.Container.mp4;

import java.util.List;

public class Stco {

    private int mEntryCount;
    private List<Integer> mEntries;

    public Stco(int entryCount, List<Integer> list) {
        this.mEntryCount = entryCount;
        this.mEntries = list;
    }

    public int getChunkOffset(int i) {
        return mEntries.get(i);
    }

    public int getEntriesCount() {
        return mEntryCount;
    }
}
