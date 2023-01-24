package com.example.mediaplayer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayer.reader.MediaFile;

import java.io.File;
import java.util.ArrayList;

import static com.example.mediaplayer.MainActivity.stReader;


public class PageFragment extends Fragment {
    private static final String FRAGMENT_INDEX = "index";

    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private ArrayList<MediaFile> mMediaFiles;
    private RecyclerViewAdapter.OnClickFileListener mOnClick;
    public static PageFragment newInstance(int index) {
        PageFragment fragment = new PageFragment();
        Bundle args = new Bundle();
        args.putSerializable(FRAGMENT_INDEX, index);
        fragment.setArguments(args);

        return fragment;
    }
    public void setOnClickListener(RecyclerViewAdapter.OnClickFileListener onClick){
        mOnClick = onClick;
    }
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_page_view, container, false);

    }


    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        assert getArguments() != null;
        int pageNumber = getArguments().getInt(FRAGMENT_INDEX);
        if (pageNumber == 0) {

            mMediaFiles = stReader.getAudioFies();
            mRecyclerViewAdapter = new RecyclerViewAdapter(mOnClick , mMediaFiles, R.drawable.ic_music);

        }
        if (pageNumber == 1) {

            mMediaFiles = stReader.getVideoFies();
            mRecyclerViewAdapter = new RecyclerViewAdapter(mOnClick , mMediaFiles, R.drawable.ic_video);
        }
        else {
            mMediaFiles = stReader.getAudioFies();
        }

        mRecyclerView = view.findViewById(R.id.recyclerView);

        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(mRecyclerView.getContext()
                , R.anim.layout_animation_waterfall);

        mRecyclerView.setLayoutAnimation(animation);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //to avoid lack in scrolling
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(20);
        mRecyclerView.setDrawingCacheEnabled(true);
        mRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        mRecyclerView.setNestedScrollingEnabled(false);

        mRecyclerView.setAdapter(mRecyclerViewAdapter);

    }

}
