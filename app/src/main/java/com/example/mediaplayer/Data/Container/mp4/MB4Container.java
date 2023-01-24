package com.example.mediaplayer.Data.Container.mp4;

import com.example.mediaplayer.Data.Container.Container;
import com.example.mediaplayer.Data.Container.MB3Container;

import java.io.InputStream;
import java.util.ArrayList;

public class MB4Container extends Container {

    ArrayList<Trak> mTraks;

    public MB4Container(InputStream in) {
        super(in);
    }

    public ArrayList<Trak> getTraks (){
        return mTraks;
    }

}
