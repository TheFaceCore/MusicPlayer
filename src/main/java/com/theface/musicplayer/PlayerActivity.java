package com.theface.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.theface.musicplayer.dialogs.PlayerErrorDialogFragment;
import com.theface.musicplayer.info.InfoStorage;
import com.theface.musicplayer.info.Mp3NameResolver;
import com.theface.musicplayer.info.TrackInfo;
import com.theface.musicplayer.info.TrackState;
import com.theface.musicplayer.services.DownloadService;
import com.theface.musicplayer.services.PlayerService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PlayerActivity extends FragmentActivity implements AdapterView.OnItemClickListener,
                                                        PlayerErrorDialogFragment.Listener,
                                                        PlayerServiceListener{
    private static final String LOG_TAG = "Player";

    private static final String KEY_DIALOG_IN_FOREGROUND = "dialog_foreground";
    private static final String KEY_DOWNLOADING_TRACK_URI = "downloading_track_uri";
    private static final String KEY_TRACK_INFOS = "track_infos";
    private static final String KEY_TRACKS_TO_DOWNLOAD = "track_to_download";

    private ArrayList<String> trackUrisToDownload = new ArrayList<>();
    private ArrayList<TrackInfo> trackInfos;
    private Map<String, TrackInfo> trackIndex;
    private Map<String, String> downloadedTracksInfo;
    private TrackInfo downloadingTrackInfo;
    private TrackInfo playingTrackInfo;

    private ListView listView;
    private PlayerListAdapter trackListAdapter;

    private boolean errorDialogInForeground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        listView = (ListView) findViewById(R.id.playlist);

        downloadedTracksInfo = InfoStorage.getDownloadedTracksInfo(this);

        String downloadingTrackUri;
        if (savedInstanceState != null) {
            trackUrisToDownload = savedInstanceState.getStringArrayList(KEY_TRACKS_TO_DOWNLOAD);
            trackInfos = savedInstanceState.getParcelableArrayList(KEY_TRACK_INFOS);
            downloadingTrackUri = savedInstanceState.getString(KEY_DOWNLOADING_TRACK_URI);
            playingTrackInfo = findPlayingTrack();
        } else {
            PlayerApp app = PlayerApp.inst();
            trackUrisToDownload = new ArrayList<>(app.getPlaylist());
            populateTrackInfos();
            filterDownloadedTracks();
            downloadingTrackUri = app.getDownloadUri();
        }
        indexTracks();
        if (downloadingTrackUri != null) {
            downloadingTrackInfo = getTrackByUri(downloadingTrackUri);
        }

        initListView();
    }

    private void populateTrackInfos() {
        trackInfos = new ArrayList<>();
        for (String trackUri : trackUrisToDownload) {
            TrackInfo info;
            if (downloadedTracksInfo.containsKey(trackUri)) {
                String trackFilename = downloadedTracksInfo.get(trackUri);
                String trackName = resolveTrackName(trackFilename);
                info = new TrackInfo(trackUri, trackFilename, trackName);
            } else {
                info = new TrackInfo(trackUri, false);
            }
            trackInfos.add(info);
        }
    }

    private void indexTracks() {
        trackIndex = new HashMap<>();
        for (TrackInfo trackInfo : trackInfos) {
            trackIndex.put(trackInfo.getUri(), trackInfo);
        }
    }

    private void initListView() {
        trackListAdapter = new PlayerListAdapter(this, R.layout.row_track, trackInfos);
        listView.setAdapter(trackListAdapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayerApp app = PlayerApp.inst();
        app.setPlayerListener(this);
        restoreState(app);
    }

    private void restoreState(PlayerApp app) {
        //Check if playing error occur after onPause
        String errorTrackFilename = app.getErrorTrackFilenameAndClear();
        if (errorTrackFilename != null) {
            handleTrackPlayingError();
        }

        if (errorDialogInForeground) {
            return;
        }

        if (downloadingTrackInfo == null) {
            downloadNextTrack();
            return;
        }

        switch (app.getDownloadState()) {
            case IN_PROGRESS:
                if (TrackState.LOADING != downloadingTrackInfo.getState()) {
                    downloadingTrackInfo.loading();
                }
                app.restoreReceiver(new TrackReceiver(new Handler()));
                break;
            case COMPLETED:
                trackDownloaded(app.getDownloadedFilename());
                break;
            case ERROR:
                showDownloadErrorDialog();
                break;
        }
    }

    private void handleTrackPlayingError() {
        stopTrack(playingTrackInfo, false);
        showInfoDialog(R.string.track_playing_error);
    }

    private void showDownloadErrorDialog() {
        showErrorDialog(R.string.track_loading_error);
    }

    private void showErrorDialog(int errorMsgId) {
        DialogFragment dialog = PlayerErrorDialogFragment.newInstance(errorMsgId, false);
        dialog.show(getSupportFragmentManager(), "PlaylistLoaderErrorDialogFragment.Error");
        errorDialogInForeground = true;
    }

    private void showInfoDialog(int errorMsgId) {
        DialogFragment dialog = PlayerErrorDialogFragment.newInstance(errorMsgId, true);
        dialog.show(getSupportFragmentManager(), "PlaylistLoaderErrorDialogFragment.Info");
    }

    @Override
    public void onReloadClick() {
        downloadingTrackInfo.progress(0);
        downloadTrack(downloadingTrackInfo.getUri());
        updateListView();
    }

    @Override
    public void onError(String trackFilename) {
        handleTrackPlayingError();
    }

    /*
        Remove track only from this session,
        so if the user finish this Activity and come back later -
        he will be able to download this track again.
        In case of invalid track format - track deletion caches at Application level.
    */
    @Override
    public void onSkipClick() {
        trackInfos.remove(downloadingTrackInfo);
        trackUrisToDownload.remove(downloadingTrackInfo.getUri());
        downloadingTrackInfo = null;
        downloadNextTrack();
        updateListView();
    }

    @Override
    public void onBackButtonClick(DialogFragment dialog) {
        dialog.dismiss();
        onReloadClick();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayerApp app = PlayerApp.inst();
        app.receiverVanished();
        app.setPlayerListener(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (downloadingTrackInfo != null) {
            outState.putString(KEY_DOWNLOADING_TRACK_URI, downloadingTrackInfo.getUri());
        }
        outState.putBoolean(KEY_DIALOG_IN_FOREGROUND, errorDialogInForeground);
        outState.putParcelableArrayList(KEY_TRACK_INFOS, trackInfos);
        outState.putStringArrayList(KEY_TRACKS_TO_DOWNLOAD, trackUrisToDownload);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            stopService(new Intent(this, PlayerService.class));
        }
    }

    private static boolean isTrackLoaded(TrackInfo trackInfo) {
        return TrackState.LOADED == trackInfo.getState();
    }

    private void downloadTrack(String trackUri) {
        ResultReceiver resultReceiver = PlayerApp.inst().wrapReceiver(new TrackReceiver(new Handler()), trackUri);
        Intent intent = new Intent(this, DownloadService.class);
        Bundle downloadInfo = DownloadService.createDownloadInfo(trackUri, resultReceiver);
        intent.putExtras(downloadInfo);
        startService(intent);
    }

    private void stopTrack(TrackInfo trackInfo, boolean sendIntent) {
        trackInfo.stopped();
        playingTrackInfo = null;
        if (sendIntent) {
            startService(new Intent(this, PlayerService.class));
        }
        updateListView();
    }

    private void playTrack(TrackInfo trackInfo) {
        Intent intent = new Intent(this, PlayerService.class);
        intent.putExtras(PlayerService.createPlayInfo(trackInfo.getFilename()));
        startService(intent);

        if (playingTrackInfo != null) {
            playingTrackInfo.stopped();
        }
        playingTrackInfo = trackInfo;
        playingTrackInfo.playing();
        updateListView();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListView l = (ListView) parent;
        TrackInfo trackInfo = (TrackInfo) l.getItemAtPosition(position);
        if (!isTrackLoaded(trackInfo)) {
            return;
        }
        if (trackInfo.isPlaying()) {
            stopTrack(trackInfo, true);
        } else {
            playTrack(trackInfo);
        }
    }

    private class TrackReceiver extends ResultReceiver {
        public TrackReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch (resultCode) {
                case DownloadService.ERROR:
                    Log.i(LOG_TAG, resultData.getString(DownloadService.KEY_ERROR_MSG));
                    showDownloadErrorDialog();
                    break;
                case DownloadService.UPDATE_PROGRESS:
                    downloadingTrackInfo.progress(resultData.getInt(DownloadService.KEY_PROGRESS));
                    updateListView();
                    break;
                case DownloadService.COMPLETE:
                    String trackFilename = resultData.getString(DownloadService.KEY_TARGET_FILENAME);
                    trackDownloaded(trackFilename);
                    break;
            }
        }
    }

    private void updateListView() {
        trackListAdapter.notifyDataSetChanged();
    }

    private void trackDownloaded(String trackFilename) {
        String trackName = resolveTrackName(trackFilename);
        PlayerApp app = PlayerApp.inst();
        if (trackName != null) {
            downloadingTrackInfo.loaded(trackFilename, trackName);
            downloadedTracksInfo.put(downloadingTrackInfo.getUri(), trackFilename);
            InfoStorage.saveDownloadedTrackInfo(downloadingTrackInfo.getUri(), trackFilename, this);
        } else {
            deleteFile(trackFilename);
            trackInfos.remove(downloadingTrackInfo);
            app.getPlaylist().remove(downloadingTrackInfo.getUri());
            showInfoDialog(R.string.invalid_track);
        }
        app.clearDownloadUri();

        trackUrisToDownload.remove(downloadingTrackInfo.getUri());
        downloadingTrackInfo = null;
        downloadNextTrack();
        updateListView();
    }

    private TrackInfo getTrackByUri(String trackUri) {
        TrackInfo track = trackIndex.get(trackUri);
        if (track == null)  {
            throw new RuntimeException("Something went wrong.");
        }
        return track;
    }

    private String resolveTrackName(String trackFilename) {
        File trackFile = new File(getFilesDir(), trackFilename);
        return Mp3NameResolver.resolveName(trackFile);
    }

    private void downloadNextTrack() {
        if (trackUrisToDownload.isEmpty()) {
            return;
        }

        String trackUri = trackUrisToDownload.get(0);
        downloadingTrackInfo = getTrackByUri(trackUri);
        downloadingTrackInfo.loading();
        downloadTrack(trackUri);
    }

    private void filterDownloadedTracks() {
        Iterator<String> urisIterator = trackUrisToDownload.iterator();
        while (urisIterator.hasNext()) {
            String trackUri = urisIterator.next();
            if (downloadedTracksInfo.containsKey(trackUri)) {
                urisIterator.remove();
            }
        }
    }

    private TrackInfo findPlayingTrack() {
        for (TrackInfo trackInfo : trackInfos) {
            if (trackInfo.isPlaying()) {
                return trackInfo;
            }
        }
        return null;
    }
}