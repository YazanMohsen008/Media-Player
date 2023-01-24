package com.example.mediaplayer.MediaControl;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mediaplayer.MainActivity;
import com.example.mediaplayer.R;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class AudioPlayerFragment extends Fragment implements Runnable{
    private ImageButton nextButton;
    private ImageButton playButton;
    private ImageButton previousButton;
    private ImageButton shuffleButton;
    private ImageButton repeatButton;
    private SeekBar timeProgress;
    private TextView fullDuration;
    private TextView currentDuration;
    private TextView songName;
    private Thread timeThread;
    private AtomicInteger resumeProgress;

    public AudioPlayerFragment(){}

    public static AudioPlayerFragment newInstance(){return new AudioPlayerFragment();}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.audio_control_fragment, container, false);

        nextButton = view.findViewById(R.id.next);
        playButton = view.findViewById(R.id.play);
        previousButton = view.findViewById(R.id.previous);
        shuffleButton = view.findViewById(R.id.shuffle);
        repeatButton = view.findViewById(R.id.repeat);
        timeProgress = view.findViewById(R.id.time_seek_bar);
        fullDuration = view.findViewById(R.id.tv_full_time);
        currentDuration = view.findViewById(R.id.tv_current_time);
        songName = view.findViewById(R.id.tv_song);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        nextButton.setOnClickListener(v -> {
            MainActivity.playNextSong(getContext());

        });
        playButton.setOnClickListener(v -> {

            MainActivity.playSong(getContext());
        });

        previousButton.setOnClickListener(v -> {
            MainActivity.playPreviousSong(getContext());

        });

        shuffleButton.setOnClickListener(v -> {
        });

        repeatButton.setOnClickListener(v -> {
        });

        songName.setText(MainActivity.getCurrentFileName().replace(".mp3",""));

        resumeProgress = new AtomicInteger();

       String time = getTime(Long.parseLong(MainActivity.getCurrentFileDuration()));
        fullDuration.setText(time);

        timeProgress.setEnabled(true);
        timeProgress.setMax(Integer.parseInt(MainActivity.getCurrentFileDuration()));
        timeThread = new Thread(this);
        timeThread.start();
    }
    public void initNewPlayer(){
        timeThread.interrupt();
        timeProgress.setMax(Integer.parseInt(MainActivity.getCurrentFileDuration()));
        timeProgress.setProgress(0);

        resumeProgress.set(0);

        songName.setText(MainActivity.getCurrentFileName().replace(".mp3",""));

        String time = getTime(Long.parseLong(MainActivity.getCurrentFileDuration()));
        fullDuration.setText(time);
        currentDuration.setText("0:00");
        timeThread = new Thread(this);
        timeThread.start();
    }
    public void changPlayButton(boolean playing){
        if(playing){
            playButton.setImageResource(R.drawable.ic_play);
        timeThread.interrupt();}
        else {playButton.setImageResource(R.drawable.pause);
            timeThread = new Thread(this);
            timeThread.start();}
    }
    @Override
    public void onStop() {
        super.onStop();
        timeThread.interrupt();
        timeThread = null;
    }

    @Override
    public void run() {
        AtomicInteger currentDur = new AtomicInteger();
        if(resumeProgress.get() != 0) {
            currentDur.set(resumeProgress.get());
            timeProgress.setProgress(currentDur.get() * 1000);
            String time = getTime(currentDur.longValue()*1000);
            currentDuration.setText(time);
        }
        int fullDur = Integer.parseInt(MainActivity.getCurrentFileDuration());

        try {
            while (currentDur.get()*1000 <= fullDur && !Thread.currentThread().isInterrupted()){
                getActivity().runOnUiThread(() -> {

                    timeProgress.setProgress(currentDur.getAndIncrement() * 1000);
                    resumeProgress.set(currentDur.get());
                     String time = getTime(currentDur.longValue()*1000);
                    currentDuration.setText(time);
                });
                Thread.sleep(1000);
            }
        } catch (InterruptedException ignored) {
        }
    }
    @SuppressLint("DefaultLocale")
    public String getTime(Long time){
        int second = (int) (TimeUnit.MILLISECONDS.toSeconds(time) % 60);

        return String.format("%d:%s",
                TimeUnit.MILLISECONDS.toMinutes(time),
                second < 10?"0" + second:second
                );
    }

}
