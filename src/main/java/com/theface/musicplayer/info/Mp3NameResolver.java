package com.theface.musicplayer.info;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.File;
import java.io.IOException;

/**
 * Created by TheFace Date: 31.03.2015 Time: 9:02
 */
public class Mp3NameResolver {

    /**
     * @return null, if any trouble will occur
     */
    public static String resolveName(File file) {
        Mp3File mp3File;
        try {
            mp3File = new Mp3File(file);
        } catch (IOException | UnsupportedTagException | InvalidDataException e) {
            return null;
        }

        if (mp3File.hasId3v1Tag()) {
            return composeName(mp3File.getId3v1Tag());
        }

        if (mp3File.hasId3v2Tag()) {
            return composeName(mp3File.getId3v2Tag());
        }

        return null;
    }

    private static String composeName(ID3v1 tag) {
        if (tag.getTrack() != null && !tag.getTrack().isEmpty()) {
            return tag.getTrack() + " - " + tag.getTitle();
        }
        return tag.getTitle();
    }
}
