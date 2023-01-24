package com.example.mediaplayer.MediaControl;

/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.example.mediaplayer.ContainerManager.Decoder.mp3Decoder.Mp3AudioTrack;

import java.io.IOException;
import java.io.InputStream;

public class PlaybackAudio implements MediaController{
    static final int SAMPLE_RATE = 44100;
    public static boolean mShouldContinue;
    private PlaybackListener mListener;
    AudioTrack audioTrack;
    Mp3AudioTrack sound;
    InputStream in;
    Thread decoderThread;
    public PlaybackAudio(PlaybackListener listener) {
        mListener = listener;
    }

    public boolean playing() {
        return mShouldContinue;
    }

    public void startPlayback(InputStream in ) {
        if (in == null || decoderThread != null)
            return;
        this.in = in;

        // Start streaming in a thread
        mShouldContinue = true;

        sound = new Mp3AudioTrack(in);
        play();

    }
    @Override
    public void stop() {
        if (decoderThread == null)
            return;
        mShouldContinue = false;
        audioTrack.stop();
        decoderThread.interrupt();
        decoderThread = null;
        sound = null;
        audioTrack.release();

    }
    @Override
    public void play() {

        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                sound.isStereo() ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO ,
                AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
            if(sound.isStereo())
                bufferSize = SAMPLE_RATE * 2*2;
            else bufferSize = SAMPLE_RATE * 2;
        }

        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                sound.isStereo() ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);

        audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onPeriodicNotification(AudioTrack track) {
                if (mListener != null && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    mListener.onProgress((track.getPlaybackHeadPosition() * 1000) / SAMPLE_RATE);
                }
            }

            @Override
            public void onMarkerReached(AudioTrack track) {
                track.release();
                if (mListener != null) {
                    mListener.onCompletion();
                }
            }
        });
        audioTrack.setPositionNotificationPeriod(SAMPLE_RATE / 30); // 30 times per second

        audioTrack.play();
        decoderThread = sound.decodeFullyInto(audioTrack);

    }

    @Override
    public void pause() {
        if (decoderThread == null)
            return;

        mShouldContinue = false;
        decoderThread.interrupt();
        decoderThread = null;
    }

    @Override
    public void seek() {

    }

    @Override
    public void reverse() {

    }

    @Override
    public void speed() {

    }

    @Override
    public void previous(InputStream in) {
        startPlayback(in);
    }

    @Override
    public void next(InputStream in) {
        startPlayback(in);
    }

    @Override
    public void resume() {

        mShouldContinue = true;
        decoderThread = sound.decodeFullyInto(audioTrack);

    }
}
