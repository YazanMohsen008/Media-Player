package com.example.mediaplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayer.reader.MediaFile;

import java.io.File;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<MediaFile> mediaFiles;
    int mImageResourceId;
    private OnClickFileListener mOnClick;
    RecyclerViewAdapter(OnClickFileListener onClick,List<MediaFile> mediaFiles , int thumbnailImage){
        this.mediaFiles = mediaFiles;
        mImageResourceId = thumbnailImage;
        mOnClick = onClick;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.files_list,
                parent,false);
        return new FileLayoutHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((FileLayoutHolder)holder).mediaTitle.setText(mediaFiles.get(mediaFiles.size() - 1 -position).getName());
        ((FileLayoutHolder)holder).thumbnail.setImageResource(mImageResourceId);
    }

    @Override
    public int getItemCount() {
        return mediaFiles.size();
    }

    interface OnClickFileListener{
        public void onClick(MediaFile file);
    }
    class FileLayoutHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView thumbnail;
        TextView mediaTitle;
        ImageView ic_more_btn;

        public FileLayoutHolder(@NonNull View itemView) {
            super(itemView);

            thumbnail = itemView.findViewById(R.id.thumbnail);
            mediaTitle = itemView.findViewById(R.id.media_title_text_view);
            ic_more_btn = itemView.findViewById(R.id.ic_more_btn);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mOnClick.onClick(mediaFiles.get(mediaFiles.size() -1 - getAdapterPosition()));
        }
    }

}
