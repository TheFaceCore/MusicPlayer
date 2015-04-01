package com.theface.musicplayer.info;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by TheFace Date: 17.03.2015 Time: 14:26
 */
public class InfoStorage {
    //Metadata stored in shared preferences for sake of development speed and clarity
    private static final String PREFS_NAME = "PlayerPrefs";

    private static final String KEY_DOWNLOADED_TRACKS = "DownloadedTracks";

    public static Map<String, String> getDownloadedTracksInfo(Context context) {
        SharedPreferences prefs = getPreferences(context);
        String json = prefs.getString(KEY_DOWNLOADED_TRACKS, null);
        if (json == null) {
            return new HashMap<>();
        }
        return new Gson().fromJson(json, Map.class);
    }

    public static void saveDownloadedTrackInfo(String trackUri, String trackFilename, Context context) {
        Map<String, String> downloadedTracks = getDownloadedTracksInfo(context);
        downloadedTracks.put(trackUri, trackFilename);
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        String json = new Gson().toJson(downloadedTracks);
        editor.putString(KEY_DOWNLOADED_TRACKS, json);
        editor.apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
