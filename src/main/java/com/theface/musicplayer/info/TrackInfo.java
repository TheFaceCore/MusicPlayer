package com.theface.musicplayer.info;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by TheFace Date: 17.03.2015 Time: 17:30
 */
public class TrackInfo implements Parcelable{
    private TrackState state;
    private String uri;
    private String name;
    private String filename;
    private int progress;
    private boolean playing;

    public TrackInfo(String uri, boolean isLoading) {
        this.uri = uri;
        if (isLoading) {
            state = TrackState.LOADING;
        } else {
            state = TrackState.NOT_LOADED;
        }
    }

    public TrackInfo(String uri, String filename, String name) {
        this.uri = uri;
        this.name = name;
        this.filename = filename;
        state = TrackState.LOADED;
    }

    private TrackInfo(Parcel in) {
        state = TrackState.values()[in.readInt()];
        uri = in.readString();
        name = in.readString();
        filename = in.readString();
        progress = in.readInt();
        playing = in.readInt() != 0;
    }

    public void loading() {
        if (state != TrackState.NOT_LOADED) {
            throw new IllegalStateException("Something went wrong.");
        }
        state = TrackState.LOADING;
    }

    public void progress(int progress) {
        if (state != TrackState.LOADING) {
            throw new IllegalStateException("Something went wrong.");
        }
        this.progress = progress;
    }

    public void loaded(String filename, String name) {
        if (state == TrackState.LOADED) {
            throw new IllegalStateException("Something went wrong.");
        }
        this.name = name;
        this.filename = filename;
        state = TrackState.LOADED;
    }

    public void playing() {
        playing = true;
    }

    public void stopped() {
        playing = false;
    }

    public boolean isPlaying() {
        return playing;
    }

    public TrackState getState() {
        return state;
    }

    public String getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public int getProgress() {
        return progress;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(state.ordinal());
        dest.writeString(uri);
        dest.writeString(name);
        dest.writeString(filename);
        dest.writeInt(progress);
        dest.writeInt(playing ? 1 : 0);
    }

    public static final Creator<TrackInfo> CREATOR = new Creator<TrackInfo>() {
        @Override
        public TrackInfo createFromParcel(final Parcel source) {
            return new TrackInfo(source);
        }

        @Override
        public TrackInfo[] newArray(final int size) {
            return new TrackInfo[size];
        }
    };
}
