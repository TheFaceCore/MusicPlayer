package com.theface.musicplayer.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import com.theface.musicplayer.PlayerApp;

import java.io.File;
import java.io.IOException;

/**
 * Created by TheFace Date: 26.03.2015 Time: 12:40
 */
public class PlayerService extends Service{
    private static final String PARAM_TRACK_FILENAME = "track_filename";

    private MediaPlayer mp;

    private String currentTrackFilename;

    public static Bundle createPlayInfo(String trackFilename) {
        Bundle info = new Bundle();
        info.putString(PARAM_TRACK_FILENAME, trackFilename);
        return info;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getExtras() != null) {
            String trackFilename = intent.getStringExtra(PARAM_TRACK_FILENAME);
            playTrack(trackFilename);
        } else {
            stopTrack();
        }
        return START_NOT_STICKY;
    }

    private void stopTrack() {
        mp.reset();
    }

    private void playTrack(String trackFilename) {
        File fileStreamPath = getFileStreamPath(trackFilename);
        Uri trackUri = Uri.fromFile(fileStreamPath);
        if (mp == null) {
            mp = MediaPlayer.create(this, trackUri);
            mp.setLooping(true);
        } else {
            mp.reset();
            try {
                mp.setDataSource(this, Uri.fromFile(fileStreamPath));
                mp.prepare();
            } catch (IOException e) {
                mp = null;
            }
        }
        if (mp == null) {
            PlayerApp.inst().trackPlayingError(currentTrackFilename);
            currentTrackFilename = null;
            return;
        }
        currentTrackFilename = trackFilename;
        mp.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mp != null) {
            mp.stop();
            mp.release();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
