package com.theface.musicplayer;

import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import com.theface.musicplayer.services.DownloadService;
import com.theface.musicplayer.services.DownloadState;

import java.util.List;

/**
 * Created by TheFace Date: 19.03.2015 Time: 11:00
 */
public class PlayerApp extends Application{
    private static PlayerApp inst;

    private PlayerServiceListener playerListener;

    private ResultReceiver resultReceiver;
    private ResultReceiver resultReceiverWrapper = new ResultReceiverWrapper(new Handler());
    private DownloadState downloadState;
    private String downloadUri;
    private String downloadedFilename;
    private String errorTrackFilename;

    private List<String> playlist;

    public static PlayerApp inst() {
        if (inst == null) {
            throw new IllegalStateException();
        }
        return inst;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (inst != null) {
            throw new IllegalStateException();
        }
        inst = this;
        resultReceiverWrapper = new ResultReceiverWrapper(new Handler());
    }

    public ResultReceiver wrapReceiver(ResultReceiver resultReceiver, String downloadUri) {
        downloadState = DownloadState.IN_PROGRESS;
        this.downloadUri = downloadUri;
        this.resultReceiver = resultReceiver;
        return resultReceiverWrapper;
    }

    public void receiverVanished() {
        resultReceiver = null;
    }

    public void restoreReceiver(ResultReceiver resultReceiver) {
        this.resultReceiver = resultReceiver;
    }

    public void trackPlayingError(String trackFilename) {
        if (playerListener != null) {
            playerListener.onError(trackFilename);
            return;
        }
        errorTrackFilename = trackFilename;
    }

    public void clearDownloadUri() {
        downloadUri = null;
    }

    public void setPlaylist(List<String> playlist) {
        this.playlist = playlist;
    }

    public String getErrorTrackFilenameAndClear() {
        String result = errorTrackFilename;
        errorTrackFilename = null;
        return result;
    }

    public void setPlayerListener(PlayerServiceListener playerListener) {
        this.playerListener = playerListener;
    }

    public List<String> getPlaylist() {
        return playlist;
    }

    public String getDownloadedFilename() {
        return downloadedFilename;
    }

    public String getDownloadUri() {
        return downloadUri;
    }

    public DownloadState getDownloadState() {
        return downloadState;
    }

    private class ResultReceiverWrapper extends ResultReceiver {

        public ResultReceiverWrapper(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultReceiver != null) {
                resultReceiver.send(resultCode, resultData);
                return;
            }
            switch (resultCode) {
                case DownloadService.COMPLETE:
                    downloadState = DownloadState.COMPLETED;
                    downloadedFilename = resultData.getString(DownloadService.KEY_TARGET_FILENAME);
                    break;
                case DownloadService.ERROR:
                    downloadState = DownloadState.ERROR;
                    break;
            }
        }
    }
}
