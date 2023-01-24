package com.example.mediaplayer.Data;

import com.example.mediaplayer.Data.Container.Container;

import java.util.List;

public class PlayList {
    String Name;
    List<Container> Containers;

    public PlayList(String Name) {
        this.Name=Name;
    }
    public void AddContainer(Container Container )
    {
        Containers.add(Container);
    }
    public void EditName(String Name)
    {
        this.Name=Name;
    }
    public void RemoveContainer(Container Container)
    {
        Containers.remove(Container);
    }

    public Container getContainer(int Containerindex)
    {
       return Containers.get(Containerindex);
    }
}
