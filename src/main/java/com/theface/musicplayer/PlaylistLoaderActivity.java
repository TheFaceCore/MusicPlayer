package com.theface.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.theface.musicplayer.dialogs.PlaylistLoaderErrorDialogFragment;
import com.theface.musicplayer.services.DownloadService;
import com.theface.musicplayer.services.DownloadState;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by TheFace Date: 20.03.2015 Time: 0:56
 */
public class PlaylistLoaderActivity extends FragmentActivity implements PlaylistLoaderErrorDialogFragment.Listener {
    private static final String LOG_TAG = "Player";
    private static final String PLAYLIST_URI = "https://www.dropbox.com/s/5r7he1k9rvzaex4/musicList.txt?dl=1";
    private static final String KEY_DIALOG_IN_FOREGROUND = "dialog_foreground";

    public static final String PLAYLIST_FILE_NAME = "playlist.txt";

    private boolean dialogInForeground;
    private List<String> playlist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_loader);

        playlist = PlayerApp.inst().getPlaylist();
        if (playlist != null) {
            return;
        }

        initLoadingAnim();

        if (savedInstanceState != null) {
            dialogInForeground = savedInstanceState.getBoolean(KEY_DIALOG_IN_FOREGROUND);
            return;
        }
        downloadPlaylist();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playlist != null) {
            startPlayer(playlist);
        } else {
            restoreState();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayerApp.inst().receiverVanished();
    }

    @Override
    public void onReloadClick() {
        downloadPlaylist();
        dialogInForeground = false;
    }

    @Override
    public void onCancelClick() {
        finish();
    }

    @Override
    public void onFinishingClick() {
        finish();
    }

    @Override
    public void onBackButtonClick(DialogFragment dialog) {
        dialog.dismiss();
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_DIALOG_IN_FOREGROUND, dialogInForeground);
    }

    private void downloadPlaylist() {
        ResultReceiver receiver = PlayerApp.inst().wrapReceiver(new PlaylistReceiver(new Handler()), null);
        Intent intent = new Intent(this, DownloadService.class);
        Bundle downloadInfo = DownloadService.createDownloadInfo(PLAYLIST_URI,
                PLAYLIST_FILE_NAME, receiver);
        intent.putExtras(downloadInfo);
        startService(intent);
    }

    private void restoreState() {
        if (dialogInForeground) {
            return;
        }
        PlayerApp app = PlayerApp.inst();
        DownloadState state = app.getDownloadState();
        switch (state) {
            case IN_PROGRESS:
                app.restoreReceiver(new PlaylistReceiver(new Handler()));
                break;
            case COMPLETED:
                playlistDownloaded();
                break;
            //Error occurred not in onResume state, let's show dialog
            case ERROR:
                showDownloadErrorDialog();
                break;
        }
    }

    private void initLoadingAnim() {
        Animation animRotation = AnimationUtils.loadAnimation(this, R.anim.rotation);
        View loaderIndicator = findViewById(R.id.loader_indicator);
        loaderIndicator.setDrawingCacheEnabled(true);
        loaderIndicator.startAnimation(animRotation);
    }

    private class PlaylistReceiver extends ResultReceiver {
        public PlaylistReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch (resultCode) {
                case DownloadService.ERROR:
                    showDownloadErrorDialog();
                    break;
                case DownloadService.COMPLETE:
                    playlistDownloaded();
                    break;
            }
        }
    }

    private void showDownloadErrorDialog() {
        showErrorDialog(R.string.playlist_loading_error, false);
    }

    private void showErrorDialog(int errorMsgId, boolean finishingMode) {
        DialogFragment dialog = PlaylistLoaderErrorDialogFragment.newInstance(errorMsgId, finishingMode);
        dialog.show(getSupportFragmentManager(), "PlaylistLoaderErrorDialogFragment");
        dialogInForeground = true;
    }

    private void playlistDownloaded() {
        List<String> playlist = readPlaylist();
        if (playlist == null) {
            showErrorDialog(R.string.playlist_parsing_error, false);
            return;
        }
        if (playlist.isEmpty()) {
            showErrorDialog(R.string.playlist_empty_error, true);
            return;
        }
        startPlayer(playlist);
    }

    private List<String> readPlaylist() {
        List<String> result = null;
        Set<String> filter = new HashSet<>();
        try {
            InputStream inputStream = openFileInput(PlaylistLoaderActivity.PLAYLIST_FILE_NAME);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    filter.add(receiveString);
                }
                inputStream.close();
            }
            result = new ArrayList<>(filter);
        }
        catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Can not read file: " + e.toString());
        }
        return result;
    }

    private void startPlayer(List<String> playlist) {
        PlayerApp.inst().setPlaylist(playlist);
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }
}
