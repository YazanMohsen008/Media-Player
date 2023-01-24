package com.example.mediaplayer.Data;

import com.example.mediaplayer.Data.Container.Container;

import java.util.List;

public class Data {
    List<PlayList> PlayLists;

    public Data(List<PlayList> playLists) {
        PlayLists = playLists;
    }

    public void AddToPlayList(int playListIndex, Container container) {
        PlayLists.get(playListIndex).AddContainer(container);
    }

    public void AddCustomerPlayList(String PlayListName) {
        PlayList customerLst = new PlayList(PlayListName);
        PlayLists.add(customerLst);
    }

    public Container GetContainer(int playListIndex, int containerIndex) {
 return PlayLists.get(playListIndex).getContainer(containerIndex);
    }

    public void RemoveContainer(int PlayListIndex, Container container) {
        PlayLists.get(PlayListIndex).RemoveContainer(container);
    }

    public void RemovePlayList(int PlayListIndex) {
        PlayLists.remove(PlayListIndex);
    }
}